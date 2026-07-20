package com.narrativedj.app.byok.llm

import com.narrativedj.app.dj.DjAudioControl
import com.narrativedj.app.dj.DjAudioControlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class OpenAiLlmClient(
    private val apiKey: String,
    private val model: String = "gpt-4o-mini",
) : LlmClient {

    override suspend fun generateAudioControl(story: String, profileLabel: String): DjAudioControl {
        return withContext(Dispatchers.IO) {
            val prompt = LlmPromptBuilder.build(story, profileLabel)
            val endpoint = URL("https://api.openai.com/v1/chat/completions")
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("response_format", JSONObject().put("type", "json_object"))
            }
            val responseText = postJson(
                endpoint,
                body.toString(),
                mapOf(
                    "Content-Type" to "application/json",
                    "Authorization" to "Bearer $apiKey",
                ),
            )
            val root = JSONObject(responseText)
            val text = root.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
            val jsonPayload = LlmResponseExtractor.extractJsonPayload(text)
            DjAudioControlParser.parse(jsonPayload)
                ?: DjAudioControlParser.fallbackForStory(story)
        }
    }

    private fun postJson(url: URL, body: String, headers: Map<String, String>): String {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 30_000
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
        }
        connection.outputStream.bufferedWriter().use { it.write(body) }
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        val response = stream.bufferedReader().use { it.readText() }
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("OpenAI API ${connection.responseCode}: $response")
        }
        return response
    }
}
