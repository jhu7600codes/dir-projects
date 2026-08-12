package com.orbitalsurf.core.progression

import com.orbitalsurf.core.world.PowerupType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivePowerupsTest {
    @Test
    fun `collecting a magnet activates it and reports its radius`() {
        val powerups = ActivePowerups()
        powerups.collect(PowerupType.Magnet(durationSeconds = 5.0, radius = 9.0))
        assertTrue(powerups.isActive(PowerupType.ID_MAGNET))
        assertEquals(9.0, powerups.magnetRadius(defaultRadius = 1.0), 1e-9)
    }

    @Test
    fun `a powerup expires after its duration elapses`() {
        val powerups = ActivePowerups()
        powerups.collect(PowerupType.Flight(durationSeconds = 2.0))
        powerups.tick(1.0)
        assertTrue(powerups.isFlying())
        powerups.tick(1.5)
        assertFalse(powerups.isFlying())
    }

    @Test
    fun `re-collecting the same powerup refreshes its duration instead of stacking`() {
        val powerups = ActivePowerups()
        powerups.collect(PowerupType.Flight(durationSeconds = 2.0))
        powerups.tick(1.9)
        assertTrue(powerups.isFlying())
        powerups.collect(PowerupType.Flight(durationSeconds = 2.0))
        powerups.tick(1.9)
        // Had it stacked, remaining time would now be negative-ish and expired; refreshed, it's still active.
        assertTrue(powerups.isFlying())
    }

    @Test
    fun `shield is a one-time flag, not a timed buff, and is consumed on use`() {
        val powerups = ActivePowerups()
        assertFalse(powerups.consumeShieldIfAvailable())
        powerups.collect(PowerupType.Shield)
        assertTrue(powerups.isShielded)
        assertTrue(powerups.consumeShieldIfAvailable())
        assertFalse(powerups.isShielded)
        assertFalse(powerups.consumeShieldIfAvailable())
    }

    @Test
    fun `collecting a second shield while already shielded is a harmless no-op`() {
        val powerups = ActivePowerups()
        powerups.collect(PowerupType.Shield)
        powerups.collect(PowerupType.Shield)
        assertTrue(powerups.consumeShieldIfAvailable())
        assertFalse("a second shield should not have been banked", powerups.consumeShieldIfAvailable())
    }

    @Test
    fun `score and plates multipliers default to 1_0 with nothing active`() {
        val powerups = ActivePowerups()
        assertEquals(1.0, powerups.scoreMultiplier(), 1e-9)
        assertEquals(1.0, powerups.platesMultiplier(), 1e-9)
    }

    @Test
    fun `active score and plates multipliers report their factors`() {
        val powerups = ActivePowerups()
        powerups.collect(PowerupType.ScoreMultiplier(durationSeconds = 5.0, factor = 3.0))
        powerups.collect(PowerupType.PlatesMultiplier(durationSeconds = 5.0, factor = 2.5))
        assertEquals(3.0, powerups.scoreMultiplier(), 1e-9)
        assertEquals(2.5, powerups.platesMultiplier(), 1e-9)
    }

    @Test
    fun `magnet radius falls back to the default when no magnet is active`() {
        assertEquals(1.5, ActivePowerups().magnetRadius(defaultRadius = 1.5), 1e-9)
    }
}
