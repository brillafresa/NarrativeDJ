/**
 * Instrumentation harness: SVD selector resolution on local DOM fixtures.
 * Fixtures: assets/www/fixtures/ytm-poc-fixture*.html
 * Run: cd android && ./gradlew connectedDebugAndroidTest --tests *.YtmSvdFixtureTest
 */
package com.narrativedj.app.webview

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class YtmSvdFixtureTest {

    @Test
    fun svdOnCanonicalFixture_resolvesProductionSelectors() {
        runSvdHarness("file:///android_asset/www/fixtures/ytm-poc-fixture.html") { report ->
            assertTrue(report.healthy)
            assertEquals("ytmusic-player-bar .title", report.fields["title"]?.selector)
            assertEquals("ytmusic-player-bar .byline", report.fields["artist"]?.selector)
        }
    }

    @Test
    fun svdOnAltFixture_fallsBackToYtmClassSelectors() {
        runSvdHarness("file:///android_asset/www/fixtures/ytm-poc-fixture-alt.html") { report ->
            assertTrue(report.healthy)
            assertEquals(".ytmusic-player-bar .title", report.fields["title"]?.selector)
            assertEquals(".ytmusic-player-bar .byline", report.fields["artist"]?.selector)
        }
    }

    private fun runSvdHarness(
        url: String,
        assertReport: (YtmSvdReport) -> Unit,
    ) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val latch = CountDownLatch(1)
        var report: YtmSvdReport? = null

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, pageUrl: String?) {
                    YtmAssetInjector.inject(webView, context) {
                        webView.evaluateJavascript("NarrativeDJSvd.run();") { rawJson ->
                            report = YtmSvdReportParser.parse(unquoteJsString(rawJson))
                            latch.countDown()
                        }
                    }
                }
            }
            webView.loadUrl(url)
        }

        assertTrue("SVD harness timed out for $url", latch.await(15, TimeUnit.SECONDS))
        assertReport(requireNotNull(report))
    }

    private fun unquoteJsString(evalResult: String?): String {
        if (evalResult == null || evalResult == "null") return "null"
        return evalResult.trim().removeSurrounding("\"")
            .replace("\\\\", "\\")
            .replace("\\\"", "\"")
    }
}
