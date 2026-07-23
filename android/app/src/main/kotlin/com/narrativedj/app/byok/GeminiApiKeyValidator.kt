package com.narrativedj.app.byok

/**
 * Local usability checks for BYOK Gemini keys (production gate + debug seeder).
 *
 * Purpose: treat blank / short / known harness placeholders as "no key"
 * so [GeminiKeyGateActivity] and live QA never proceed with fixtures like `test-key-123`.
 *
 * Does not call the network — API auth is verified when Gemini requests run.
 * Harness: [GeminiApiKeyValidatorTest] (JVM).
 */
object GeminiApiKeyValidator {

    private const val MIN_LENGTH = 20

    /** Exact matches used by instrumentation / docs — must stay rejected in production. */
    private val blockedExact = setOf(
        "test-key-123",
        "gemini-key",
        "your_api_key",
        "your-api-key",
        "changeme",
        "placeholder",
        "api_key",
        "apikey",
        "xxx",
        "test",
        "dummy",
        "sample",
    )

    fun isUsable(apiKey: String?): Boolean {
        val key = apiKey?.trim().orEmpty()
        if (key.length < MIN_LENGTH) return false
        if (key.lowercase() in blockedExact) return false
        if (looksLikePlaceholder(key)) return false
        return key.any { it.isLetterOrDigit() }
    }

    private fun looksLikePlaceholder(key: String): Boolean {
        val lower = key.lowercase()
        return (lower.contains("your") && lower.contains("key")) ||
            lower.startsWith("test-key") ||
            lower.startsWith("dummy") ||
            lower.startsWith("example")
    }
}
