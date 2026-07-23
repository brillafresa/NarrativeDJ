package com.narrativedj.app.byok

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation harness: encrypted Gemini key round-trip.
 * Clears prefs after each test so the device store is not left with fixture keys.
 */
@RunWith(AndroidJUnit4::class)
class SecureKeyStoreTest {

    private lateinit var keyStore: SecureKeyStore

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        keyStore = SecureKeyStore(context)
        keyStore.clearGeminiApiKey()
    }

    @After
    fun tearDown() {
        keyStore.clearGeminiApiKey()
    }

    @Test
    fun saveAndLoad_roundTripsApiKey() {
        keyStore.saveGeminiApiKey("test-key-123")
        assertEquals("test-key-123", keyStore.getGeminiApiKey())
        assertTrue(keyStore.hasGeminiApiKey())
    }

    @Test
    fun clearApiKey_removesValue() {
        keyStore.saveGeminiApiKey("gemini-key")
        keyStore.clearGeminiApiKey()
        assertNull(keyStore.getGeminiApiKey())
    }
}
