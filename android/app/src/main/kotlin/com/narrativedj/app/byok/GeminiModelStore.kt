package com.narrativedj.app.byok

import android.content.Context
import com.narrativedj.app.byok.llm.GeminiModelCatalog

/**
 * Persists the selected Gemini model id (not a secret — plain SharedPreferences).
 *
 * Verify: `./gradlew test --tests com.narrativedj.app.byok.llm.GeminiModelCatalogTest`
 */
class GeminiModelStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    fun getModelId(): String {
        return GeminiModelCatalog.resolve(prefs.getString(PREF_MODEL, null))
    }

    fun setModelId(modelId: String) {
        val resolved = GeminiModelCatalog.resolve(modelId)
        prefs.edit().putString(PREF_MODEL, resolved).apply()
    }

    companion object {
        private const val PREFS_FILE = "narrativedj_settings"
        private const val PREF_MODEL = "gemini_model_id"
    }
}
