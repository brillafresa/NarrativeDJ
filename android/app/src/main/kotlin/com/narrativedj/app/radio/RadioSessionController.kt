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
 * Orchestrates messenger send → pool → similarity cushion plan → YTM search playback.
 *
 * Next track B is chosen from the candidate pool (most similar to now-playing A via LLM).
 * If similarity is below threshold, invented bridge search queries C are played before B.
 * No fixed song catalog is required at runtime.
 *
 * Occupancy phases: see [RadioPlaybackPolicy] (Idle / Live / PausedUser / StalePaused).
 */
class RadioSessionController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val requestParser: RequestParserService,
    private val scheduler: RadioScheduler,
    private val cushionPlanner: CushionBridgePlannerService,
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
    private var phase: RadioPlaybackPhase = RadioPlaybackPhase.IDLE
    private var confirmedPlaying = false
    private var idleMissCount = 0
    private var staleResumeAttempted = false
    private var pendingStaleResume = false
    private var isScheduling = false

    /** Host should call `NarrativeDJYtm.playPause(true)` once when this returns true. */
    fun consumeStaleResumeRequest(): Boolean {
        if (!pendingStaleResume) return false
        pendingStaleResume = false
        staleResumeAttempted = true
        return true
    }

    fun updateNowPlaying(title: String?, artist: String?, isPlaying: Boolean = false) {
        val label = listOfNotNull(title, artist).joinToString(" — ").ifBlank { null }
        val update = RadioPlaybackPolicy.nextPhase(
            previousPhase = phase,
            confirmedPlaying = confirmedPlaying,
            hasMetadata = label != null,
            isPlaying = isPlaying,
            idleMissCount = idleMissCount,
            staleResumeAttempted = staleResumeAttempted,
        )
        idleMissCount = update.idleMissCount
        val wasOccupied = phase != RadioPlaybackPhase.IDLE
        phase = update.phase
        confirmedPlaying = update.confirmedPlaying
        if (update.needsStaleResume && !staleResumeAttempted) {
            pendingStaleResume = true
        }

        if (label == null) {
            if (update.released && !isPlayingSequence && pendingEntry == null && !candidatePool.isEmpty()) {
                scheduleNextIfNeeded()
            }
            return
        }

        val playKey = CandidateEntry.normalizeKey(label)

        if (lastNowPlayingKey != null && lastNowPlayingKey != playKey && !isPlayingSequence) {
            idleMissCount = 0
            phase = RadioPlaybackPhase.LIVE
            confirmedPlaying = true
            onTrackTransition(
                previousTitle = currentTrackTitle,
                previousKey = lastNowPlayingKey!!,
                newTitle = title,
                newKey = playKey,
            )
        } else if (pendingEntry != null && (isPlaying || phase != RadioPlaybackPhase.IDLE)) {
            currentTrackKey = playKey
            currentTrackTitle = title
            pendingEntry = null
            idleMissCount = 0
            phase = RadioPlaybackPhase.LIVE
            confirmedPlaying = true
        }

        lastNowPlayingKey = playKey
        currentTrackTitle = title
        currentTrackKey = playKey

        if (update.released && wasOccupied && !isPlayingSequence && pendingEntry == null && !candidatePool.isEmpty()) {
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
        if (RadioPlaybackPolicy.shouldDeferPlayback(isPlayingSequence, pendingEntry != null, phase)) {
            if (!candidatePool.isEmpty() && !isPlayingSequence && pendingEntry == null) {
                when (phase) {
                    RadioPlaybackPhase.LIVE ->
                        onStatus(context.getString(R.string.status_queued_after_current, candidatePool.size()))
                    RadioPlaybackPhase.PAUSED_USER ->
                        onStatus(context.getString(R.string.status_queued_while_paused, candidatePool.size()))
                    RadioPlaybackPhase.STALE_PAUSED ->
                        onStatus(context.getString(R.string.status_resuming_track))
                    RadioPlaybackPhase.IDLE -> Unit
                }
            }
            return
        }
        if (candidatePool.isEmpty()) {
            onStatus(context.getString(R.string.status_nothing_to_play))
            return
        }
        if (isScheduling || isPlayingSequence) return
        scope.launch {
            isScheduling = true
            try {
                val decision = planNextDecision() ?: run {
                    onStatus(context.getString(R.string.status_nothing_to_play))
                    return@launch
                }
                playDecision(decision, afterTransition = currentTrackKey != null)
            } catch (e: Exception) {
                onStatus(context.getString(R.string.status_cushion_plan_error, e.message ?: "unknown"))
                val fallback = scheduler.pickNext(currentTrackKey, candidatePool, playHistory)
                if (fallback != null) {
                    playDecision(fallback, afterTransition = currentTrackKey != null)
                }
            } finally {
                isScheduling = false
            }
        }
    }

    private suspend fun planNextDecision(): ScheduleDecision? {
        val eligible = scheduler.eligibleEntries(candidatePool, playHistory)
        if (eligible.isEmpty()) return null
        val currentLabel = currentTrackTitle?.takeIf { it.isNotBlank() }
            ?: currentTrackKey?.takeIf { it.isNotBlank() }
        if (currentLabel == null) {
            return scheduler.directDecision(eligible.first())
        }
        onStatus(context.getString(R.string.status_planning_cushion))
        val plan = cushionPlanner.plan(
            currentTrackLabel = currentLabel,
            candidates = eligible,
            language = languageProvider(),
        )
        return scheduler.decisionFromPlan(plan, eligible)
            ?: scheduler.directDecision(eligible.first())
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
        currentTrackKey = newKey
        currentTrackTitle = newTitle

        if (candidatePool.isEmpty()) return
        if (isScheduling || isPlayingSequence) return

        scope.launch {
            isScheduling = true
            try {
                val decision = planNextDecision() ?: run {
                    isScheduling = false
                    return@launch
                }
                val shouldMent = djGate.onTrackTransition()
                if (shouldMent) {
                    djPipeline.runTransitionMent(
                        transition = DjTransitionContext(
                            channelName = context.getString(R.string.app_name),
                            language = languageProvider(),
                            previousTrackTitle = previousTitle,
                            nextTrackTitle = decision.targetEntry?.requestedLabel
                                ?: decision.targetEntry?.searchQuery,
                            nextSearchQuery = decision.targetEntry?.searchQuery,
                            isSubstitute = decision.targetEntry?.isSubstitute == true,
                            substituteNote = decision.targetEntry?.substituteReason,
                            moodHint = decision.targetEntry?.moodHint,
                            listenerSnippets = listenerMemory.recent(),
                        ),
                        onStatus = onStatus,
                        onComplete = {
                            playDecision(decision, afterTransition = true)
                            isScheduling = false
                        },
                    )
                } else {
                    playDecision(decision, afterTransition = true)
                    isScheduling = false
                }
            } catch (e: Exception) {
                onStatus(context.getString(R.string.status_cushion_plan_error, e.message ?: "unknown"))
                val fallback = scheduler.pickNext(newKey, candidatePool, playHistory)
                if (fallback != null) playDecision(fallback, afterTransition = true)
                isScheduling = false
            }
        }
    }

    private fun playDecision(decision: ScheduleDecision, afterTransition: Boolean) {
        if (decision.queries.isEmpty()) return
        isPlayingSequence = true
        pendingEntry = decision.targetEntry
        phase = RadioPlaybackPhase.LIVE
        confirmedPlaying = true
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
                phase = RadioPlaybackPhase.LIVE
                confirmedPlaying = true
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
