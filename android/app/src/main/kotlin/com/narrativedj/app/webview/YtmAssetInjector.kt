package com.narrativedj.app.webview

import android.content.Context
import android.webkit.WebView

/** Injects bridge, HackTimer, ducking, SVD, and YTM controller scripts into WebView. */
object YtmAssetInjector {
    private val SCRIPT_ASSETS = listOf(
        "www/bridge.js",
        "www/hack-timer.js",
        "www/audio-ducking.js",
        "www/ytm-svd.js",
        "www/ytm-controller.js",
    )

    fun inject(webView: WebView, context: Context, onComplete: (() -> Unit)? = null) {
        val dictionaryJson = context.assets.open("www/selector_dictionary.json")
            .bufferedReader()
            .use { it.readText() }
        val bootstrap = "window.__NarrativeDJ_SELECTORS__ = $dictionaryJson;"
        webView.evaluateJavascript(bootstrap) {
            injectAt(webView, context, 0, onComplete)
        }
    }

    private fun injectAt(
        webView: WebView,
        context: Context,
        index: Int,
        onComplete: (() -> Unit)?,
    ) {
        if (index >= SCRIPT_ASSETS.size) {
            onComplete?.invoke()
            return
        }
        val assetPath = SCRIPT_ASSETS[index]
        val script = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        webView.evaluateJavascript(script) {
            injectAt(webView, context, index + 1, onComplete)
        }
    }
}
