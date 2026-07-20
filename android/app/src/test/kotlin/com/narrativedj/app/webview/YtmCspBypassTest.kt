package com.narrativedj.app.webview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YtmCspBypassTest {

    @Test
    fun transformHtml_stripsCspMetaAndInjectsPermissivePolicy() {
        val html = """
            <html><head>
            <meta http-equiv="Content-Security-Policy" content="default-src 'self';">
            <title>YT Music</title>
            </head><body>ok</body></html>
        """.trimIndent()

        val result = YtmCspBypass.transformHtml(html, "<script>window.__injected=true;</script>")

        assertFalse(result.contains("default-src 'self'"))
        assertTrue(result.contains("default-src * 'unsafe-inline'"))
        assertTrue(result.contains("window.__injected=true"))
    }

    @Test
    fun shouldInterceptDocument_musicHome_returnsTrue() {
        assertTrue(YtmCspBypass.shouldInterceptDocument("https://music.youtube.com/", null))
    }

    @Test
    fun shouldInterceptDocument_jsAsset_returnsFalse() {
        assertFalse(YtmCspBypass.shouldInterceptDocument("https://music.youtube.com/some/path/app.js", null))
    }

    @Test
    fun shouldInterceptDocument_subFrame_returnsFalse() {
        val request = object : android.webkit.WebResourceRequest {
            override fun getUrl() = android.net.Uri.parse("https://music.youtube.com/")
            override fun isForMainFrame() = false
            override fun isRedirect() = false
            override fun hasGesture() = false
            override fun getMethod() = "GET"
            override fun getRequestHeaders(): Map<String, String> = emptyMap()
        }
        assertFalse(YtmCspBypass.shouldInterceptDocument("https://music.youtube.com/", request))
    }
}
