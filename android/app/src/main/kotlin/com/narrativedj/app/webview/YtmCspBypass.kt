package com.narrativedj.app.webview

import android.webkit.WebResourceRequest
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

/**
 * CSP bypass for YouTube Music HTML documents (Phase 1-C).
 */
object YtmCspBypass {
    private val CSP_META_PATTERN = Pattern.compile(
        "<meta[^>]*http-equiv\\s*=\\s*['\"]Content-Security-Policy['\"][^>]*>",
        Pattern.CASE_INSENSITIVE,
    )

    private const val PERMISSIVE_CSP_META =
        """<meta http-equiv="Content-Security-Policy" content="default-src * 'unsafe-inline' 'unsafe-eval' data: blob:;">"""

    fun shouldInterceptDocument(url: String, request: WebResourceRequest?): Boolean {
        if (!url.contains("music.youtube.com")) return false
        if (request != null && !request.isForMainFrame) return false
        if (!isLikelyHtmlDocument(url)) return false
        return true
    }

    fun transformHtml(html: String, headInjection: String): String {
        var result = CSP_META_PATTERN.matcher(html).replaceAll("")
        val injectionBlock = "$PERMISSIVE_CSP_META\n$headInjection"
        val headClose = Regex("</head>", RegexOption.IGNORE_CASE).find(result)
        return if (headClose != null) {
            result.replaceRange(headClose.range, "$injectionBlock\n</head>")
        } else {
            "$injectionBlock\n$result"
        }
    }

    fun fetchAndTransform(
        url: String,
        cookieHeader: String?,
        headInjection: String,
    ): ByteArrayInputStream? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", DEFAULT_USER_AGENT)
            if (!cookieHeader.isNullOrBlank()) {
                setRequestProperty("Cookie", cookieHeader)
            }
        }
        return try {
            val contentType = connection.contentType.orEmpty()
            if (!contentType.contains("text/html", ignoreCase = true)) {
                return null
            }
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            val modified = transformHtml(html, headInjection)
            ByteArrayInputStream(modified.toByteArray(Charsets.UTF_8))
        } finally {
            connection.disconnect()
        }
    }

    private fun isLikelyHtmlDocument(url: String): Boolean {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        if (path.endsWith(".js") || path.endsWith(".css") || path.endsWith(".png")) return false
        if (path.endsWith(".woff") || path.endsWith(".woff2") || path.endsWith(".json")) return false
        return true
    }

    const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
}
