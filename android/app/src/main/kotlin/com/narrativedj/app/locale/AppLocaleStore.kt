package com.narrativedj.app.locale

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** Persists and applies per-app locale (default Korean). */
object AppLocaleStore {
    private const val PREFS = "app_locale"
    private const val KEY_TAG = "language_tag"

    fun getLanguage(context: Context): AppLanguage {
        val tag = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TAG, AppLanguage.KOREAN.tag)
        return AppLanguage.fromTag(tag)
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TAG, language.tag)
            .apply()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }

    fun applyStoredLocale(context: Context) {
        val language = getLanguage(context)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }
}
