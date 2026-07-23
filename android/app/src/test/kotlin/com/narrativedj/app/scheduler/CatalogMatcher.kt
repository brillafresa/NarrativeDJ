package com.narrativedj.app.scheduler

import com.narrativedj.app.radio.CandidateEntry

/**
 * Harness-only helper: resolve demo-catalog track ids from labels / search queries.
 *
 * Purpose: support vector-cushion JVM tests against mock_tracks.json (not used in production radio).
 * Production cushion uses Gemini pool similarity + invented bridge search queries.
 * Run: cd android && ./gradlew testDebugUnitTest --tests com.narrativedj.app.scheduler.CatalogMatcherTest
 */
object CatalogMatcher {
    fun resolveId(catalog: List<CatalogTrack>, keyOrLabel: String?): String? {
        if (keyOrLabel.isNullOrBlank()) return null
        val normalized = CandidateEntry.normalizeKey(keyOrLabel)
        catalog.firstOrNull { CandidateEntry.normalizeKey(it.id) == normalized }?.id?.let { return it }
        catalog.firstOrNull { CandidateEntry.normalizeKey(it.title) == normalized }?.id?.let { return it }
        catalog.firstOrNull {
            val q = it.searchQuery
            q != null && CandidateEntry.normalizeKey(q) == normalized
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
