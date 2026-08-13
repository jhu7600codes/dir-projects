package com.vaultgame.core.progression

import com.vaultgame.core.powerups.PowerupType
import kotlinx.serialization.Serializable

/** A single, harder-than-a-mission objective for one calendar day (UTC). [dayKey] is
 * epochMillis / 86_400_000 -- see [DailyChallengeSystem.dayKeyFor]. */
@Serializable
data class DailyChallenge(
    val dayKey: Long,
    val description: String,
    val targetType: MissionTargetType,
    val targetValue: Int,
    val powerupType: PowerupType? = null,
    val progress: Int = 0,
    val rewardPlates: Int = 500,
    val claimed: Boolean = false,
) {
    val isComplete: Boolean get() = progress >= targetValue

    fun applyRunResult(summary: RunSummary): DailyChallenge {
        if (isComplete) return this
        val newProgress = ProgressCalculator.next(targetType, powerupType, progress, targetValue, summary)
        return copy(progress = newProgress)
    }
}
