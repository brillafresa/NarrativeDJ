package com.narrativedj.app.scheduler

import android.os.Handler
import android.webkit.WebView
import com.narrativedj.app.webview.YtmSearchNavigation
import org.json.JSONObject

/**
 * Executes YTM search/play sequences via WebView JS API (bridge then target queries).
 *
 * Verify: `./gradlew test --tests com.narrativedj.app.scheduler.CushionPlaybackControllerTest`
 */
class CushionPlaybackController(
    private val webView: WebView? = null,
    private val handler: Handler? = null,
    private val stepDelayMs: Long = DEFAULT_STEP_DELAY_MS,
) {
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
        YtmSearchNavigation.begin()
        view.evaluateJavascript("NarrativeDJYtm.searchAndPlay($escaped);") {
            scheduleDelayed({ YtmSearchNavigation.end() }, SEARCH_NAV_FLAG_MS)
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
        const val DEFAULT_STEP_DELAY_MS = 8_000L
        private const val SEARCH_NAV_FLAG_MS = 4_000L
    }
}
