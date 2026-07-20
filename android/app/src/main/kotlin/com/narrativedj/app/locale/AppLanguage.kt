package com.narrativedj.app.locale

/** App UI language. Default: Korean (`ko`). English available via settings. */
enum class AppLanguage(val tag: String) {
    KOREAN("ko"),
    ENGLISH("en"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage {
            return if (tag == ENGLISH.tag) ENGLISH else KOREAN
        }
    }
}
