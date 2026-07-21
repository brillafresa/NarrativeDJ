/**
 * JVM harness: DjAudioControl JSON parse, SSML strip, transition/story fallbacks.
 * Fixture: mock_dj_transition.json (sync via harness/scripts/sync_fixtures.py)
 * Run: cd android && ./gradlew test --tests com.narrativedj.app.dj.DjAudioControlParserTest
 */
package com.narrativedj.app.dj

import com.narrativedj.app.byok.llm.DjTransitionContext
import com.narrativedj.app.locale.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DjAudioControlParserTest {

    @Test
    fun parse_validJson_returnsControl() {
        val json = """
            {
              "ducking_volume": 0.15,
              "ramp_duration": 0.5,
              "ramp_out_duration": 0.8,
              "script": "Welcome back.",
              "ssml": "<speak>Welcome</speak>"
            }
        """.trimIndent()

        val control = requireNotNull(DjAudioControlParser.parse(json))
        assertEquals(0.15, control.duckingVolume, 0.001)
        assertEquals(0.5, control.rampInSeconds, 0.001)
        assertEquals("Welcome back.", control.script)
        assertEquals("<speak>Welcome</speak>", control.ssml)
    }

    @Test
    fun fallbackForStory_emptyStory_usesDefaultLine() {
        val control = DjAudioControlParser.fallbackForStory("", AppLanguage.KOREAN)
        assertEquals("NarrativeDJ와 함께하고 계십니다.", control.script)

        val english = DjAudioControlParser.fallbackForStory("", AppLanguage.ENGLISH)
        assertEquals("You're listening to NarrativeDJ.", english.script)
    }

    @Test
    fun spokenText_stripsSsmlTags() {
        val control = DjAudioControl(
            duckingVolume = 0.2,
            rampInSeconds = 0.4,
            rampOutSeconds = 0.6,
            script = "fallback",
            ssml = "<speak>Hello <break time=\"500ms\"/> world</speak>",
        )
        assertEquals("Hello world", DjAudioControlParser.spokenText(control))
    }

    @Test
    fun fallbackForTransition_includesSubstituteNote() {
        val control = DjAudioControlParser.fallbackForTransition(
            DjTransitionContext(
                profileLabel = "Café",
                language = AppLanguage.KOREAN,
                previousTrackTitle = "몽중인",
                nextTrackTitle = "California Dreamin'",
                nextSearchQuery = "California Dreamin'",
                isSubstitute = true,
                substituteNote = "Hotel California을 찾지 못했습니다",
            ),
        )
        assert(control.script.contains("Hotel California"))
        assert(control.script.contains("California Dreamin'"))
    }

    @Test
    fun parse_invalidJson_returnsNull() {
        assertNull(DjAudioControlParser.parse("{bad"))
    }
}
