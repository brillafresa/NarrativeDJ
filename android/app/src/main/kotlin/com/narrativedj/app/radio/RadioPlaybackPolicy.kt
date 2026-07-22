package com.narrativedj.app.radio

/**
 * When to hold new pool items instead of starting YTM search immediately.
 *
 * Purpose: validate queue-after-current radio policy without interrupting live playback.
 * Verify: `./gradlew test --tests com.narrativedj.app.radio.RadioPlaybackPolicyTest`
 */
object RadioPlaybackPolicy {
    fun shouldDeferPlayback(
        isPlayingSequence: Boolean,
        hasPendingEntry: Boolean,
        isNowPlaying: Boolean,
    ): Boolean {
        return isPlayingSequence || hasPendingEntry || isNowPlaying
    }
}
