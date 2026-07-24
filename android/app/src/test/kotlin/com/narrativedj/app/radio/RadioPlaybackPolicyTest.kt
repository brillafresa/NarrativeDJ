/**
 * JVM harness: Idle / Live / PausedUser / StalePaused radio occupancy.
 * Run: cd android && ./gradlew testDebugUnitTest --tests com.narrativedj.app.radio.RadioPlaybackPolicyTest
 */
package com.narrativedj.app.radio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioPlaybackPolicyTest {
    @Test
    fun defers_whenLiveOrPausedOrStale() {
        assertTrue(
            RadioPlaybackPolicy.shouldDeferPlayback(
                isPlayingSequence = false,
                hasPendingEntry = false,
                phase = RadioPlaybackPhase.LIVE,
            ),
        )
        assertTrue(
            RadioPlaybackPolicy.shouldDeferPlayback(
                isPlayingSequence = false,
                hasPendingEntry = false,
                phase = RadioPlaybackPhase.PAUSED_USER,
            ),
        )
        assertTrue(
            RadioPlaybackPolicy.shouldDeferPlayback(
                isPlayingSequence = false,
                hasPendingEntry = false,
                phase = RadioPlaybackPhase.STALE_PAUSED,
            ),
        )
    }

    @Test
    fun starts_whenIdle() {
        assertFalse(
            RadioPlaybackPolicy.shouldDeferPlayback(
                isPlayingSequence = false,
                hasPendingEntry = false,
                phase = RadioPlaybackPhase.IDLE,
            ),
        )
    }

    @Test
    fun defers_whileSearchSequenceOrPending() {
        assertTrue(
            RadioPlaybackPolicy.shouldDeferPlayback(
                isPlayingSequence = true,
                hasPendingEntry = false,
                phase = RadioPlaybackPhase.IDLE,
            ),
        )
        assertTrue(
            RadioPlaybackPolicy.shouldDeferPlayback(
                isPlayingSequence = false,
                hasPendingEntry = true,
                phase = RadioPlaybackPhase.IDLE,
            ),
        )
    }

    @Test
    fun playingPoll_entersLiveAndConfirms() {
        val update = RadioPlaybackPolicy.nextPhase(
            previousPhase = RadioPlaybackPhase.IDLE,
            confirmedPlaying = false,
            hasMetadata = true,
            isPlaying = true,
            idleMissCount = 3,
        )
        assertEquals(RadioPlaybackPhase.LIVE, update.phase)
        assertTrue(update.confirmedPlaying)
        assertEquals(0, update.idleMissCount)
        assertFalse(update.released)
        assertFalse(update.needsStaleResume)
    }

    @Test
    fun liveThenNotPlayingWithMetadata_becomesPausedUser() {
        val update = RadioPlaybackPolicy.nextPhase(
            previousPhase = RadioPlaybackPhase.LIVE,
            confirmedPlaying = true,
            hasMetadata = true,
            isPlaying = false,
            idleMissCount = 0,
        )
        assertEquals(RadioPlaybackPhase.PAUSED_USER, update.phase)
        assertTrue(update.confirmedPlaying)
        assertFalse(update.released)
        assertTrue(
            RadioPlaybackPolicy.shouldDeferPlayback(
                isPlayingSequence = false,
                hasPendingEntry = false,
                phase = update.phase,
            ),
        )
    }

    @Test
    fun pausedUser_metadataGap_releasesAfterThreshold() {
        val first = RadioPlaybackPolicy.nextPhase(
            previousPhase = RadioPlaybackPhase.PAUSED_USER,
            confirmedPlaying = true,
            hasMetadata = false,
            isPlaying = false,
            idleMissCount = 0,
            idleMissThreshold = 2,
        )
        assertTrue(first.confirmedPlaying)
        assertFalse(first.released)
        assertTrue(first.phase != RadioPlaybackPhase.IDLE)

        val second = RadioPlaybackPolicy.nextPhase(
            previousPhase = first.phase,
            confirmedPlaying = true,
            hasMetadata = false,
            isPlaying = false,
            idleMissCount = first.idleMissCount,
            idleMissThreshold = 2,
        )
        assertEquals(RadioPlaybackPhase.IDLE, second.phase)
        assertFalse(second.confirmedPlaying)
        assertTrue(second.released)
    }

    @Test
    fun coldMetadataWithoutPlaying_isStalePaused_needsResume() {
        val update = RadioPlaybackPolicy.nextPhase(
            previousPhase = RadioPlaybackPhase.IDLE,
            confirmedPlaying = false,
            hasMetadata = true,
            isPlaying = false,
            idleMissCount = 0,
            staleResumeAttempted = false,
        )
        assertEquals(RadioPlaybackPhase.STALE_PAUSED, update.phase)
        assertTrue(update.needsStaleResume)
        assertFalse(update.confirmedPlaying)
        assertTrue(
            RadioPlaybackPolicy.shouldDeferPlayback(
                isPlayingSequence = false,
                hasPendingEntry = false,
                phase = update.phase,
            ),
        )
    }

    @Test
    fun coldMetadataAfterResumeAttempt_becomesIdle_allowsSchedule() {
        val update = RadioPlaybackPolicy.nextPhase(
            previousPhase = RadioPlaybackPhase.STALE_PAUSED,
            confirmedPlaying = false,
            hasMetadata = true,
            isPlaying = false,
            idleMissCount = 0,
            staleResumeAttempted = true,
        )
        assertEquals(RadioPlaybackPhase.IDLE, update.phase)
        assertFalse(update.needsStaleResume)
        assertFalse(
            RadioPlaybackPolicy.shouldDeferPlayback(
                isPlayingSequence = false,
                hasPendingEntry = false,
                phase = update.phase,
            ),
        )
    }

    @Test
    fun browsingWithoutConfirm_noMetadata_staysIdle() {
        val update = RadioPlaybackPolicy.nextPhase(
            previousPhase = RadioPlaybackPhase.IDLE,
            confirmedPlaying = false,
            hasMetadata = false,
            isPlaying = false,
            idleMissCount = 0,
        )
        assertEquals(RadioPlaybackPhase.IDLE, update.phase)
        assertFalse(update.needsStaleResume)
    }
}
