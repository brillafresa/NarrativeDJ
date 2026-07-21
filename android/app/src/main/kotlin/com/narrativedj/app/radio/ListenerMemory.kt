package com.narrativedj.app.radio

/**
 * Short-term listener chat snippets for DJ transition ments.
 */
class ListenerMemory(private val maxEntries: Int = DEFAULT_MAX) {
    private val snippets = ArrayDeque<String>()

    fun add(snippet: String?) {
        val trimmed = snippet?.trim().orEmpty()
        if (trimmed.isEmpty()) return
        snippets.addLast(trimmed)
        while (snippets.size > maxEntries) {
            snippets.removeFirst()
        }
    }

    fun recent(limit: Int = 3): List<String> {
        return snippets.toList().takeLast(limit)
    }

    fun peekLatest(): String? = snippets.lastOrNull()

    companion object {
        const val DEFAULT_MAX = 10
    }
}
