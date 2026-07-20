package com.narrativedj.app.byok.llm

import com.narrativedj.app.dj.DjAudioControl

fun interface LlmClient {
    suspend fun generateAudioControl(story: String, profileLabel: String): DjAudioControl
}

object LlmPromptBuilder {
    fun build(story: String, profileLabel: String): String = """
        You are a radio DJ for a $profileLabel space.
        Listener story: ${story.ifBlank { "(no story — welcome listeners warmly)" }}
        Respond with ONLY valid JSON (no markdown) using keys:
        ducking_volume (0.0-1.0), ramp_duration (seconds), ramp_out_duration (seconds),
        script (short spoken line, max 2 sentences), ssml (optional SSML string).
    """.trimIndent()
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
