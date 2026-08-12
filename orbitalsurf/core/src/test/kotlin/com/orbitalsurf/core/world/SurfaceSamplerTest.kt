package com.orbitalsurf.core.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SurfaceSamplerTest {
    private fun chunkOf(vararg segments: SurfaceSegment, obstacles: List<Obstacle> = emptyList(), pickups: List<PickupPlacement> = emptyList()) =
        Chunk(
            index = 0L,
            startDistance = segments.minOf { it.startDistance },
            endDistance = segments.maxOf { it.endDistance },
            segments = segments.toList(),
            obstacles = obstacles,
            pickups = pickups,
        )

    @Test
    fun `flat slab returns its constant height`() {
        val slab = SurfaceSegment.RooftopSlab(0.0, 10.0, -3.0, 3.0, height = 12.0)
        val chunk = chunkOf(slab)
        val sample = SurfaceSampler.sampleHeight(listOf(chunk), 5.0, 0.0)
        assertEquals(12.0, sample!!.height, 1e-9)
    }

    @Test
    fun `ramp height interpolates linearly between its endpoints`() {
        val ramp = SurfaceSegment.Ramp(0.0, 10.0, -3.0, 3.0, startHeight = 10.0, endHeight = 20.0)
        val chunk = chunkOf(ramp)
        assertEquals(10.0, SurfaceSampler.sampleHeight(listOf(chunk), 0.0, 0.0)!!.height, 1e-9)
        assertEquals(15.0, SurfaceSampler.sampleHeight(listOf(chunk), 5.0, 0.0)!!.height, 1e-9)
        assertEquals(19.0, SurfaceSampler.sampleHeight(listOf(chunk), 9.0, 0.0)!!.height, 1e-9)
    }

    @Test
    fun `ramp normal tilts against the slope direction and stays a unit vector`() {
        val upRamp = SurfaceSegment.Ramp(0.0, 10.0, -3.0, 3.0, startHeight = 0.0, endHeight = 10.0)
        val chunk = chunkOf(upRamp)
        val sample = SurfaceSampler.sampleHeight(listOf(chunk), 5.0, 0.0)!!
        assertEquals(1.0, sample.normal.length(), 1e-9)
        // Slope is positive (climbing), so the normal should lean backward (negative z).
        assertTrue(sample.normal.z < 0.0)
        assertTrue(sample.normal.y > 0.0)
    }

    @Test
    fun `flat ground has a straight-up normal`() {
        val slab = SurfaceSegment.RooftopSlab(0.0, 10.0, -3.0, 3.0, height = 12.0)
        val chunk = chunkOf(slab)
        val sample = SurfaceSampler.sampleHeight(listOf(chunk), 5.0, 0.0)!!
        assertEquals(0.0, sample.normal.x, 1e-9)
        assertEquals(1.0, sample.normal.y, 1e-9)
        assertEquals(0.0, sample.normal.z, 1e-9)
    }

    @Test
    fun `a gap has no surface`() {
        val gap = SurfaceSegment.Gap(0.0, 10.0, -3.0, 3.0)
        val chunk = chunkOf(gap)
        assertNull(SurfaceSampler.sampleHeight(listOf(chunk), 5.0, 0.0))
    }

    @Test
    fun `outside every segment's lateral range returns no surface`() {
        val slab = SurfaceSegment.RooftopSlab(0.0, 10.0, -3.0, 3.0, height = 12.0)
        val chunk = chunkOf(slab)
        assertNull(SurfaceSampler.sampleHeight(listOf(chunk), 5.0, 10.0))
    }

    @Test
    fun `outside every chunk's distance range returns no surface`() {
        val slab = SurfaceSegment.RooftopSlab(0.0, 10.0, -3.0, 3.0, height = 12.0)
        val chunk = chunkOf(slab)
        assertNull(SurfaceSampler.sampleHeight(listOf(chunk), 500.0, 0.0))
    }

    @Test
    fun `obstacle overlap hits inside its footprint and misses outside`() {
        val slab = SurfaceSegment.RooftopSlab(0.0, 40.0, -5.0, 5.0, height = 12.0)
        val obstacle = Obstacle("obs-1", distance = 20.0, lateral = 0.0, halfWidthLateral = 1.0, halfWidthDistance = 1.0, height = 1.5)
        val chunk = chunkOf(slab, obstacles = listOf(obstacle))

        assertTrue(SurfaceSampler.overlappingObstacle(listOf(chunk), 20.0, 0.0, radius = 0.5) != null)
        assertNull(SurfaceSampler.overlappingObstacle(listOf(chunk), 30.0, 0.0, radius = 0.5))
    }

    @Test
    fun `a ball radius extends the obstacle hit zone beyond the raw footprint`() {
        val obstacle = Obstacle("obs-1", distance = 20.0, lateral = 0.0, halfWidthLateral = 1.0, halfWidthDistance = 1.0, height = 1.5)
        val slab = SurfaceSegment.RooftopSlab(0.0, 40.0, -5.0, 5.0, height = 12.0)
        val chunk = chunkOf(slab, obstacles = listOf(obstacle))

        // 1.5m past the box edge: misses a point-sized ball, hits a 2m-radius ball.
        assertNull(SurfaceSampler.overlappingObstacle(listOf(chunk), 22.5, 0.0, radius = 0.0))
        assertTrue(SurfaceSampler.overlappingObstacle(listOf(chunk), 22.5, 0.0, radius = 2.0) != null)
    }

    @Test
    fun `nearbyPickups finds pickups within radius and excludes ones outside it`() {
        val slab = SurfaceSegment.RooftopSlab(0.0, 40.0, -5.0, 5.0, height = 12.0)
        val near = PickupPlacement("p1", distance = 20.0, lateral = 1.0, height = 1.0, kind = PickupKind.PlatesCoin)
        val far = PickupPlacement("p2", distance = 39.0, lateral = 0.0, height = 1.0, kind = PickupKind.PlatesCoin)
        val chunk = chunkOf(slab, pickups = listOf(near, far))

        val found = SurfaceSampler.nearbyPickups(listOf(chunk), 20.0, 0.0, radius = 3.0)
        assertTrue(found.contains(near))
        assertTrue(!found.contains(far))
    }
}
