package com.narrativedj.app.admin

data class LocationSchedule(
    val locationId: String,
    val name: String,
    val profileId: String,
    val activeHours: String,
    val timezone: String,
)

data class MultiLocationSchedule(
    val version: Int,
    val locations: List<LocationSchedule>,
)

object ScheduleParser {
    fun parse(json: String): MultiLocationSchedule? {
        return try {
            val root = org.json.JSONObject(json)
            val locationsJson = root.getJSONArray("locations")
            val locations = ArrayList<LocationSchedule>(locationsJson.length())
            for (i in 0 until locationsJson.length()) {
                val entry = locationsJson.getJSONObject(i)
                locations.add(
                    LocationSchedule(
                        locationId = entry.getString("location_id"),
                        name = entry.getString("name"),
                        profileId = entry.getString("profile_id"),
                        activeHours = entry.getString("active_hours"),
                        timezone = entry.getString("timezone"),
                    ),
                )
            }
            MultiLocationSchedule(
                version = root.getInt("version"),
                locations = locations,
            )
        } catch (_: Exception) {
            null
        }
    }
}
