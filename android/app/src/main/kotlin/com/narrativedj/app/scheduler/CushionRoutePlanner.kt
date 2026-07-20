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

    fun profileCandidates(profile: com.narrativedj.app.profile.SpaceProfile): List<CatalogTrack> {
        val metrics = catalog.map { com.narrativedj.app.profile.TrackMetrics(it.id, it.bpm, it.energy) }
        val matchingIds = com.narrativedj.app.profile.SpaceProfileMatcher
            .filterTracks(profile, metrics)
            .map { it.id }
            .toSet()
        return catalog.filter { it.id in matchingIds }
    }

    fun planRoute(
        currentTrackId: String?,
        targetTrackId: String,
        profile: com.narrativedj.app.profile.SpaceProfile,
    ): CushionPlan? {
        val currentId = currentTrackId ?: return null
        val target = catalog.firstOrNull { it.id == targetTrackId } ?: return null
        val profileFilteredDb = profileFilteredVectorDb(profile)
        if (currentId !in profileFilteredDb || targetTrackId !in profileFilteredDb) {
            return CushionPlan(
                currentTrackId = currentId,
                targetTrackId = targetTrackId,
                bridgeIds = emptyList(),
                targetTitle = target.title,
                isDirect = false,
                dropped = true,
            )
        }
        val profileScheduler = CushionMusicScheduler(profileFilteredDb)
        val route = profileScheduler.calculateCushionRoute(currentId, targetTrackId)
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

    fun suggestTarget(
        currentTrackId: String?,
        profile: com.narrativedj.app.profile.SpaceProfile,
    ): CatalogTrack? {
        val currentId = currentTrackId ?: return profileCandidates(profile).firstOrNull()
        val candidates = profileCandidates(profile).filter { it.id != currentId }
        if (candidates.isEmpty()) return null
        return candidates.maxByOrNull { track ->
            val route = planRoute(currentId, track.id, profile)
            when {
                route == null -> -1
                route.dropped -> -1
                route.isDirect -> 100
                else -> 50 - route.bridgeIds.size * 10
            }
        }
    }

    private fun profileFilteredVectorDb(profile: com.narrativedj.app.profile.SpaceProfile): Map<String, DoubleArray> {
        val allowedIds = profileCandidates(profile).map { it.id }.toSet()
        return vectorDb.filterKeys { it in allowedIds }
    }

    private fun normalizeTitle(title: String): String {
        return title.trim().lowercase()
    }
}
