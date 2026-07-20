package com.narrativedj.app.webview

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class HackTimerFixtureTest {

    @Test
    fun hackTimer_firesCallbackOnFixturePage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val latch = CountDownLatch(1)
        val fired = AtomicBoolean(false)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    YtmAssetInjector.inject(webView, context) {
                        webView.evaluateJavascript(
                            """
                            (function(){
                              if (!window.NarrativeDJHackTimer) return 'missing';
                              var done = false;
                              window.NarrativeDJHackTimer.setTimeout(function(){ done = true; }, 50);
                              return 'scheduled';
                            })();
                            """.trimIndent(),
                            null,
                        )
                        webView.postDelayed({
                            webView.evaluateJavascript(
                                "(function(){ return !!window.NarrativeDJHackTimer && window.NarrativeDJHackTimer.isInstalled(); })();",
                            ) { installed ->
                                if (installed == "true") fired.set(true)
                                latch.countDown()
                            }
                        }, 200)
                    }
                }
            }
            webView.loadUrl("file:///android_asset/www/fixtures/ytm-poc-fixture.html")
        }

        assertTrue("HackTimer harness timed out", latch.await(15, TimeUnit.SECONDS))
        assertEquals(true, fired.get())
    }
}
