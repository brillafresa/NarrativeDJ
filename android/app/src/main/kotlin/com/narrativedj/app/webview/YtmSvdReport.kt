package com.narrativedj.app.webview

data class YtmSvdFieldReport(
    val field: String,
    val selector: String?,
    val ok: Boolean,
    val index: Int,
)

data class YtmSvdReport(
    val fields: Map<String, YtmSvdFieldReport>,
    val healthy: Boolean,
)

object YtmSvdReportParser {
    fun parse(json: String): YtmSvdReport? {
        return try {
            val root = org.json.JSONObject(json)
            val fieldsJson = root.getJSONObject("fields")
            val fields = linkedMapOf<String, YtmSvdFieldReport>()
            val keys = fieldsJson.keys()
            while (keys.hasNext()) {
                val name = keys.next()
                val entry = fieldsJson.getJSONObject(name)
                fields[name] = YtmSvdFieldReport(
                    field = entry.optString("field", name),
                    selector = entry.optNullableString("selector"),
                    ok = entry.optBoolean("ok"),
                    index = entry.optInt("index", -1),
                )
            }
            YtmSvdReport(
                fields = fields,
                healthy = root.optBoolean("healthy"),
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun org.json.JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key).trim()
        return value.takeIf { it.isNotEmpty() }
    }
}
