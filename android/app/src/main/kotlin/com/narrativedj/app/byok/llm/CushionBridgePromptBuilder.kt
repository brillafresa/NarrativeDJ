package com.narrativedj.app.byok.llm

import com.narrativedj.app.locale.AppLanguage
import com.narrativedj.app.radio.CandidateEntry
import com.narrativedj.app.radio.CushionBridgePlan

data class CushionBridgeContext(
    val currentTrackLabel: String,
    val candidates: List<CandidateEntry>,
    val language: AppLanguage,
    val similarityThreshold: Double = CushionBridgePlan.SIMILARITY_THRESHOLD,
)

/**
 * Prompt: pick most-similar pool track B vs now-playing A; invent YTM bridge queries if needed.
 * No fixed catalog — bridges are invented search strings only.
 */
object CushionBridgePromptBuilder {
    fun build(context: CushionBridgeContext): String {
        val poolLines = context.candidates.mapIndexed { index, entry ->
            val label = entry.requestedLabel?.takeIf { it.isNotBlank() } ?: entry.searchQuery
            "$index. search_query=${entry.searchQuery} | label=$label"
        }.joinToString("\n")

        return when (context.language) {
            AppLanguage.KOREAN -> """
                당신은 라디오 DJ의 쿠션(브릿지) 스케줄러입니다. JSON만 반환하세요.
                고정 곡 카탈로그는 없습니다. YouTube Music 검색어만 사용합니다.

                현재 재생 중(A): ${context.currentTrackLabel.trim()}

                후보 풀(B는 반드시 이 목록에서만 고르세요):
                $poolLines

                규칙:
                1) 풀에서 A와 음악적으로 가장 유사한 곡을 하나 고르고 selected_search_query에 그 search_query를 그대로 넣으세요.
                2) similarity는 0.0~1.0 (1=거의 같음, 0=전혀 다름).
                3) similarity >= ${context.similarityThreshold} 이면 충분히 유사 → bridge_search_queries는 [].
                4) similarity < ${context.similarityThreshold} 이면 A와 B 사이를 잇는 쿠션곡 검색어를 1~2개 bridge_search_queries에 넣으세요.
                   각 항목은 실제 YT Music 검색 가능한 문자열(아티스트+곡 또는 분위기 검색어).
                   A→쿠션→B 순으로 들으면 자연스럽도록 고르세요. B 자체는 풀 밖에서 만들지 마세요.
                5) reason은 한 줄 설명(선택).

                JSON 키: selected_search_query, similarity, bridge_search_queries, reason
            """.trimIndent()
            AppLanguage.ENGLISH -> """
                You are a radio DJ cushion (bridge) scheduler. Return ONLY JSON.
                There is no fixed song catalog — use YouTube Music search queries only.

                Now playing (A): ${context.currentTrackLabel.trim()}

                Candidate pool (B MUST be chosen from this list only):
                $poolLines

                Rules:
                1) Pick the pool track musically closest to A; set selected_search_query to that entry's search_query verbatim.
                2) similarity is 0.0–1.0 (1=nearly same vibe, 0=unrelated).
                3) If similarity >= ${context.similarityThreshold}, set bridge_search_queries to [].
                4) If similarity < ${context.similarityThreshold}, invent 1–2 YT Music search strings as bridges so A→bridge→B feels natural.
                   Do not invent B outside the pool.
                5) Optional one-line reason.

                JSON keys: selected_search_query, similarity, bridge_search_queries, reason
            """.trimIndent()
        }
    }
}
