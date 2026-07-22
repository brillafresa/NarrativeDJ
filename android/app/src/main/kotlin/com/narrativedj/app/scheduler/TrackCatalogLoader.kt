package com.narrativedj.app.scheduler

import android.content.Context

/**
 * Parses harness fixture track catalog JSON.
 * SSOT: harness/tests/mock_tracks.json — not loaded at app runtime.
 */
object TrackCatalogLoader {
    const val HARNESS_CATALOG_ASSET = "catalog/demo_tracks.json"
    /** @deprecated use [HARNESS_CATALOG_ASSET]; kept for harness sync path name. */
    const val DEMO_CATALOG_ASSET = HARNESS_CATALOG_ASSET

    fun load(context: Context, assetPath: String = DEMO_CATALOG_ASSET): List<CatalogTrack> {
        val jsonText = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        return parse(jsonText)
    }

    fun parse(jsonText: String): List<CatalogTrack> {
        val root = org.json.JSONObject(jsonText)
        val tracks = root.getJSONArray("tracks")
        val result = ArrayList<CatalogTrack>(tracks.length())
        for (i in 0 until tracks.length()) {
            val track = tracks.getJSONObject(i)
            val embeddingJson = track.getJSONArray("embedding")
            val embedding = DoubleArray(embeddingJson.length()) { idx ->
                embeddingJson.getDouble(idx)
            }
            result.add(
                CatalogTrack(
                    id = track.getString("id"),
                    title = track.getString("title"),
                    bpm = track.getDouble("bpm"),
                    energy = track.getDouble("energy"),
                    valence = track.getDouble("valence"),
                    embedding = embedding,
                    searchQuery = track.optString("search_query").takeIf { it.isNotBlank() },
                ),
            )
        }
        return result
    }
}
