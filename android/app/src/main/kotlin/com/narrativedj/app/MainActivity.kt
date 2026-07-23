package com.narrativedj.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.narrativedj.app.byok.DebugByokSeeder
import com.narrativedj.app.byok.GeminiApiKeyValidator
import com.narrativedj.app.byok.SecureKeyStore
import com.narrativedj.app.databinding.ActivityMainBinding
import com.narrativedj.app.dj.DjPipeline
import com.narrativedj.app.locale.AppLocaleStore
import com.narrativedj.app.radio.RadioScheduler
import com.narrativedj.app.radio.RadioSessionController
import com.narrativedj.app.radio.RequestParserService
import com.narrativedj.app.scheduler.CushionPlaybackController
import com.narrativedj.app.service.MediaPlaybackService
import com.narrativedj.app.service.PlaybackSessionState
import com.narrativedj.app.webview.YtmJsBridge
import com.narrativedj.app.webview.YtmNowPlayingParser
import com.narrativedj.app.webview.YtmSvdReportParser
import com.narrativedj.app.webview.YtmWebViewClient

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var keyStore: SecureKeyStore
    private lateinit var djPipeline: DjPipeline
    private lateinit var radioSession: RadioSessionController
    private var tts: TextToSpeech? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var musicPageReady = false

    private val nowPlayingPoller = object : Runnable {
        override fun run() {
            pollNowPlaying()
            mainHandler.postDelayed(this, NOW_PLAYING_POLL_MS)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keyStore = SecureKeyStore(this)
        DebugByokSeeder.seedIfNeeded(keyStore)
        if (!keyStore.hasUsableGeminiApiKey()) {
            redirectToKeyGate()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        djPipeline = DjPipeline(
            context = applicationContext,
            keyStore = keyStore,
            webViewProvider = { binding.webView },
            scope = lifecycleScope,
            languageProvider = { AppLocaleStore.getLanguage(this) },
        )
        tts = TextToSpeech(this, this)

        val cushionPlayback = CushionPlaybackController(
            webView = binding.webView,
            handler = mainHandler,
        )
        val requestParser = RequestParserService(keyStore)
        val radioScheduler = RadioScheduler()
        radioSession = RadioSessionController(
            context = this,
            scope = lifecycleScope,
            requestParser = requestParser,
            scheduler = radioScheduler,
            cushionPlayback = cushionPlayback,
            djPipeline = djPipeline,
            languageProvider = { AppLocaleStore.getLanguage(this) },
            onStatus = ::updateStatus,
        )

        setupSendControl()
        setupPlaybackTransport()

        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.userAgentString = settings.userAgentString.replace("; wv", "")
            addJavascriptInterface(YtmJsBridge(::onBridgeMessage), JS_BRIDGE_NAME)
            webChromeClient = WebChromeClient()
            webViewClient = YtmWebViewClient(this@MainActivity, ::onMusicPageReady)
            updateStatus(getString(R.string.status_loading_ytm))
            loadUrl(YTMUSIC_URL)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::keyStore.isInitialized && !keyStore.hasUsableGeminiApiKey()) {
            redirectToKeyGate()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_gemini_key_settings -> {
                showGeminiKeyDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine -> djPipeline.bindTts(engine) }
        }
    }

    override fun onDestroy() {
        if (::binding.isInitialized) {
            mainHandler.removeCallbacks(nowPlayingPoller)
        }
        PlaybackSessionState.transportHandler = null
        PlaybackSessionState.reset()
        stopService(Intent(this, MediaPlaybackService::class.java))
        if (::djPipeline.isInitialized) {
            djPipeline.shutdown()
        }
        tts?.shutdown()
        super.onDestroy()
    }

    private fun redirectToKeyGate() {
        startActivity(Intent(this, GeminiKeyGateActivity::class.java))
        finish()
    }

    private fun setupSendControl() {
        binding.btnSend.setOnClickListener { submitMessage() }
        binding.storyInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                submitMessage()
                true
            } else {
                false
            }
        }
    }

    private fun submitMessage() {
        val text = binding.storyInput.text?.toString().orEmpty()
        if (text.isBlank()) return
        binding.storyInput.text?.clear()
        radioSession.handleUserSend(text)
    }

    private fun setupPlaybackTransport() {
        PlaybackSessionState.transportHandler = object : PlaybackSessionState.TransportHandler {
            override fun onPlay() {
                binding.webView.evaluateJavascript("NarrativeDJYtm.playPause(true);", null)
            }

            override fun onPause() {
                binding.webView.evaluateJavascript("NarrativeDJYtm.playPause(false);", null)
            }
        }
    }

    private fun startPlaybackService() {
        val intent = Intent(this, MediaPlaybackService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun onMusicPageReady(url: String) {
        musicPageReady = true
        startPlaybackService()
        binding.webView.evaluateJavascript("NarrativeDJYtm.onPageReady();", null)
        binding.webView.evaluateJavascript("NarrativeDJYtm.runSvd();") { rawJson ->
            val report = YtmSvdReportParser.parse(unquoteJsString(rawJson))
            val svdStatus = if (report?.healthy == true) {
                getString(R.string.status_svd_ok)
            } else {
                getString(R.string.status_svd_degraded)
            }
            updateStatus(
                getString(
                    R.string.status_page_ready,
                    svdStatus,
                    getString(R.string.provider_ytm),
                ),
            )
        }
        mainHandler.removeCallbacks(nowPlayingPoller)
        mainHandler.post(nowPlayingPoller)
    }

    private fun pollNowPlaying() {
        if (!musicPageReady) return
        binding.webView.evaluateJavascript("NarrativeDJYtm.getNowPlaying();") { rawJson ->
            val nowPlaying = YtmNowPlayingParser.parse(unquoteJsString(rawJson)) ?: return@evaluateJavascript
            if (nowPlaying.title != null || nowPlaying.artist != null) {
                radioSession.updateNowPlaying(
                    nowPlaying.title,
                    nowPlaying.artist,
                    nowPlaying.isPlaying,
                )
                PlaybackSessionState.updateNowPlaying(
                    nowPlaying.title,
                    nowPlaying.artist,
                    nowPlaying.isPlaying,
                )
                val poolNote = getString(R.string.status_pool_size, radioSession.candidatePool.size())
                updateStatus(
                    getString(
                        R.string.status_now_playing,
                        nowPlaying.displayLabel() + " | $poolNote",
                    ),
                )
            }
        }
    }

    private fun showGeminiKeyDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        val geminiInput = EditText(this).apply {
            hint = getString(R.string.gemini_key_hint)
            val stored = keyStore.getGeminiApiKey().orEmpty()
            setText(if (GeminiApiKeyValidator.isUsable(stored)) stored else "")
        }
        layout.addView(geminiInput)
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.gemini_key_settings)
            .setView(layout)
            .setPositiveButton(R.string.save_keys, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (trySaveGeminiKey(geminiInput)) {
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    /** @return true when save/clear completed and dialog may close. */
    private fun trySaveGeminiKey(geminiInput: EditText): Boolean {
        val gemini = geminiInput.text?.toString()?.trim().orEmpty()
        if (gemini.isBlank()) {
            keyStore.clearGeminiApiKey()
            redirectToKeyGate()
            return true
        }
        if (!GeminiApiKeyValidator.isUsable(gemini)) {
            geminiInput.error = getString(R.string.gemini_key_invalid)
            return false
        }
        keyStore.saveGeminiApiKey(gemini)
        updateStatus(getString(R.string.status_keys_saved))
        return true
    }

    private fun onBridgeMessage(data: String) {
        try {
            val json = org.json.JSONObject(data)
            djPipeline.onBridgeEvent(json.optString("event"))
        } catch (_: Exception) {
            Unit
        }
    }

    private fun updateStatus(message: String) {
        binding.statusText.text = message
    }

    private fun unquoteJsString(evalResult: String?): String {
        if (evalResult == null || evalResult == "null") return "null"
        return evalResult.trim().removeSurrounding("\"")
            .replace("\\\\", "\\")
            .replace("\\\"", "\"")
    }

    companion object {
        private const val YTMUSIC_URL = "https://music.youtube.com"
        private const val JS_BRIDGE_NAME = "NarrativeDJAndroid"
        private const val NOW_PLAYING_POLL_MS = 5_000L
    }
}
