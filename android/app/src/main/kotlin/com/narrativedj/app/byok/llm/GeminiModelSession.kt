package com.narrativedj.app.byok.llm

/**
 * Process-lifetime sticky Gemini model override (not persisted).
 *
 * Purpose: after a 503 capacity failure, keep using the fallback model until
 * process exit or an explicit [clearSticky] (e.g. user picks another model).
 * Verify: `./gradlew test --tests com.narrativedj.app.byok.llm.GeminiModelSessionTest`
 */
class GeminiModelSession(
    private val preferredModel: () -> String,
) {
    @Volatile
    private var stickyModel: String? = null

    private val exhausted = linkedSetOf<String>()

    fun current(): String = stickyModel ?: GeminiModelCatalog.resolve(preferredModel())

    fun clearSticky() {
        stickyModel = null
        exhausted.clear()
    }

    fun hasSticky(): Boolean = stickyModel != null

    /**
     * Mark [current] as capacity-exhausted and stick to the next allow-listed model.
     * @return new model id, or null if every allow-listed model was already tried
     */
    @Synchronized
    fun advanceAfterCapacityError(): String? {
        exhausted.add(current())
        val next = GeminiModelCatalog.capacityFallbacksAfter(current())
            .firstOrNull { it !in exhausted }
            ?: GeminiModelCatalog.ids().firstOrNull { it !in exhausted }
            ?: return null
        stickyModel = next
        return next
    }
}
