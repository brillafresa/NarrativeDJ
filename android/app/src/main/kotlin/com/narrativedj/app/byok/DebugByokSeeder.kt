package com.narrativedj.app.byok

import com.narrativedj.app.BuildConfig

/**
 * DEBUG-only seed of Gemini key from `local.properties` → [BuildConfig.GEMINI_API_KEY].
 *
 * Purpose: live QA / emulator without typing the key each launch.
 * Overwrites empty or unusable placeholders left by instrumentation.
 * Never used in release builds; never commit `local.properties`.
 */
object DebugByokSeeder {
    fun seedIfNeeded(keyStore: SecureKeyStore) {
        if (!BuildConfig.DEBUG) return
        val value = BuildConfig.GEMINI_API_KEY.trim()
        if (!GeminiApiKeyValidator.isUsable(value)) return
        if (!keyStore.hasUsableGeminiApiKey()) {
            keyStore.saveGeminiApiKey(value)
        }
    }
}
