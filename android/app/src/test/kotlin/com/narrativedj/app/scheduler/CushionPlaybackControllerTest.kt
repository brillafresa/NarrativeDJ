package com.narrativedj.app.scheduler

/**
 * JVM harness: cushion route → YTM search/play sequence planning.
 * Fixture: src/test/resources/mock_tracks.json + harness/tests/mock_cushion_playback.json (canonical order)
 * Run: cd android && ./gradlew test --tests com.narrativedj.app.scheduler.CushionPlaybackControllerTest
 */
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.charset.StandardCharsets

class CushionPlaybackControllerTest {

    private lateinit var catalog: List<CatalogTrack>
    private lateinit var planner: CushionRoutePlanner

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
            planner.planRoute("mongjungin", "sweet_child"),
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
