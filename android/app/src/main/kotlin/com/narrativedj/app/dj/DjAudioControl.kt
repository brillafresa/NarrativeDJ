package com.narrativedj.app.dj

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

    fun fallbackForStory(story: String): DjAudioControl {
        val trimmed = story.trim()
        return DjAudioControl(
            duckingVolume = DjAudioControl.DEFAULT_DUCKING_VOLUME,
            rampInSeconds = DjAudioControl.DEFAULT_RAMP_IN,
            rampOutSeconds = DjAudioControl.DEFAULT_RAMP_OUT,
            script = if (trimmed.isEmpty()) {
                "You're listening to NarrativeDJ."
            } else {
                "A listener wrote in: $trimmed"
            },
            ssml = null,
        )
    }

    private fun org.json.JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key).trim()
        return value.takeIf { it.isNotEmpty() }
    }
}
