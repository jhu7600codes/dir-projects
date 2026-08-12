package com.orbitalsurf.core.progression

import com.orbitalsurf.core.world.PowerupType

/** One currently-running timed powerup and how much longer it lasts. */
data class RemainingPowerup(val type: PowerupType, val remainingSeconds: Double)

/**
 * Tracks which powerups are currently active. Collecting the same kind again refreshes its
 * duration to full rather than stacking durations (the simplest rule that avoids unbounded
 * stacking) -- Shield is the exception: it's an instantaneous "the next fall/hit is absorbed"
 * flag rather than a timed buff, so collecting a second one while already shielded is a
 * harmless no-op rather than "two shields banked".
 */
class ActivePowerups {
    private val timers = mutableMapOf<String, RemainingPowerup>()
    private var shielded = false

    val active: List<RemainingPowerup> get() = timers.values.toList()
    val isShielded: Boolean get() = shielded

    fun collect(type: PowerupType) {
        when (type) {
            is PowerupType.Shield -> shielded = true
            is PowerupType.Magnet -> timers[type.id] = RemainingPowerup(type, type.durationSeconds)
            is PowerupType.Flight -> timers[type.id] = RemainingPowerup(type, type.durationSeconds)
            is PowerupType.PlatesMultiplier -> timers[type.id] = RemainingPowerup(type, type.durationSeconds)
            is PowerupType.ScoreMultiplier -> timers[type.id] = RemainingPowerup(type, type.durationSeconds)
        }
    }

    fun tick(dtSeconds: Double) {
        val expired = mutableListOf<String>()
        for ((id, remaining) in timers) {
            val updated = remaining.remainingSeconds - dtSeconds
            if (updated <= 0.0) expired += id else timers[id] = remaining.copy(remainingSeconds = updated)
        }
        expired.forEach { timers.remove(it) }
    }

    /** Consumes the shield if one is banked, returning whether it was available to absorb a fall/hit. */
    fun consumeShieldIfAvailable(): Boolean {
        if (!shielded) return false
        shielded = false
        return true
    }

    fun isActive(id: String): Boolean = timers.containsKey(id)

    fun scoreMultiplier(): Double =
        (timers[PowerupType.ID_SCORE_MULTIPLIER]?.type as? PowerupType.ScoreMultiplier)?.factor ?: 1.0

    fun platesMultiplier(): Double =
        (timers[PowerupType.ID_PLATES_MULTIPLIER]?.type as? PowerupType.PlatesMultiplier)?.factor ?: 1.0

    fun magnetRadius(defaultRadius: Double): Double =
        (timers[PowerupType.ID_MAGNET]?.type as? PowerupType.Magnet)?.radius ?: defaultRadius

    fun isFlying(): Boolean = isActive(PowerupType.ID_FLIGHT)
}
