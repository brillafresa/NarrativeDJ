/**
 * JVM harness: YTM WebViewClient redirect policy (auth allowlist + post-login landings).
 * Run: cd android && ./gradlew test --tests com.narrativedj.app.webview.YtmWebViewClientTest
 */
package com.narrativedj.app.webview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YtmWebViewClientTest {

    @Test
    fun shouldRedirectToMusic_musicHome_doesNotRedirect() {
        assertFalse(YtmWebViewClient.shouldRedirectToMusic("https://music.youtube.com/"))
    }

    @Test
    fun shouldRedirectToMusic_harnessFixture_doesNotRedirect() {
        assertFalse(
            YtmWebViewClient.shouldRedirectToMusic(
                "file:///android_asset/www/fixtures/ytm-poc-fixture.html",
            ),
        )
    }

    @Test
    fun shouldRedirectToMusic_googleAccounts_doesNotRedirect() {
        assertFalse(
            YtmWebViewClient.shouldRedirectToMusic(
                "https://accounts.google.com/o/oauth2/auth?client_id=test",
            ),
        )
        assertFalse(
            YtmWebViewClient.shouldRedirectToMusic(
                "https://accounts.google.com/v3/signin/identifier",
            ),
        )
    }

    @Test
    fun shouldRedirectToMusic_youtubeSignIn_doesNotRedirect() {
        assertFalse(
            YtmWebViewClient.shouldRedirectToMusic(
                "https://www.youtube.com/signin?action_handle_signin=true",
            ),
        )
        assertFalse(
            YtmWebViewClient.shouldRedirectToMusic(
                "https://www.youtube.com/ServiceLogin?continue=https://music.youtube.com",
            ),
        )
        assertFalse(
            YtmWebViewClient.shouldRedirectToMusic(
                "https://www.youtube.com/o/oauth2/v2/auth?redirect_uri=test",
            ),
        )
        assertFalse(
            YtmWebViewClient.shouldRedirectToMusic(
                "https://www.youtube.com/redirect?event=login",
            ),
        )
    }

    @Test
    fun shouldRedirectToMusic_youtubeHome_redirects() {
        assertTrue(YtmWebViewClient.shouldRedirectToMusic("https://www.youtube.com/"))
        assertTrue(YtmWebViewClient.shouldRedirectToMusic("https://youtube.com/?feature=ytca"))
        assertTrue(YtmWebViewClient.shouldRedirectToMusic("https://www.youtube.com/feed/trending"))
    }

    @Test
    fun shouldRedirectToMusic_youtubeWatch_doesNotRedirect() {
        assertFalse(
            YtmWebViewClient.shouldRedirectToMusic(
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            ),
        )
    }
}
