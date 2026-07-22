package com.narrativedj.app.byok.llm

import com.narrativedj.app.locale.AppLanguage

data class UserRequestContext(
    val message: String,
    val language: AppLanguage,
)

object UserRequestPromptBuilder {
    fun build(context: UserRequestContext): String {
        return when (context.language) {
            AppLanguage.KOREAN -> """
                당신은 라디오 DJ 앱의 요청 해석기입니다. 사용자 메시지를 분석해 JSON만 반환하세요.
                재생은 YouTube Music 검색(search_query)으로만 이루어집니다. 고정 곡 목록은 없습니다.

                intent: explicit_tracks | mood_request | chat_only | mixed
                tracks: [{ requested_title, search_query, is_substitute, substitute_note }]
                mood_hint: string or null
                chat_snippet: string or null

                규칙:
                - 곡/아티스트 언급 → explicit_tracks 또는 mixed. search_query는 YT Music에 넣을 검색어.
                - 분위기/상황 → mood_request, tracks 1~3개(각각 search_query), mood_hint 포함.
                - 잡담만 → chat_only, tracks=[], chat_snippet에 요약.
                - search_query는 항상 실제 검색 가능한 문자열(예: "4 Non Blondes What's Up", "비 오는 날 잔잔한 음악").
                - 즉시 멘트 생성 금지. JSON만.

                사용자: ${context.message.trim()}
            """.trimIndent()
            AppLanguage.ENGLISH -> """
                You parse listener messages for a radio DJ app. Return ONLY JSON.
                Playback uses YouTube Music search (search_query) only. There is no fixed song catalog.

                Keys: intent (explicit_tracks|mood_request|chat_only|mixed),
                tracks [{ requested_title, search_query, is_substitute, substitute_note }],
                mood_hint, chat_snippet.

                Rules:
                - Song/artist → explicit_tracks or mixed with YT Music search_query.
                - Mood/situation → mood_request, 1-3 search_query tracks, mood_hint.
                - Chat only → chat_only, empty tracks, chat_snippet summary.
                - search_query must be a real YT Music search string.
                Do NOT generate spoken DJ lines. JSON only.

                User: ${context.message.trim()}
            """.trimIndent()
        }
    }
}
