package com.narrativedj.app.service

object PlaybackSessionState {
    @Volatile
    var mediaSessionActive: Boolean = false

    @Volatile
    var wakeLockHeld: Boolean = false

    @Volatile
    var title: String? = null

    @Volatile
    var artist: String? = null

    @Volatile
    var isPlaying: Boolean = false

    var transportHandler: TransportHandler? = null

    @Volatile
    var metadataListener: (() -> Unit)? = null

    interface TransportHandler {
        fun onPlay()
        fun onPause()
    }

    fun updateNowPlaying(title: String?, artist: String?, playing: Boolean) {
        this.title = title?.takeIf { it.isNotBlank() }
        this.artist = artist?.takeIf { it.isNotBlank() }
        isPlaying = playing
        metadataListener?.invoke()
    }

    fun reset() {
        mediaSessionActive = false
        wakeLockHeld = false
        title = null
        artist = null
        isPlaying = false
        transportHandler = null
        metadataListener = null
    }
}

object PlaybackMetadataFormatter {
    fun notificationLine(title: String?, artist: String?, fallback: String): String {
        return when {
            !title.isNullOrBlank() && !artist.isNullOrBlank() -> "$title — $artist"
            !title.isNullOrBlank() -> title
            else -> fallback
        }
    }
}
