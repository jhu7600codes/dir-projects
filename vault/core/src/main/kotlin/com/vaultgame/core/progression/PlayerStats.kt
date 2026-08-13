package com.vaultgame.core.progression

import com.vaultgame.core.powerups.PowerupType
import kotlinx.serialization.Serializable

/** Lifetime totals, independent of any one run -- what achievements are checked against. */
@Serializable
data class PlayerStats(
    val totalDistanceMeters: Double = 0.0,
    val totalCoinsCollected: Long = 0,
    val gamesPlayed: Int = 0,
    val bestScore: Long = 0,
    val bestSingleRunDistance: Double = 0.0,
    val currentNoHitStreak: Int = 0,
    val bestNoHitStreak: Int = 0,
    val powerupUsageTotals: Map<PowerupType, Int> = emptyMap(),
) {
    fun applyRunResult(summary: RunSummary): PlayerStats {
        val mergedPowerupUsage = powerupUsageTotals.toMutableMap()
        for ((type, count) in summary.powerupActivations) {
            mergedPowerupUsage[type] = (mergedPowerupUsage[type] ?: 0) + count
        }
        val newStreak = if (summary.wasCleanRun) currentNoHitStreak + 1 else 0
        return copy(
            totalDistanceMeters = totalDistanceMeters + summary.distanceMeters,
            totalCoinsCollected = totalCoinsCollected + summary.coinsCollected,
            gamesPlayed = gamesPlayed + 1,
            bestScore = maxOf(bestScore, summary.score),
            bestSingleRunDistance = maxOf(bestSingleRunDistance, summary.distanceMeters),
            currentNoHitStreak = newStreak,
            bestNoHitStreak = maxOf(bestNoHitStreak, newStreak),
            powerupUsageTotals = mergedPowerupUsage,
        )
    }
}
