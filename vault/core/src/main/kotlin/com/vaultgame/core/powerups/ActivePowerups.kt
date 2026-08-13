package com.vaultgame.core.powerups

/**
 * Tracks which powerups are live during the current run and their remaining time. Owned by
 * [com.vaultgame.core.session.GameSession] and stepped every simulation tick.
 *
 * Shield is special: it has no timer (see [PowerupConfig], duration 0) and instead lives until
 * [consumeShield] is called on the next obstacle hit, or the run ends.
 */
class ActivePowerups(private val upgradeLevels: Map<PowerupType, Int> = emptyMap()) {
    private val remaining: MutableMap<PowerupType, Double> = mutableMapOf()
    private var shieldActive: Boolean = false
    val usageCounts: MutableMap<PowerupType, Int> = mutableMapOf()

    fun activate(type: PowerupType) {
        if (type == PowerupType.SHIELD) {
            shieldActive = true
        } else {
            val tuning = PowerupConfig.tuningFor(type, upgradeLevels[type] ?: 0)
            // Re-collecting the same powerup refreshes (doesn't stack) the timer.
            remaining[type] = tuning.durationSeconds
        }
        usageCounts[type] = (usageCounts[type] ?: 0) + 1
    }

    fun tick(dtSeconds: Double): List<PowerupType> {
        val justExpired = mutableListOf<PowerupType>()
        val iterator = remaining.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val newRemaining = entry.value - dtSeconds
            if (newRemaining <= 0.0) {
                justExpired += entry.key
                iterator.remove()
            } else {
                entry.setValue(newRemaining)
            }
        }
        return justExpired
    }

    fun isActive(type: PowerupType): Boolean =
        if (type == PowerupType.SHIELD) shieldActive else remaining.containsKey(type)

    fun remainingSeconds(type: PowerupType): Double = remaining[type] ?: 0.0

    fun magnitude(type: PowerupType): Double {
        if (!isActive(type)) return 0.0
        return PowerupConfig.tuningFor(type, upgradeLevels[type] ?: 0).magnitude
    }

    /** Coin value multiplier currently in effect (1.0 if none active). */
    fun coinMultiplier(): Double =
        if (isActive(PowerupType.COIN_MULTIPLIER)) magnitude(PowerupType.COIN_MULTIPLIER) else 1.0

    /** Speed multiplier currently in effect on top of the base distance speed curve. */
    fun speedMultiplier(): Double =
        if (isActive(PowerupType.SPEED_BOOST)) magnitude(PowerupType.SPEED_BOOST) else 1.0

    fun magnetRangeLanes(): Double =
        if (isActive(PowerupType.MAGNET)) magnitude(PowerupType.MAGNET) else 0.0

    /** Consumes the shield on a hit. Returns true if a shield was present (hit absorbed). */
    fun consumeShield(): Boolean {
        if (!shieldActive) return false
        shieldActive = false
        return true
    }

    fun isFlying(): Boolean = isActive(PowerupType.JETPACK)
}
