package com.narrativedj.app.radio

/**
 * Pending track queue for the radio session.
 * Dedupes by normalized searchQuery (one entry per query).
 */
class CandidatePool {
    private val entries = mutableListOf<CandidateEntry>()

    fun addAll(newEntries: List<CandidateEntry>): Int {
        var added = 0
        for (entry in newEntries) {
            val key = CandidateEntry.normalizeKey(entry.searchQuery)
            if (entries.any { CandidateEntry.normalizeKey(it.searchQuery) == key }) continue
            entries.add(entry)
            added++
        }
        return added
    }

    fun remove(entry: CandidateEntry) {
        val key = CandidateEntry.normalizeKey(entry.searchQuery)
        entries.removeAll { CandidateEntry.normalizeKey(it.searchQuery) == key }
    }

    fun removeByPlayKey(playKey: String) {
        val normalized = CandidateEntry.normalizeKey(playKey)
        entries.removeAll { it.playKey() == normalized }
    }

    fun all(): List<CandidateEntry> = entries.toList()

    fun isEmpty(): Boolean = entries.isEmpty()

    fun size(): Int = entries.size
}
