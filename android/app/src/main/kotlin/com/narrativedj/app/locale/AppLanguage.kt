package com.narrativedj.app.locale

import java.util.Locale

/** App language derived from the device system locale. */
enum class AppLanguage(val tag: String) {
    KOREAN("ko"),
    ENGLISH("en"),
    ;

    companion object {
        fun fromLocale(locale: Locale): AppLanguage {
            return if (locale.language.equals(ENGLISH.tag, ignoreCase = true)) ENGLISH else KOREAN
        }

        fun fromTag(tag: String?): AppLanguage {
            return if (tag.equals(ENGLISH.tag, ignoreCase = true)) ENGLISH else KOREAN
        }
    }
}
