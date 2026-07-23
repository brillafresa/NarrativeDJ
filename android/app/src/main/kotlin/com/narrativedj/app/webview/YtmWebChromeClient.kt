package com.narrativedj.app.webview

import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView

/**
 * WebChromeClient that auto-confirms YTM beforeunload during app-driven search navigation.
 *
 * Purpose: avoid blocking "leave page?" dialogs when NarrativeDJ navigates to search.
 * Verify: `./gradlew test --tests com.narrativedj.app.webview.YtmWebChromeClientTest`
 */
class YtmWebChromeClient(
    private val isAppDrivenSearch: () -> Boolean = {
        YtmSearchNavigation.isAppDriven
    },
) : WebChromeClient() {

    override fun onJsBeforeUnload(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?,
    ): Boolean {
        if (shouldAutoConfirm(view)) {
            result?.confirm()
            return true
        }
        return super.onJsBeforeUnload(view, url, message, result)
    }

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?,
    ): Boolean {
        if (shouldAutoConfirm(view)) {
            result?.confirm()
            return true
        }
        return super.onJsConfirm(view, url, message, result)
    }

    private fun shouldAutoConfirm(@Suppress("UNUSED_PARAMETER") view: WebView?): Boolean {
        return isAppDrivenSearch()
    }

    companion object {
        fun shouldAutoConfirmBeforeUnload(appDrivenSearch: Boolean): Boolean = appDrivenSearch
    }
}

/**
 * Set true immediately before NarrativeDJ initiates a YTM search navigation.
 * Cleared after a short delay once navigation has started.
 */
object YtmSearchNavigation {
    @Volatile
    var isAppDriven: Boolean = false
        private set

    fun begin() {
        isAppDriven = true
    }

    fun end() {
        isAppDriven = false
    }
}
