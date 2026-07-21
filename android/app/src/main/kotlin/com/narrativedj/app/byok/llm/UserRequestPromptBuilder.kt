package com.narrativedj.app.byok.llm

import com.narrativedj.app.locale.AppLanguage
import com.narrativedj.app.profile.SpaceProfile

data class UserRequestContext(
    val message: String,
    val profile: SpaceProfile,
    val language: AppLanguage,
    val catalogTitles: List<String>,
)

object UserRequestPromptBuilder {
    fun build(context: UserRequestContext): String {
        val catalogList = context.catalogTitles.joinToString(", ")
        val profileLabel = profileLabel(context.profile, context.language)
        return when (context.language) {
            AppLanguage.KOREAN -> """
                당신은 라디오 DJ 앱의 요청 해석기입니다. 사용자 메시지를 분석해 JSON만 반환하세요.
                공간 프로필: $profileLabel
                카탈로그 곡 (참고): $catalogList

                intent: explicit_tracks | mood_request | chat_only | mixed
                tracks: [{ requested_title, search_query, is_substitute, substitute_note }]
                mood_hint: string or null
                chat_snippet: string or null

                규칙:
                - 곡명/아티스트 언급 → explicit_tracks 또는 mixed. search_query는 YT Music 검색용.
                - 카탈로그에 없는 곡 → is_substitute=true, 유사곡 search_query, substitute_note에 원곡명.
                - 분위기/상황만 → mood_request, tracks 1~3개 search_query, mood_hint 포함.
                - 잡담만 → chat_only, tracks=[], chat_snippet에 요약.
                - 즉시 멘트 생성 금지. JSON만.

                사용자: ${context.message.trim()}
            """.trimIndent()
            AppLanguage.ENGLISH -> """
                You parse listener messages for a radio DJ app. Return ONLY JSON.
                Space profile: $profileLabel
                Catalog (reference): $catalogList

                Keys: intent (explicit_tracks|mood_request|chat_only|mixed),
                tracks [{ requested_title, search_query, is_substitute, substitute_note }],
                mood_hint, chat_snippet.

                Rules:
                - Song/artist mentions → explicit_tracks or mixed with YT Music search_query.
                - Unknown song → is_substitute=true, similar search_query, substitute_note with original.
                - Mood/situation only → mood_request, 1-3 search_query tracks, mood_hint.
                - Chat only → chat_only, empty tracks, chat_snippet summary.
                Do NOT generate spoken DJ lines. JSON only.

                User: ${context.message.trim()}
            """.trimIndent()
        }
    }

    private fun profileLabel(profile: SpaceProfile, language: AppLanguage): String {
        return when (profile.id) {
            "cozy_brunch_cafe" -> if (language == AppLanguage.KOREAN) "아늑한 브런치 카페" else "Cozy brunch café"
            "analog_lp_bar" -> if (language == AppLanguage.KOREAN) "아날로그 LP 바" else "Analog LP bar"
            "quiet_bookstore" -> if (language == AppLanguage.KOREAN) "조용한 서점" else "Quiet bookstore"
            else -> profile.id
        }
    }
}
