package com.narrativedj.app.scheduler

import com.narrativedj.app.R
import com.narrativedj.app.profile.SpaceProfile
import com.narrativedj.app.profile.SpaceProfiles
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
        val wideProfile = SpaceProfile(
            id = "harness_wide",
            labelResId = R.string.profile_cozy_brunch,
            bpmMin = 0,
            bpmMax = 999,
            energyMin = 0.0,
            energyMax = 1.0,
            mood = "Test",
        )
        val plan = requireNotNull(
            planner.planRoute("mongjungin", "sweet_child", wideProfile),
        )
        assertEquals(listOf("california_dreamin", "hotel_california"), plan.bridgeIds)
        assertEquals(false, plan.dropped)
    }

    @Test
    fun suggestTarget_returnsProfileCompatibleTrack() {
        val target = planner.suggestTarget("mongjungin", SpaceProfiles.analogLpBar)
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
