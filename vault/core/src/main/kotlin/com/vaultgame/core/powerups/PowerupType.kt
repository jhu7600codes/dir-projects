package com.vaultgame.core.powerups

import kotlinx.serialization.Serializable

/** The five pickup-triggered powerups. Order here is stable -- it's used as a serialized key. */
@Serializable
enum class PowerupType {
    /** Auto-collects nearby coins for a duration. */
    MAGNET,

    /** Temporary flight over a stretch of the level: ignores lane obstacles, still collects coins. */
    JETPACK,

    /** Temporary extra speed on top of the base distance/score speed curve. */
    SPEED_BOOST,

    /** Absorbs exactly one obstacle hit, then breaks (does not expire on a timer). */
    SHIELD,

    /** Doubles/triples coin value for a duration (magnitude configurable via [PowerupConfig]). */
    COIN_MULTIPLIER,
}
