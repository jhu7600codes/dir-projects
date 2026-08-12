package com.orbitalsurf.core.world

import com.orbitalsurf.core.math.MathUtils

/** Generation parameters `ChunkGenerator` draws from at a given distance along the path. */
data class DifficultyParams(
    val gapWidthMin: Double,
    val gapWidthMax: Double,
    /** Probability weight (0..1) used when deciding whether a given spot gets an obstacle. */
    val obstacleDensity: Double,
    /** Probability weight (0..1) used when deciding whether a given spot gets a pickup. */
    val powerupDensity: Double,
    /** How far a chunk boundary's rooftop height may jitter from the smooth skyline envelope. */
    val buildingHeightVariance: Double,
)

/**
 * Pure function of distance travelled -> how hard the city gets. Every field ramps up
 * smoothly from an easy baseline to a capped maximum over [RAMP_UP_DISTANCE] meters, then
 * holds steady -- monotonic non-decreasing and bounded, which is what makes an infinite run
 * playable instead of impossible past some arbitrary point.
 */
object DifficultyCurve {
    private const val RAMP_UP_DISTANCE = 6000.0

    private const val GAP_WIDTH_MIN_EASY = 1.5
    private const val GAP_WIDTH_MIN_HARD = 2.5
    private const val GAP_WIDTH_MAX_EASY = 3.0
    private const val GAP_WIDTH_MAX_HARD = 6.0
    private const val OBSTACLE_DENSITY_EASY = 0.10
    private const val OBSTACLE_DENSITY_HARD = 0.45
    private const val POWERUP_DENSITY_EASY = 0.15
    private const val POWERUP_DENSITY_HARD = 0.30
    private const val HEIGHT_VARIANCE_EASY = 2.0
    private const val HEIGHT_VARIANCE_HARD = 8.0

    fun paramsAt(distance: Double): DifficultyParams {
        val t = MathUtils.smoothstep(0.0, RAMP_UP_DISTANCE, distance.coerceAtLeast(0.0))
        return DifficultyParams(
            gapWidthMin = MathUtils.lerp(GAP_WIDTH_MIN_EASY, GAP_WIDTH_MIN_HARD, t),
            gapWidthMax = MathUtils.lerp(GAP_WIDTH_MAX_EASY, GAP_WIDTH_MAX_HARD, t),
            obstacleDensity = MathUtils.lerp(OBSTACLE_DENSITY_EASY, OBSTACLE_DENSITY_HARD, t),
            powerupDensity = MathUtils.lerp(POWERUP_DENSITY_EASY, POWERUP_DENSITY_HARD, t),
            buildingHeightVariance = MathUtils.lerp(HEIGHT_VARIANCE_EASY, HEIGHT_VARIANCE_HARD, t),
        )
    }
}
