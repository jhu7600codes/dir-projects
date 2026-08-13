package com.vaultgame.core.progression

import com.vaultgame.core.powerups.PowerupType

/**
 * The end-of-run digest [com.vaultgame.core.session.RunResultApplier] hands to every progression
 * system (missions, daily challenge, stats/achievements, economy). Built once from the events a
 * [com.vaultgame.core.physics.RunSimulator] emitted over the whole run, so those systems never
 * need to know about ticks or physics.
 */
data class RunSummary(
    val distanceMeters: Double,
    val coinsCollected: Int,
    val powerupActivations: Map<PowerupType, Int>,
    /** True if the player took zero obstacle hits (shielded or not) the entire run. */
    val wasCleanRun: Boolean,
    val score: Long,
)
