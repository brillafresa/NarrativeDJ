package com.narrativedj.app.scheduler

import com.narrativedj.app.R
import com.narrativedj.app.profile.SpaceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.charset.StandardCharsets

class CushionPlaybackControllerTest {

    private lateinit var catalog: List<CatalogTrack>
    private lateinit var planner: CushionRoutePlanner

    private val wideProfile = SpaceProfile(
        id = "harness_wide",
        labelResId = R.string.profile_cozy_brunch,
        bpmMin = 0,
        bpmMax = 999,
        energyMin = 0.0,
        energyMax = 1.0,
        mood = "Test",
    )

    @Before
    fun setUp() {
        catalog = TrackCatalogLoader.parse(readResource("mock_tracks.json"))
        planner = CushionRoutePlanner(
            CushionMusicScheduler(
                catalog.associate { track ->
                    track.id to CushionMusicScheduler.trackToVector(
                        track.bpm,
                        track.energy,
                        track.valence,
                        track.embedding,
                    )
                },
            ),
            catalog,
        )
    }

    @Test
    fun buildPlayOrder_canonicalTwoBridgeRoute() {
        val plan = requireNotNull(
            planner.planRoute("mongjungin", "sweet_child", wideProfile),
        )
        assertEquals(listOf("california_dreamin", "hotel_california"), plan.bridgeIds)

        val controller = CushionPlaybackController(catalog = catalog)
        val order = controller.buildPlayOrder(plan)
        assertTrue(order.any { it.contains("California Dreamin", ignoreCase = true) })
        assertTrue(order.any { it.contains("Hotel California", ignoreCase = true) })
        assertTrue(order.any { it.contains("Sweet Child", ignoreCase = true) })
        assertEquals(3, order.size)
    }

    @Test
    fun searchQueryFor_fallsBackToTitle() {
        val controller = CushionPlaybackController(catalog = catalog)
        val query = requireNotNull(controller.searchQueryFor("mongjungin"))
        assertTrue(query.contains("몽중인"))
    }

    private fun readResource(name: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(name)
            ?: error("Missing resource $name")
        return stream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
    }
}
