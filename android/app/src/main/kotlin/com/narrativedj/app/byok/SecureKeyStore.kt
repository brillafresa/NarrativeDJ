package com.narrativedj.app.byok

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureKeyStore(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun saveApiKey(provider: Provider, apiKey: String) {
        prefs.edit().putString(provider.prefKey, apiKey.trim()).apply()
    }

    fun getApiKey(provider: Provider): String? {
        return prefs.getString(provider.prefKey, null)?.takeIf { it.isNotBlank() }
    }

    fun hasApiKey(provider: Provider): Boolean = getApiKey(provider) != null

    fun clearApiKey(provider: Provider) {
        prefs.edit().remove(provider.prefKey).apply()
    }

    enum class Provider(val prefKey: String, val label: String) {
        GEMINI("api_key_gemini", "Gemini"),
        OPENAI("api_key_openai", "OpenAI"),
    }

    companion object {
        private const val PREFS_FILE = "narrativedj_byok"
    }
}
