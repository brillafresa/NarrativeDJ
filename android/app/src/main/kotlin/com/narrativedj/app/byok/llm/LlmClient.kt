package com.narrativedj.app.byok.llm

import com.narrativedj.app.dj.DjAudioControl
import com.narrativedj.app.locale.AppLanguage

data class DjStoryContext(
    val story: String,
    val profileLabel: String,
    val language: AppLanguage,
    val currentTrackTitle: String? = null,
    val targetTrackTitle: String? = null,
)

fun interface LlmClient {
    suspend fun generateAudioControl(context: DjStoryContext): DjAudioControl
}

object LlmPromptBuilder {
    fun build(context: DjStoryContext): String {
        val storyText = context.story.ifBlank {
            when (context.language) {
                AppLanguage.KOREAN -> "(사연 없음 — 청취자를 따뜻하게 맞이하세요)"
                AppLanguage.ENGLISH -> "(no story — welcome listeners warmly)"
            }
        }
        val trackContext = buildTrackContext(context)
        return when (context.language) {
            AppLanguage.KOREAN -> """
                당신은 '${context.profileLabel}' 공간을 위한 라디오 DJ입니다.
                $trackContext
                청취자 사연: $storyText
                script 필드는 반드시 자연스러운 한국어로 작성하세요 (최대 2문장).
                markdown 없이 JSON만 반환하세요. 키:
                ducking_volume (0.0-1.0), ramp_duration (초), ramp_out_duration (초),
                script (짧은 DJ 멘트), ssml (선택 SSML 문자열).
            """.trimIndent()
            AppLanguage.ENGLISH -> """
                You are a radio DJ for a ${context.profileLabel} space.
                $trackContext
                Listener story: $storyText
                Write the script field in natural English (max 2 sentences).
                Respond with ONLY valid JSON (no markdown) using keys:
                ducking_volume (0.0-1.0), ramp_duration (seconds), ramp_out_duration (seconds),
                script (short spoken line), ssml (optional SSML string).
            """.trimIndent()
        }
    }

    private fun buildTrackContext(context: DjStoryContext): String {
        val current = context.currentTrackTitle
        val target = context.targetTrackTitle
        if (current.isNullOrBlank() && target.isNullOrBlank()) return ""
        return when (context.language) {
            AppLanguage.KOREAN -> buildString {
                if (!current.isNullOrBlank()) append("현재 재생: $current. ")
                if (!target.isNullOrBlank()) append("다음 쿠션 목표: $target.")
            }
            AppLanguage.ENGLISH -> buildString {
                if (!current.isNullOrBlank()) append("Now playing: $current. ")
                if (!target.isNullOrBlank()) append("Cushion target: $target.")
            }
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
