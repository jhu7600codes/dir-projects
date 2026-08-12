package com.orbitalsurf.core.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedCurveTest {
    @Test
    fun `speed is monotonic non-decreasing with score`() {
        val scores = listOf(0L, 100L, 1_000L, 10_000L, 50_000L, 200_000L, 1_000_000L)
        val speeds = scores.map { SpeedCurve.speedAt(it) }
        for (i in 1 until speeds.size) {
            assertTrue("speed regressed between score ${scores[i - 1]} and ${scores[i]}", speeds[i] >= speeds[i - 1])
        }
    }

    @Test
    fun `speed is capped and does not keep growing forever`() {
        val far = SpeedCurve.speedAt(1_000_000L)
        val fartherStill = SpeedCurve.speedAt(100_000_000L)
        assertEquals(far, fartherStill, 1e-9)
    }

    @Test
    fun `same score always yields the same speed`() {
        assertEquals(SpeedCurve.speedAt(12_345L), SpeedCurve.speedAt(12_345L), 0.0)
    }

    @Test
    fun `negative score is treated the same as zero`() {
        assertEquals(SpeedCurve.speedAt(0L), SpeedCurve.speedAt(-500L), 1e-9)
    }

    @Test
    fun `speed at zero score is strictly less than the cap`() {
        val zero = SpeedCurve.speedAt(0L)
        val capped = SpeedCurve.speedAt(1_000_000L)
        assertTrue(zero < capped)
    }
}
