/**
 * JVM harness: RadioScheduler — pool pick, history skip, cushion routing.
 * Fixture catalog: assets/catalog/demo_tracks.json (synced from mock_tracks.json)
 * Run: cd android && ./gradlew test --tests com.narrativedj.app.radio.RadioSchedulerTest
 */
package com.narrativedj.app.radio

import com.narrativedj.app.R
import com.narrativedj.app.locale.AppLanguage
import com.narrativedj.app.profile.SpaceProfile
import com.narrativedj.app.profile.SpaceProfiles
import com.narrativedj.app.scheduler.CatalogTrack
import com.narrativedj.app.scheduler.CushionMusicScheduler
import com.narrativedj.app.scheduler.CushionRoutePlanner
import com.narrativedj.app.scheduler.TrackCatalogLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.charset.StandardCharsets

class RadioSchedulerTest {

    private lateinit var catalog: List<CatalogTrack>
    private lateinit var planner: CushionRoutePlanner
    private lateinit var scheduler: RadioScheduler

    private val profile = SpaceProfile(
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
        val vectorDb = catalog.associate { track ->
            track.id to CushionMusicScheduler.trackToVector(
                track.bpm,
                track.energy,
                track.valence,
                track.embedding,
            )
        }
        planner = CushionRoutePlanner(CushionMusicScheduler(vectorDb), catalog)
        scheduler = RadioScheduler(planner, catalog)
    }

    @Test
    fun pickImmediate_skipsRecentlyPlayed() {
        val pool = CandidatePool()
        pool.addAll(
            listOf(
                CandidateEntry(
                    searchQuery = "California Dreamin'",
                    catalogTrackId = "california_dreamin",
                ),
                CandidateEntry(
                    searchQuery = "Hotel California Eagles",
                    catalogTrackId = "hotel_california",
                ),
            ),
        )
        val history = PlayHistory()
        history.record("california_dreamin")
        val decision = scheduler.pickImmediate(pool, history, SpaceProfiles.cozyBrunchCafe)
        assertNotNull(decision)
        assertTrue(decision!!.queries.first().contains("Hotel California"))
    }

    @Test
    fun pickNext_fromPoolWhenCurrentKnown() {
        val pool = CandidatePool()
        pool.addAll(
            listOf(
                CandidateEntry(
                    searchQuery = "Sweet Child O' Mine",
                    catalogTrackId = "sweet_child",
                ),
            ),
        )
        val decision = scheduler.pickNext("mongjungin", pool, PlayHistory(), profile)
        assertNotNull(decision)
        assertTrue(decision!!.queries.isNotEmpty())
    }

    @Test
    fun parseLocal_explicitTrack() {
        val result = UserRequestParser.parseLocal(
            "California Dreamin' 틀어줘",
            catalog,
            planner,
            profile,
            AppLanguage.KOREAN,
        )
        assertTrue(result.tracks.isNotEmpty())
        assertEquals("California Dreamin'", result.tracks.first().requestedTitle)
    }

    private fun readResource(name: String): String {
        val stream = javaClass.classLoader.getResourceAsStream(name)
            ?: error("Missing resource $name")
        return stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }
}
