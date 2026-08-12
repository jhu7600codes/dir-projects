package com.maytube.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * MainActivity's rotate-to-landscape-means-fullscreen behavior lives on top
 * of this bucketing math -- see MainActivity.setupOrientationListener's
 * kdoc for why it's driven by raw OrientationEventListener degrees rather
 * than onConfigurationChanged, and orientationBucketFor's kdoc for exactly
 * what each bucket covers.
 */
class OrientationBucketTest {

    @Test
    fun `buckets angles near 0 and 360 as portrait`() {
        assertEquals(OrientationBucket.PORTRAIT, orientationBucketFor(0))
        assertEquals(OrientationBucket.PORTRAIT, orientationBucketFor(10))
        assertEquals(OrientationBucket.PORTRAIT, orientationBucketFor(350))
        assertEquals(OrientationBucket.PORTRAIT, orientationBucketFor(359))
    }

    @Test
    fun `buckets angles near 90 and 270 as landscape`() {
        assertEquals(OrientationBucket.LANDSCAPE, orientationBucketFor(90))
        assertEquals(OrientationBucket.LANDSCAPE, orientationBucketFor(100))
        assertEquals(OrientationBucket.LANDSCAPE, orientationBucketFor(80))
        assertEquals(OrientationBucket.LANDSCAPE, orientationBucketFor(270))
        assertEquals(OrientationBucket.LANDSCAPE, orientationBucketFor(260))
        assertEquals(OrientationBucket.LANDSCAPE, orientationBucketFor(280))
    }

    @Test
    fun `buckets upside-down portrait near 180 as portrait, not landscape`() {
        // regression case: a naive "d in 90 until 270 -> LANDSCAPE" range
        // check would wrongly catch this -- 180 is upside-down portrait,
        // not landscape, and needs its own PORTRAIT bucket the same as 0.
        assertEquals(OrientationBucket.PORTRAIT, orientationBucketFor(180))
        assertEquals(OrientationBucket.PORTRAIT, orientationBucketFor(170))
        assertEquals(OrientationBucket.PORTRAIT, orientationBucketFor(190))
    }

    @Test
    fun `leaves the diagonal dead zones around 45,135,225,315 unbucketed`() {
        assertEquals(OrientationBucket.UNKNOWN, orientationBucketFor(45))
        assertEquals(OrientationBucket.UNKNOWN, orientationBucketFor(135))
        assertEquals(OrientationBucket.UNKNOWN, orientationBucketFor(225))
        assertEquals(OrientationBucket.UNKNOWN, orientationBucketFor(315))
    }

    @Test
    fun `a wider dead zone widens the unbucketed range around each diagonal`() {
        // default dead zone (15) already excludes 45 itself; a wider one
        // (30) should also swallow angles further from it, like 60/120
        assertEquals(OrientationBucket.LANDSCAPE, orientationBucketFor(60, deadZoneDegrees = 15))
        assertEquals(OrientationBucket.UNKNOWN, orientationBucketFor(60, deadZoneDegrees = 30))
    }

    @Test
    fun `negative degrees (OrientationEventListener's ORIENTATION_UNKNOWN sentinel) are unknown`() {
        assertEquals(OrientationBucket.UNKNOWN, orientationBucketFor(-1))
    }
}
