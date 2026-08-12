package com.orbitalsurf.core.world

/**
 * What a powerup pickup does when collected, with its actual tuning (durations, factors)
 * attached. Deliberately data-driven/sealed rather than a bare enum so adding a new powerup
 * later is a new subtype, not a switch statement scattered across the codebase.
 *
 * Lives in `world/` rather than `progression/`: `ChunkGenerator` needs a concrete
 * `PowerupType` (with real parameters, not just a label) the moment it scatters pickups,
 * and `world/` is built and testable before `progression/` exists. `progression.ActivePowerups`
 * is the piece that later applies these effects to a running session; this type is just data.
 */
sealed class PowerupType(val id: String) {
    data class Magnet(val durationSeconds: Double = 8.0, val radius: Double = 6.0) : PowerupType(ID_MAGNET)

    data class Flight(val durationSeconds: Double = 6.0) : PowerupType(ID_FLIGHT)

    data object Shield : PowerupType(ID_SHIELD)

    data class PlatesMultiplier(val durationSeconds: Double = 10.0, val factor: Double = 2.0) :
        PowerupType(ID_PLATES_MULTIPLIER)

    data class ScoreMultiplier(val durationSeconds: Double = 10.0, val factor: Double = 2.0) :
        PowerupType(ID_SCORE_MULTIPLIER)

    companion object {
        const val ID_MAGNET = "magnet"
        const val ID_FLIGHT = "flight"
        const val ID_SHIELD = "shield"
        const val ID_PLATES_MULTIPLIER = "plates_multiplier"
        const val ID_SCORE_MULTIPLIER = "score_multiplier"

        /** One default-tuned instance of every powerup type -- what `ChunkGenerator` picks from when scattering pickups. */
        fun defaults(): List<PowerupType> = listOf(Magnet(), Flight(), Shield, PlatesMultiplier(), ScoreMultiplier())
    }
}
