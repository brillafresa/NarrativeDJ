package com.narrativedj.app.b2b

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * B2B partner streaming API client.
 * Uses mock fixture fallback when partner endpoint is unavailable (Phase 3 scaffold).
 */
class B2bPartnerApiClient(
    private val baseUrl: String?,
    private val licenseKey: String?,
    private val mockFixture: Map<String, String> = DEFAULT_MOCK_STREAMS,
) {
    suspend fun resolveStreamUrl(trackId: String): String? {
        if (baseUrl.isNullOrBlank() || licenseKey.isNullOrBlank()) {
            return mockFixture[trackId]
        }
        return try {
            fetchFromPartner(trackId)
        } catch (_: Exception) {
            mockFixture[trackId]
        }
    }

    suspend fun validateLicense(): Boolean {
        if (licenseKey.isNullOrBlank()) return false
        if (baseUrl.isNullOrBlank()) return licenseKey.startsWith("b2b-")
        return try {
            withContext(Dispatchers.IO) {
                val url = URL("$baseUrl/v1/license/validate")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("Authorization", "Bearer $licenseKey")
                }
                connection.responseCode in 200..299
            }
        } catch (_: Exception) {
            licenseKey.startsWith("b2b-")
        }
    }

    private suspend fun fetchFromPartner(trackId: String): String? {
        return withContext(Dispatchers.IO) {
            val url = URL("$baseUrl/v1/stream/$trackId")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Authorization", "Bearer $licenseKey")
            }
            if (connection.responseCode !in 200..299) return@withContext null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body).optString("stream_url").takeIf { it.isNotBlank() }
        }
    }

    companion object {
        val DEFAULT_MOCK_STREAMS = mapOf(
            "california_dreamin" to "https://partner.mock/stream/california_dreamin",
            "hotel_california" to "https://partner.mock/stream/hotel_california",
            "mongjungin" to "https://partner.mock/stream/mongjungin",
        )
    }
}
