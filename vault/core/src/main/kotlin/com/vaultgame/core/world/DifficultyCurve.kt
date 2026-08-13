package com.vaultgame.core.world

import kotlin.math.min

/**
 * Speed and obstacle-density scale with distance, both monotonically non-decreasing and both
 * capped so the game stays fair/readable at long distances instead of becoming unbeatable.
 */
object DifficultyCurve {
    const val BASE_SPEED = 7.0 // m/s
    const val MAX_SPEED = 18.0 // m/s
    /** Distance at which speed reaches [MAX_SPEED]. */
    private const val SPEED_RAMP_DISTANCE = 3000.0

    const val BASE_OBSTACLE_DENSITY = 0.35
    const val MAX_OBSTACLE_DENSITY = 0.85
    private const val DENSITY_RAMP_DISTANCE = 2000.0

    /** Base run speed (m/s) for [distance] meters traveled, before powerup multipliers. */
    fun speedForDistance(distance: Double): Double {
        val t = min(1.0, distance / SPEED_RAMP_DISTANCE)
        return BASE_SPEED + (MAX_SPEED - BASE_SPEED) * t
    }

    /** Fraction (0..1) of eligible obstacle slots the generator fills at [distance]. */
    fun obstacleDensityForDistance(distance: Double): Double {
        val t = min(1.0, distance / DENSITY_RAMP_DISTANCE)
        return BASE_OBSTACLE_DENSITY + (MAX_OBSTACLE_DENSITY - BASE_OBSTACLE_DENSITY) * t
    }
}
