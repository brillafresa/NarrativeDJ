package com.narrativedj.app.byok.llm

import androidx.annotation.StringRes
import com.narrativedj.app.R

/**
 * Selectable Gemini model ids for NarrativeDJ (request parse, DJ ment, cushion plan).
 *
 * Purpose: harness + Settings menu use the same allow-list; unknown prefs fall back to [DEFAULT_MODEL].
 * Verify: `./gradlew test --tests com.narrativedj.app.byok.llm.GeminiModelCatalogTest`
 */
object GeminiModelCatalog {
    /** Default for production and first launch. */
    const val DEFAULT_MODEL = "gemini-3.5-flash-lite"

    data class Option(
        val id: String,
        @StringRes val labelRes: Int,
    )

    /**
     * Lighter options first, then previous full Flash for A/B during testing.
     * Ids must match Google AI Studio / generativelanguage.googleapis.com model codes.
     */
    val OPTIONS: List<Option> = listOf(
        Option("gemini-3.5-flash-lite", R.string.gemini_model_35_flash_lite),
        Option("gemini-3.1-flash-lite", R.string.gemini_model_31_flash_lite),
        Option("gemini-2.5-flash-lite", R.string.gemini_model_25_flash_lite),
        Option("gemini-2.5-flash", R.string.gemini_model_25_flash),
        Option("gemini-3.5-flash", R.string.gemini_model_35_flash),
    )

    fun ids(): List<String> = OPTIONS.map { it.id }

    fun isAllowed(modelId: String): Boolean = ids().any { it == modelId }

    fun resolve(modelId: String?): String {
        val trimmed = modelId?.trim().orEmpty()
        return if (isAllowed(trimmed)) trimmed else DEFAULT_MODEL
    }

    fun indexOf(modelId: String): Int {
        val resolved = resolve(modelId)
        return ids().indexOf(resolved).coerceAtLeast(0)
    }

    /**
     * Models to try after [failedModel] hits capacity (503).
     * Prefer later allow-list entries; if [failedModel] is last, walk back toward lighter ids.
     */
    fun capacityFallbacksAfter(failedModel: String): List<String> {
        val all = ids()
        val idx = all.indexOf(resolve(failedModel))
        if (idx < 0) return all
        val later = all.drop(idx + 1)
        if (later.isNotEmpty()) return later
        return all.take(idx).asReversed()
    }
}
