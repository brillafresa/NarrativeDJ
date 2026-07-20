package com.narrativedj.app.admin

import com.narrativedj.app.profile.SpaceProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class SchedulePlannerTest {

    private val planner = SchedulePlanner()

    @Test
    fun parseFixture_loadsThreeLocations() {
        val json = readResource("mock_b2b_schedule.json")
        val schedule = requireNotNull(ScheduleParser.parse(json))
        assertEquals(3, schedule.locations.size)
        assertEquals("cafe-gangnam-01", schedule.locations.first().locationId)
    }

    @Test
    fun resolveProfile_withinActiveHours_returnsProfile() {
        val json = readResource("mock_b2b_schedule.json")
        val schedule = requireNotNull(ScheduleParser.parse(json))
        val cafe = schedule.locations.first { it.locationId == "cafe-gangnam-01" }
        val profile = planner.resolveProfile(cafe, hourMinute = 12 * 60)
        assertEquals(SpaceProfiles.cozyBrunchCafe.id, profile?.id)
    }

    @Test
    fun resolveProfile_outsideActiveHours_returnsNull() {
        val json = readResource("mock_b2b_schedule.json")
        val schedule = requireNotNull(ScheduleParser.parse(json))
        val cafe = schedule.locations.first { it.locationId == "cafe-gangnam-01" }
        assertNull(planner.resolveProfile(cafe, hourMinute = 3 * 60))
    }

    @Test
    fun isWithinActiveHours_overnightWindow() {
        assertTrue(planner.isWithinActiveHours("18:00-02:00", 23 * 60))
        assertTrue(planner.isWithinActiveHours("18:00-02:00", 1 * 60))
        assertFalse(planner.isWithinActiveHours("18:00-02:00", 12 * 60))
    }

    private fun readResource(name: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(name)
            ?: error("Missing resource $name")
        return stream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
    }
}
