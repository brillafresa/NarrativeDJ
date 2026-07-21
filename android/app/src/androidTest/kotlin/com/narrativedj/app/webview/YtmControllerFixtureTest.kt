package com.narrativedj.app.webview

/**
 * Instrumentation harness: YTM controller + SVD on local DOM fixtures.
 * Fixtures: assets/www/fixtures/ytm-poc-fixture*.html
 * Run: cd android && ./gradlew connectedDebugAndroidTest --tests *.YtmControllerFixtureTest
 */
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class YtmControllerFixtureTest {

    @Test
    fun fixtureNowPlaying_parsesTitleArtistAndPlayState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val latch = CountDownLatch(1)
        var parsed: YtmNowPlaying? = null
        var error: String? = null

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    YtmAssetInjector.inject(webView, context) {
                        webView.evaluateJavascript("NarrativeDJYtm.getNowPlaying();") { rawJson ->
                            try {
                                parsed = YtmNowPlayingParser.parse(unquoteJsString(rawJson))
                            } catch (e: Exception) {
                                error = e.message
                            } finally {
                                latch.countDown()
                            }
                        }
                    }
                }
            }
            webView.loadUrl("file:///android_asset/www/fixtures/ytm-poc-fixture.html")
        }

        assertTrue("Fixture harness timed out", latch.await(15, TimeUnit.SECONDS))
        assertNull("Unexpected harness error: $error", error)

        val nowPlaying = requireNotNull(parsed)
        assertEquals("California Dreamin'", nowPlaying.title)
        assertEquals("The Mamas & The Papas", nowPlaying.artist)
        assertTrue(nowPlaying.isPlaying)
    }

    @Test
    fun fixtureSearchAndPlay_updatesNowPlayingTitle() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val latch = CountDownLatch(1)
        var parsed: YtmNowPlaying? = null

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    YtmAssetInjector.inject(webView, context) {
                        webView.evaluateJavascript(
                            "NarrativeDJYtm.searchAndPlay('Hotel California');",
                        ) {
                            webView.evaluateJavascript("NarrativeDJYtm.getNowPlaying();") { rawJson ->
                                parsed = YtmNowPlayingParser.parse(unquoteJsString(rawJson))
                                latch.countDown()
                            }
                        }
                    }
                }
            }
            webView.loadUrl("file:///android_asset/www/fixtures/ytm-poc-fixture.html")
        }

        assertTrue("Search/play harness timed out", latch.await(15, TimeUnit.SECONDS))
        assertEquals("Hotel California", requireNotNull(parsed).title)
    }

    private fun unquoteJsString(evalResult: String?): String {
        if (evalResult == null || evalResult == "null") return "null"
        return evalResult.trim().removeSurrounding("\"")
            .replace("\\\\", "\\")
            .replace("\\\"", "\"")
    }
}
