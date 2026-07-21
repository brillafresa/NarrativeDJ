package com.narrativedj.app.radio

import android.content.Context
import com.narrativedj.app.R
import com.narrativedj.app.byok.llm.DjTransitionContext
import com.narrativedj.app.dj.DjPipeline
import com.narrativedj.app.locale.AppLanguage
import com.narrativedj.app.profile.SpaceProfile
import com.narrativedj.app.scheduler.CatalogTrack
import com.narrativedj.app.scheduler.CushionPlaybackController
import com.narrativedj.app.scheduler.CushionRoutePlanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Orchestrates messenger send → pool → auto scheduling → DJ interstitials.
 */
class RadioSessionController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val requestParser: RequestParserService,
    private val scheduler: RadioScheduler,
    private val cushionPlayback: CushionPlaybackController,
    private val djPipeline: DjPipeline,
    private val catalog: List<CatalogTrack>,
    private val planner: CushionRoutePlanner,
    private val languageProvider: () -> AppLanguage,
    private val onStatus: (String) -> Unit,
) {
    val candidatePool = CandidatePool()
    val playHistory = PlayHistory()
    val listenerMemory = ListenerMemory()
    private val djGate = DjInterstitialGate()

    private var currentTrackId: String? = null
    private var currentTrackTitle: String? = null
    private var lastNowPlayingKey: String? = null
    private var pendingEntry: CandidateEntry? = null
    private var isPlayingSequence = false
    private var selectedProfile: SpaceProfile = com.narrativedj.app.profile.SpaceProfiles.cozyBrunchCafe

    fun setProfile(profile: SpaceProfile) {
        selectedProfile = profile
    }

    fun updateNowPlaying(title: String?, artist: String?) {
        val label = listOfNotNull(title, artist).joinToString(" — ").ifBlank { null }
        if (label == null) return

        val resolvedId = planner.resolveTrackId(title)
        val playKey = resolvedId ?: CandidateEntry.normalizeKey(label)

        if (lastNowPlayingKey != null && lastNowPlayingKey != playKey && !isPlayingSequence) {
            onTrackTransition(
                previousTitle = currentTrackTitle,
                previousId = currentTrackId,
                previousKey = lastNowPlayingKey!!,
                newTitle = title,
                newId = resolvedId,
            )
        }

        lastNowPlayingKey = playKey
        currentTrackTitle = title
        if (resolvedId != null) currentTrackId = resolvedId
    }

    fun handleUserSend(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return
        scope.launch {
            onStatus(context.getString(R.string.status_parsing_request))
            val language = languageProvider()
            val result = requestParser.parse(trimmed, selectedProfile, language)
            applyParseResult(result)
            scheduleNextIfNeeded()
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
        if (isPlayingSequence) return
        if (lastNowPlayingKey != null) return
        val decision = scheduler.pickImmediate(candidatePool, playHistory, selectedProfile) ?: run {
            onStatus(context.getString(R.string.status_nothing_to_play))
            return
        }
        playDecision(decision, afterTransition = false)
    }

    private fun onTrackTransition(
        previousTitle: String?,
        previousId: String?,
        previousKey: String,
        newTitle: String?,
        newId: String?,
    ) {
        playHistory.record(previousKey)
        pendingEntry?.let { candidatePool.remove(it) }
        pendingEntry = null

        val shouldMent = djGate.onTrackTransition()
        if (shouldMent) {
            val nextDecision = scheduler.pickNext(newId, candidatePool, playHistory, selectedProfile)
            scope.launch {
                djPipeline.runTransitionMent(
                    transition = DjTransitionContext(
                        profileLabel = selectedProfile.label(context),
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
            val nextDecision = scheduler.pickNext(newId, candidatePool, playHistory, selectedProfile)
            if (nextDecision != null) playDecision(nextDecision, afterTransition = true)
        }

        currentTrackId = newId
        currentTrackTitle = newTitle
    }

    private fun playDecision(decision: ScheduleDecision, afterTransition: Boolean) {
        if (decision.queries.isEmpty()) return
        isPlayingSequence = true
        pendingEntry = decision.targetEntry
        if (decision.fromPool && decision.targetEntry != null) {
            candidatePool.remove(decision.targetEntry)
        }
        val summary = decision.queries.joinToString(" → ")
        onStatus(context.getString(R.string.status_scheduled_play, summary))
        cushionPlayback.playSequence(
            queries = decision.queries,
            onStep = { _, query -> onStatus(context.getString(R.string.status_now_playing_query, query)) },
            onComplete = {
                isPlayingSequence = false
                decision.targetEntry?.catalogTrackId?.let { currentTrackId = it }
                decision.targetEntry?.requestedLabel?.let { currentTrackTitle = it }
                if (!afterTransition) {
                    decision.targetEntry?.playKey()?.let { playHistory.record(it) }
                }
            },
        )
    }

    fun getCurrentTrackId(): String? = currentTrackId
}

private fun CatalogTrack.playbackQuery(): String = searchQuery?.takeIf { it.isNotBlank() } ?: title
