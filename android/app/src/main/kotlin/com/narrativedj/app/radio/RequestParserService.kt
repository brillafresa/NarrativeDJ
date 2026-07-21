package com.narrativedj.app.radio

import com.narrativedj.app.byok.SecureKeyStore
import com.narrativedj.app.byok.llm.GeminiLlmClient
import com.narrativedj.app.byok.llm.OpenAiLlmClient
import com.narrativedj.app.byok.llm.UserRequestContext
import com.narrativedj.app.byok.llm.UserRequestLlmClient
import com.narrativedj.app.byok.llm.UserRequestPromptBuilder
import com.narrativedj.app.locale.AppLanguage
import com.narrativedj.app.profile.SpaceProfile
import com.narrativedj.app.scheduler.CatalogTrack
import com.narrativedj.app.scheduler.CushionRoutePlanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.narrativedj.app.byok.llm.LlmResponseExtractor

class RequestParserService(
    private val keyStore: SecureKeyStore,
    private val catalog: List<CatalogTrack>,
    private val planner: CushionRoutePlanner,
) {
    suspend fun parse(
        message: String,
        profile: SpaceProfile,
        language: AppLanguage,
    ): UserRequestParseResult {
        val client = resolveClient()
        if (client != null) {
            try {
                val context = UserRequestContext(
                    message = message,
                    profile = profile,
                    language = language,
                    catalogTitles = catalog.map { it.title },
                )
                return client.parseUserRequest(context)
            } catch (_: Exception) {
                // fall through to local parser
            }
        }
        return UserRequestParser.parseLocal(message, catalog, planner, profile, language)
    }

    private fun resolveClient(): UserRequestLlmClient? {
        keyStore.getApiKey(SecureKeyStore.Provider.OPENAI)?.let {
            return OpenAiUserRequestClient(it, catalog, planner)
        }
        keyStore.getApiKey(SecureKeyStore.Provider.GEMINI)?.let {
            return GeminiUserRequestClient(it)
        }
        return null
    }
}

private class OpenAiUserRequestClient(
    private val apiKey: String,
    private val catalog: List<CatalogTrack>,
    private val planner: CushionRoutePlanner,
) : UserRequestLlmClient {
    override suspend fun parseUserRequest(context: UserRequestContext): UserRequestParseResult {
        return withContext(Dispatchers.IO) {
            val prompt = UserRequestPromptBuilder.build(context)
            val body = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("messages", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                }))
                put("response_format", JSONObject().put("type", "json_object"))
            }
            val text = postOpenAi(body.toString())
            UserRequestParser.parseJson(text)
                ?: UserRequestParser.parseLocal(
                    context.message,
                    catalog,
                    planner,
                    context.profile,
                    context.language,
                )
        }
    }

    private fun postOpenAi(body: String): String {
        val endpoint = URL("https://api.openai.com/v1/chat/completions")
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }
        connection.outputStream.bufferedWriter().use { it.write(body) }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("OpenAI ${connection.responseCode}")
        }
        return JSONObject(response).getJSONArray("choices")
            .getJSONObject(0).getJSONObject("message").getString("content")
    }
}

private class GeminiUserRequestClient(private val apiKey: String) : UserRequestLlmClient {
    override suspend fun parseUserRequest(context: UserRequestContext): UserRequestParseResult {
        return withContext(Dispatchers.IO) {
            val prompt = UserRequestPromptBuilder.build(context)
            val endpoint = URL(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey",
            )
            val body = JSONObject().apply {
                put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt))),
                ))
            }
            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 20_000
                readTimeout = 30_000
                setRequestProperty("Content-Type", "application/json")
            }
            connection.outputStream.bufferedWriter().use { it.write(body.toString()) }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = stream.bufferedReader().use { it.readText() }
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Gemini ${connection.responseCode}")
            }
            val text = JSONObject(response).getJSONArray("candidates")
                .getJSONObject(0).getJSONObject("content")
                .getJSONArray("parts").getJSONObject(0).getString("text")
            UserRequestParser.parseJson(LlmResponseExtractor.extractJsonPayload(text))
                ?: throw IllegalStateException("invalid user request json")
        }
    }
}
