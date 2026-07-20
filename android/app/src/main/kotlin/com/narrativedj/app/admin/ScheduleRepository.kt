package com.narrativedj.app.admin

import android.content.Context
import com.narrativedj.app.profile.SpaceProfile
import com.narrativedj.app.profile.SpaceProfiles

class ScheduleRepository(context: Context) {

    private val appContext = context.applicationContext

    fun loadDefaultSchedule(): MultiLocationSchedule? {
        val json = appContext.assets.open(DEFAULT_ASSET).bufferedReader().use { it.readText() }
        return ScheduleParser.parse(json)
    }

    fun findLocation(schedule: MultiLocationSchedule, locationId: String): LocationSchedule? {
        return schedule.locations.firstOrNull { it.locationId == locationId }
    }

    companion object {
        const val DEFAULT_ASSET = "admin/default_schedule.json"
    }
}

class SchedulePlanner {
    fun resolveProfile(
        location: LocationSchedule,
        hourMinute: Int,
    ): SpaceProfile? {
        if (!isWithinActiveHours(location.activeHours, hourMinute)) return null
        return SpaceProfiles.findById(location.profileId)
    }

    fun isWithinActiveHours(activeHours: String, hourMinute: Int): Boolean {
        val parts = activeHours.split("-")
        if (parts.size != 2) return true
        val start = parseHourMinute(parts[0].trim()) ?: return true
        val end = parseHourMinute(parts[1].trim()) ?: return true
        return if (start <= end) {
            hourMinute in start..end
        } else {
            hourMinute >= start || hourMinute <= end
        }
    }

    private fun parseHourMinute(value: String): Int? {
        val bits = value.split(":")
        if (bits.size != 2) return null
        val hour = bits[0].toIntOrNull() ?: return null
        val minute = bits[1].toIntOrNull() ?: return null
        return hour * 60 + minute
    }
}
