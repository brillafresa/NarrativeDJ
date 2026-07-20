package com.narrativedj.app.b2b

enum class MusicProviderMode {
    BYOK_WEBVIEW,
    B2B_STREAMING,
}

interface MusicProvider {
    val mode: MusicProviderMode
    fun playbackSourceLabel(): String
    suspend fun resolveStreamUrl(trackId: String): String?
}

class ByokWebViewMusicProvider : MusicProvider {
    override val mode = MusicProviderMode.BYOK_WEBVIEW
    override fun playbackSourceLabel() = "YouTube Music (BYOK WebView)"
    override suspend fun resolveStreamUrl(trackId: String): String? = null
}

class B2bStreamingMusicProvider(
    private val apiClient: B2bPartnerApiClient,
) : MusicProvider {
    override val mode = MusicProviderMode.B2B_STREAMING
    override fun playbackSourceLabel() = "B2B Partner Stream"
    override suspend fun resolveStreamUrl(trackId: String): String? {
        return apiClient.resolveStreamUrl(trackId)
    }
}

object MusicProviderFactory {
    fun create(mode: MusicProviderMode, apiClient: B2bPartnerApiClient): MusicProvider {
        return when (mode) {
            MusicProviderMode.BYOK_WEBVIEW -> ByokWebViewMusicProvider()
            MusicProviderMode.B2B_STREAMING -> B2bStreamingMusicProvider(apiClient)
        }
    }
}
