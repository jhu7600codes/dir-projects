package com.vaultgame.core.powerups

/**
 * Tunable magnitudes for a powerup. The shop sells upgrades that raise [durationSeconds] and/or
 * [magnitude] per [PowerupType] -- see [com.vaultgame.core.economy.ShopCatalog] -- so these are
 * looked up per *upgrade level*, not a single constant per type.
 */
data class PowerupTuning(
    val durationSeconds: Double,
    /** Meaning depends on the type: magnet pickup radius (lanes), coin multiplier factor, speed
     * boost multiplier on top of the base speed curve. Shield/Jetpack ignore it. */
    val magnitude: Double,
)

object PowerupConfig {
    /** Max upgrade level purchasable in the shop, per type. Level 0 is the base/default tuning. */
    const val MAX_UPGRADE_LEVEL = 3

    private val base: Map<PowerupType, PowerupTuning> = mapOf(
        PowerupType.MAGNET to PowerupTuning(durationSeconds = 8.0, magnitude = 1.5),
        PowerupType.JETPACK to PowerupTuning(durationSeconds = 10.0, magnitude = 0.0),
        PowerupType.SPEED_BOOST to PowerupTuning(durationSeconds = 6.0, magnitude = 1.5),
        PowerupType.SHIELD to PowerupTuning(durationSeconds = 0.0, magnitude = 0.0),
        PowerupType.COIN_MULTIPLIER to PowerupTuning(durationSeconds = 12.0, magnitude = 2.0),
    )

    /** Duration/magnitude bump applied per upgrade level, additive on top of [base]. */
    private val perLevelBoost: Map<PowerupType, PowerupTuning> = mapOf(
        PowerupType.MAGNET to PowerupTuning(durationSeconds = 3.0, magnitude = 0.5),
        PowerupType.JETPACK to PowerupTuning(durationSeconds = 4.0, magnitude = 0.0),
        PowerupType.SPEED_BOOST to PowerupTuning(durationSeconds = 2.0, magnitude = 0.25),
        PowerupType.SHIELD to PowerupTuning(durationSeconds = 0.0, magnitude = 0.0),
        PowerupType.COIN_MULTIPLIER to PowerupTuning(durationSeconds = 4.0, magnitude = 0.5),
    )

    fun tuningFor(type: PowerupType, upgradeLevel: Int): PowerupTuning {
        val level = upgradeLevel.coerceIn(0, MAX_UPGRADE_LEVEL)
        val b = base.getValue(type)
        val boost = perLevelBoost.getValue(type)
        return PowerupTuning(
            durationSeconds = b.durationSeconds + boost.durationSeconds * level,
            magnitude = b.magnitude + boost.magnitude * level,
        )
    }
}
