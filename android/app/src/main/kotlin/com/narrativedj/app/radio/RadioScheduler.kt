package com.narrativedj.app.radio

import com.narrativedj.app.profile.SpaceProfile
import com.narrativedj.app.scheduler.CatalogTrack
import com.narrativedj.app.scheduler.CushionPlan
import com.narrativedj.app.scheduler.CushionRoutePlanner

data class ScheduleDecision(
    val queries: List<String>,
    val targetEntry: CandidateEntry?,
    val plan: CushionPlan?,
    val fromPool: Boolean,
)

class RadioScheduler(
    private val planner: CushionRoutePlanner,
    private val catalog: List<CatalogTrack>,
) {
    fun pickNext(
        currentTrackId: String?,
        pool: CandidatePool,
        history: PlayHistory,
        profile: SpaceProfile,
    ): ScheduleDecision? {
        val eligible = pool.all().filter { !history.wasRecentlyPlayed(it.playKey()) }
        if (currentTrackId == null) {
            val entry = eligible.firstOrNull() ?: pickSimilarFallback(currentTrackId, history, profile)
                ?: return null
            return directDecision(entry, fromPool = eligible.isNotEmpty())
        }

        if (eligible.isNotEmpty()) {
            val best = eligible.maxByOrNull { scoreCandidate(currentTrackId, it, profile) } ?: return null
            return buildDecision(currentTrackId, best, profile, fromPool = true)
        }

        val fallback = pickSimilarFallback(currentTrackId, history, profile) ?: return null
        return directDecision(fallback, fromPool = false)
    }

    fun pickImmediate(
        pool: CandidatePool,
        history: PlayHistory,
        profile: SpaceProfile,
    ): ScheduleDecision? {
        return pickNext(currentTrackId = null, pool = pool, history = history, profile = profile)
    }

    private fun pickSimilarFallback(
        currentTrackId: String?,
        history: PlayHistory,
        profile: SpaceProfile,
    ): CandidateEntry? {
        val suggested = planner.suggestTarget(currentTrackId, profile) ?: return null
        val entry = suggested.toEntry()
        if (history.wasRecentlyPlayed(entry.playKey())) return null
        return entry
    }

    private fun scoreCandidate(
        currentTrackId: String,
        entry: CandidateEntry,
        profile: SpaceProfile,
    ): Int {
        val targetId = entry.catalogTrackId ?: planner.resolveTrackId(entry.searchQuery)
        if (targetId == null) return 10
        val plan = planner.planRoute(currentTrackId, targetId, profile) ?: return -1
        return when {
            plan.dropped -> -1
            plan.isDirect -> 100
            else -> 60 - plan.bridgeIds.size * 15
        }
    }

    private fun buildDecision(
        currentTrackId: String,
        entry: CandidateEntry,
        profile: SpaceProfile,
        fromPool: Boolean,
    ): ScheduleDecision {
        val targetId = entry.catalogTrackId ?: planner.resolveTrackId(entry.searchQuery)
        if (targetId == null) {
            return ScheduleDecision(
                queries = listOf(entry.searchQuery),
                targetEntry = entry,
                plan = null,
                fromPool = fromPool,
            )
        }
        val plan = planner.planRoute(currentTrackId, targetId, profile)
        if (plan == null || plan.dropped) {
            return ScheduleDecision(
                queries = listOf(entry.searchQuery),
                targetEntry = entry,
                plan = plan,
                fromPool = fromPool,
            )
        }
        val bridgeQueries = plan.bridgeIds.mapNotNull { id ->
            catalog.firstOrNull { it.id == id }?.playbackQuery()
        }
        val targetQuery = entry.searchQuery
        return ScheduleDecision(
            queries = bridgeQueries + targetQuery,
            targetEntry = entry,
            plan = plan,
            fromPool = fromPool,
        )
    }

    private fun directDecision(entry: CandidateEntry, fromPool: Boolean): ScheduleDecision {
        return ScheduleDecision(
            queries = listOf(entry.searchQuery),
            targetEntry = entry,
            plan = null,
            fromPool = fromPool,
        )
    }

    private fun CatalogTrack.toEntry(): CandidateEntry {
        return CandidateEntry(
            searchQuery = playbackQuery(),
            requestedLabel = title,
            catalogTrackId = id,
        )
    }

    private fun CatalogTrack.playbackQuery(): String =
        searchQuery?.takeIf { it.isNotBlank() } ?: title
}
