package com.vaultgame.core.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentGeneratorTest {
    private fun generateChain(seed: Long, count: Int): List<RoofSegment> {
        val generator = SegmentGenerator(seed)
        var distance = 0.0
        val out = mutableListOf<RoofSegment>()
        repeat(count) {
            val segment = generator.nextSegment(distance)
            out += segment
            distance = segment.endDistance
        }
        return out
    }

    @Test
    fun sameSeedProducesIdenticalSegments() {
        val a = generateChain(1234L, 30)
        val b = generateChain(1234L, 30)
        assertEquals(a, b)
    }

    @Test
    fun differentSeedsDivergeEventually() {
        val a = generateChain(1L, 30)
        val b = generateChain(2L, 30)
        assertTrue(a != b)
    }

    @Test
    fun everySlotIsSolvable() {
        val segments = generateChain(999L, 200)
        val allObstacles = segments.flatMap { it.obstacles }
        val byDistance = allObstacles.groupBy { it.distance }

        for ((distance, group) in byDistance) {
            val solvable = Lane.entries.any { lane -> isLanePassable(lane, group) }
            assertTrue("no passable lane at distance $distance for $group", solvable)
        }
    }

    private fun isLanePassable(lane: Lane, obstaclesAtDistance: List<Obstacle>): Boolean {
        val blockers = obstaclesAtDistance.filter { it.blocksLane(lane) }
        if (blockers.isEmpty()) return true
        // A lane stays passable only if every obstacle blocking it can be cleared by the same
        // single action (jump or slide) -- a CRATE_STACK (switch-lane-only) is never passable
        // by staying in its lane.
        val actions = blockers.map { it.type.avoidedBy }.toSet()
        return actions.size == 1 && actions.first() != AvoidAction.SWITCH_LANE_ONLY
    }

    @Test
    fun fullSpanSlotsHaveExactlyOneObstacle() {
        val segments = generateChain(55L, 200)
        val byDistance = segments.flatMap { it.obstacles }.groupBy { it.distance }
        for ((_, group) in byDistance) {
            if (group.any { it.type.spansAllLanes }) {
                assertEquals(1, group.size)
            }
        }
    }

    @Test
    fun laneLocalSlotsNeverBlockAllThreeLanes() {
        val segments = generateChain(77L, 200)
        val byDistance = segments.flatMap { it.obstacles }.groupBy { it.distance }
        for ((_, group) in byDistance) {
            if (group.none { it.type.spansAllLanes }) {
                val blockedLanes = group.map { it.lane }.toSet()
                assertTrue(blockedLanes.size < 3)
            }
        }
    }

    @Test
    fun themesRespectUnlockDistance() {
        val segments = generateChain(4242L, 400)
        for (segment in segments) {
            assertTrue(
                "theme ${segment.theme} unlocks at ${segment.theme.unlockDistance} but was used at ${segment.startDistance}",
                segment.theme.unlockDistance <= segment.startDistance,
            )
        }
    }

    @Test
    fun segmentsTileWithoutGapsOrOverlap() {
        val segments = generateChain(88L, 20)
        for (i in 1 until segments.size) {
            assertEquals(segments[i - 1].endDistance, segments[i].startDistance, 1e-9)
        }
    }
}
