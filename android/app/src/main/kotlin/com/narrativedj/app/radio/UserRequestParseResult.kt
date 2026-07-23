package com.narrativedj.app.radio

enum class UserRequestIntent {
    EXPLICIT_TRACKS,
    MOOD_REQUEST,
    CHAT_ONLY,
    MIXED,
}

data class ParsedTrack(
    val requestedTitle: String?,
    val searchQuery: String,
    val isSubstitute: Boolean = false,
    val substituteNote: String? = null,
)

data class UserRequestParseResult(
    val intent: UserRequestIntent,
    val tracks: List<ParsedTrack> = emptyList(),
    val moodHint: String? = null,
    val chatSnippet: String? = null,
) {
    fun toCandidateEntries(): List<CandidateEntry> {
        return tracks.map { track ->
            CandidateEntry(
                searchQuery = track.searchQuery,
                requestedLabel = track.requestedTitle,
                isSubstitute = track.isSubstitute,
                substituteReason = track.substituteNote,
                moodHint = moodHint,
            )
        }
    }

    /** True when the parser produced enough data to queue playback (or intentional chat-only). */
    fun isComplete(): Boolean {
        return when (intent) {
            UserRequestIntent.CHAT_ONLY -> true
            else -> tracks.isNotEmpty()
        }
    }
}
