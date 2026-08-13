package com.vaultgame.core.powerups

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivePowerupsTest {
    @Test
    fun activateThenExpireAfterDuration() {
        val powerups = ActivePowerups()
        powerups.activate(PowerupType.SPEED_BOOST)
        assertTrue(powerups.isActive(PowerupType.SPEED_BOOST))

        val tuning = PowerupConfig.tuningFor(PowerupType.SPEED_BOOST, 0)
        val expired = powerups.tick(tuning.durationSeconds + 0.01)
        assertTrue(PowerupType.SPEED_BOOST in expired)
        assertFalse(powerups.isActive(PowerupType.SPEED_BOOST))
    }

    @Test
    fun reactivatingRefreshesTimerInsteadOfStacking() {
        val powerups = ActivePowerups()
        powerups.activate(PowerupType.MAGNET)
        powerups.tick(1.0)
        val remainingBefore = powerups.remainingSeconds(PowerupType.MAGNET)
        powerups.activate(PowerupType.MAGNET)
        val remainingAfter = powerups.remainingSeconds(PowerupType.MAGNET)
        assertTrue(remainingAfter > remainingBefore)
        assertEquals(PowerupConfig.tuningFor(PowerupType.MAGNET, 0).durationSeconds, remainingAfter, 1e-9)
    }

    @Test
    fun shieldHasNoTimerAndPersistsUntilConsumed() {
        val powerups = ActivePowerups()
        powerups.activate(PowerupType.SHIELD)
        powerups.tick(10_000.0)
        assertTrue(powerups.isActive(PowerupType.SHIELD))
        assertTrue(powerups.consumeShield())
        assertFalse(powerups.isActive(PowerupType.SHIELD))
        assertFalse(powerups.consumeShield())
    }

    @Test
    fun coinAndSpeedMultipliersDefaultToOne() {
        val powerups = ActivePowerups()
        assertEquals(1.0, powerups.coinMultiplier(), 1e-9)
        assertEquals(1.0, powerups.speedMultiplier(), 1e-9)
        assertEquals(0.0, powerups.magnetRangeLanes(), 1e-9)
    }

    @Test
    fun upgradeLevelsIncreaseDurationAndMagnitude() {
        val level0 = ActivePowerups(emptyMap()).apply { activate(PowerupType.COIN_MULTIPLIER) }
        val level3 = ActivePowerups(mapOf(PowerupType.COIN_MULTIPLIER to 3)).apply { activate(PowerupType.COIN_MULTIPLIER) }
        assertTrue(level3.magnitude(PowerupType.COIN_MULTIPLIER) > level0.magnitude(PowerupType.COIN_MULTIPLIER))
        assertTrue(level3.remainingSeconds(PowerupType.COIN_MULTIPLIER) > level0.remainingSeconds(PowerupType.COIN_MULTIPLIER))
    }

    @Test
    fun usageCountsTrackActivations() {
        val powerups = ActivePowerups()
        powerups.activate(PowerupType.JETPACK)
        powerups.tick(100.0)
        powerups.activate(PowerupType.JETPACK)
        powerups.activate(PowerupType.SHIELD)
        assertEquals(2, powerups.usageCounts[PowerupType.JETPACK])
        assertEquals(1, powerups.usageCounts[PowerupType.SHIELD])
    }
}
