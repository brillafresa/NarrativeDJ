/**
 * JVM harness: queue-after-current deferral policy for radio scheduling.
 * Run: cd android && ./gradlew test --tests com.narrativedj.app.radio.RadioPlaybackPolicyTest
 */
package com.narrativedj.app.radio

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
}
