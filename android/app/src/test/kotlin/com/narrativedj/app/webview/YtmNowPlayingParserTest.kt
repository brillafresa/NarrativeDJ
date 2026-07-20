package com.narrativedj.app.webview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YtmNowPlayingParserTest {

    @Test
    fun parse_fixturePayload_returnsNowPlaying() {
        val json = """
            {
              "title": "California Dreamin'",
              "artist": "The Mamas & The Papas",
              "isPlaying": true,
              "pageUrl": "file:///android_asset/www/fixtures/ytm-poc-fixture.html"
            }
        """.trimIndent()

        val result = YtmNowPlayingParser.parse(json)

        requireNotNull(result)
        assertEquals("California Dreamin'", result.title)
        assertEquals("The Mamas & The Papas", result.artist)
        assertTrue(result.isPlaying)
        assertTrue(result.pageUrl.contains("ytm-poc-fixture"))
    }

    @Test
    fun parse_nullTitleAndArtist_returnsObjectWithNulls() {
        val json = """{"title":null,"artist":null,"isPlaying":false,"pageUrl":"https://music.youtube.com"}"""

        val result = YtmNowPlayingParser.parse(json)

        requireNotNull(result)
        assertNull(result.title)
        assertNull(result.artist)
        assertFalse(result.isPlaying)
    }

    @Test
    fun parse_invalidJson_returnsNull() {
        assertNull(YtmNowPlayingParser.parse("not-json"))
    }

    @Test
    fun displayLabel_formatsTrackAndState() {
        val nowPlaying = YtmNowPlaying(
            title = "Hotel California",
            artist = "Eagles",
            isPlaying = true,
        )
        assertEquals("Hotel California — Eagles (playing)", nowPlaying.displayLabel())
    }
}
