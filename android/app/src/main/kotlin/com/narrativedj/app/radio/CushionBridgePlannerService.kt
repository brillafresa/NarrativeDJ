package com.narrativedj.app.radio

import com.narrativedj.app.byok.SecureKeyStore
import com.narrativedj.app.byok.llm.CushionBridgeContext
import com.narrativedj.app.byok.llm.CushionBridgePromptBuilder
import com.narrativedj.app.byok.llm.GeminiApi
import com.narrativedj.app.byok.llm.LlmResponseExtractor
import com.narrativedj.app.locale.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Asks Gemini to pick the most-similar pool track and optional invented bridge search queries.
 */
class CushionBridgePlannerService(
    private val keyStore: SecureKeyStore,
) {
    suspend fun plan(
        currentTrackLabel: String,
        candidates: List<CandidateEntry>,
        language: AppLanguage,
    ): CushionBridgePlan {
        if (candidates.isEmpty()) {
            throw IllegalStateException("No candidates for cushion plan")
        }
        val apiKey = keyStore.getGeminiApiKey()
            ?: throw IllegalStateException("Gemini API key required")
        return withContext(Dispatchers.IO) {
            val prompt = CushionBridgePromptBuilder.build(
                CushionBridgeContext(
                    currentTrackLabel = currentTrackLabel,
                    candidates = candidates,
                    language = language,
                ),
            )
            val text = GeminiApi.generateText(
                apiKey = apiKey,
                prompt = prompt,
                jsonMimeType = true,
            )
            val plan = CushionBridgePlanParser.parseJson(
                LlmResponseExtractor.extractJsonPayload(text),
            ) ?: throw IllegalStateException("Gemini cushion plan was not valid JSON")
            resolveAgainstPool(plan, candidates)
        }
    }

    companion object {
        /**
         * Ensure selected_search_query maps to a pool entry (exact or normalized match).
         */
        fun resolveAgainstPool(
            plan: CushionBridgePlan,
            candidates: List<CandidateEntry>,
        ): CushionBridgePlan {
            val match = candidates.firstOrNull {
                CandidateEntry.normalizeKey(it.searchQuery) ==
                    CandidateEntry.normalizeKey(plan.selectedSearchQuery)
            } ?: candidates.firstOrNull {
                val label = it.requestedLabel ?: return@firstOrNull false
                CandidateEntry.normalizeKey(label) ==
                    CandidateEntry.normalizeKey(plan.selectedSearchQuery)
            } ?: throw IllegalStateException(
                "Cushion plan selected a track not in the pool: ${plan.selectedSearchQuery}",
            )
            return plan.copy(selectedSearchQuery = match.searchQuery)
        }
    }
}
