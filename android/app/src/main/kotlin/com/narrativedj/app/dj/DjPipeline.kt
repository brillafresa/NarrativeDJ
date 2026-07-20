package com.narrativedj.app.dj

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.webkit.WebView
import com.narrativedj.app.R
import com.narrativedj.app.byok.SecureKeyStore
import com.narrativedj.app.byok.llm.GeminiLlmClient
import com.narrativedj.app.byok.llm.LlmClient
import com.narrativedj.app.byok.llm.OpenAiLlmClient
import com.narrativedj.app.byok.tts.OpenAiTtsClient
import com.narrativedj.app.locale.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class DjPipeline(
    private val context: Context,
    private val keyStore: SecureKeyStore,
    private val webViewProvider: () -> WebView?,
    private val scope: CoroutineScope,
    private val languageProvider: () -> AppLanguage,
) {
    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null

    fun bindTts(engine: TextToSpeech) {
        tts = engine
        applyTtsLocale(engine)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                if (utteranceId?.startsWith("narrativedj") == true) {
                    restoreDucking(DEFAULT_RAMP_OUT)
                }
            }

            @Deprecated("Deprecated in API")
            override fun onError(utteranceId: String?) {
                restoreDucking(DEFAULT_RAMP_OUT)
            }
        })
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

    fun runStorySegment(story: String, profileLabel: String, onStatus: (String) -> Unit) {
        scope.launch {
            try {
                val language = languageProvider()
                onStatus(context.getString(R.string.dj_generating))
                val client = resolveLlmClient()
                val control = if (client != null) {
                    client.generateAudioControl(story, profileLabel, language)
                } else {
                    DjAudioControlParser.fallbackForStory(story, language)
                }
                onStatus(context.getString(R.string.dj_line_prefix, control.script))
                playSegment(control)
            } catch (e: Exception) {
                onStatus(context.getString(R.string.dj_error, e.message ?: "unknown"))
            }
        }
    }

    fun shutdown() {
        tts?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private suspend fun playSegment(control: DjAudioControl) {
        duck(control.duckingVolume, control.rampInSeconds)
        val openAiKey = keyStore.getApiKey(SecureKeyStore.Provider.OPENAI)
        if (!openAiKey.isNullOrBlank()) {
            try {
                val audio = OpenAiTtsClient(openAiKey).synthesize(control.script, "alloy")
                playOpenAiTts(audio, control)
                return
            } catch (_: Exception) {
                // fall through to Android TTS
            }
        }
        playAndroidTts(control)
    }

    private suspend fun playOpenAiTts(audio: ByteArray, control: DjAudioControl) {
        withContext(Dispatchers.Main) {
            mediaPlayer?.release()
            val tempFile = File.createTempFile("narrativedj-tts-", ".mp3")
            tempFile.writeBytes(audio)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                setOnCompletionListener {
                    restoreDucking(control.rampOutSeconds)
                    tempFile.delete()
                }
                setOnErrorListener { _, _, _ ->
                    restoreDucking(control.rampOutSeconds)
                    tempFile.delete()
                    true
                }
                prepare()
                start()
            }
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

    private fun resolveLlmClient(): LlmClient? {
        keyStore.getApiKey(SecureKeyStore.Provider.GEMINI)?.let { return GeminiLlmClient(it) }
        keyStore.getApiKey(SecureKeyStore.Provider.OPENAI)?.let { return OpenAiLlmClient(it) }
        return null
    }

    private fun estimateSpeechMs(script: String): Long {
        val words = script.split(Regex("\\s+")).count { it.isNotBlank() }
        return (words * 400L).coerceAtLeast(2_000L)
    }

    companion object {
        private const val DEFAULT_RAMP_OUT = 0.55
    }
}
