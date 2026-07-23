/**
 * JVM harness: RadioScheduler — pool pick, direct YTM search, and catalog cushion bridges.
 * Fixture: src/test/resources/mock_tracks.json
 * Run: cd android && ./gradlew testDebugUnitTest --tests com.narrativedj.app.radio.RadioSchedulerTest
 */
package com.narrativedj.app.radio

import com.narrativedj.app.scheduler.TrackCatalogLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.charset.StandardCharsets

class RadioSchedulerTest {

    private lateinit var catalogScheduler: RadioScheduler
    private lateinit var directScheduler: RadioScheduler

    @Before
    fun setUp() {
        val catalog = TrackCatalogLoader.parse(readResource("mock_tracks.json"))
        catalogScheduler = RadioScheduler(catalog)
        directScheduler = RadioScheduler()
    }

    @Test
    fun pickImmediate_skipsRecentlyPlayed() {
        val pool = CandidatePool()
        pool.addAll(
            listOf(
                CandidateEntry(searchQuery = "California Dreamin'"),
                CandidateEntry(searchQuery = "Hotel California Eagles"),
            ),
        )
        val history = PlayHistory()
        history.record("california dreamin'")
        val decision = directScheduler.pickImmediate(pool, history)
        assertNotNull(decision)
        assertEquals(listOf("Hotel California Eagles"), decision!!.queries)
    }

    @Test
    fun pickNext_playsDirectSearchQuery_withoutCatalog() {
        val pool = CandidatePool()
        pool.addAll(listOf(CandidateEntry(searchQuery = "4 Non Blondes What's Up")))
        val decision = directScheduler.pickNext("some track", pool, PlayHistory())
        assertNotNull(decision)
        assertEquals(listOf("4 Non Blondes What's Up"), decision!!.queries)
        assertFalse(decision.usedCushion)
    }

    @Test
    fun pickNext_insertsTwoBridges_whenBothEndsInCatalog() {
        val pool = CandidatePool()
        pool.addAll(
            listOf(
                CandidateEntry(
                    searchQuery = "Sweet Child O' Mine Guns N Roses",
                    requestedLabel = "Sweet Child O' Mine",
                    catalogTrackId = "sweet_child",
                ),
            ),
        )
        val decision = catalogScheduler.pickNext("몽중인", pool, PlayHistory())
        assertNotNull(decision)
        assertTrue(decision!!.usedCushion)
        assertEquals(2, decision.bridgeCount)
        assertEquals(
            listOf(
                "California Dreamin'",
                "Hotel California Eagles",
                "Sweet Child O' Mine Guns N Roses",
            ),
            decision.queries,
        )
    }

    @Test
    fun pickNext_directWhenNearbyInCatalog() {
        val pool = CandidatePool()
        pool.addAll(
            listOf(
                CandidateEntry(
                    searchQuery = "Hotel California Eagles",
                    catalogTrackId = "hotel_california",
                ),
            ),
        )
        val decision = catalogScheduler.pickNext("California Dreamin'", pool, PlayHistory())
        assertNotNull(decision)
        assertFalse(decision!!.usedCushion)
        assertEquals(listOf("Hotel California Eagles"), decision.queries)
    }

    @Test
    fun pickNext_fallsBackDirect_onDropMismatch() {
        val pool = CandidatePool()
        pool.addAll(
            listOf(
                CandidateEntry(
                    searchQuery = "Synthetic Death Core",
                    catalogTrackId = "death_metal_extreme",
                ),
            ),
        )
        val decision = catalogScheduler.pickNext("몽중인", pool, PlayHistory())
        assertNotNull(decision)
        assertFalse(decision!!.usedCushion)
        assertEquals(listOf("Synthetic Death Core"), decision.queries)
    }

    private fun readResource(name: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(name)
            ?: error("Missing test resource: $name")
        return stream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
    }
}
