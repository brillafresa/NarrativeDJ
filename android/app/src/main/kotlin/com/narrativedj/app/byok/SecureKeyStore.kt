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

    fun saveGeminiApiKey(apiKey: String) {
        prefs.edit().putString(PREF_KEY_GEMINI, apiKey.trim()).apply()
    }

    fun getGeminiApiKey(): String? {
        return prefs.getString(PREF_KEY_GEMINI, null)?.takeIf { it.isNotBlank() }
    }

    fun hasGeminiApiKey(): Boolean = getGeminiApiKey() != null

    /** True when a stored key passes [GeminiApiKeyValidator] (not blank/placeholder). */
    fun hasUsableGeminiApiKey(): Boolean = GeminiApiKeyValidator.isUsable(getGeminiApiKey())

    fun clearGeminiApiKey() {
        prefs.edit().remove(PREF_KEY_GEMINI).apply()
    }

    companion object {
        private const val PREFS_FILE = "narrativedj_byok"
        private const val PREF_KEY_GEMINI = "api_key_gemini"
    }
}
