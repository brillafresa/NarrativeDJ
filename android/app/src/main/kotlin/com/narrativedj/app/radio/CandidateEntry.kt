package com.narrativedj.app.radio

data class CandidateEntry(
    val searchQuery: String,
    val requestedLabel: String? = null,
    val isSubstitute: Boolean = false,
    val substituteReason: String? = null,
    val moodHint: String? = null,
) {
    fun playKey(): String = normalizeKey(searchQuery)

    companion object {
        fun normalizeKey(raw: String): String = raw.trim().lowercase()
    }
}
