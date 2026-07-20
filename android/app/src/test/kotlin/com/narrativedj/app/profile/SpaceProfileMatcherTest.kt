package com.narrativedj.app.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceProfileMatcherTest {

    @Test
    fun cozyBrunchCafe_acceptsInRangeTrack() {
        val matches = SpaceProfileMatcher.filterTracks(
            SpaceProfiles.cozyBrunchCafe,
            listOf(
                TrackMetrics("a", bpm = 95.0, energy = 0.42),
                TrackMetrics("b", bpm = 130.0, energy = 0.42),
            ),
        )
        assertEquals(1, matches.size)
        assertEquals("a", matches.first().id)
    }

    @Test
    fun quietBookstore_rejectsHighEnergyTrack() {
        val matches = SpaceProfileMatcher.filterTracks(
            SpaceProfiles.quietBookstore,
            listOf(TrackMetrics("loud", bpm = 80.0, energy = 0.55)),
        )
        assertTrue(matches.isEmpty())
    }

    @Test
    fun allProfiles_hasThreeTemplates() {
        assertEquals(3, SpaceProfiles.all.size)
    }
}
