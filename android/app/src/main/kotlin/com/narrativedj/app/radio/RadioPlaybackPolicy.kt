package com.narrativedj.app.radio

/**
 * When to hold new pool items instead of starting YTM search immediately.
 *
 * Purpose: validate queue-after-current radio policy without interrupting live playback.
 * Verify: `./gradlew test --tests com.narrativedj.app.radio.RadioPlaybackPolicyTest`
 *
 * Live YTM `isPlaying` (play-button aria-label) is flaky — false negatives are common on
 * emulator audio stalls and DOM churn. Once the radio session is occupied, keep holding
 * while now-playing **metadata** is still visible; only release after
 * [idleMissThreshold] consecutive polls with no metadata and not playing.
 */
object RadioPlaybackPolicy {
    /** Polls at ~5s → two metadata gaps ≈ 10s before starting the queued next track. */
    const val DEFAULT_IDLE_MISS_THRESHOLD = 2

    fun shouldDeferPlayback(
        isPlayingSequence: Boolean,
        hasPendingEntry: Boolean,
        isNowPlaying: Boolean,
    ): Boolean {
        return isPlayingSequence || hasPendingEntry || isNowPlaying
    }

    /**
     * Updates sticky playback occupancy from a single now-playing poll.
     *
     * Acquire / keep:
     * - `isPlaying` true, or
     * - already occupied and title/artist metadata still present (ignores flaky isPlaying)
     *
     * Release:
     * - occupied but no metadata and not playing for [idleMissThreshold] polls
     */
    fun nextOccupancy(
        currentlyOccupied: Boolean,
        hasMetadata: Boolean,
        isPlaying: Boolean,
        idleMissCount: Int,
        idleMissThreshold: Int = DEFAULT_IDLE_MISS_THRESHOLD,
    ): OccupancyUpdate {
        if (isPlaying || (currentlyOccupied && hasMetadata)) {
            return OccupancyUpdate(
                occupied = true,
                idleMissCount = 0,
                released = false,
            )
        }
        if (!currentlyOccupied) {
            return OccupancyUpdate(
                occupied = false,
                idleMissCount = 0,
                released = false,
            )
        }
        val misses = idleMissCount + 1
        if (misses < idleMissThreshold) {
            return OccupancyUpdate(
                occupied = true,
                idleMissCount = misses,
                released = false,
            )
        }
        return OccupancyUpdate(
            occupied = false,
            idleMissCount = misses,
            released = true,
        )
    }
}

data class OccupancyUpdate(
    val occupied: Boolean,
    val idleMissCount: Int,
    /** True when this poll transitioned from occupied → idle (queue may start). */
    val released: Boolean,
)
