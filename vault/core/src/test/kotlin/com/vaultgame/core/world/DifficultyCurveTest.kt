package com.vaultgame.core.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultyCurveTest {
    @Test
    fun speedIsMonotonicNonDecreasing() {
        var prev = DifficultyCurve.speedForDistance(0.0)
        var d = 0.0
        while (d <= 6000.0) {
            val speed = DifficultyCurve.speedForDistance(d)
            assertTrue("speed dropped at $d: $speed < $prev", speed >= prev - 1e-9)
            prev = speed
            d += 137.0
        }
    }

    @Test
    fun speedStartsAtBaseAndCapsAtMax() {
        assertEquals(DifficultyCurve.BASE_SPEED, DifficultyCurve.speedForDistance(0.0), 1e-9)
        assertEquals(DifficultyCurve.MAX_SPEED, DifficultyCurve.speedForDistance(3_000.0), 1e-9)
        assertEquals(DifficultyCurve.MAX_SPEED, DifficultyCurve.speedForDistance(999_999.0), 1e-9)
    }

    @Test
    fun obstacleDensityIsMonotonicAndCapped() {
        var prev = DifficultyCurve.obstacleDensityForDistance(0.0)
        var d = 0.0
        while (d <= 5000.0) {
            val density = DifficultyCurve.obstacleDensityForDistance(d)
            assertTrue(density >= prev - 1e-9)
            assertTrue(density <= DifficultyCurve.MAX_OBSTACLE_DENSITY + 1e-9)
            prev = density
            d += 211.0
        }
    }
}
