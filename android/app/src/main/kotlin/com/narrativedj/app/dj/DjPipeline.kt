package com.narrativedj.app.dj

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import android.webkit.WebView
import com.narrativedj.app.R
import com.narrativedj.app.byok.SecureKeyStore
import com.narrativedj.app.byok.llm.DjTransitionContext
import com.narrativedj.app.byok.llm.GeminiLlmClient
import com.narrativedj.app.byok.llm.LlmClient
import com.narrativedj.app.byok.llm.OpenAiLlmClient
import com.narrativedj.app.byok.tts.OpenAiTtsClient
import com.narrativedj.app.locale.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class DjPipeline(
    private val context: Context,
    private val keyStore: SecureKeyStore,
    private val webViewProvider: () -> WebView?,
    private val scope: CoroutineScope,
    private val languageProvider: () -> AppLanguage,
) {
    private var tts: TextToSpeech? = null
    private var segmentCompleteListener: (() -> Unit)? = null
    private var awaitingWebSpeechEnd = false

    fun bindTts(engine: TextToSpeech) {
        tts = engine
        applyTtsLocale(engine)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                if (utteranceId?.startsWith("narrativedj") == true) {
                    restoreDucking(DEFAULT_RAMP_OUT)
                    notifySegmentComplete()
                }
            }

            @Deprecated("Deprecated in API")
            override fun onError(utteranceId: String?) {
                restoreDucking(DEFAULT_RAMP_OUT)
                notifySegmentComplete()
            }
        })
    }

    fun setOnSegmentCompleteListener(listener: (() -> Unit)?) {
        segmentCompleteListener = listener
    }

    fun onBridgeEvent(event: String) {
        if (event == "speech_ended" && awaitingWebSpeechEnd) {
            awaitingWebSpeechEnd = false
            notifySegmentComplete()
        }
    }

    fun applyTtsLocale(engine: TextToSpeech) {
        val locale = when (languageProvider()) {
            AppLanguage.KOREAN -> Locale.KOREAN
            AppLanguage.ENGLISH -> Locale.US
        }
        engine.language = locale
    }

    fun isByokConfigured(): Boolean {
        return keyStore.hasApiKey(SecureKeyStore.Provider.GEMINI) ||
            keyStore.hasApiKey(SecureKeyStore.Provider.OPENAI)
    }

    fun runTransitionMent(
        transition: DjTransitionContext,
        onStatus: (String) -> Unit,
        onComplete: (() -> Unit)? = null,
    ) {
        scope.launch {
            try {
                onStatus(context.getString(R.string.dj_generating))
                val control = resolveTransitionControl(transition)
                onStatus(context.getString(R.string.dj_line_prefix, control.script))
                val previousListener = segmentCompleteListener
                setOnSegmentCompleteListener {
                    previousListener?.invoke()
                    onComplete?.invoke()
                    setOnSegmentCompleteListener(previousListener)
                }
                playSegment(control)
            } catch (e: Exception) {
                onStatus(context.getString(R.string.dj_error, e.message ?: "unknown"))
                onComplete?.invoke()
            }
        }
    }

    fun shutdown() {
        tts?.stop()
        segmentCompleteListener = null
    }

    private suspend fun playSegment(control: DjAudioControl) {
        val spoken = DjAudioControlParser.spokenText(control)
        val segment = control.copy(script = spoken)
        duck(segment.duckingVolume, segment.rampInSeconds)
        val openAiKey = keyStore.getApiKey(SecureKeyStore.Provider.OPENAI)
        if (!openAiKey.isNullOrBlank()) {
            try {
                val audio = OpenAiTtsClient(openAiKey).synthesize(spoken, "alloy")
                if (playOpenAiTtsViaWebAudio(audio, segment)) return
            } catch (_: Exception) {
                // fall through to Android TTS
            }
        }
        playAndroidTts(segment)
    }

    private suspend fun playOpenAiTtsViaWebAudio(audio: ByteArray, control: DjAudioControl): Boolean {
        val webView = webViewProvider() ?: return false
        return withContext(Dispatchers.Main) {
            awaitingWebSpeechEnd = true
            val b64 = Base64.encodeToString(audio, Base64.NO_WRAP)
            webView.evaluateJavascript(
                "NarrativeDJ.playSpeechBufferFromBase64('$b64', ${control.rampInSeconds}, ${control.rampOutSeconds});",
                null,
            )
            true
        }
    }

    private suspend fun playAndroidTts(control: DjAudioControl) {
        withContext(Dispatchers.Main) {
            applyTtsLocale(requireNotNull(tts))
            val utteranceId = "narrativedj-${System.currentTimeMillis()}"
            webViewProvider()?.evaluateJavascript(
                "NarrativeDJ.duckForSpeech(${estimateSpeechMs(control.script)}, ${control.duckingVolume});",
                null,
            )
            tts?.speak(control.script, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    private suspend fun duck(volume: Double, rampIn: Double) {
        withContext(Dispatchers.Main) {
            webViewProvider()?.evaluateJavascript(
                "NarrativeDJ.duck($volume, $rampIn);",
                null,
            )
        }
    }

    private fun restoreDucking(rampOut: Double) {
        webViewProvider()?.evaluateJavascript("NarrativeDJ.restore($rampOut);", null)
    }

    private fun notifySegmentComplete() {
        segmentCompleteListener?.invoke()
    }

    private fun resolveLlmClient(): LlmClient? {
        keyStore.getApiKey(SecureKeyStore.Provider.GEMINI)?.let { return GeminiLlmClient(it) }
        keyStore.getApiKey(SecureKeyStore.Provider.OPENAI)?.let { return OpenAiLlmClient(it) }
        return null
    }

    private suspend fun resolveTransitionControl(context: DjTransitionContext): DjAudioControl {
        val client = resolveLlmClient()
        if (client != null) {
            try {
                return client.generateTransitionMent(context)
            } catch (_: Exception) {
                // fall through
            }
        }
        return DjAudioControlParser.fallbackForTransition(context)
    }

    private fun estimateSpeechMs(script: String): Long {
        val words = script.split(Regex("\\s+")).count { it.isNotBlank() }
        return (words * 400L).coerceAtLeast(2_000L)
    }

    companion object {
        private const val DEFAULT_RAMP_OUT = 0.55
    }
}

private fun DjAudioControl.copy(script: String = this.script): DjAudioControl {
    return DjAudioControl(
        duckingVolume = duckingVolume,
        rampInSeconds = rampInSeconds,
        rampOutSeconds = rampOutSeconds,
        script = script,
        ssml = ssml,
    )
}
