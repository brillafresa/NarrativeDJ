package com.narrativedj.app.radio

data class ScheduleDecision(
    val queries: List<String>,
    val targetEntry: CandidateEntry?,
    val fromPool: Boolean,
)

/**
 * Picks the next YTM search query from the candidate pool.
 * Cushion bridge routing is harness-only; runtime plays search_query directly.
 */
class RadioScheduler {
    fun pickNext(
        @Suppress("UNUSED_PARAMETER") currentTrackKey: String?,
        pool: CandidatePool,
        history: PlayHistory,
    ): ScheduleDecision? {
        val eligible = pool.all().filter { !history.wasRecentlyPlayed(it.playKey()) }
        val entry = eligible.firstOrNull() ?: return null
        return directDecision(entry, fromPool = true)
    }

    fun pickImmediate(pool: CandidatePool, history: PlayHistory): ScheduleDecision? {
        return pickNext(currentTrackKey = null, pool = pool, history = history)
    }

    private fun directDecision(entry: CandidateEntry, fromPool: Boolean): ScheduleDecision {
        return ScheduleDecision(
            queries = listOf(entry.searchQuery),
            targetEntry = entry,
            fromPool = fromPool,
        )
    }
}
