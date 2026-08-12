package com.orbitalsurf.core.progression

import com.orbitalsurf.core.world.PowerupType

/** What kind of pickup a mission (or daily challenge) goal counts. `ANY_POWERUP` only ever appears as a goal, never as a collected kind. */
enum class MissionPickupKind { PLATES_COIN, MAGNET, FLIGHT, SHIELD, PLATES_MULTIPLIER, SCORE_MULTIPLIER, ANY_POWERUP }

fun PowerupType.toMissionPickupKind(): MissionPickupKind = when (this) {
    is PowerupType.Magnet -> MissionPickupKind.MAGNET
    is PowerupType.Flight -> MissionPickupKind.FLIGHT
    PowerupType.Shield -> MissionPickupKind.SHIELD
    is PowerupType.PlatesMultiplier -> MissionPickupKind.PLATES_MULTIPLIER
    is PowerupType.ScoreMultiplier -> MissionPickupKind.SCORE_MULTIPLIER
}

sealed class MissionGoal {
    data class TravelDistance(val meters: Double) : MissionGoal()
    data class CollectPickupCount(val kind: MissionPickupKind, val count: Int) : MissionGoal()
}

data class Mission(
    val id: String,
    val description: String,
    val goal: MissionGoal,
    val progress: Double = 0.0,
) {
    val target: Double = when (goal) {
        is MissionGoal.TravelDistance -> goal.meters
        is MissionGoal.CollectPickupCount -> goal.count.toDouble()
    }

    val isComplete: Boolean get() = progress >= target

    fun withProgress(newProgress: Double): Mission = copy(progress = newProgress.coerceIn(0.0, target))
}
