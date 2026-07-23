/**
 * JVM harness: waiting-queue marquee formatting.
 * Run: cd android && ./gradlew testDebugUnitTest --tests com.narrativedj.app.radio.WaitingQueueFormatterTest
 */
package com.narrativedj.app.radio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaitingQueueFormatterTest {

    @Test
    fun empty_usesPlaceholder() {
        assertEquals(
            "대기 없음",
            WaitingQueueFormatter.format("대기 신청곡:", emptyList(), "대기 없음"),
        )
    }

    @Test
    fun formats_labelsWithPrefix() {
        val text = WaitingQueueFormatter.format(
            prefix = "대기 신청곡:",
            labels = listOf("Piano Man", "What's Up"),
            emptyPlaceholder = "대기 없음",
        )
        assertTrue(text.startsWith("대기 신청곡:"))
        assertTrue(text.contains("Piano Man"))
        assertTrue(text.contains("What's Up"))
    }

    @Test
    fun labelsFromPool_prefersRequestedLabel() {
        val pool = CandidatePool()
        pool.addAll(
            listOf(
                CandidateEntry(searchQuery = "q1", requestedLabel = "Label One"),
                CandidateEntry(searchQuery = "query two"),
            ),
        )
        assertEquals(listOf("Label One", "query two"), WaitingQueueFormatter.labelsFromPool(pool))
    }
}
