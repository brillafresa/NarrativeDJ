package com.narrativedj.app.profile

data class SpaceProfile(
    val id: String,
    val label: String,
    val bpmMin: Int,
    val bpmMax: Int,
    val energyMin: Double,
    val energyMax: Double,
    val mood: String,
) {
    fun matches(bpm: Double, energy: Double): Boolean {
        return bpm in bpmMin.toDouble()..bpmMax.toDouble() &&
            energy in energyMin..energyMax
    }
}

object SpaceProfiles {
    val cozyBrunchCafe = SpaceProfile(
        id = "cozy_brunch_cafe",
        label = "Cozy brunch café",
        bpmMin = 80,
        bpmMax = 110,
        energyMin = 0.35,
        energyMax = 0.50,
        mood = "Calm, warm",
    )

    val analogLpBar = SpaceProfile(
        id = "analog_lp_bar",
        label = "Analog LP bar",
        bpmMin = 70,
        bpmMax = 100,
        energyMin = 0.25,
        energyMax = 0.45,
        mood = "Nostalgic",
    )

    val quietBookstore = SpaceProfile(
        id = "quiet_bookstore",
        label = "Quiet bookstore",
        bpmMin = 60,
        bpmMax = 90,
        energyMin = 0.10,
        energyMax = 0.30,
        mood = "Focused",
    )

    val all: List<SpaceProfile> = listOf(cozyBrunchCafe, analogLpBar, quietBookstore)

    fun findById(id: String): SpaceProfile? = all.firstOrNull { it.id == id }
}

object SpaceProfileMatcher {
    fun filterTracks(
        profile: SpaceProfile,
        tracks: List<TrackMetrics>,
    ): List<TrackMetrics> {
        return tracks.filter { profile.matches(it.bpm, it.energy) }
    }
}

data class TrackMetrics(
    val id: String,
    val bpm: Double,
    val energy: Double,
)
