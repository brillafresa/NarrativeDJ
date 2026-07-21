/**
 * JVM harness: user request JSON parser (▶ Send LLM schema).
 * Fixture: mock_user_request.json (sync via harness/scripts/sync_fixtures.py)
 * Run: cd android && ./gradlew test --tests com.narrativedj.app.radio.UserRequestParserTest
 */
package com.narrativedj.app.radio

import org.json.JSONObject
import org.junit.Assert.assertEquals
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
    }

    private fun readResource(name: String): String {
        val stream = checkNotNull(javaClass.classLoader.getResourceAsStream(name)) {
            "Missing resource $name"
        }
        return stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }
}
