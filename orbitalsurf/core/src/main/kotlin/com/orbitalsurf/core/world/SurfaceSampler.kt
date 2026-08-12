package com.orbitalsurf.core.world

import com.orbitalsurf.core.math.Vec3

/** The result of sampling the world at a given (distance, lateral): what's underfoot there. */
data class SurfaceSample(val height: Double, val normal: Vec3, val segment: SurfaceSegment)

/**
 * The collision backbone: given the currently-active chunks (from `WorldStreamer`) and a
 * position, answers "is there ground here, and how high/steep is it" -- the heightmap-raycast
 * equivalent `BallSimulator` calls every physics tick. Stateless by design: it never owns the
 * chunk list itself, just takes whatever window is passed in, so it has no lifecycle of its
 * own to keep in sync with anything.
 */
object SurfaceSampler {
    fun sampleHeight(chunks: List<Chunk>, distance: Double, lateral: Double): SurfaceSample? {
        val chunk = chunks.firstOrNull { distance >= it.startDistance && distance < it.endDistance } ?: return null
        val segment = chunk.segments.firstOrNull { it.containsDistance(distance) && it.containsLateral(lateral) }
            ?: return null
        val height = segment.heightAt(distance, lateral) ?: return null
        val slope = segment.slopeAt(distance)
        // World convention: x = lateral, y = up, z = forward (distance). A ramp's tangent
        // along z is (0, slope, 1); the corresponding up-facing normal is (0, 1, -slope) --
        // orthogonal to the tangent (slope*1 + 1*-slope == 0) and equal to +Y on flat ground.
        val normal = Vec3(0.0, 1.0, -slope).normalized()
        return SurfaceSample(height, normal, segment)
    }

    /** The first obstacle (if any) whose footprint overlaps a ball of the given radius at this position. */
    fun overlappingObstacle(chunks: List<Chunk>, distance: Double, lateral: Double, radius: Double): Obstacle? {
        val chunk = chunks.firstOrNull { distance >= it.startDistance && distance < it.endDistance } ?: return null
        return chunk.obstacles.firstOrNull { it.overlaps(distance, lateral, radius) }
    }

    /** Every pickup within `radius` of this position, across all active chunks (a magnet can reach across a chunk seam). */
    fun nearbyPickups(chunks: List<Chunk>, distance: Double, lateral: Double, radius: Double): List<PickupPlacement> {
        val radiusSq = radius * radius
        return chunks.flatMap { it.pickups }.filter { pickup ->
            val dz = pickup.distance - distance
            val dx = pickup.lateral - lateral
            dx * dx + dz * dz <= radiusSq
        }
    }
}
