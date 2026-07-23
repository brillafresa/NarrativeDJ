/**
 * JVM harness: pool resolution for LLM cushion plans.
 * Run: cd android && ./gradlew testDebugUnitTest --tests com.narrativedj.app.radio.CushionBridgePlannerServiceTest
 */
package com.narrativedj.app.radio

import org.junit.Assert.assertEquals
import org.junit.Test

class CushionBridgePlannerServiceTest {

    @Test
    fun resolveAgainstPool_normalizesSelectedQuery() {
        val candidates = listOf(
            CandidateEntry(searchQuery = "Hotel California Eagles", requestedLabel = "Hotel California"),
        )
        val plan = CushionBridgePlan(
            selectedSearchQuery = "hotel california eagles",
            similarity = 0.5,
            bridgeSearchQueries = emptyList(),
        )
        val resolved = CushionBridgePlannerService.resolveAgainstPool(plan, candidates)
        assertEquals("Hotel California Eagles", resolved.selectedSearchQuery)
    }

    @Test(expected = IllegalStateException::class)
    fun resolveAgainstPool_rejectsUnknownSelection() {
        CushionBridgePlannerService.resolveAgainstPool(
            CushionBridgePlan(
                selectedSearchQuery = "not in pool",
                similarity = 0.5,
                bridgeSearchQueries = emptyList(),
            ),
            listOf(CandidateEntry(searchQuery = "Hotel California Eagles")),
        )
    }
}
