package com.narrativedj.app.byok

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runtime-only key inject for live QA on emulator/device.
 *
 * Does **not** bake keys into the APK. Pass the key via instrumentation args:
 * ```
 * adb shell am instrument -w \
 *   -e class com.narrativedj.app.byok.AgentByokInjectTest#injectUsableGeminiKey \
 *   -e gemini_api_key '<key>' \
 *   com.narrativedj.app.test/androidx.test.runner.AndroidJUnitRunner
 * ```
 *
 * Skips (passes) when `gemini_api_key` arg is absent so normal harness runs stay green.
 */
@RunWith(AndroidJUnit4::class)
class AgentByokInjectTest {

    @Test
    fun injectUsableGeminiKey() {
        val args = InstrumentationRegistry.getArguments()
        val key = args.getString(ARG_KEY)?.trim().orEmpty()
        if (key.isEmpty()) {
            // Default connectedAndroidTest suite: no-op pass.
            return
        }
        assertTrue("instrumentation gemini_api_key must pass usability checks", GeminiApiKeyValidator.isUsable(key))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SecureKeyStore(context).saveGeminiApiKey(key)
        assertTrue(SecureKeyStore(context).hasUsableGeminiApiKey())
    }

    companion object {
        const val ARG_KEY = "gemini_api_key"
    }
}
