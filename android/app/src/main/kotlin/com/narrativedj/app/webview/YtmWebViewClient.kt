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

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        val webView = view ?: return
        val pageUrl = url.orEmpty()
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
    }
}
