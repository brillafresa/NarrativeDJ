package com.narrativedj.app.radio

/**
 * Ring buffer of recently played track keys (default 20).
 * Checked at selection time, not when adding to the candidate pool.
 */
class PlayHistory(private val capacity: Int = DEFAULT_CAPACITY) {
    private val keys = ArrayDeque<String>()

    fun record(playKey: String) {
        val normalized = CandidateEntry.normalizeKey(playKey)
        keys.remove(normalized)
        keys.addLast(normalized)
        while (keys.size > capacity) {
            keys.removeFirst()
        }
    }

    fun wasRecentlyPlayed(playKey: String): Boolean {
        return keys.contains(CandidateEntry.normalizeKey(playKey))
    }

    fun clear() {
        keys.clear()
    }

    companion object {
        const val DEFAULT_CAPACITY = 20
    }
}
