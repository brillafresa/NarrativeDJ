/**
 * JVM harness: Gemini BYOK key usability (reject placeholders / short dummies).
 *
 * Purpose: lock gate/seeder behavior so instrumentation fixtures cannot pass as live keys.
 * Run: cd android && ./gradlew test --tests com.narrativedj.app.byok.GeminiApiKeyValidatorTest
 */
package com.narrativedj.app.byok

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiApiKeyValidatorTest {

    @Test
    fun rejectsBlankAndShort() {
        assertFalse(GeminiApiKeyValidator.isUsable(null))
        assertFalse(GeminiApiKeyValidator.isUsable(""))
        assertFalse(GeminiApiKeyValidator.isUsable("   "))
        assertFalse(GeminiApiKeyValidator.isUsable("short-key"))
    }

    @Test
    fun rejectsInstrumentationAndPlaceholderFixtures() {
        assertFalse(GeminiApiKeyValidator.isUsable("test-key-123"))
        assertFalse(GeminiApiKeyValidator.isUsable("gemini-key"))
        assertFalse(GeminiApiKeyValidator.isUsable("YOUR_API_KEY_GOES_HERE_NOW"))
        assertFalse(GeminiApiKeyValidator.isUsable("test-key-abcdefghijklmnopqrstuvwxyz"))
        assertFalse(GeminiApiKeyValidator.isUsable("dummy-key-for-local-testing"))
    }

    @Test
    fun acceptsLikelyRealKeys() {
        assertTrue(
            GeminiApiKeyValidator.isUsable("AIzaSyA-realLookingHarnessOnlyKeyValue99"),
        )
        assertTrue(
            GeminiApiKeyValidator.isUsable("AQ.SyntheticHarnessOnlyKeyValue000001"),
        )
    }
}
