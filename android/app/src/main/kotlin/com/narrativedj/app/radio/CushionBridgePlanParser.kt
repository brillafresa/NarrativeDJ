package com.narrativedj.app.radio

import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses Gemini cushion-bridge JSON.
 * Fixture SSOT: harness/tests/mock_cushion_bridge.json
 */
object CushionBridgePlanParser {
    fun parseJson(raw: String): CushionBridgePlan? {
        return try {
            val root = JSONObject(raw)
            val selected = root.optString("selected_search_query").trim()
            if (selected.isEmpty()) return null
            val similarity = root.optDouble("similarity", Double.NaN)
            if (similarity.isNaN() || similarity < 0.0 || similarity > 1.0) return null
            val bridgesJson = root.optJSONArray("bridge_search_queries") ?: JSONArray()
            val bridges = buildList {
                for (i in 0 until bridgesJson.length()) {
                    val q = bridgesJson.optString(i).trim()
                    if (q.isNotEmpty()) add(q)
                }
            }.take(CushionBridgePlan.MAX_BRIDGES)
            CushionBridgePlan(
                selectedSearchQuery = selected,
                similarity = similarity,
                bridgeSearchQueries = bridges,
                reason = root.optString("reason").takeIf { it.isNotBlank() },
            )
        } catch (_: Exception) {
            null
        }
    }
}
