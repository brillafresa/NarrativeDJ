/**
 * JVM harness: queue-after-current deferral + sticky occupancy for flaky YTM isPlaying.
 * Run: cd android && ./gradlew test --tests com.narrativedj.app.radio.RadioPlaybackPolicyTest
 */
package com.narrativedj.app.radio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioPlaybackPolicyTest {
    @Test
    fun defers_whenTrackIsPlaying() {
        assertTrue(
            RadioPlaybackPolicy.shouldDeferPlayback(
                isPlayingSequence = false,
                hasPendingEntry = false,
                isNowPlaying = true,
            ),
        )
    }

    @Test
    fun starts_whenIdle() {
        assertFalse(
            RadioPlaybackPolicy.shouldDeferPlayback(
                isPlayingSequence = false,
                hasPendingEntry = false,
                isNowPlaying = false,
            ),
        )
    }

    @Test
    fun defers_whileSearchSequenceOrPending() {
        assertTrue(
            RadioPlaybackPolicy.shouldDeferPlayback(
                isPlayingSequence = true,
                hasPendingEntry = false,
                isNowPlaying = false,
            ),
        )
        assertTrue(
            RadioPlaybackPolicy.shouldDeferPlayback(
                isPlayingSequence = false,
                hasPendingEntry = true,
                isNowPlaying = false,
            ),
        )
    }

    @Test
    fun occupancy_playingPoll_setsOccupiedAndClearsMisses() {
        val update = RadioPlaybackPolicy.nextOccupancy(
            currentlyOccupied = false,
            hasMetadata = true,
            isPlaying = true,
            idleMissCount = 3,
        )
        assertTrue(update.occupied)
        assertEquals(0, update.idleMissCount)
        assertFalse(update.released)
    }

    @Test
    fun occupancy_metadataWhileOccupied_staysEvenIfIsPlayingFalse() {
        // Live QA regression: NewJeans playing, DOM says isPlaying=false → must still defer.
        val update = RadioPlaybackPolicy.nextOccupancy(
            currentlyOccupied = true,
            hasMetadata = true,
            isPlaying = false,
            idleMissCount = 0,
            idleMissThreshold = 2,
        )
        assertTrue(update.occupied)
        assertEquals(0, update.idleMissCount)
        assertFalse(update.released)
        assertTrue(
            RadioPlaybackPolicy.shouldDeferPlayback(
                isPlayingSequence = false,
                hasPendingEntry = false,
                isNowPlaying = update.occupied,
            ),
        )
    }

    @Test
    fun occupancy_metadataGapWhileOccupied_staysStickyUntilThreshold() {
        val first = RadioPlaybackPolicy.nextOccupancy(
            currentlyOccupied = true,
            hasMetadata = false,
            isPlaying = false,
            idleMissCount = 0,
            idleMissThreshold = 2,
        )
        assertTrue(first.occupied)
        assertFalse(first.released)

        val second = RadioPlaybackPolicy.nextOccupancy(
            currentlyOccupied = true,
            hasMetadata = false,
            isPlaying = false,
            idleMissCount = first.idleMissCount,
            idleMissThreshold = 2,
        )
        assertFalse(second.occupied)
        assertTrue(second.released)
    }

    @Test
    fun occupancy_coldMetadataWithoutPlaying_doesNotAcquire() {
        // Browsing YTM library should not block the radio scheduler.
        val update = RadioPlaybackPolicy.nextOccupancy(
            currentlyOccupied = false,
            hasMetadata = true,
            isPlaying = false,
            idleMissCount = 0,
        )
        assertFalse(update.occupied)
        assertFalse(update.released)
    }
}
