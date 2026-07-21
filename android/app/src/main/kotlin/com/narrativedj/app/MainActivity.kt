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
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.narrativedj.app.admin.AdminConsoleActivity
import com.narrativedj.app.admin.ScheduleRepository
import com.narrativedj.app.b2b.B2bLicenseStore
import com.narrativedj.app.b2b.B2bPartnerApiClient
import com.narrativedj.app.b2b.CommercialSpaceGuard
import com.narrativedj.app.b2b.MusicProvider
import com.narrativedj.app.b2b.MusicProviderFactory
import com.narrativedj.app.b2b.MusicProviderMode
import com.narrativedj.app.b2b.localizedLabel
import com.narrativedj.app.b2b.localizedMessage
import com.narrativedj.app.byok.SecureKeyStore
import com.narrativedj.app.databinding.ActivityMainBinding
import com.narrativedj.app.dj.DjPipeline
import com.narrativedj.app.locale.AppLanguage
import com.narrativedj.app.locale.AppLocaleStore
import com.narrativedj.app.profile.SpaceProfile
import com.narrativedj.app.profile.SpaceProfiles
import com.narrativedj.app.radio.RadioScheduler
import com.narrativedj.app.radio.RadioSessionController
import com.narrativedj.app.radio.RequestParserService
import com.narrativedj.app.scheduler.CushionPlaybackController
import com.narrativedj.app.scheduler.CushionRoutePlanner
import com.narrativedj.app.scheduler.TrackCatalogLoader
import com.narrativedj.app.service.MediaPlaybackService
import com.narrativedj.app.service.PlaybackSessionState
import com.narrativedj.app.webview.YtmJsBridge
import com.narrativedj.app.webview.YtmNowPlayingParser
import com.narrativedj.app.webview.YtmSvdReportParser
import com.narrativedj.app.webview.YtmWebViewClient
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var keyStore: SecureKeyStore
    private lateinit var b2bStore: B2bLicenseStore
    private lateinit var djPipeline: DjPipeline
    private lateinit var radioSession: RadioSessionController
    private lateinit var musicProvider: MusicProvider
    private var tts: TextToSpeech? = null
    private var selectedProfile: SpaceProfile = SpaceProfiles.cozyBrunchCafe

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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        keyStore = SecureKeyStore(this)
        b2bStore = B2bLicenseStore(this)
        djPipeline = DjPipeline(
            context = applicationContext,
            keyStore = keyStore,
            webViewProvider = { binding.webView },
            scope = lifecycleScope,
            languageProvider = { AppLocaleStore.getLanguage(this) },
        )
        tts = TextToSpeech(this, this)
        refreshMusicProvider()

        val trackCatalog = TrackCatalogLoader.load(this)
        val vectorDb = trackCatalog.associate { track ->
            track.id to com.narrativedj.app.scheduler.CushionMusicScheduler.trackToVector(
                track.bpm,
                track.energy,
                track.valence,
                track.embedding,
            )
        }
        val cushionPlanner = CushionRoutePlanner(
            com.narrativedj.app.scheduler.CushionMusicScheduler(vectorDb),
            trackCatalog,
        )
        val cushionPlayback = CushionPlaybackController(
            catalog = trackCatalog,
            webView = binding.webView,
            handler = mainHandler,
        )
        val requestParser = RequestParserService(keyStore, trackCatalog, cushionPlanner)
        val radioScheduler = RadioScheduler(cushionPlanner, trackCatalog)
        radioSession = RadioSessionController(
            context = this,
            scope = lifecycleScope,
            requestParser = requestParser,
            scheduler = radioScheduler,
            cushionPlayback = cushionPlayback,
            djPipeline = djPipeline,
            catalog = trackCatalog,
            planner = cushionPlanner,
            languageProvider = { AppLocaleStore.getLanguage(this) },
            onStatus = ::updateStatus,
        )

        setupProfileSpinner()
        setupSendControl()
        setupPlaybackTransport()
        applyCommercialGuardStatus()

        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.userAgentString = settings.userAgentString.replace("; wv", "")
            addJavascriptInterface(YtmJsBridge(::onBridgeMessage), JS_BRIDGE_NAME)
            webChromeClient = WebChromeClient()
            webViewClient = YtmWebViewClient(this@MainActivity, ::onMusicPageReady)
            if (musicProvider.mode == MusicProviderMode.BYOK_WEBVIEW) {
                updateStatus(getString(R.string.status_loading_ytm))
                loadUrl(YTMUSIC_URL)
            } else {
                updateStatus(getString(R.string.status_b2b_mode, musicProvider.localizedLabel(this@MainActivity)))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_language_settings -> {
                showLanguageDialog()
                true
            }
            R.id.action_byok_settings -> {
                showByokDialog()
                true
            }
            R.id.action_b2b_settings -> {
                showB2bDialog()
                true
            }
            R.id.action_admin_console -> {
                startActivity(Intent(this, AdminConsoleActivity::class.java))
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
        mainHandler.removeCallbacks(nowPlayingPoller)
        PlaybackSessionState.transportHandler = null
        PlaybackSessionState.reset()
        stopService(Intent(this, MediaPlaybackService::class.java))
        djPipeline.shutdown()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun refreshMusicProvider() {
        val apiClient = B2bPartnerApiClient(
            b2bStore.getPartnerBaseUrl(),
            b2bStore.getLicenseKey(),
        )
        musicProvider = MusicProviderFactory.create(b2bStore.getProviderMode(), apiClient)
    }

    private fun applyCommercialGuardStatus() {
        lifecycleScope.launch {
            val apiClient = B2bPartnerApiClient(
                b2bStore.getPartnerBaseUrl(),
                b2bStore.getLicenseKey(),
            )
            val hasLicense = apiClient.validateLicense()
            val guard = CommercialSpaceGuard.evaluate(
                b2bStore.getVenueType(),
                musicProvider.mode,
                hasLicense,
            )
            if (!guard.allowed) {
                updateStatus(guard.localizedMessage(this@MainActivity))
            }
        }
    }

    private fun setupProfileSpinner() {
        val labels = SpaceProfiles.all.map { it.label(this) }
        binding.profileSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            labels,
        )
        binding.profileSpinner.setSelection(0)
        binding.profileSpinner.setOnItemSelectedListenerCompat { position ->
            selectedProfile = SpaceProfiles.all[position]
            radioSession.setProfile(selectedProfile)
        }
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
                    musicProvider.localizedLabel(this),
                    selectedProfile.label(this),
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
                radioSession.updateNowPlaying(nowPlaying.title, nowPlaying.artist)
                PlaybackSessionState.updateNowPlaying(
                    nowPlaying.title,
                    nowPlaying.artist,
                    nowPlaying.isPlaying,
                )
                val poolNote = getString(R.string.status_pool_size, radioSession.candidatePool.size())
                updateStatus(
                    getString(
                        R.string.status_now_playing,
                        selectedProfile.label(this),
                        nowPlaying.displayLabel() + " | $poolNote",
                    ),
                )
            }
        }
    }

    private fun showLanguageDialog() {
        val options = arrayOf(
            getString(R.string.language_korean),
            getString(R.string.language_english),
        )
        val currentLanguage = AppLocaleStore.getLanguage(this)
        val checkedItem = if (currentLanguage == AppLanguage.ENGLISH) 1 else 0
        AlertDialog.Builder(this)
            .setTitle(R.string.language_settings)
            .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                val language = if (which == 1) AppLanguage.ENGLISH else AppLanguage.KOREAN
                if (language != currentLanguage) {
                    AppLocaleStore.setLanguage(this, language)
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showByokDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        val geminiInput = EditText(this).apply {
            hint = getString(R.string.gemini_key_hint)
            setText(keyStore.getApiKey(SecureKeyStore.Provider.GEMINI).orEmpty())
        }
        val openaiInput = EditText(this).apply {
            hint = getString(R.string.openai_key_hint)
            setText(keyStore.getApiKey(SecureKeyStore.Provider.OPENAI).orEmpty())
        }
        layout.addView(geminiInput)
        layout.addView(openaiInput)
        AlertDialog.Builder(this)
            .setTitle(R.string.byok_settings)
            .setView(layout)
            .setPositiveButton(R.string.save_keys) { _, _ ->
                saveByokKeys(geminiInput, openaiInput)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun saveByokKeys(geminiInput: EditText, openaiInput: EditText) {
        val gemini = geminiInput.text?.toString().orEmpty()
        val openai = openaiInput.text?.toString().orEmpty()
        if (gemini.isNotBlank()) {
            keyStore.saveApiKey(SecureKeyStore.Provider.GEMINI, gemini)
        } else {
            keyStore.clearApiKey(SecureKeyStore.Provider.GEMINI)
        }
        if (openai.isNotBlank()) {
            keyStore.saveApiKey(SecureKeyStore.Provider.OPENAI, openai)
        } else {
            keyStore.clearApiKey(SecureKeyStore.Provider.OPENAI)
        }
        updateStatus(
            if (djPipeline.isByokConfigured()) {
                getString(R.string.status_keys_saved)
            } else {
                getString(R.string.status_keys_cleared)
            },
        )
    }

    private fun showB2bDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        val licenseInput = EditText(this).apply {
            hint = getString(R.string.b2b_license_hint)
            setText(b2bStore.getLicenseKey().orEmpty())
        }
        val partnerInput = EditText(this).apply {
            hint = getString(R.string.b2b_partner_url_hint)
            setText(b2bStore.getPartnerBaseUrl().orEmpty())
        }
        val modeSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(getString(R.string.provider_byok), getString(R.string.provider_b2b)),
            )
            setSelection(if (b2bStore.getProviderMode() == MusicProviderMode.B2B_STREAMING) 1 else 0)
        }
        val venueSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(getString(R.string.venue_personal), getString(R.string.venue_commercial)),
            )
            setSelection(if (b2bStore.getVenueType() == CommercialSpaceGuard.VenueType.COMMERCIAL) 1 else 0)
        }
        layout.addView(licenseInput)
        layout.addView(partnerInput)
        layout.addView(modeSpinner)
        layout.addView(venueSpinner)
        val scheduleCount = ScheduleRepository(this).loadDefaultSchedule()?.locations?.size ?: 0
        AlertDialog.Builder(this)
            .setTitle(R.string.b2b_settings)
            .setMessage(getString(R.string.admin_summary, scheduleCount))
            .setView(layout)
            .setPositiveButton(R.string.save_keys) { _, _ ->
                val license = licenseInput.text?.toString().orEmpty()
                val partner = partnerInput.text?.toString().orEmpty()
                if (license.isNotBlank()) b2bStore.saveLicenseKey(license) else b2bStore.saveLicenseKey("")
                if (partner.isNotBlank()) b2bStore.savePartnerBaseUrl(partner) else b2bStore.savePartnerBaseUrl("")
                b2bStore.saveProviderMode(
                    if (modeSpinner.selectedItemPosition == 1) MusicProviderMode.B2B_STREAMING
                    else MusicProviderMode.BYOK_WEBVIEW,
                )
                b2bStore.saveVenueType(
                    if (venueSpinner.selectedItemPosition == 1) CommercialSpaceGuard.VenueType.COMMERCIAL
                    else CommercialSpaceGuard.VenueType.PERSONAL,
                )
                refreshMusicProvider()
                applyCommercialGuardStatus()
                updateStatus(getString(R.string.status_b2b_saved, musicProvider.localizedLabel(this)))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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
