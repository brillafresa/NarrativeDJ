package com.narrativedj.app.byok.llm

import com.narrativedj.app.locale.AppLanguage
import com.narrativedj.app.radio.UserRequestParseResult
import com.narrativedj.app.radio.UserRequestParser

data class DjTransitionContext(
    val channelName: String,
    val language: AppLanguage,
    val previousTrackTitle: String?,
    val nextTrackTitle: String?,
    val nextSearchQuery: String?,
    val isSubstitute: Boolean = false,
    val substituteNote: String? = null,
    val moodHint: String? = null,
    val listenerSnippets: List<String> = emptyList(),
)

object DjTransitionPromptBuilder {
    fun build(context: DjTransitionContext): String {
        val memory = context.listenerSnippets.joinToString(" | ").ifBlank { "없음" }
        val substituteLine = if (context.isSubstitute) {
            context.substituteNote.orEmpty()
        } else {
            ""
        }
        return when (context.language) {
            AppLanguage.KOREAN -> """
                당신은 ${context.channelName} 라디오 DJ입니다.
                방금 재생: ${context.previousTrackTitle ?: "없음"}
                다음 재생: ${context.nextTrackTitle ?: context.nextSearchQuery ?: "알 수 없음"}
                ${if (substituteLine.isNotBlank()) "대체 안내: $substituteLine" else ""}
                ${context.moodHint?.let { "분위기 힌트: $it" }.orEmpty()}
                청취자 메모: $memory

                곡 사이 DJ 멘트 JSON만 반환 (markdown 없음):
                ducking_volume (0.0-1.0), ramp_duration, ramp_out_duration, script (한국어 1-2문장), ssml (선택)
                이전 곡과 다음 곡을 자연스럽게 연결. 대체곡이면 사과 포함. 청취자 잡담이 있으면 가볍게 언급.
            """.trimIndent()
            AppLanguage.ENGLISH -> """
                You are the radio DJ for ${context.channelName}.
                Previous: ${context.previousTrackTitle ?: "none"}
                Up next: ${context.nextTrackTitle ?: context.nextSearchQuery ?: "unknown"}
                ${if (substituteLine.isNotBlank()) "Substitute note: $substituteLine" else ""}
                ${context.moodHint?.let { "Mood: $it" }.orEmpty()}
                Listener notes: $memory

                Return ONLY JSON: ducking_volume, ramp_duration, ramp_out_duration, script (1-2 English sentences), ssml (optional).
                Bridge previous and next track. Apologize if substitute. Reference listener chat naturally if present.
            """.trimIndent()
        }
    }
}

fun interface UserRequestLlmClient {
    suspend fun parseUserRequest(context: UserRequestContext): UserRequestParseResult
}
