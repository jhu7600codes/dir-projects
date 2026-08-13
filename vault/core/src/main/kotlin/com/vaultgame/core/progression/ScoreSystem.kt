package com.vaultgame.core.progression

import kotlin.math.roundToLong

/** Turns raw run numbers into the final score: distance + coins + any mission multiplier bonus. */
object ScoreSystem {
    const val SCORE_PER_METER = 1.0
    const val SCORE_PER_COIN = 5.0

    /**
     * [multiplier] is the mission-set completion bonus in effect for this run (1.0 if no boost
     * is active) -- see MissionState.pendingScoreMultiplier.
     */
    fun computeScore(distanceMeters: Double, coinsCollected: Int, multiplier: Double): Long {
        val base = distanceMeters * SCORE_PER_METER + coinsCollected * SCORE_PER_COIN
        return (base * multiplier).roundToLong()
    }
}
