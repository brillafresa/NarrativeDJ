package com.narrativedj.app.radio

/**
 * LLM (or harness) plan: pick most-similar pool track B vs current A,
 * and optionally invent bridge search queries C when similarity is low.
 *
 * Similarity is 0.0–1.0 (higher = closer). If similarity >= [SIMILARITY_THRESHOLD],
 * play B directly; otherwise play bridge queries then B.
 *
 * Verify: `./gradlew test --tests com.narrativedj.app.radio.CushionBridgePlanParserTest`
 */
data class CushionBridgePlan(
    val selectedSearchQuery: String,
    val similarity: Double,
    val bridgeSearchQueries: List<String>,
    val reason: String? = null,
) {
    fun bridgesForPlayback(threshold: Double = SIMILARITY_THRESHOLD): List<String> {
        if (similarity >= threshold) return emptyList()
        return bridgeSearchQueries
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(MAX_BRIDGES)
    }

    companion object {
        const val SIMILARITY_THRESHOLD = 0.55
        const val MAX_BRIDGES = 2
    }
}
