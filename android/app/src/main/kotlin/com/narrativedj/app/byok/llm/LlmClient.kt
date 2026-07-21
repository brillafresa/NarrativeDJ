package com.narrativedj.app.byok.llm

import com.narrativedj.app.dj.DjAudioControl

interface LlmClient {
    suspend fun generateTransitionMent(context: DjTransitionContext): DjAudioControl
}

object LlmResponseExtractor {
    fun extractJsonPayload(raw: String): String {
        val trimmed = raw.trim()
        val fenceMatch = Regex("```(?:json)?\\s*([\\s\\S]*?)```").find(trimmed)
        if (fenceMatch != null) return fenceMatch.groupValues[1].trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1)
        return trimmed
    }
}
