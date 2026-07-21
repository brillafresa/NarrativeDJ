package com.narrativedj.app.service

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackMetadataFormatterTest {

    @Test
    fun notificationLine_titleAndArtist() {
        val line = PlaybackMetadataFormatter.notificationLine(
            title = "Hotel California",
            artist = "Eagles",
            fallback = "Playing",
        )
        assertEquals("Hotel California — Eagles", line)
    }

    @Test
    fun notificationLine_fallbackWhenEmpty() {
        val line = PlaybackMetadataFormatter.notificationLine(
            title = null,
            artist = null,
            fallback = "NarrativeDJ 재생 중",
        )
        assertEquals("NarrativeDJ 재생 중", line)
    }
}
