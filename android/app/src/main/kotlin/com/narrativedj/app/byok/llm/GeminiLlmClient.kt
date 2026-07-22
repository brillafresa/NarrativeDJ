package com.narrativedj.app.byok.llm

import com.narrativedj.app.dj.DjAudioControl
import com.narrativedj.app.dj.DjAudioControlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiLlmClient(
    private val apiKey: String,
    private val model: String = GeminiApi.DEFAULT_MODEL,
) : LlmClient {

    override suspend fun generateTransitionMent(context: DjTransitionContext): DjAudioControl {
        return withContext(Dispatchers.IO) {
            val prompt = DjTransitionPromptBuilder.build(context)
            val text = GeminiApi.generateText(apiKey, prompt, model = model)
            val jsonPayload = LlmResponseExtractor.extractJsonPayload(text)
            DjAudioControlParser.parse(jsonPayload)
                ?: throw IllegalStateException("Gemini transition response was not valid JSON")
        }
    }
}
