package com.narrativedj.app.webview

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * YouTube Music WebView client with CSP bypass (Phase 1-C) and script injection (Phase 1-A).
 */
class YtmWebViewClient(
    private val context: Context,
    private val onMusicPageReady: (String) -> Unit,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString().orEmpty()
        if (shouldRedirectToMusic(url)) {
            view?.loadUrl(YTM_HOME)
            return true
        }
        return super.shouldOverrideUrlLoading(view, request)
    }

    @Deprecated("Deprecated in API")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        val pageUrl = url.orEmpty()
        if (shouldRedirectToMusic(pageUrl)) {
            view?.loadUrl(YTM_HOME)
            return true
        }
        return super.shouldOverrideUrlLoading(view, url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        val webView = view ?: return
        val pageUrl = url.orEmpty()
        if (shouldRedirectToMusic(pageUrl)) {
            webView.loadUrl(YTM_HOME)
            return
        }
        if (!isYouTubeMusicUrl(pageUrl)) return

        YtmAssetInjector.inject(webView, context) {
            onMusicPageReady(pageUrl)
        }
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
    ): WebResourceResponse? {
        val url = request?.url?.toString().orEmpty()
        if (!YtmCspBypass.shouldInterceptDocument(url, request)) {
            return super.shouldInterceptRequest(view, request)
        }

        return try {
            val cookieHeader = CookieManager.getInstance().getCookie(url)
            val headInjection = buildHeadInjection()
            val stream = YtmCspBypass.fetchAndTransform(url, cookieHeader, headInjection)
                ?: return super.shouldInterceptRequest(view, request)
            WebResourceResponse("text/html", "utf-8", stream)
        } catch (_: Exception) {
            super.shouldInterceptRequest(view, request)
        }
    }

    private fun buildHeadInjection(): String {
        val bridgeBootstrap = context.assets.open("www/bridge.js")
            .bufferedReader()
            .use { it.readText() }
        return "<script>\n$bridgeBootstrap\n</script>"
    }

    companion object {
        fun isHarnessFixtureUrl(url: String): Boolean {
            return url.contains("www/fixtures/ytm-poc-fixture")
        }

        fun isYouTubeMusicUrl(url: String): Boolean {
            return url.contains("music.youtube.com") || isHarnessFixtureUrl(url)
        }

        fun shouldRedirectToMusic(url: String): Boolean {
            if (url.isBlank()) return false
            if (isYouTubeMusicUrl(url)) return false
            if (isHarnessFixtureUrl(url)) return false
            return url.contains("youtube.com") && !url.contains("music.youtube.com")
        }

        private const val YTM_HOME = "https://music.youtube.com"
    }
}
