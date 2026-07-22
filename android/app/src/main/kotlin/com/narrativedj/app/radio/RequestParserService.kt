package com.narrativedj.app.radio

import com.narrativedj.app.byok.SecureKeyStore
import com.narrativedj.app.byok.llm.GeminiApi
import com.narrativedj.app.byok.llm.LlmResponseExtractor
import com.narrativedj.app.byok.llm.UserRequestContext
import com.narrativedj.app.byok.llm.UserRequestLlmClient
import com.narrativedj.app.byok.llm.UserRequestPromptBuilder
import com.narrativedj.app.locale.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RequestParserService(
    private val keyStore: SecureKeyStore,
) {
    suspend fun parse(
        message: String,
        language: AppLanguage,
    ): UserRequestParseResult {
        val apiKey = keyStore.getGeminiApiKey()
            ?: throw IllegalStateException("Gemini API key required")
        val context = UserRequestContext(message = message, language = language)
        val result = GeminiUserRequestClient(apiKey).parseUserRequest(context)
        if (!result.isComplete()) {
            throw IllegalStateException("Gemini returned an incomplete parse result")
        }
        return result
    }
}

private class GeminiUserRequestClient(private val apiKey: String) : UserRequestLlmClient {
    override suspend fun parseUserRequest(context: UserRequestContext): UserRequestParseResult {
        return withContext(Dispatchers.IO) {
            val prompt = UserRequestPromptBuilder.build(context)
            val text = GeminiApi.generateText(
                apiKey = apiKey,
                prompt = prompt,
                jsonMimeType = true,
            )
            UserRequestParser.parseJson(LlmResponseExtractor.extractJsonPayload(text))
                ?: throw IllegalStateException("Gemini response was not valid JSON")
        }
    }
}
