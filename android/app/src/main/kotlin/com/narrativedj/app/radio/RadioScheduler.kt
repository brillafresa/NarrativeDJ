package com.narrativedj.app.radio

import com.narrativedj.app.scheduler.CatalogMatcher
import com.narrativedj.app.scheduler.CatalogTrack
import com.narrativedj.app.scheduler.CushionMusicScheduler
import com.narrativedj.app.scheduler.CushionRoutePlanner

data class ScheduleDecision(
    val queries: List<String>,
    val targetEntry: CandidateEntry?,
    val fromPool: Boolean,
    /** True when cushion bridges were inserted before the target query. */
    val usedCushion: Boolean = false,
    val bridgeCount: Int = 0,
)

/**
 * Picks the next YTM search query from the candidate pool.
 * When [catalog] is provided and both current + target resolve, inserts cushion bridges.
 */
class RadioScheduler(
    private val catalog: List<CatalogTrack> = emptyList(),
) {
    private val planner: CushionRoutePlanner? = if (catalog.isEmpty()) {
        null
    } else {
        val vectorDb = catalog.associate { track ->
            track.id to CushionMusicScheduler.trackToVector(
                track.bpm,
                track.energy,
                track.valence,
                track.embedding,
            )
        }
        CushionRoutePlanner(CushionMusicScheduler(vectorDb), catalog)
    }

    fun pickNext(
        currentTrackKey: String?,
        pool: CandidatePool,
        history: PlayHistory,
    ): ScheduleDecision? {
        val eligible = pool.all().filter { !history.wasRecentlyPlayed(it.playKey()) }
        val entry = eligible.firstOrNull() ?: return null
        return decisionFor(entry, currentTrackKey, fromPool = true)
    }

    fun pickImmediate(pool: CandidatePool, history: PlayHistory): ScheduleDecision? {
        return pickNext(currentTrackKey = null, pool = pool, history = history)
    }

    private fun decisionFor(
        entry: CandidateEntry,
        currentTrackKey: String?,
        fromPool: Boolean,
    ): ScheduleDecision {
        val direct = directDecision(entry, fromPool)
        val activePlanner = planner ?: return direct
        if (currentTrackKey.isNullOrBlank()) return direct

        val currentId = CatalogMatcher.resolveId(catalog, currentTrackKey) ?: return direct
        val targetId = CatalogMatcher.resolveIdForEntry(catalog, entry) ?: return direct
        val plan = activePlanner.planRoute(currentId, targetId) ?: return direct
        if (plan.dropped) return direct

        val queries = buildList {
            for (bridgeId in plan.bridgeIds) {
                val q = catalog.firstOrNull { it.id == bridgeId }?.let { playbackQuery(it) }
                if (q != null) add(q)
            }
            add(playbackQuery(catalog.first { it.id == targetId }))
        }
        if (queries.isEmpty()) return direct
        return ScheduleDecision(
            queries = queries,
            targetEntry = entry,
            fromPool = fromPool,
            usedCushion = plan.bridgeIds.isNotEmpty(),
            bridgeCount = plan.bridgeIds.size,
        )
    }

    private fun directDecision(entry: CandidateEntry, fromPool: Boolean): ScheduleDecision {
        return ScheduleDecision(
            queries = listOf(entry.searchQuery),
            targetEntry = entry,
            fromPool = fromPool,
            usedCushion = false,
            bridgeCount = 0,
        )
    }

    private fun playbackQuery(track: CatalogTrack): String {
        return track.searchQuery?.takeIf { it.isNotBlank() } ?: track.title
    }
}
