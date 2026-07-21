package com.narrativedj.app.radio

import kotlin.random.Random

/**
 * Fires DJ interstitial ments randomly every 1–2 track transitions.
 */
class DjInterstitialGate(
    private val random: Random = Random.Default,
) {
    private var songsSinceLastMent = 0
    private var threshold = nextThreshold()

    fun onTrackTransition(): Boolean {
        songsSinceLastMent++
        if (songsSinceLastMent >= threshold) {
            songsSinceLastMent = 0
            threshold = nextThreshold()
            return true
        }
        return false
    }

    fun reset() {
        songsSinceLastMent = 0
        threshold = nextThreshold()
    }

    private fun nextThreshold(): Int = random.nextInt(1, 3)
}
