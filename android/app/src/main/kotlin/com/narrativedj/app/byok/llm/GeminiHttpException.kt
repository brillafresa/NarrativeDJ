package com.narrativedj.app.byok.llm

/**
 * HTTP failure from Gemini generateContent.
 *
 * Purpose: distinguish capacity (503) for session model fallback retries.
 * Verify: `./gradlew test --tests com.narrativedj.app.byok.llm.GeminiApiTest`
 */
class GeminiHttpException(
    val statusCode: Int,
    message: String,
) : IllegalStateException(message) {
    val isCapacityUnavailable: Boolean
        get() = statusCode == 503
}
