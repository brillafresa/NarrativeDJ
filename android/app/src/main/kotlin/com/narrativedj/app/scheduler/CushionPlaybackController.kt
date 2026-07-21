package com.narrativedj.app.scheduler

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import org.json.JSONObject

/**
 * Executes cushion bridge sequences via YTM JS search/play API.
 */
class CushionPlaybackController(
    private val catalog: List<CatalogTrack>,
    private val webView: WebView? = null,
    private val handler: Handler? = null,
    private val stepDelayMs: Long = DEFAULT_STEP_DELAY_MS,
) {
    fun searchQueryFor(trackId: String): String? {
        return catalog.firstOrNull { it.id == trackId }?.playbackQuery()
    }

    fun buildPlayOrder(plan: CushionPlan): List<String> {
        if (plan.dropped) return emptyList()
        val ids = plan.bridgeIds + plan.targetTrackId
        return ids.mapNotNull { searchQueryFor(it) }
    }

    fun playSequence(
        queries: List<String>,
        onStep: ((index: Int, query: String) -> Unit)? = null,
        onComplete: (() -> Unit)? = null,
    ) {
        if (queries.isEmpty()) {
            onComplete?.invoke()
            return
        }
        playStep(queries, 0, onStep, onComplete)
    }

    fun playPlan(
        plan: CushionPlan,
        onStep: ((index: Int, query: String) -> Unit)? = null,
        onComplete: (() -> Unit)? = null,
    ) {
        playSequence(buildPlayOrder(plan), onStep, onComplete)
    }

    private fun playStep(
        queries: List<String>,
        index: Int,
        onStep: ((Int, String) -> Unit)?,
        onComplete: (() -> Unit)?,
    ) {
        if (index >= queries.size) {
            onComplete?.invoke()
            return
        }
        val query = queries[index]
        onStep?.invoke(index, query)
        val view = webView ?: run {
            onComplete?.invoke()
            return
        }
        val escaped = JSONObject.quote(query)
        view.evaluateJavascript("NarrativeDJYtm.searchAndPlay($escaped);") {
            scheduleDelayed(
                { playStep(queries, index + 1, onStep, onComplete) },
                stepDelayMs,
            )
        }
    }

    private fun scheduleDelayed(action: () -> Unit, delayMs: Long) {
        val h = handler
        if (h != null) {
            h.postDelayed({ action() }, delayMs)
        } else {
            action()
        }
    }

    companion object {
        const val DEFAULT_STEP_DELAY_MS = 2_500L
    }
}

fun CatalogTrack.playbackQuery(): String = searchQuery?.takeIf { it.isNotBlank() } ?: title
