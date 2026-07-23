/**
 * JVM harness: CushionPlaybackController empty / no-WebView sequence completion.
 *
 * Purpose: Validate playSequence completion callbacks without YTM WebView (bridge→target queries).
 * Run: cd android && ./gradlew testDebugUnitTest --tests com.narrativedj.app.scheduler.CushionPlaybackControllerTest
 */
package com.narrativedj.app.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CushionPlaybackControllerTest {

    @Test
    fun playSequence_empty_invokesComplete() {
        var completed = false
        CushionPlaybackController().playSequence(emptyList(), onComplete = { completed = true })
        assertTrue(completed)
    }

    @Test
    fun playSequence_withoutWebView_reportsFirstStepThenComplete() {
        val steps = mutableListOf<String>()
        var completed = false
        CushionPlaybackController().playSequence(
            queries = listOf("Hotel California Eagles", "Sweet Child O Mine"),
            onStep = { _, query -> steps.add(query) },
            onComplete = { completed = true },
        )
        assertEquals(listOf("Hotel California Eagles"), steps)
        assertTrue(completed)
    }
}
