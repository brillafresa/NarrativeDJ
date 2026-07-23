/**
 * JVM harness: GeminiApi error formatting + default model + 503 capacity flag.
 *
 * Purpose: Shared HTTP error surface for BYOK Gemini calls (formatError / GeminiHttpException).
 * Run: cd android && ./gradlew testDebugUnitTest --tests com.narrativedj.app.byok.llm.GeminiApiTest
 */
package com.narrativedj.app.byok.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiApiTest {
    @Test
    fun formatError_extractsMessageFromGeminiPayload() {
        val body = """{"error":{"code":404,"message":"models/gemini-1.5-flash is not found","status":"NOT_FOUND"}}"""
        val message = GeminiApi.formatError(404, body)
        assertEquals("Gemini 404: models/gemini-1.5-flash is not found", message)
    }

    @Test
    fun formatError_fallsBackWhenBodyIsNotJson() {
        val message = GeminiApi.formatError(500, "plain failure")
        assertTrue(message.startsWith("Gemini 500"))
    }

    @Test
    fun defaultModel_isFlashLite() {
        assertEquals("gemini-3.5-flash-lite", GeminiApi.DEFAULT_MODEL)
    }

    @Test
    fun httpException_503IsCapacityUnavailable() {
        val e = GeminiHttpException(503, "Gemini 503")
        assertTrue(e.isCapacityUnavailable)
        assertFalse(GeminiHttpException(429, "Gemini 429").isCapacityUnavailable)
    }
}