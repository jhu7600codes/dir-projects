package com.vaultgame.core.progression

import kotlinx.serialization.Serializable

/** A rolling set of exactly 3 mission goals. Completing all 3 grants [rewardMultiplier] as a
 * score boost on a future run -- see MissionState.pendingScoreMultiplier. */
@Serializable
data class MissionSet(
    val missions: List<Mission>,
    val rewardMultiplier: Double = DEFAULT_REWARD_MULTIPLIER,
) {
    init {
        require(missions.size == 3) { "a mission set is always exactly 3 goals, got ${missions.size}" }
    }

    val isComplete: Boolean get() = missions.all { it.isComplete }

    fun applyRunResult(summary: RunSummary): MissionSet {
        if (isComplete) return this
        return copy(missions = missions.map { it.applyRunResult(summary) })
    }

    companion object {
        const val DEFAULT_REWARD_MULTIPLIER = 1.5
    }
}
