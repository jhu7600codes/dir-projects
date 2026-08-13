package com.vaultgame.core.progression

import com.vaultgame.core.powerups.PowerupType
import kotlin.math.max
import kotlin.math.min

/** The one place that knows how a [RunSummary] moves a goal's progress -- shared by [Mission]
 * (rolling sets of 3) and [DailyChallenge] (a single harder daily goal) so their semantics never
 * drift apart. */
object ProgressCalculator {
    fun next(
        type: MissionTargetType,
        powerupType: PowerupType?,
        currentProgress: Int,
        targetValue: Int,
        summary: RunSummary,
    ): Int = when (type) {
        MissionTargetType.COLLECT_COINS ->
            min(targetValue, currentProgress + summary.coinsCollected)

        MissionTargetType.RUN_DISTANCE_SINGLE_RUN ->
            max(currentProgress, min(targetValue, summary.distanceMeters.toInt()))

        MissionTargetType.USE_POWERUP ->
            min(targetValue, currentProgress + (summary.powerupActivations[powerupType] ?: 0))

        MissionTargetType.COLLECT_POWERUPS_TOTAL ->
            min(targetValue, currentProgress + summary.powerupActivations.values.sum())

        MissionTargetType.CLEAN_RUN_STREAK ->
            if (summary.wasCleanRun) min(targetValue, currentProgress + 1) else 0
    }
}
