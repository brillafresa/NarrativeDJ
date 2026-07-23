/**
 * JVM harness: RadioScheduler — pool pick + apply LLM cushion plan (no catalog).
 * Run: cd android && ./gradlew testDebugUnitTest --tests com.narrativedj.app.radio.RadioSchedulerTest
 */
package com.narrativedj.app.radio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
    fun pickNext_directSearchQuery_withoutCurrent() {
        val pool = CandidatePool()
        pool.addAll(listOf(CandidateEntry(searchQuery = "4 Non Blondes What's Up")))
        val decision = scheduler.pickNext(null, pool, PlayHistory())
        assertNotNull(decision)
        assertEquals(listOf("4 Non Blondes What's Up"), decision!!.queries)
        assertFalse(decision.usedCushion)
    }

    @Test
    fun decisionFromPlan_insertsBridgesWhenSimilarityLow() {
        val candidates = listOf(
            CandidateEntry(searchQuery = "Hotel California Eagles", requestedLabel = "Hotel California"),
        )
        val plan = CushionBridgePlan(
            selectedSearchQuery = "Hotel California Eagles",
            similarity = 0.4,
            bridgeSearchQueries = listOf("Eagles Take It Easy", "Fleetwood Mac Dreams"),
        )
        val decision = scheduler.decisionFromPlan(plan, candidates)
        assertNotNull(decision)
        assertTrue(decision!!.usedCushion)
        assertEquals(2, decision.bridgeCount)
        assertEquals(
            listOf(
                "Eagles Take It Easy",
                "Fleetwood Mac Dreams",
                "Hotel California Eagles",
            ),
            decision.queries,
        )
    }

    @Test
    fun decisionFromPlan_directWhenSimilarityHigh() {
        val candidates = listOf(
            CandidateEntry(searchQuery = "California Dreamin'"),
        )
        val plan = CushionBridgePlan(
            selectedSearchQuery = "California Dreamin'",
            similarity = 0.9,
            bridgeSearchQueries = listOf("should be ignored"),
        )
        val decision = scheduler.decisionFromPlan(plan, candidates)
        assertNotNull(decision)
        assertFalse(decision!!.usedCushion)
        assertEquals(listOf("California Dreamin'"), decision.queries)
    }
}
