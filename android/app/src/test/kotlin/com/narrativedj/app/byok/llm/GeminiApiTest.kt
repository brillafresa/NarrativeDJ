/**
 * JVM harness: GeminiApi error formatting + default model id.
 * Run: cd android && ./gradlew test --tests com.narrativedj.app.byok.llm.GeminiApiTest
 */
package com.narrativedj.app.byok.llm

import org.junit.Assert.assertEquals
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
    fun defaultModel_isCurrentFlashGa() {
        assertEquals("gemini-3.5-flash", GeminiApi.DEFAULT_MODEL)
    }
}
