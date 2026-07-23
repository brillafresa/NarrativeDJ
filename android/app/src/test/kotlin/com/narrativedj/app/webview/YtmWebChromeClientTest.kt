/**
 * JVM harness: leave-page auto-confirm policy for app-driven YTM search.
 * Run: cd android && ./gradlew testDebugUnitTest --tests com.narrativedj.app.webview.YtmWebChromeClientTest
 */
package com.narrativedj.app.webview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YtmWebChromeClientTest {

    @Test
    fun autoConfirms_whenAppDrivenSearch() {
        assertTrue(YtmWebChromeClient.shouldAutoConfirmBeforeUnload(appDrivenSearch = true))
    }

    @Test
    fun doesNotAutoConfirm_whenUserNavigation() {
        assertFalse(YtmWebChromeClient.shouldAutoConfirmBeforeUnload(appDrivenSearch = false))
    }
}
