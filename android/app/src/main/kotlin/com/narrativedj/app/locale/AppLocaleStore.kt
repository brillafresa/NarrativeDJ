package com.narrativedj.app.locale

import android.content.Context
import android.os.Build
import java.util.Locale

/** Resolves UI/LLM language from the system locale (no in-app override). */
object AppLocaleStore {
    fun getLanguage(context: Context): AppLanguage {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        return AppLanguage.fromLocale(locale)
    }
}
