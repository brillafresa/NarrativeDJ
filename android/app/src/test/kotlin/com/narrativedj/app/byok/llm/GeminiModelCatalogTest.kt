/**
 * JVM harness: Gemini model allow-list + default resolution.
 *
 * Purpose: Keep menu picker / DEFAULT_MODEL / capacity-fallback order in sync with production.
 * Run: cd android && ./gradlew testDebugUnitTest --tests com.narrativedj.app.byok.llm.GeminiModelCatalogTest
 */
package com.narrativedj.app.byok.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiModelCatalogTest {

    @Test
    fun default_isFlashLite() {
        assertEquals("gemini-3.5-flash-lite", GeminiModelCatalog.DEFAULT_MODEL)
        assertEquals(GeminiModelCatalog.DEFAULT_MODEL, GeminiApi.DEFAULT_MODEL)
    }

    @Test
    fun resolve_unknown_fallsBackToDefault() {
        assertEquals(
            GeminiModelCatalog.DEFAULT_MODEL,
            GeminiModelCatalog.resolve("gemini-1.5-flash"),
        )
        assertEquals(
            GeminiModelCatalog.DEFAULT_MODEL,
            GeminiModelCatalog.resolve(null),
        )
    }

    @Test
    fun resolve_keepsAllowedIds() {
        assertTrue(GeminiModelCatalog.isAllowed("gemini-2.5-flash"))
        assertEquals("gemini-2.5-flash", GeminiModelCatalog.resolve("gemini-2.5-flash"))
        assertFalse(GeminiModelCatalog.isAllowed("not-a-model"))
    }

    @Test
    fun indexOf_defaultIsZero() {
        assertEquals(0, GeminiModelCatalog.indexOf(GeminiModelCatalog.DEFAULT_MODEL))
    }

    @Test
    fun capacityFallbacksAfter_defaultWalksDownAllowList() {
        assertEquals(
            listOf(
                "gemini-3.1-flash-lite",
                "gemini-2.5-flash-lite",
                "gemini-2.5-flash",
                "gemini-3.5-flash",
            ),
            GeminiModelCatalog.capacityFallbacksAfter("gemini-3.5-flash-lite"),
        )
    }

    @Test
    fun capacityFallbacksAfter_heaviestWalksBackTowardLighter() {
        assertEquals(
            listOf(
                "gemini-2.5-flash",
                "gemini-2.5-flash-lite",
                "gemini-3.1-flash-lite",
                "gemini-3.5-flash-lite",
            ),
            GeminiModelCatalog.capacityFallbacksAfter("gemini-3.5-flash"),
        )
    }
}
