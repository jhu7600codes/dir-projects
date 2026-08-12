package com.orbitalsurf.core.world

/**
 * One stretch of walkable-or-not surface along the path, spanning a distance range and a
 * lateral (x) range. `SurfaceSampler` walks a chunk's segment list to answer "is there
 * ground under the ball here, and if so how high/how steep". Distance ranges are
 * half-open: `[startDistance, endDistance)`.
 */
sealed class SurfaceSegment {
    abstract val startDistance: Double
    abstract val endDistance: Double
    abstract val lateralMin: Double
    abstract val lateralMax: Double

    fun containsDistance(distance: Double): Boolean = distance >= startDistance && distance < endDistance

    fun containsLateral(lateral: Double): Boolean = lateral >= lateralMin && lateral <= lateralMax

    /** Surface height at (distance, lateral) if this segment provides ground there, else null. */
    abstract fun heightAt(distance: Double, lateral: Double): Double?

    /** dHeight/dDistance at this point -- 0 for flat segments, constant along a [Ramp]. */
    abstract fun slopeAt(distance: Double): Double

    /** A flat rooftop -- the plain "walking on top of a building" surface. */
    data class RooftopSlab(
        override val startDistance: Double,
        override val endDistance: Double,
        override val lateralMin: Double,
        override val lateralMax: Double,
        val height: Double,
    ) : SurfaceSegment() {
        override fun heightAt(distance: Double, lateral: Double): Double? =
            if (containsDistance(distance) && containsLateral(lateral)) height else null

        override fun slopeAt(distance: Double): Double = 0.0
    }

    /** A sloped connector between two rooftop heights -- what the ball rolls up/down to bridge a height change. */
    data class Ramp(
        override val startDistance: Double,
        override val endDistance: Double,
        override val lateralMin: Double,
        override val lateralMax: Double,
        val startHeight: Double,
        val endHeight: Double,
    ) : SurfaceSegment() {
        private val slope: Double
            get() {
                val span = endDistance - startDistance
                return if (span <= 0.0) 0.0 else (endHeight - startHeight) / span
            }

        override fun heightAt(distance: Double, lateral: Double): Double? {
            if (!containsDistance(distance) || !containsLateral(lateral)) return null
            val t = (distance - startDistance) / (endDistance - startDistance)
            return startHeight + (endHeight - startHeight) * t
        }

        override fun slopeAt(distance: Double): Double = slope
    }

    /** A flat surface bridging a [Gap] at a fixed height -- a plank/pipe, narrower than a rooftop. */
    data class Bridge(
        override val startDistance: Double,
        override val endDistance: Double,
        override val lateralMin: Double,
        override val lateralMax: Double,
        val height: Double,
    ) : SurfaceSegment() {
        override fun heightAt(distance: Double, lateral: Double): Double? =
            if (containsDistance(distance) && containsLateral(lateral)) height else null

        override fun slopeAt(distance: Double): Double = 0.0
    }

    /** No surface: missing this (with no Flight/Shield powerup active) means falling. */
    data class Gap(
        override val startDistance: Double,
        override val endDistance: Double,
        override val lateralMin: Double,
        override val lateralMax: Double,
    ) : SurfaceSegment() {
        override fun heightAt(distance: Double, lateral: Double): Double? = null

        override fun slopeAt(distance: Double): Double = 0.0
    }

    /**
     * The flat floor of a checkpoint room inside a building. Functionally the same as a
     * [RooftopSlab] (flat, always has a surface) -- kept as a distinct type so the renderer
     * can theme it differently (indoor lighting/fog/materials) and so `GameSession` can emit
     * `RunEvent.CheckpointReached` when the ball is on one.
     */
    data class CheckpointInterior(
        override val startDistance: Double,
        override val endDistance: Double,
        override val lateralMin: Double,
        override val lateralMax: Double,
        val height: Double,
        val checkpointIndex: Int,
    ) : SurfaceSegment() {
        override fun heightAt(distance: Double, lateral: Double): Double? =
            if (containsDistance(distance) && containsLateral(lateral)) height else null

        override fun slopeAt(distance: Double): Double = 0.0
    }
}
