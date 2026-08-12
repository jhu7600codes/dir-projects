package com.orbitalsurf.core.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreSystemTest {
    @Test
    fun `distance increases both score and total distance traveled`() {
        val system = ScoreSystem()
        system.addDistance(10.0, multiplier = 1.0)
        assertTrue(system.score > 0)
        assertEquals(10.0, system.totalDistanceTraveled, 1e-9)
    }

    @Test
    fun `a higher multiplier yields more score for the same distance`() {
        val low = ScoreSystem()
        val high = ScoreSystem()
        low.addDistance(100.0, multiplier = 1.0)
        high.addDistance(100.0, multiplier = 2.0)
        assertTrue(high.score > low.score)
    }

    @Test
    fun `fractional per-tick gains still accumulate instead of rounding away to nothing`() {
        val system = ScoreSystem(distancePointsPerMeter = 1.0)
        // 0.05m per tick * 1 point/m = 0.05 points/tick -- would truncate to 0 every tick without an internal accumulator.
        repeat(1000) { system.addDistance(0.05, multiplier = 1.0) }
        assertTrue("expected accumulated fractional gains to show up as real score, got ${system.score}", system.score > 0)
    }

    @Test
    fun `collecting a plates pickup adds points scaled by multiplier`() {
        val system = ScoreSystem()
        val before = system.score
        system.addPlatesPickupPoints(multiplier = 2.0)
        assertTrue(system.score > before)
    }

    @Test
    fun `collecting a powerup pickup adds points`() {
        val system = ScoreSystem()
        val before = system.score
        system.addPowerupPickupPoints(multiplier = 1.0)
        assertTrue(system.score > before)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative distance is rejected -- the ball never moves backward`() {
        ScoreSystem().addDistance(-1.0, multiplier = 1.0)
    }
}
