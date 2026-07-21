/**
 * JVM harness: LLM JSON extraction + audio-control fixture parsing.
 * Fixtures: mock_llm_response.json, mock_dj_transition.json (sync via harness/scripts/sync_fixtures.py)
 * Run: cd android && ./gradlew test --tests com.narrativedj.app.byok.llm.LlmResponseExtractorTest
 */
package com.narrativedj.app.byok.llm

import com.narrativedj.app.dj.DjAudioControlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class LlmResponseExtractorTest {

    @Test
    fun extractJsonPayload_fromMarkdownFence() {
        val raw = """
            Here is the JSON:
            ```json
            {"ducking_volume":0.2,"ramp_duration":0.4,"script":"Hello"}
            ```
        """.trimIndent()
        val json = LlmResponseExtractor.extractJsonPayload(raw)
        val control = requireNotNull(DjAudioControlParser.parse(json))
        assertEquals("Hello", control.script)
    }

    @Test
    fun extractJsonPayload_fromPlainObject() {
        val raw = """{"ducking_volume":0.18,"ramp_duration":0.35,"script":"Test"}"""
        val json = LlmResponseExtractor.extractJsonPayload(raw)
        assertTrue(json.startsWith("{"))
    }

    @Test
    fun parseFixtureResource_matchesSchema() {
        val stream = javaClass.classLoader?.getResourceAsStream("mock_llm_response.json")
            ?: error("missing fixture")
        val json = stream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
        val control = requireNotNull(DjAudioControlParser.parse(json))
        assertEquals(0.18, control.duckingVolume, 0.001)
        assertTrue(control.script.contains("rainy afternoon"))
    }

    @Test
    fun parseDjTransitionFixture_matchesSchema() {
        val stream = javaClass.classLoader?.getResourceAsStream("mock_dj_transition.json")
            ?: error("missing fixture — run python harness/scripts/sync_fixtures.py")
        val json = stream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
        val control = requireNotNull(DjAudioControlParser.parse(json))
        assertTrue(control.script.contains("rainy mood"))
    }
}
