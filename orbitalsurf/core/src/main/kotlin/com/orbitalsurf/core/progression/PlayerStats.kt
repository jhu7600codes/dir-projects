package com.orbitalsurf.core.progression

import kotlinx.serialization.Serializable

/** Cumulative account-wide totals -- what stat-threshold achievements are evaluated against. */
@Serializable
data class PlayerStats(
    val totalDistance: Double = 0.0,
    val totalPlatesEarned: Long = 0L,
    val missionsCompleted: Long = 0L,
    val dailiesCompleted: Long = 0L,
) {
    fun valueFor(key: StatKey): Long = when (key) {
        StatKey.TOTAL_DISTANCE -> totalDistance.toLong()
        StatKey.TOTAL_PLATES_EARNED -> totalPlatesEarned
        StatKey.MISSIONS_COMPLETED -> missionsCompleted
        StatKey.DAILIES_COMPLETED -> dailiesCompleted
    }
}

enum class StatKey { TOTAL_DISTANCE, TOTAL_PLATES_EARNED, MISSIONS_COMPLETED, DAILIES_COMPLETED }
