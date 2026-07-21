/**
 * JVM harness: 20-track play history ring buffer (selection-time skip).
 * Run: cd android && ./gradlew test --tests com.narrativedj.app.radio.PlayHistoryTest
 */
package com.narrativedj.app.radio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayHistoryTest {

    @Test
    fun ringBuffer_evictsOldestAfter20() {
        val history = PlayHistory(capacity = 20)
        repeat(20) { i -> history.record("track$i") }
        assertTrue(history.wasRecentlyPlayed("track0"))
        history.record("track20")
        assertFalse(history.wasRecentlyPlayed("track0"))
        assertTrue(history.wasRecentlyPlayed("track20"))
    }

    @Test
    fun wasRecentlyPlayed_normalizesKeys() {
        val history = PlayHistory()
        history.record("Hotel California")
        assertTrue(history.wasRecentlyPlayed("hotel california"))
    }
}
