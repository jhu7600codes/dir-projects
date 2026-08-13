package com.vaultgame.core.progression

import com.vaultgame.core.powerups.PowerupType
import kotlinx.serialization.Serializable

@Serializable
data class Mission(
    val id: String,
    val description: String,
    val targetType: MissionTargetType,
    val targetValue: Int,
    val powerupType: PowerupType? = null,
    val progress: Int = 0,
) {
    val isComplete: Boolean get() = progress >= targetValue

    fun applyRunResult(summary: RunSummary): Mission {
        if (isComplete) return this
        val newProgress = ProgressCalculator.next(targetType, powerupType, progress, targetValue, summary)
        return copy(progress = newProgress)
    }
}
