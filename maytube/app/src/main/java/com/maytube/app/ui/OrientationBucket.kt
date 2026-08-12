package com.maytube.app.ui

/**
 * Pure degrees-to-bucket math for MainActivity's rotate-to-landscape-means-
 * fullscreen behavior, pulled out of the Activity specifically so it's a
 * plain JVM unit-testable function instead of a private method that would
 * otherwise need an Activity instance (or Robolectric, which nothing else
 * in this codebase uses) just to exercise. See MainActivity.setupOrientationListener's
 * kdoc for why this is driven by raw sensor degrees (OrientationEventListener)
 * rather than onConfigurationChanged.
 */
enum class OrientationBucket { UNKNOWN, PORTRAIT, LANDSCAPE }

/**
 * OrientationEventListener's degrees run 0-359, increasing clockwise from
 * the device's natural (portrait, for every phone this targets) orientation:
 * ~0 is upright portrait, ~90/~270 are the two landscape orientations, ~180
 * is upside-down portrait -- all three non-landscape angles bucket as
 * PORTRAIT, since none of them are "rotated into landscape". Bucketed by
 * distance to the nearest cardinal (0/90/180/270) rather than a plain
 * "is it past the 90 mark" range check, specifically so ~180 doesn't fall on
 * the landscape side of such a range by accident. A device more than
 * [deadZoneDegrees] away from every cardinal (i.e. near one of the
 * 45/135/225/315 diagonals) is neither bucket -- exactly the angles where a
 * phone resting at an angle, or briefly passing through mid-rotation,
 * shouldn't flap fullscreen on/off.
 *
 * [orientationDegrees] takes [android.view.OrientationEventListener]'s raw
 * callback value directly, including its ORIENTATION_UNKNOWN sentinel (-1,
 * kept as a literal here rather than referencing that Android class, so
 * this function -- the part with actual logic worth covering -- stays a
 * plain, trivially unit-testable Kotlin function with zero Android
 * framework dependency).
 */
fun orientationBucketFor(orientationDegrees: Int, deadZoneDegrees: Int = 15): OrientationBucket {
    if (orientationDegrees < 0) {
        return OrientationBucket.UNKNOWN
    }
    val d = ((orientationDegrees % 360) + 360) % 360
    val distanceToNearestCardinal = minOf(d % 90, 90 - (d % 90))
    if (distanceToNearestCardinal > 45 - deadZoneDegrees) {
        return OrientationBucket.UNKNOWN
    }
    val nearestCardinal = ((d + 45) / 90 * 90) % 360
    return if (nearestCardinal == 0 || nearestCardinal == 180) {
        OrientationBucket.PORTRAIT
    } else {
        OrientationBucket.LANDSCAPE
    }
}
