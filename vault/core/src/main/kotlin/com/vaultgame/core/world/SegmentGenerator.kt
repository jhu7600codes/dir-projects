package com.vaultgame.core.world

import com.vaultgame.core.math.SeededRandom
import com.vaultgame.core.powerups.PowerupType

/**
 * Deterministically generates the endless rooftop path, one [RoofSegment] at a time, driven off
 * a single world seed. Two generators built from the same seed and called the same number of
 * times always produce byte-identical segments -- see SegmentGeneratorTest.
 *
 * Solvability guarantee: at any one obstacle "slot" (a distance along the segment), we place
 * *at most two* lane-local obstacles (leaving a third lane always fully clear) or exactly one
 * full-span obstacle (which has a single required action -- jump or slide -- that clears it
 * regardless of which lane the player is standing in). A run can therefore never be blocked by
 * an unavoidable combination; see SegmentGeneratorTest#everySlotIsSolvable.
 */
class SegmentGenerator(seed: Long) {
    private val rng = SeededRandom(seed)
    private var nextIndex: Long = 0

    private val lanes = Lane.entries.toList()
    private val fullSpanTypes = listOf(ObstacleType.ROOF_GAP, ObstacleType.CLOTHESLINE)
    private val laneLocalTypes =
        listOf(ObstacleType.LOW_VENT, ObstacleType.OVERHEAD_PIPE, ObstacleType.CRATE_STACK)
    private val powerupTypes = PowerupType.entries.toList()

    fun nextSegment(startDistance: Double): RoofSegment {
        val theme = rng.pick(SegmentTheme.availableAt(startDistance))
        val density = DifficultyCurve.obstacleDensityForDistance(startDistance)

        val obstacles = mutableListOf<Obstacle>()
        val pickups = mutableListOf<Pickup>()

        var offset = WorldConstants.MIN_OBSTACLE_SPACING
        while (offset < WorldConstants.SEGMENT_LENGTH) {
            val slotDistance = startDistance + offset
            val blockedLanes: Set<Lane> =
                if (rng.chance(density)) placeObstacleSlot(slotDistance, obstacles) else emptySet()

            placeCoinsAndPowerups(slotDistance, blockedLanes, pickups)

            offset += WorldConstants.MIN_OBSTACLE_SPACING
        }

        return RoofSegment(
            index = nextIndex++,
            theme = theme,
            startDistance = startDistance,
            length = WorldConstants.SEGMENT_LENGTH,
            obstacles = obstacles,
            pickups = pickups,
        )
    }

    /** Returns the set of lanes rendered impassable at this slot (for coin placement to avoid). */
    private fun placeObstacleSlot(distance: Double, out: MutableList<Obstacle>): Set<Lane> {
        // 60% full-span (single required action), 40% one-or-two lane-local obstacles.
        return if (rng.chance(0.6)) {
            val type = rng.pick(fullSpanTypes)
            out += Obstacle(distance, Lane.CENTER, type)
            lanes.toSet()
        } else {
            val obstacleCount = if (rng.chance(0.5)) 1 else 2
            val chosenLanes = rng.shuffle(lanes).take(obstacleCount)
            for (lane in chosenLanes) {
                out += Obstacle(distance, lane, rng.pick(laneLocalTypes))
            }
            chosenLanes.toSet()
        }
    }

    private fun placeCoinsAndPowerups(
        slotDistance: Double,
        blockedLanes: Set<Lane>,
        out: MutableList<Pickup>,
    ) {
        val openLanes = lanes.filter { it !in blockedLanes }
        if (openLanes.isEmpty()) return

        if (rng.chance(WorldConstants.POWERUP_SPAWN_CHANCE)) {
            val lane = rng.pick(openLanes)
            out += Pickup(slotDistance, lane, PickupType.Powerup(rng.pick(powerupTypes)))
            return
        }

        if (!rng.chance(0.5)) return
        val lane = rng.pick(openLanes)
        val arcStart = slotDistance - WorldConstants.MIN_OBSTACLE_SPACING / 2
        for (i in 0 until WorldConstants.COIN_ARC_LENGTH) {
            val d = arcStart + i * WorldConstants.COIN_SPACING
            if (d < 0.0) continue
            out += Pickup(d, lane, PickupType.Coin)
        }
    }
}
