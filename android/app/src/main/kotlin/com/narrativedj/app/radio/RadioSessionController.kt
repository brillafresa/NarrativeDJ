package com.narrativedj.app.radio

import android.content.Context
import com.narrativedj.app.R
import com.narrativedj.app.byok.llm.DjTransitionContext
import com.narrativedj.app.dj.DjPipeline
import com.narrativedj.app.locale.AppLanguage
import com.narrativedj.app.scheduler.CushionPlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Orchestrates messenger send → pool → auto scheduling → DJ interstitials.
 * Playback: user text → YTM search_query (no bundled demo catalog).
 *
 * While a track is actively playing, new requests stay in the candidate pool
 * and only start after the current track changes (or playback is idle).
 */
class RadioSessionController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val requestParser: RequestParserService,
    private val scheduler: RadioScheduler,
    private val cushionPlayback: CushionPlaybackController,
    private val djPipeline: DjPipeline,
    private val languageProvider: () -> AppLanguage,
    private val onStatus: (String) -> Unit,
) {
    val candidatePool = CandidatePool()
    val playHistory = PlayHistory()
    val listenerMemory = ListenerMemory()
    private val djGate = DjInterstitialGate()

    private var currentTrackKey: String? = null
    private var currentTrackTitle: String? = null
    private var lastNowPlayingKey: String? = null
    private var pendingEntry: CandidateEntry? = null
    private var isPlayingSequence = false
    /** Sticky occupancy — see [RadioPlaybackPolicy.nextOccupancy]. */
    private var isNowPlaying = false
    private var idleMissCount = 0

    fun updateNowPlaying(title: String?, artist: String?, isPlaying: Boolean = false) {
        val label = listOfNotNull(title, artist).joinToString(" — ").ifBlank { null }
        val occupancy = RadioPlaybackPolicy.nextOccupancy(
            currentlyOccupied = isNowPlaying,
            hasMetadata = label != null,
            isPlaying = isPlaying,
            idleMissCount = idleMissCount,
        )
        idleMissCount = occupancy.idleMissCount
        val wasOccupied = isNowPlaying
        isNowPlaying = occupancy.occupied

        if (label == null) {
            // Metadata gap: keep sticky hold; only advance queue after confirmed release.
            if (occupancy.released && !isPlayingSequence && pendingEntry == null && !candidatePool.isEmpty()) {
                scheduleNextIfNeeded()
            }
            return
        }

        val playKey = CandidateEntry.normalizeKey(label)

        if (lastNowPlayingKey != null && lastNowPlayingKey != playKey && !isPlayingSequence) {
            idleMissCount = 0
            isNowPlaying = true
            onTrackTransition(
                previousTitle = currentTrackTitle,
                previousKey = lastNowPlayingKey!!,
                newTitle = title,
                newKey = playKey,
            )
        } else if (pendingEntry != null && (isPlaying || occupancy.occupied)) {
            // First metadata after our search started — mark session as occupied.
            currentTrackKey = playKey
            currentTrackTitle = title
            pendingEntry = null
            idleMissCount = 0
            isNowPlaying = true
        }

        lastNowPlayingKey = playKey
        currentTrackTitle = title
        currentTrackKey = playKey

        // Confirmed idle after sticky misses — start queued next track.
        if (occupancy.released && wasOccupied && !isPlayingSequence && pendingEntry == null && !candidatePool.isEmpty()) {
            scheduleNextIfNeeded()
        }
    }

    fun handleUserSend(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return
        scope.launch {
            onStatus(context.getString(R.string.status_parsing_request))
            val language = languageProvider()
            try {
                val result = requestParser.parse(trimmed, language)
                applyParseResult(result)
                scheduleNextIfNeeded()
            } catch (e: Exception) {
                onStatus(context.getString(R.string.status_parse_error, e.message ?: "unknown"))
            }
        }
    }

    private fun applyParseResult(result: UserRequestParseResult) {
        val added = candidatePool.addAll(result.toCandidateEntries())
        listenerMemory.add(result.chatSnippet)
        val poolSize = candidatePool.size()
        onStatus(
            when (result.intent) {
                UserRequestIntent.CHAT_ONLY -> context.getString(R.string.status_chat_noted, poolSize)
                UserRequestIntent.MOOD_REQUEST -> context.getString(R.string.status_mood_queued, added, poolSize)
                else -> context.getString(R.string.status_tracks_queued, added, poolSize)
            },
        )
    }

    fun scheduleNextIfNeeded() {
        if (RadioPlaybackPolicy.shouldDeferPlayback(isPlayingSequence, pendingEntry != null, isNowPlaying)) {
            if (!candidatePool.isEmpty() && isNowPlaying && !isPlayingSequence && pendingEntry == null) {
                onStatus(context.getString(R.string.status_queued_after_current, candidatePool.size()))
            }
            return
        }
        if (candidatePool.isEmpty()) {
            onStatus(context.getString(R.string.status_nothing_to_play))
            return
        }
        val decision = scheduler.pickNext(
            currentTrackKey = currentTrackKey,
            pool = candidatePool,
            history = playHistory,
        ) ?: run {
            onStatus(context.getString(R.string.status_nothing_to_play))
            return
        }
        playDecision(decision, afterTransition = currentTrackKey != null)
    }

    private fun onTrackTransition(
        previousTitle: String?,
        previousKey: String,
        newTitle: String?,
        newKey: String,
    ) {
        playHistory.record(previousKey)
        pendingEntry?.let { candidatePool.remove(it) }
        pendingEntry = null

        val shouldMent = djGate.onTrackTransition()
        if (shouldMent) {
            val nextDecision = scheduler.pickNext(newKey, candidatePool, playHistory)
            scope.launch {
                djPipeline.runTransitionMent(
                    transition = DjTransitionContext(
                        channelName = context.getString(R.string.app_name),
                        language = languageProvider(),
                        previousTrackTitle = previousTitle,
                        nextTrackTitle = nextDecision?.targetEntry?.requestedLabel
                            ?: nextDecision?.targetEntry?.searchQuery,
                        nextSearchQuery = nextDecision?.targetEntry?.searchQuery,
                        isSubstitute = nextDecision?.targetEntry?.isSubstitute == true,
                        substituteNote = nextDecision?.targetEntry?.substituteReason,
                        moodHint = nextDecision?.targetEntry?.moodHint,
                        listenerSnippets = listenerMemory.recent(),
                    ),
                    onStatus = onStatus,
                    onComplete = {
                        if (nextDecision != null) playDecision(nextDecision, afterTransition = true)
                    },
                )
            }
        } else {
            val nextDecision = scheduler.pickNext(newKey, candidatePool, playHistory)
            if (nextDecision != null) {
                playDecision(nextDecision, afterTransition = true)
            }
        }

        currentTrackKey = newKey
        currentTrackTitle = newTitle
    }

    private fun playDecision(decision: ScheduleDecision, afterTransition: Boolean) {
        if (decision.queries.isEmpty()) return
        isPlayingSequence = true
        pendingEntry = decision.targetEntry
        // Optimistic hold: do not start another search while this one loads/plays.
        isNowPlaying = true
        idleMissCount = 0
        if (decision.fromPool && decision.targetEntry != null) {
            candidatePool.remove(decision.targetEntry)
        }
        val summary = decision.queries.joinToString(" → ")
        val statusMsg = if (decision.usedCushion) {
            context.getString(R.string.status_cushion_play, decision.bridgeCount, summary)
        } else {
            context.getString(R.string.status_scheduled_play, summary)
        }
        onStatus(statusMsg)
        cushionPlayback.playSequence(
            queries = decision.queries,
            onStep = { _, query -> onStatus(context.getString(R.string.status_now_playing_query, query)) },
            onComplete = {
                isPlayingSequence = false
                // Keep occupancy sticky until now-playing polls confirm idle or track change.
                isNowPlaying = true
                idleMissCount = 0
                decision.targetEntry?.playKey()?.let { currentTrackKey = it }
                decision.targetEntry?.requestedLabel?.let { currentTrackTitle = it }
                if (!afterTransition) {
                    decision.targetEntry?.playKey()?.let { playHistory.record(it) }
                }
            },
        )
    }
}
