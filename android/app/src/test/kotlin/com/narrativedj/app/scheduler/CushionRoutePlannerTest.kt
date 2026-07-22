package com.narrativedj.app.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.nio.charset.StandardCharsets

class CushionRoutePlannerTest {

    private lateinit var planner: CushionRoutePlanner

    @Before
    fun setUp() {
        val json = readResource("mock_tracks.json")
        val catalog = TrackCatalogLoader.parse(json)
        val vectorDb = catalog.associate { track ->
            track.id to CushionMusicScheduler.trackToVector(
                track.bpm,
                track.energy,
                track.valence,
                track.embedding,
            )
        }
        planner = CushionRoutePlanner(CushionMusicScheduler(vectorDb), catalog)
    }

    @Test
    fun resolveTrackId_matchesTitleCaseInsensitive() {
        assertEquals("mongjungin", planner.resolveTrackId("몽중인"))
        assertEquals("california_dreamin", planner.resolveTrackId("California Dreamin'"))
    }

    @Test
    fun planRoute_canonicalTwoBridgePath() {
        val plan = requireNotNull(
            planner.planRoute("mongjungin", "sweet_child"),
        )
        assertEquals(listOf("california_dreamin", "hotel_california"), plan.bridgeIds)
        assertEquals(false, plan.dropped)
    }

    @Test
    fun suggestTarget_returnsCompatibleTrack() {
        val target = planner.suggestTarget("mongjungin")
        assertNotNull(target)
    }

    @Test
    fun resolveTrackId_unknownTitle_returnsNull() {
        assertNull(planner.resolveTrackId("Unknown Song XYZ"))
    }

    private fun readResource(name: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(name)
            ?: error("Missing resource $name")
        return stream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
    }
}
