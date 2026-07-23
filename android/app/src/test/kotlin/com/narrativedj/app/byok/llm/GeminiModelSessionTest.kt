/**
 * JVM harness: session sticky model after 503 capacity fallback.
 *
 * Purpose: Prove 503 advances allow-list model, sticks until clearSticky, and never loops.
 * Run: cd android && ./gradlew testDebugUnitTest --tests com.narrativedj.app.byok.llm.GeminiModelSessionTest
 */
package com.narrativedj.app.byok.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiModelSessionTest {

    @Test
    fun current_usesPreferredUntilSticky() {
        val session = GeminiModelSession { "gemini-3.5-flash" }
        assertEquals("gemini-3.5-flash", session.current())
        assertFalse(session.hasSticky())
    }

    @Test
    fun advanceAfterCapacityError_sticksUntilCleared() {
        var preferred = "gemini-3.5-flash-lite"
        val session = GeminiModelSession { preferred }
        assertEquals("gemini-3.1-flash-lite", session.advanceAfterCapacityError())
        assertTrue(session.hasSticky())
        preferred = "gemini-2.5-flash"
        // Sticky wins over preferred until cleared
        assertEquals("gemini-3.1-flash-lite", session.current())
        session.clearSticky()
        assertEquals("gemini-2.5-flash", session.current())
    }

    @Test
    fun withCapacityFallback_retriesOn503AndSticks() {
        val session = GeminiModelSession { "gemini-3.5-flash-lite" }
        val attempts = mutableListOf<String>()
        val fallbacks = mutableListOf<Pair<String, String>>()
        val result = GeminiApi.withCapacityFallback(
            session = session,
            onCapacityFallback = { from, to -> fallbacks.add(from to to) },
        ) { model ->
            attempts.add(model)
            if (model == "gemini-3.5-flash-lite") {
                throw GeminiHttpException(503, "Gemini 503: overloaded")
            }
            "ok:$model"
        }
        assertEquals("ok:gemini-3.1-flash-lite", result)
        assertEquals(
            listOf("gemini-3.5-flash-lite", "gemini-3.1-flash-lite"),
            attempts,
        )
        assertEquals(
            listOf("gemini-3.5-flash-lite" to "gemini-3.1-flash-lite"),
            fallbacks,
        )
        assertEquals("gemini-3.1-flash-lite", session.current())
    }

    @Test
    fun withCapacityFallback_doesNotRetryNon503() {
        val session = GeminiModelSession { "gemini-3.5-flash-lite" }
        val thrown = assertThrows(GeminiHttpException::class.java) {
            GeminiApi.withCapacityFallback(session) {
                throw GeminiHttpException(404, "Gemini 404: not found")
            }
        }
        assertEquals(404, thrown.statusCode)
        assertFalse(session.hasSticky())
    }

    @Test
    fun withCapacityFallback_exhaustsChain() {
        val session = GeminiModelSession { "gemini-3.5-flash" }
        var calls = 0
        val thrown = assertThrows(GeminiHttpException::class.java) {
            GeminiApi.withCapacityFallback(session) {
                calls++
                throw GeminiHttpException(503, "Gemini 503")
            }
        }
        assertEquals(503, thrown.statusCode)
        assertEquals(GeminiModelCatalog.ids().size, calls)
    }
}
