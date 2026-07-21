package com.narrativedj.app.radio

import com.narrativedj.app.locale.AppLanguage
import com.narrativedj.app.profile.SpaceProfile
import com.narrativedj.app.scheduler.CatalogTrack
import com.narrativedj.app.scheduler.CushionRoutePlanner
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

    fun parseLocal(
        message: String,
        catalog: List<CatalogTrack>,
        planner: CushionRoutePlanner,
        profile: SpaceProfile,
        language: AppLanguage,
    ): UserRequestParseResult {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) {
            return UserRequestParseResult(UserRequestIntent.CHAT_ONLY)
        }

        val matchedTracks = mutableListOf<ParsedTrack>()
        val lower = trimmed.lowercase()
        for (track in catalog) {
            if (lower.contains(track.title.lowercase())) {
                matchedTracks.add(
                    ParsedTrack(
                        requestedTitle = track.title,
                        searchQuery = track.playbackQuery(),
                        catalogTrackId = track.id,
                    ),
                )
            }
        }

        val isMood = MOOD_KEYWORDS.any { lower.contains(it) }
        if (matchedTracks.isNotEmpty()) {
            val intent = if (isMood || trimmed.length > matchedTracks.size * 20) {
                UserRequestIntent.MIXED
            } else {
                UserRequestIntent.EXPLICIT_TRACKS
            }
            return UserRequestParseResult(
                intent = intent,
                tracks = matchedTracks.distinctBy { it.searchQuery },
                moodHint = if (isMood) trimmed else null,
                chatSnippet = if (intent == UserRequestIntent.MIXED) trimmed else null,
            )
        }

        if (isMood || looksLikeMoodRequest(trimmed, language)) {
            val candidates = planner.profileCandidates(profile).take(3)
            val tracks = candidates.map { track ->
                ParsedTrack(
                    requestedTitle = null,
                    searchQuery = track.playbackQuery(),
                    catalogTrackId = track.id,
                )
            }
            return UserRequestParseResult(
                intent = UserRequestIntent.MOOD_REQUEST,
                tracks = tracks,
                moodHint = trimmed,
            )
        }

        if (looksLikeSongRequest(trimmed, language)) {
            val substitute = findSubstitute(trimmed, catalog, planner, profile, language)
            if (substitute != null) {
                return UserRequestParseResult(
                    intent = UserRequestIntent.EXPLICIT_TRACKS,
                    tracks = listOf(substitute),
                )
            }
        }

        return UserRequestParseResult(
            intent = UserRequestIntent.CHAT_ONLY,
            chatSnippet = trimmed,
        )
    }

    private fun looksLikeSongRequest(text: String, language: AppLanguage): Boolean {
        val words = when (language) {
            AppLanguage.KOREAN -> listOf("틀어", "들려", "신청", "플레이", "재생", "곡", "노래")
            AppLanguage.ENGLISH -> listOf("play", "song", "track", "request", "put on")
        }
        val lower = text.lowercase()
        return words.any { lower.contains(it) } || text.length <= 40
    }

    private fun findSubstitute(
        message: String,
        catalog: List<CatalogTrack>,
        planner: CushionRoutePlanner,
        profile: SpaceProfile,
        language: AppLanguage,
    ): ParsedTrack? {
        val suggested = planner.suggestTarget(null, profile) ?: return null
        val note = when (language) {
            AppLanguage.KOREAN -> "'$message'을(를) 찾지 못해 '${suggested.title}'(으)로 대체"
            AppLanguage.ENGLISH -> "Could not find '$message'; substituting '${suggested.title}'"
        }
        return ParsedTrack(
            requestedTitle = message,
            searchQuery = suggested.playbackQuery(),
            isSubstitute = true,
            substituteNote = note,
            catalogTrackId = suggested.id,
        )
    }

    private fun looksLikeMoodRequest(text: String, language: AppLanguage): Boolean {
        val requestWords = when (language) {
            AppLanguage.KOREAN -> listOf("틀어", "들려", "추천", "분위기", "어울", "신청", "플레이")
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
                    catalogTrackId = item.optString("catalog_track_id").takeIf { it.isNotBlank() },
                ),
            )
        }
        return result
    }

    private val MOOD_KEYWORDS = listOf(
        "비", "rain", "브런치", "brunch", "카페", "cafe", "분위기", "mood", "vibe",
        "조용", "quiet", "신나", "energy", "우울", "sad", "행복", "happy",
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

private fun CatalogTrack.playbackQuery(): String = searchQuery?.takeIf { it.isNotBlank() } ?: title
