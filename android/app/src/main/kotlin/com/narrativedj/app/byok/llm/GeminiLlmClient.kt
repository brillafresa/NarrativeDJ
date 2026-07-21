package com.narrativedj.app.byok.llm

import com.narrativedj.app.dj.DjAudioControl
import com.narrativedj.app.dj.DjAudioControlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GeminiLlmClient(
    private val apiKey: String,
    private val model: String = "gemini-1.5-flash",
) : LlmClient {

    override suspend fun generateTransitionMent(context: DjTransitionContext): DjAudioControl {
        return withContext(Dispatchers.IO) {
            val prompt = DjTransitionPromptBuilder.build(context)
            val text = postGenerateContent(prompt)
            val jsonPayload = LlmResponseExtractor.extractJsonPayload(text)
            DjAudioControlParser.parse(jsonPayload)
                ?: DjAudioControlParser.fallbackForTransition(context)
        }
    }

    private fun postGenerateContent(prompt: String): String {
        val endpoint = URL(
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey",
        )
        val body = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt))),
            ))
        }
        val responseText = postJson(endpoint, body.toString(), mapOf("Content-Type" to "application/json"))
        return JSONObject(responseText).getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
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
            throw IllegalStateException("Gemini API ${connection.responseCode}: $response")
        }
        return response
    }
}
