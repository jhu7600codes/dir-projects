package com.orbitalsurf.core.world

import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultyCurveTest {
    @Test
    fun `every field is monotonic non-decreasing with distance`() {
        val samples = (0..20).map { DifficultyCurve.paramsAt(it * 500.0) }
        for (i in 1 until samples.size) {
            val prev = samples[i - 1]
            val cur = samples[i]
            assertTrue("gapWidthMin regressed at sample $i", cur.gapWidthMin >= prev.gapWidthMin - 1e-9)
            assertTrue("gapWidthMax regressed at sample $i", cur.gapWidthMax >= prev.gapWidthMax - 1e-9)
            assertTrue("obstacleDensity regressed at sample $i", cur.obstacleDensity >= prev.obstacleDensity - 1e-9)
            assertTrue("powerupDensity regressed at sample $i", cur.powerupDensity >= prev.powerupDensity - 1e-9)
            assertTrue(
                "buildingHeightVariance regressed at sample $i",
                cur.buildingHeightVariance >= prev.buildingHeightVariance - 1e-9,
            )
        }
    }

    @Test
    fun `difficulty caps out and does not keep growing forever`() {
        val far = DifficultyCurve.paramsAt(50_000.0)
        val fartherStill = DifficultyCurve.paramsAt(500_000.0)
        assertTrue(far.gapWidthMax == fartherStill.gapWidthMax)
        assertTrue(far.obstacleDensity == fartherStill.obstacleDensity)
    }

    @Test
    fun `at distance zero difficulty is at its easy baseline`() {
        val start = DifficultyCurve.paramsAt(0.0)
        val end = DifficultyCurve.paramsAt(50_000.0)
        assertTrue(start.obstacleDensity < end.obstacleDensity)
        assertTrue(start.gapWidthMax < end.gapWidthMax)
    }

    @Test
    fun `negative distance is treated the same as zero`() {
        val negative = DifficultyCurve.paramsAt(-100.0)
        val zero = DifficultyCurve.paramsAt(0.0)
        assertTrue(negative == zero)
    }
}
