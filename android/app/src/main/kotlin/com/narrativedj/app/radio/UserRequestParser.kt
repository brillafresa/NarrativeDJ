package com.narrativedj.app.radio

import com.narrativedj.app.locale.AppLanguage
import org.json.JSONArray
import org.json.JSONObject

object UserRequestParser {
    fun parseJson(payload: String): UserRequestParseResult? {
        return try {
            val root = JSONObject(LlmJsonHelper.extractJsonPayload(payload))
            parseJsonObject(root)
        } catch (_: Exception) {
            null
        }
    }

    fun parseJsonObject(root: JSONObject): UserRequestParseResult {
        val intent = parseIntent(root.optString("intent", "chat_only"))
        val tracks = parseTracksArray(root.optJSONArray("tracks"))
        val moodHint = root.optString("mood_hint").takeIf { it.isNotBlank() && it != "null" }
        val chatSnippet = root.optString("chat_snippet").takeIf { it.isNotBlank() && it != "null" }
        return UserRequestParseResult(intent, tracks, moodHint, chatSnippet)
    }

    /**
     * Harness-only edge-case parser (JVM tests).
     * Production always uses Gemini via [RequestParserService] — do not call from MainActivity.
     */
    fun parseLocal(message: String, language: AppLanguage): UserRequestParseResult {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) {
            return UserRequestParseResult(UserRequestIntent.CHAT_ONLY)
        }

        val lower = trimmed.lowercase()
        val isMood = MOOD_KEYWORDS.any { lower.contains(it) }

        if (looksLikeSongRequest(trimmed, language) && !isMood) {
            val directQuery = extractSearchQuery(trimmed, language)
            if (directQuery.isNotBlank()) {
                return UserRequestParseResult(
                    intent = UserRequestIntent.EXPLICIT_TRACKS,
                    tracks = listOf(ParsedTrack(requestedTitle = trimmed, searchQuery = directQuery)),
                )
            }
        }

        if (isMood || looksLikeMoodRequest(trimmed, language)) {
            val queries = moodSearchQueries(trimmed, language)
            return UserRequestParseResult(
                intent = UserRequestIntent.MOOD_REQUEST,
                tracks = queries.map { ParsedTrack(requestedTitle = null, searchQuery = it) },
                moodHint = trimmed,
            )
        }

        if (looksLikeSongRequest(trimmed, language)) {
            val directQuery = extractSearchQuery(trimmed, language)
            if (directQuery.isNotBlank()) {
                return UserRequestParseResult(
                    intent = UserRequestIntent.EXPLICIT_TRACKS,
                    tracks = listOf(ParsedTrack(requestedTitle = trimmed, searchQuery = directQuery)),
                )
            }
        }

        return UserRequestParseResult(
            intent = UserRequestIntent.CHAT_ONLY,
            chatSnippet = trimmed,
        )
    }

    private fun moodSearchQueries(text: String, language: AppLanguage): List<String> {
        val cleaned = extractSearchQuery(text, language).ifBlank { text.trim() }
        return listOf(cleaned)
    }

    private fun extractSearchQuery(text: String, language: AppLanguage): String {
        var query = text.trim()
        val suffixes = when (language) {
            AppLanguage.KOREAN -> listOf(
                "틀어줘", "틀어 주세요", "들려줘", "들려 주세요", "재생해줘", "플레이해줘", "신청", "추천해줘",
            )
            AppLanguage.ENGLISH -> listOf("please play", "play", "put on", "queue", "recommend")
        }
        for (suffix in suffixes.sortedByDescending { it.length }) {
            val pattern = Regex("${Regex.escape(suffix)}[.!?\\s]*$", RegexOption.IGNORE_CASE)
            query = query.replace(pattern, "").trim()
        }
        return query.trim(' ', '.', '!', '?', '"', '\'')
    }

    private fun looksLikeSongRequest(text: String, language: AppLanguage): Boolean {
        val words = when (language) {
            AppLanguage.KOREAN -> listOf("틀어", "들려", "신청", "플레이", "재생", "곡", "노래")
            AppLanguage.ENGLISH -> listOf("play", "song", "track", "request", "put on")
        }
        val lower = text.lowercase()
        return words.any { lower.contains(it) } || text.length <= 48
    }

    private fun looksLikeMoodRequest(text: String, language: AppLanguage): Boolean {
        val requestWords = when (language) {
            AppLanguage.KOREAN -> listOf("틀어", "들려", "추천", "분위기", "어울", "신청", "플레이", "음악")
            AppLanguage.ENGLISH -> listOf("play", "recommend", "mood", "vibe", "something", "music")
        }
        val lower = text.lowercase()
        return requestWords.any { lower.contains(it) }
    }

    private fun parseIntent(raw: String): UserRequestIntent {
        return when (raw.lowercase()) {
            "explicit_tracks" -> UserRequestIntent.EXPLICIT_TRACKS
            "mood_request" -> UserRequestIntent.MOOD_REQUEST
            "mixed" -> UserRequestIntent.MIXED
            else -> UserRequestIntent.CHAT_ONLY
        }
    }

    private fun parseTracksArray(array: JSONArray?): List<ParsedTrack> {
        if (array == null) return emptyList()
        val result = mutableListOf<ParsedTrack>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val query = item.optString("search_query").trim()
            if (query.isEmpty()) continue
            result.add(
                ParsedTrack(
                    requestedTitle = item.optString("requested_title").takeIf { it.isNotBlank() },
                    searchQuery = query,
                    isSubstitute = item.optBoolean("is_substitute", false),
                    substituteNote = item.optString("substitute_note").takeIf { it.isNotBlank() },
                ),
            )
        }
        return result
    }

    private val MOOD_KEYWORDS = listOf(
        "비", "rain", "분위기", "mood", "vibe", "조용", "quiet", "신나", "energy",
        "우울", "sad", "행복", "happy", "잔잔", "chill", "운동", "workout",
    )
}

private object LlmJsonHelper {
    fun extractJsonPayload(text: String): String {
        val trimmed = text.trim()
        val fence = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        fence.find(trimmed)?.groupValues?.getOrNull(1)?.let { return it.trim() }
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1)
        return trimmed
    }
}
