package com.narrativedj.app.scheduler

/**
 * JVM harness: Kotlin CushionMusicScheduler parity with Python reference.
 * Fixture: src/test/resources/mock_tracks.json (sync via harness/scripts/sync_fixtures.py)
 * Run: cd android && ./gradlew test --tests com.narrativedj.app.scheduler.CushionMusicSchedulerTest
 */
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.nio.charset.StandardCharsets

class CushionMusicSchedulerTest {

    private lateinit var scheduler: CushionMusicScheduler
    private lateinit var vectorDb: Map<String, DoubleArray>

    @Before
    fun setUp() {
        vectorDb = loadMockVectorDb()
        scheduler = CushionMusicScheduler(vectorDb)
    }

    @Test
    fun canonicalRoute_mongjunginToSweetChild_usesTwoBridges() {
        val route = scheduler.calculateCushionRoute("mongjungin", "sweet_child")
        assertEquals(listOf("california_dreamin", "hotel_california"), route)
    }

    @Test
    fun nearbyTracks_californiaToHotel_directInsert() {
        val route = scheduler.calculateCushionRoute("california_dreamin", "hotel_california")
        assertEquals(emptyList<String>(), route)
    }

    @Test
    fun extremeMismatch_mongjunginToDeathMetal_drop() {
        val route = scheduler.calculateCushionRoute("mongjungin", "death_metal_extreme")
        assertNull(route)
    }

    private fun loadMockVectorDb(): Map<String, DoubleArray> {
        val jsonText = readResource("mock_tracks.json")
        val root = JSONObject(jsonText)
        val tracks = root.getJSONArray("tracks")
        val db = linkedMapOf<String, DoubleArray>()
        for (i in 0 until tracks.length()) {
            val track = tracks.getJSONObject(i)
            val embeddingJson = track.getJSONArray("embedding")
            val embedding = DoubleArray(embeddingJson.length()) { idx ->
                embeddingJson.getDouble(idx)
            }
            db[track.getString("id")] = CushionMusicScheduler.trackToVector(
                bpm = track.getDouble("bpm"),
                energy = track.getDouble("energy"),
                valence = track.getDouble("valence"),
                embedding = embedding,
            )
        }
        return db
    }

    private fun readResource(name: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(name)
            ?: error("Missing test resource: $name")
        return stream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
    }
}
