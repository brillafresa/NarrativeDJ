package com.narrativedj.app.dj

import com.narrativedj.app.locale.AppLanguage

data class DjAudioControl(
    val duckingVolume: Double,
    val rampInSeconds: Double,
    val rampOutSeconds: Double,
    val script: String,
    val ssml: String?,
) {
    companion object {
        const val DEFAULT_DUCKING_VOLUME = 0.2
        const val DEFAULT_RAMP_IN = 0.4
        const val DEFAULT_RAMP_OUT = 0.6
    }
}

object DjAudioControlParser {
    fun parse(json: String): DjAudioControl? {
        return try {
            val root = org.json.JSONObject(json)
            DjAudioControl(
                duckingVolume = root.optDouble("ducking_volume", DjAudioControl.DEFAULT_DUCKING_VOLUME),
                rampInSeconds = root.optDouble("ramp_duration", DjAudioControl.DEFAULT_RAMP_IN),
                rampOutSeconds = root.optDouble("ramp_out_duration", DjAudioControl.DEFAULT_RAMP_OUT),
                script = root.optString("script", ""),
                ssml = root.optNullableString("ssml"),
            )
        } catch (_: Exception) {
            null
        }
    }

    fun fallbackForStory(story: String, language: AppLanguage = AppLanguage.KOREAN): DjAudioControl {
        val trimmed = story.trim()
        val script = if (trimmed.isEmpty()) {
            when (language) {
                AppLanguage.KOREAN -> "NarrativeDJ와 함께하고 계십니다."
                AppLanguage.ENGLISH -> "You're listening to NarrativeDJ."
            }
        } else {
            when (language) {
                AppLanguage.KOREAN -> "청취자 사연: $trimmed"
                AppLanguage.ENGLISH -> "A listener wrote in: $trimmed"
            }
        }
        return DjAudioControl(
            duckingVolume = DjAudioControl.DEFAULT_DUCKING_VOLUME,
            rampInSeconds = DjAudioControl.DEFAULT_RAMP_IN,
            rampOutSeconds = DjAudioControl.DEFAULT_RAMP_OUT,
            script = script,
            ssml = null,
        )
    }

    private fun org.json.JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key).trim()
        return value.takeIf { it.isNotEmpty() }
    }
}
