package com.orbitalsurf.core.world

import kotlin.math.abs
import kotlin.math.max

/**
 * A barrier placed on top of a surface segment. Footprint is an axis-aligned box in the
 * (distance, lateral) plane; `height` is how far it sticks up above the surface it sits on,
 * which is what lets a well-timed jump clear it (that check lives in `BallSimulator`, which
 * compares the ball's height-above-ground to this value -- this class only answers the 2D
 * "are you within it laterally/longitudinally" question).
 */
data class Obstacle(
    val id: String,
    val distance: Double,
    val lateral: Double,
    val halfWidthLateral: Double,
    val halfWidthDistance: Double,
    val height: Double,
) {
    /**
     * Sphere-vs-AABB overlap test (ball footprint radius vs. this obstacle's box), via the
     * standard "clamp then compare distance" construction: the closest point on the box to
     * the sphere's center is at most `radius` away iff they overlap.
     */
    fun overlaps(atDistance: Double, atLateral: Double, radius: Double): Boolean {
        val dz = max(0.0, abs(atDistance - distance) - halfWidthDistance)
        val dx = max(0.0, abs(atLateral - lateral) - halfWidthLateral)
        return dx * dx + dz * dz <= radius * radius
    }
}
