package com.narrativedj.app.radio

/**
 * When to hold new pool items instead of starting YTM search immediately.
 *
 * Purpose: distinguish real playback, user pause, and cold-start stale pause.
 * Verify: `cd android && ./gradlew testDebugUnitTest --tests com.narrativedj.app.radio.RadioPlaybackPolicyTest`
 *
 * Live YTM `isPlaying` is flaky. Sticky metadata hold applies only after this process
 * has **confirmed** playing (`confirmedPlaying`). Cold metadata alone is [STALE_PAUSED]
 * (resume mid-track), not Live occupancy.
 */
object RadioPlaybackPolicy {
    /** Polls at ~5s → two metadata gaps ≈ 10s before starting the queued next track. */
    const val DEFAULT_IDLE_MISS_THRESHOLD = 2

    fun shouldDeferPlayback(
        isPlayingSequence: Boolean,
        hasPendingEntry: Boolean,
        phase: RadioPlaybackPhase,
    ): Boolean {
        return isPlayingSequence || hasPendingEntry || phase != RadioPlaybackPhase.IDLE
    }

    /**
     * Advances radio playback phase from a single now-playing poll.
     *
     * - [RadioPlaybackPhase.LIVE]: `isPlaying` true (confirms playback)
     * - [RadioPlaybackPhase.PAUSED_USER]: confirmed before, same-track metadata, not playing
     * - [RadioPlaybackPhase.STALE_PAUSED]: metadata on launch before any confirm → one-shot resume
     * - [RadioPlaybackPhase.IDLE]: no radio slot (may schedule from pool)
     */
    fun nextPhase(
        previousPhase: RadioPlaybackPhase,
        confirmedPlaying: Boolean,
        hasMetadata: Boolean,
        isPlaying: Boolean,
        idleMissCount: Int,
        staleResumeAttempted: Boolean = false,
        idleMissThreshold: Int = DEFAULT_IDLE_MISS_THRESHOLD,
    ): PhaseUpdate {
        if (isPlaying) {
            return PhaseUpdate(
                phase = RadioPlaybackPhase.LIVE,
                confirmedPlaying = true,
                idleMissCount = 0,
                released = false,
                needsStaleResume = false,
            )
        }

        if (confirmedPlaying && hasMetadata) {
            return PhaseUpdate(
                phase = RadioPlaybackPhase.PAUSED_USER,
                confirmedPlaying = true,
                idleMissCount = 0,
                released = false,
                needsStaleResume = false,
            )
        }

        if (confirmedPlaying && !hasMetadata) {
            val misses = idleMissCount + 1
            if (misses < idleMissThreshold) {
                return PhaseUpdate(
                    phase = previousPhase.coerceOccupied(),
                    confirmedPlaying = true,
                    idleMissCount = misses,
                    released = false,
                    needsStaleResume = false,
                )
            }
            return PhaseUpdate(
                phase = RadioPlaybackPhase.IDLE,
                confirmedPlaying = false,
                idleMissCount = misses,
                released = previousPhase != RadioPlaybackPhase.IDLE,
                needsStaleResume = false,
            )
        }

        // Not confirmed this process.
        if (hasMetadata) {
            if (!staleResumeAttempted) {
                return PhaseUpdate(
                    phase = RadioPlaybackPhase.STALE_PAUSED,
                    confirmedPlaying = false,
                    idleMissCount = 0,
                    released = false,
                    needsStaleResume = true,
                )
            }
            // One-shot resume already tried — allow pool scheduling.
            return PhaseUpdate(
                phase = RadioPlaybackPhase.IDLE,
                confirmedPlaying = false,
                idleMissCount = 0,
                released = previousPhase == RadioPlaybackPhase.STALE_PAUSED,
                needsStaleResume = false,
            )
        }

        return PhaseUpdate(
            phase = RadioPlaybackPhase.IDLE,
            confirmedPlaying = false,
            idleMissCount = 0,
            released = previousPhase != RadioPlaybackPhase.IDLE,
            needsStaleResume = false,
        )
    }

    private fun RadioPlaybackPhase.coerceOccupied(): RadioPlaybackPhase {
        return when (this) {
            RadioPlaybackPhase.IDLE -> RadioPlaybackPhase.PAUSED_USER
            else -> this
        }
    }
}

enum class RadioPlaybackPhase {
    IDLE,
    LIVE,
    PAUSED_USER,
    STALE_PAUSED,
}

data class PhaseUpdate(
    val phase: RadioPlaybackPhase,
    val confirmedPlaying: Boolean,
    val idleMissCount: Int,
    /** True when this poll transitioned into idle (queue may start). */
    val released: Boolean,
    /** Cold/stale mid-track — host should call playPause(true) once. */
    val needsStaleResume: Boolean,
)
