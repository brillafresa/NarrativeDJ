/**
 * JVM harness: CatalogMatcher title / search_query / play-key resolution (vector catalog tests).
 * Fixture: src/test/resources/mock_tracks.json (sync via harness/scripts/sync_fixtures.py)
 * Run: cd android && ./gradlew testDebugUnitTest --tests com.narrativedj.app.scheduler.CatalogMatcherTest
 */
package com.narrativedj.app.scheduler

import com.narrativedj.app.radio.CandidateEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.nio.charset.StandardCharsets

class CatalogMatcherTest {

    private lateinit var catalog: List<CatalogTrack>

    @Before
    fun setUp() {
        catalog = TrackCatalogLoader.parse(readResource("mock_tracks.json"))
    }

    @Test
    fun resolves_byTitle() {
        assertEquals("mongjungin", CatalogMatcher.resolveId(catalog, "몽중인"))
        assertEquals("hotel_california", CatalogMatcher.resolveId(catalog, "Hotel California"))
    }

    @Test
    fun resolves_bySearchQuery() {
        assertEquals(
            "hotel_california",
            CatalogMatcher.resolveId(catalog, "Hotel California Eagles"),
        )
    }

    @Test
    fun resolves_nowPlayingLabel_withArtist() {
        assertEquals(
            "sweet_child",
            CatalogMatcher.resolveId(catalog, "Sweet Child O' Mine — Guns N' Roses"),
        )
    }

    @Test
    fun resolves_entryCatalogId() {
        val entry = CandidateEntry(
            searchQuery = "something else",
            catalogTrackId = "california_dreamin",
        )
        assertEquals("california_dreamin", CatalogMatcher.resolveIdForEntry(catalog, entry))
    }

    @Test
    fun unknown_returnsNull() {
        assertNull(CatalogMatcher.resolveId(catalog, "NewJeans Attention"))
    }

    private fun readResource(name: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(name)
            ?: error("Missing test resource: $name")
        return stream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
    }
}
