package com.narrativedj.app.byok.llm

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Shared Gemini generateContent caller.
 *
 * Purpose: single production HTTP path for request parse + DJ transition ments.
 * Default model: [DEFAULT_MODEL] (Flash-Lite — lower load / fewer 503s than full Flash).
 * On 503, optional [capacitySession] advances to the next allow-listed model and retries.
 *
 * Verify: `./gradlew test --tests com.narrativedj.app.byok.llm.GeminiApiTest`
 */
object GeminiApi {
    const val DEFAULT_MODEL = GeminiModelCatalog.DEFAULT_MODEL

    fun generateText(
        apiKey: String,
        prompt: String,
        model: String = DEFAULT_MODEL,
        jsonMimeType: Boolean = false,
        capacitySession: GeminiModelSession? = null,
        onCapacityFallback: (from: String, to: String) -> Unit = { _, _ -> },
    ): String {
        if (capacitySession == null) {
            return generateTextOnce(apiKey, prompt, model, jsonMimeType)
        }
        return withCapacityFallback(capacitySession, onCapacityFallback) { attemptModel ->
            generateTextOnce(apiKey, prompt, attemptModel, jsonMimeType)
        }
    }

    /**
     * Retry [block] on 503 by advancing [session] sticky model until success or chain exhausted.
     */
    fun <T> withCapacityFallback(
        session: GeminiModelSession,
        onCapacityFallback: (from: String, to: String) -> Unit = { _, _ -> },
        block: (model: String) -> T,
    ): T {
        while (true) {
            val model = session.current()
            try {
                return block(model)
            } catch (e: GeminiHttpException) {
                if (!e.isCapacityUnavailable) throw e
                val next = session.advanceAfterCapacityError() ?: throw e
                onCapacityFallback(model, next)
            }
        }
    }

    private fun generateTextOnce(
        apiKey: String,
        prompt: String,
        model: String,
        jsonMimeType: Boolean,
    ): String {
        val endpoint = URL(
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey",
        )
        val body = JSONObject().apply {
            put(
                "contents",
                org.json.JSONArray().put(
                    JSONObject().put(
                        "parts",
                        org.json.JSONArray().put(JSONObject().put("text", prompt)),
                    ),
                ),
            )
            if (jsonMimeType) {
                put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
            }
        }
        val responseText = postJson(endpoint, body.toString())
        return JSONObject(responseText)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
    }

    private fun postJson(url: URL, body: String): String {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("Content-Type", "application/json")
        }
        connection.outputStream.bufferedWriter().use { it.write(body) }
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (connection.responseCode !in 200..299) {
            throw GeminiHttpException(
                statusCode = connection.responseCode,
                message = formatError(connection.responseCode, response),
            )
        }
        return response
    }

    fun formatError(statusCode: Int, responseBody: String): String {
        val detail = try {
            JSONObject(responseBody)
                .optJSONObject("error")
                ?.optString("message")
                ?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
        return if (detail != null) {
            "Gemini $statusCode: $detail"
        } else {
            "Gemini $statusCode"
        }
    }
}
