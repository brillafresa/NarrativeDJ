package com.narrativedj.app.radio

data class ScheduleDecision(
    val queries: List<String>,
    val targetEntry: CandidateEntry?,
    val fromPool: Boolean,
    /** True when invented cushion bridge queries were inserted before the target. */
    val usedCushion: Boolean = false,
    val bridgeCount: Int = 0,
    val similarity: Double? = null,
)

/**
 * Builds schedule decisions from the candidate pool.
 * Similarity / bridge invention is done by [CushionBridgePlannerService] (LLM);
 * this class only applies plans and FIFO fallback — no fixed song catalog.
 */
class RadioScheduler {

    fun eligibleEntries(pool: CandidatePool, history: PlayHistory): List<CandidateEntry> {
        return pool.all().filter { !history.wasRecentlyPlayed(it.playKey()) }
    }

    fun pickNext(
        @Suppress("UNUSED_PARAMETER") currentTrackKey: String?,
        pool: CandidatePool,
        history: PlayHistory,
    ): ScheduleDecision? {
        val entry = eligibleEntries(pool, history).firstOrNull() ?: return null
        return directDecision(entry, fromPool = true)
    }

    fun pickImmediate(pool: CandidatePool, history: PlayHistory): ScheduleDecision? {
        return pickNext(currentTrackKey = null, pool = pool, history = history)
    }

    fun decisionFromPlan(
        plan: CushionBridgePlan,
        candidates: List<CandidateEntry>,
    ): ScheduleDecision? {
        val entry = candidates.firstOrNull {
            CandidateEntry.normalizeKey(it.searchQuery) ==
                CandidateEntry.normalizeKey(plan.selectedSearchQuery)
        } ?: return null
        val bridges = plan.bridgesForPlayback()
        val queries = bridges + entry.searchQuery
        return ScheduleDecision(
            queries = queries,
            targetEntry = entry,
            fromPool = true,
            usedCushion = bridges.isNotEmpty(),
            bridgeCount = bridges.size,
            similarity = plan.similarity,
        )
    }

    fun directDecision(entry: CandidateEntry, fromPool: Boolean = true): ScheduleDecision {
        return ScheduleDecision(
            queries = listOf(entry.searchQuery),
            targetEntry = entry,
            fromPool = fromPool,
            usedCushion = false,
            bridgeCount = 0,
            similarity = null,
        )
    }
}
