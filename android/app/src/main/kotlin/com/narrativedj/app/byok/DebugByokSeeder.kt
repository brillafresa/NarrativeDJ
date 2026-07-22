package com.narrativedj.app.byok

import com.narrativedj.app.BuildConfig

object DebugByokSeeder {
    fun seedIfNeeded(keyStore: SecureKeyStore) {
        if (!BuildConfig.DEBUG) return
        val value = BuildConfig.GEMINI_API_KEY.trim()
        if (value.isNotBlank() && !keyStore.hasGeminiApiKey()) {
            keyStore.saveGeminiApiKey(value)
        }
    }
}
