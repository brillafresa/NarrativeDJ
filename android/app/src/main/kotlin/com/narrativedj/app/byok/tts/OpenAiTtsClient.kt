package com.narrativedj.app.byok.tts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

fun interface TtsClient {
    suspend fun synthesize(text: String, voice: String): ByteArray
}

class OpenAiTtsClient(
    private val apiKey: String,
    private val model: String = "tts-1",
) : TtsClient {

    override suspend fun synthesize(text: String, voice: String): ByteArray {
        return withContext(Dispatchers.IO) {
            val endpoint = URL("https://api.openai.com/v1/audio/speech")
            val body = JSONObject().apply {
                put("model", model)
                put("input", text)
                put("voice", voice)
                put("response_format", "mp3")
            }
            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 20_000
                readTimeout = 60_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
            connection.outputStream.bufferedWriter().use { it.write(body.toString()) }
            if (connection.responseCode !in 200..299) {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException("OpenAI TTS ${connection.responseCode}: $err")
            }
            connection.inputStream.use { it.readBytes() }
        }
    }
}
