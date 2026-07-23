package com.narrativedj.app.scheduler

import com.narrativedj.app.radio.CandidateEntry

/**
 * Resolve catalog track ids from now-playing labels or candidate search queries.
 *
 * Purpose: enable runtime cushion routing when both ends exist in demo_tracks.json.
 * Verify: `./gradlew test --tests com.narrativedj.app.scheduler.CatalogMatcherTest`
 */
object CatalogMatcher {
    fun resolveId(catalog: List<CatalogTrack>, keyOrLabel: String?): String? {
        if (keyOrLabel.isNullOrBlank()) return null
        val normalized = CandidateEntry.normalizeKey(keyOrLabel)
        catalog.firstOrNull { CandidateEntry.normalizeKey(it.id) == normalized }?.id?.let { return it }
        catalog.firstOrNull { CandidateEntry.normalizeKey(it.title) == normalized }?.id?.let { return it }
        catalog.firstOrNull {
            it.searchQuery != null && CandidateEntry.normalizeKey(it.searchQuery) == normalized
        }?.id?.let { return it }

        val titlePart = normalized
            .substringBefore(" — ")
            .substringBefore(" - ")
            .trim()
        if (titlePart.isNotEmpty() && titlePart != normalized) {
            catalog.firstOrNull { CandidateEntry.normalizeKey(it.title) == titlePart }?.id?.let { return it }
        }
        return null
    }

    fun resolveIdForEntry(catalog: List<CatalogTrack>, entry: CandidateEntry): String? {
        entry.catalogTrackId?.let { id ->
            if (catalog.any { it.id == id }) return id
        }
        return resolveId(catalog, entry.searchQuery)
            ?: entry.requestedLabel?.let { resolveId(catalog, it) }
    }
}
