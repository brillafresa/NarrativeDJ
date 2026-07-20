package com.narrativedj.app.webview

data class YtmNowPlaying(
    val title: String?,
    val artist: String?,
    val isPlaying: Boolean,
    val pageUrl: String = "",
) {
    fun displayLabel(): String {
        val track = listOfNotNull(title, artist).joinToString(" — ")
        if (track.isEmpty()) return "No track detected"
        val state = if (isPlaying) "playing" else "paused"
        return "$track ($state)"
    }
}

object YtmNowPlayingParser {
    fun parse(json: String): YtmNowPlaying? {
        return try {
            val root = org.json.JSONObject(json)
            YtmNowPlaying(
                title = root.optNullableString("title"),
                artist = root.optNullableString("artist"),
                isPlaying = root.optBoolean("isPlaying"),
                pageUrl = root.optString("pageUrl", ""),
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun org.json.JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key).trim()
        return value.takeIf { it.isNotEmpty() }
    }
}
