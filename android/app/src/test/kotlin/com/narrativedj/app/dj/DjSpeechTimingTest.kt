/**
 * JVM harness: DJ TTS speech-rate estimate used for ducking duration.
 * Run: cd android && ./gradlew testDebugUnitTest --tests com.narrativedj.app.dj.DjSpeechTimingTest
 */
package com.narrativedj.app.dj

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DjSpeechTimingTest {

    @Test
    fun defaultSpeechRate_isSlowerThanUnity() {
        assertTrue(DjPipeline.DEFAULT_SPEECH_RATE < 1.0f)
        assertEquals(0.85f, DjPipeline.DEFAULT_SPEECH_RATE, 0.001f)
    }

    @Test
    fun estimateSpeechMs_scalesWithRate() {
        val script = "one two three four five"
        val atUnity = DjPipeline.estimateSpeechMs(script, speechRate = 1.0f)
        val atSlow = DjPipeline.estimateSpeechMs(script, speechRate = 0.85f)
        assertTrue(atSlow > atUnity)
    }
}
