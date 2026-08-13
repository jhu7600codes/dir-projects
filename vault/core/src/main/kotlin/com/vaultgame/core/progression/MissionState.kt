package com.vaultgame.core.progression

import kotlinx.serialization.Serializable

/**
 * Persisted mission progress. [pendingScoreMultiplier] is the reward from the most recently
 * completed set, banked until it's spent on the next run's score (see
 * com.vaultgame.core.session.RunResultApplier) -- or spent early via a shop mission-skip
 * voucher, which calls [MissionSystem.skip] instead of playing it out.
 */
@Serializable
data class MissionState(
    val currentSet: MissionSet,
    val pendingScoreMultiplier: Double = 1.0,
    val setsCompletedTotal: Int = 0,
)
