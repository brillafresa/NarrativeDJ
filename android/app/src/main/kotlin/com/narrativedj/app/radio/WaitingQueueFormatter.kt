package com.narrativedj.app.radio

/**
 * Formats the waiting-request marquee line (not now-playing — that lives in YTM chrome).
 *
 * Verify: `./gradlew test --tests com.narrativedj.app.radio.WaitingQueueFormatterTest`
 */
object WaitingQueueFormatter {
    fun format(prefix: String, labels: List<String>, emptyPlaceholder: String): String {
        if (labels.isEmpty()) return emptyPlaceholder
        val body = labels.joinToString(" · ")
        // Repeat so marquee has room to scroll on short queues.
        val looped = if (body.length < 40) "$body   ★   $body   ★   $body" else body
        return "$prefix $looped"
    }

    fun labelsFromPool(pool: CandidatePool): List<String> {
        return pool.all().map { entry ->
            entry.requestedLabel?.takeIf { it.isNotBlank() } ?: entry.searchQuery
        }
    }
}
