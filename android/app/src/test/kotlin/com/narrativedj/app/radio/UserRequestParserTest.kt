/**
 * JVM harness: user request JSON parser (▶ Send LLM schema).
 * Fixture: mock_user_request.json (sync via harness/scripts/sync_fixtures.py)
 * Run: cd android && ./gradlew test --tests com.narrativedj.app.radio.UserRequestParserTest
 */
package com.narrativedj.app.radio

import com.narrativedj.app.locale.AppLanguage
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class UserRequestParserTest {

    @Test
    fun parseJson_readsFixtureShape() {
        val json = readResource("mock_user_request.json")
        val result = UserRequestParser.parseJson(json)
        assertTrue(result != null)
        assertEquals(UserRequestIntent.MIXED, result!!.intent)
        assertEquals(1, result.tracks.size)
        assertEquals("Hotel California Eagles", result.tracks.first().searchQuery)
    }

    @Test
    fun parseJsonObject_chatOnly() {
        val root = JSONObject(
            """{"intent":"chat_only","tracks":[],"chat_snippet":"hello"}""",
        )
        val result = UserRequestParser.parseJsonObject(root)
        assertEquals(UserRequestIntent.CHAT_ONLY, result.intent)
        assertTrue(result.tracks.isEmpty())
        assertEquals("hello", result.chatSnippet)
        assertTrue(result.isComplete())
    }

    @Test
    fun isComplete_moodWithoutTracks_isIncomplete() {
        val result = UserRequestParseResult(UserRequestIntent.MOOD_REQUEST, tracks = emptyList())
        assertFalse(result.isComplete())
    }

    @Test
    fun parseLocal_rainyMoodRequest_usesMessageAsSearchQuery() {
        val result = UserRequestParser.parseLocal(
            "비오는 날 듣기 좋은 음악 틀어줘",
            AppLanguage.KOREAN,
        )
        assertEquals(UserRequestIntent.MOOD_REQUEST, result.intent)
        assertEquals(1, result.tracks.size)
        assertTrue(result.tracks.first().searchQuery.contains("비"))
        assertTrue(result.isComplete())
    }

    @Test
    fun parseLocal_unknownSongRequest_usesDirectSearchQuery() {
        val result = UserRequestParser.parseLocal(
            "4 non blondes의 what's up 틀어줘",
            AppLanguage.KOREAN,
        )
        assertEquals(UserRequestIntent.EXPLICIT_TRACKS, result.intent)
        assertEquals(1, result.tracks.size)
        assertTrue(result.tracks.first().searchQuery.contains("what's up", ignoreCase = true))
    }

    private fun readResource(name: String): String {
        val stream = checkNotNull(javaClass.classLoader.getResourceAsStream(name)) {
            "Missing resource $name"
        }
        return stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }
}
