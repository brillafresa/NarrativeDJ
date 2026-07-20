package com.narrativedj.app.webview

import android.webkit.JavascriptInterface

class YtmJsBridge(
    private val onMessage: (String) -> Unit,
) {
    @JavascriptInterface
    fun postMessage(data: String) {
        onMessage(data)
    }
}
