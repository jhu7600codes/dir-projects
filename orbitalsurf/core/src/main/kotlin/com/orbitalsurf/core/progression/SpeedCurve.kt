package com.orbitalsurf.core.progression

import com.orbitalsurf.core.math.MathUtils

/**
 * The "ball gets faster as your score grows" rule. A pure function of score: monotonic
 * non-decreasing and capped at [MAX_SPEED], so an infinite run stays humanly playable instead
 * of accelerating forever.
 */
object SpeedCurve {
    private const val BASE_SPEED = 6.0
    private const val MAX_SPEED = 22.0
    private const val RAMP_SCORE = 50_000.0

    fun speedAt(score: Long): Double {
        val t = MathUtils.smoothstep(0.0, RAMP_SCORE, score.coerceAtLeast(0L).toDouble())
        return MathUtils.lerp(BASE_SPEED, MAX_SPEED, t)
    }
}
