package com.narrativedj.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.narrativedj.app.byok.DebugByokSeeder
import com.narrativedj.app.byok.GeminiApiKeyValidator
import com.narrativedj.app.byok.SecureKeyStore
import com.narrativedj.app.databinding.ActivityGeminiKeyGateBinding

/**
 * Launcher activity: blocks app use until a usable Gemini API key is stored.
 * Debug builds may auto-seed from `local.properties` via [DebugByokSeeder].
 */
class GeminiKeyGateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGeminiKeyGateBinding
    private lateinit var keyStore: SecureKeyStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keyStore = SecureKeyStore(this)
        DebugByokSeeder.seedIfNeeded(keyStore)

        if (keyStore.hasUsableGeminiApiKey()) {
            openMainAndFinish()
            return
        }

        // Drop harness/placeholder leftovers so the gate does not look "already filled".
        if (keyStore.hasGeminiApiKey()) {
            keyStore.clearGeminiApiKey()
        }

        binding = ActivityGeminiKeyGateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnContinue.setOnClickListener { saveAndContinue() }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finishAffinity()
                }
            },
        )
    }

    private fun saveAndContinue() {
        val key = binding.keyInput.text?.toString()?.trim().orEmpty()
        if (key.isBlank()) {
            binding.keyInput.error = getString(R.string.gemini_key_required)
            return
        }
        if (!GeminiApiKeyValidator.isUsable(key)) {
            binding.keyInput.error = getString(R.string.gemini_key_invalid)
            return
        }
        keyStore.saveGeminiApiKey(key)
        openMainAndFinish()
    }

    private fun openMainAndFinish() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
