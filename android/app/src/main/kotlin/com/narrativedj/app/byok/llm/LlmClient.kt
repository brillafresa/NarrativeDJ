package com.narrativedj.app.byok.llm

import com.narrativedj.app.dj.DjAudioControl
import com.narrativedj.app.locale.AppLanguage

fun interface LlmClient {
    suspend fun generateAudioControl(
        story: String,
        profileLabel: String,
        language: AppLanguage,
    ): DjAudioControl
}

object LlmPromptBuilder {
    fun build(story: String, profileLabel: String, language: AppLanguage): String {
        val storyText = story.ifBlank {
            when (language) {
                AppLanguage.KOREAN -> "(사연 없음 — 청취자를 따뜻하게 맞이하세요)"
                AppLanguage.ENGLISH -> "(no story — welcome listeners warmly)"
            }
        }
        return when (language) {
            AppLanguage.KOREAN -> """
                당신은 '$profileLabel' 공간을 위한 라디오 DJ입니다.
                청취자 사연: $storyText
                script 필드는 반드시 자연스러운 한국어로 작성하세요 (최대 2문장).
                markdown 없이 JSON만 반환하세요. 키:
                ducking_volume (0.0-1.0), ramp_duration (초), ramp_out_duration (초),
                script (짧은 DJ 멘트), ssml (선택 SSML 문자열).
            """.trimIndent()
            AppLanguage.ENGLISH -> """
                You are a radio DJ for a $profileLabel space.
                Listener story: $storyText
                Write the script field in natural English (max 2 sentences).
                Respond with ONLY valid JSON (no markdown) using keys:
                ducking_volume (0.0-1.0), ramp_duration (seconds), ramp_out_duration (seconds),
                script (short spoken line), ssml (optional SSML string).
            """.trimIndent()
        }
    }
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
