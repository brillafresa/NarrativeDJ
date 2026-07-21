/**
 * JVM harness: candidate pool dedupe by normalized search query.
 * Run: cd android && ./gradlew test --tests com.narrativedj.app.radio.CandidatePoolTest
 */
package com.narrativedj.app.radio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CandidatePoolTest {

    @Test
    fun addAll_dedupesBySearchQuery() {
        val pool = CandidatePool()
        val first = CandidateEntry(searchQuery = "Hotel California")
        val duplicate = CandidateEntry(searchQuery = "hotel california")
        val added = pool.addAll(listOf(first, duplicate))
        assertEquals(1, added)
        assertEquals(1, pool.size())
    }

    @Test
    fun removeByPlayKey() {
        val pool = CandidatePool()
        pool.addAll(listOf(CandidateEntry(searchQuery = "Track A")))
        pool.removeByPlayKey("track a")
        assertTrue(pool.isEmpty())
    }
}
