package com.narrativedj.app.scheduler

import android.content.Context
import com.narrativedj.app.R

data class CatalogTrack(
    val id: String,
    val title: String,
    val bpm: Double,
    val energy: Double,
    val valence: Double,
    val embedding: DoubleArray,
    val searchQuery: String? = null,
)

data class CushionPlan(
    val currentTrackId: String,
    val targetTrackId: String,
    val bridgeIds: List<String>,
    val targetTitle: String,
    val isDirect: Boolean,
    val dropped: Boolean,
) {
    fun localizedSummary(context: Context): String {
        if (dropped) return context.getString(R.string.cushion_drop, targetTitle)
        if (isDirect) return context.getString(R.string.cushion_direct, targetTitle)
        val hops = bridgeIds.joinToString(" → ")
        return context.getString(R.string.cushion_route, hops, targetTitle)
    }
}

class CushionRoutePlanner(
    private val scheduler: CushionMusicScheduler,
    private val catalog: List<CatalogTrack>,
) {
    private val vectorDb: Map<String, DoubleArray> = catalog.associate { track ->
        track.id to CushionMusicScheduler.trackToVector(
            track.bpm,
            track.energy,
            track.valence,
            track.embedding,
        )
    }

    private val titleIndex: Map<String, String> = catalog.associate { track ->
        normalizeTitle(track.title) to track.id
    }

    fun resolveTrackId(title: String?): String? {
        if (title.isNullOrBlank()) return null
        return titleIndex[normalizeTitle(title)]
    }

    fun planRoute(
        currentTrackId: String?,
        targetTrackId: String,
    ): CushionPlan? {
        val currentId = currentTrackId ?: return null
        val target = catalog.firstOrNull { it.id == targetTrackId } ?: return null
        if (currentId !in vectorDb || targetTrackId !in vectorDb) {
            return CushionPlan(
                currentTrackId = currentId,
                targetTrackId = targetTrackId,
                bridgeIds = emptyList(),
                targetTitle = target.title,
                isDirect = false,
                dropped = true,
            )
        }
        val route = scheduler.calculateCushionRoute(currentId, targetTrackId)
        return when (route) {
            null -> CushionPlan(
                currentTrackId = currentId,
                targetTrackId = targetTrackId,
                bridgeIds = emptyList(),
                targetTitle = target.title,
                isDirect = false,
                dropped = true,
            )
            else -> CushionPlan(
                currentTrackId = currentId,
                targetTrackId = targetTrackId,
                bridgeIds = route,
                targetTitle = target.title,
                isDirect = route.isEmpty(),
                dropped = false,
            )
        }
    }

    fun suggestTarget(currentTrackId: String?): CatalogTrack? {
        val currentId = currentTrackId
        if (currentId == null) return catalog.firstOrNull()
        val candidates = catalog.filter { it.id != currentId }
        if (candidates.isEmpty()) return null
        return candidates.maxByOrNull { track ->
            val route = planRoute(currentId, track.id)
            when {
                route == null -> -1
                route.dropped -> -1
                route.isDirect -> 100
                else -> 50 - route.bridgeIds.size * 10
            }
        }
    }

    private fun normalizeTitle(title: String): String {
        return title.trim().lowercase()
    }
}
