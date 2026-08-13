package com.vaultgame.core.progression

import kotlinx.serialization.Serializable

/** Shared by missions and the daily challenge -- what kind of run activity a goal counts. */
@Serializable
enum class MissionTargetType {
    /** Cumulative coins collected across runs while this goal is active. */
    COLLECT_COINS,

    /** Must be reached within a single run (not summed across runs). */
    RUN_DISTANCE_SINGLE_RUN,

    /** Cumulative activations of one specific [com.vaultgame.core.powerups.PowerupType]. */
    USE_POWERUP,

    /** Cumulative activations of any powerup. */
    COLLECT_POWERUPS_TOTAL,

    /** Consecutive completed runs with zero obstacle hits; any hit resets progress to 0. */
    CLEAN_RUN_STREAK,
}
