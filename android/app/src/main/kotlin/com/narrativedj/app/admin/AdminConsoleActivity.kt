package com.narrativedj.app.admin

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.narrativedj.app.databinding.ActivityAdminConsoleBinding

class AdminConsoleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminConsoleBinding
    private val scheduleRepository by lazy { ScheduleRepository(this) }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminConsoleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(com.narrativedj.app.R.string.admin_console_title)

        val schedule = scheduleRepository.loadDefaultSchedule()
        binding.summaryText.text = schedule?.let {
            getString(com.narrativedj.app.R.string.admin_summary, it.locations.size)
        } ?: getString(com.narrativedj.app.R.string.admin_summary_error)

        binding.adminWebView.apply {
            settings.javaScriptEnabled = true
            addJavascriptInterface(AdminJsBridge(), JS_BRIDGE_NAME)
            webViewClient = WebViewClient()
            loadUrl("file:///android_asset/admin/console.html")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private inner class AdminJsBridge {
        @JavascriptInterface
        fun postMessage(@Suppress("UNUSED_PARAMETER") data: String) = Unit
    }

    companion object {
        private const val JS_BRIDGE_NAME = "NarrativeDJAdmin"
    }
}
