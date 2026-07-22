/**
 * JVM harness: RadioScheduler — pool pick and direct YTM search playback.
 * Run: cd android && ./gradlew test --tests com.narrativedj.app.radio.RadioSchedulerTest
 */
package com.narrativedj.app.radio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RadioSchedulerTest {

    private val scheduler = RadioScheduler()

    @Test
    fun pickImmediate_skipsRecentlyPlayed() {
        val pool = CandidatePool()
        pool.addAll(
            listOf(
                CandidateEntry(searchQuery = "California Dreamin'"),
                CandidateEntry(searchQuery = "Hotel California Eagles"),
            ),
        )
        val history = PlayHistory()
        history.record("california dreamin'")
        val decision = scheduler.pickImmediate(pool, history)
        assertNotNull(decision)
        assertEquals(listOf("Hotel California Eagles"), decision!!.queries)
    }

    @Test
    fun pickNext_playsDirectSearchQuery() {
        val pool = CandidatePool()
        pool.addAll(listOf(CandidateEntry(searchQuery = "4 Non Blondes What's Up")))
        val decision = scheduler.pickNext("some track", pool, PlayHistory())
        assertNotNull(decision)
        assertEquals(listOf("4 Non Blondes What's Up"), decision!!.queries)
    }
}
