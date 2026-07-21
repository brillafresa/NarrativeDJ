/**
 * JVM harness: DJ interstitial gate — random ment every 1–2 track transitions.
 * Run: cd android && ./gradlew test --tests com.narrativedj.app.radio.DjInterstitialGateTest
 */
package com.narrativedj.app.radio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DjInterstitialGateTest {

    @Test
    fun firesEveryOneOrTwoTransitions() {
        val gate = DjInterstitialGate(kotlin.random.Random(42))
        var mentCount = 0
        repeat(20) {
            if (gate.onTrackTransition()) mentCount++
        }
        assertTrue(mentCount in 7..14)
    }

    @Test
    fun neverFiresEverySingleTransition() {
        val gate = DjInterstitialGate(kotlin.random.Random(1))
        var consecutive = 0
        var maxConsecutive = 0
        repeat(10) {
            if (gate.onTrackTransition()) {
                consecutive++
                maxConsecutive = maxOf(maxConsecutive, consecutive)
            } else {
                consecutive = 0
            }
        }
        assertFalse(maxConsecutive >= 10)
    }
}
