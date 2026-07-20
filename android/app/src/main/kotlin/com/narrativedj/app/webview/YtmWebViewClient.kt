package com.narrativedj.app.webview

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * YouTube Music WebView client stub.
 * TODO Phase 1-C: implement CSP bypass via shouldInterceptRequest (see docs/research.md §3.1).
 */
class YtmWebViewClient : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
    ): WebResourceResponse? {
        // val url = request?.url?.toString().orEmpty()
        // if (url.contains("music.youtube.com")) { ... CSP meta injection ... }
        return super.shouldInterceptRequest(view, request)
    }
}
