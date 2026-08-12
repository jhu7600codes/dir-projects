package com.orbitalsurf.core.progression

import com.orbitalsurf.core.math.SeededSequence

/**
 * Draws a fresh set of 3 missions for a given (seed, tier, setIndex). Always exactly one
 * mission from each of 3 fixed categories (distance, plates, powerups) rather than a random
 * subset of a larger template pool -- simpler, guarantees variety across the 3 active
 * missions, and keeps "harder at higher tiers" a direct, testable property of each category
 * individually instead of an artifact of which templates happened to get picked.
 */
object MissionPool {
    private const val DISTANCE_BASE = 300.0
    private const val DISTANCE_PER_TIER = 150.0
    private const val DISTANCE_JITTER = 30.0

    private const val COINS_BASE = 4
    private const val COINS_PER_TIER = 2
    private const val COINS_JITTER = 3

    private const val POWERUPS_BASE = 2
    private const val POWERUPS_PER_TIER = 1
    private const val POWERUPS_JITTER = 2

    fun draw(seed: Long, tier: Int, setIndex: Long): List<Mission> {
        val seq = SeededSequence(seed, streamIndex = setIndex)

        val distanceMeters = DISTANCE_BASE + tier * DISTANCE_PER_TIER + seq.nextInRange(-DISTANCE_JITTER, DISTANCE_JITTER)
        val coinCount = COINS_BASE + tier * COINS_PER_TIER + seq.nextInt(COINS_JITTER)
        val powerupCount = POWERUPS_BASE + tier * POWERUPS_PER_TIER + seq.nextInt(POWERUPS_JITTER)

        return listOf(
            Mission(
                id = "mission-$setIndex-distance",
                description = "Travel ${distanceMeters.toInt()}m this run",
                goal = MissionGoal.TravelDistance(distanceMeters),
            ),
            Mission(
                id = "mission-$setIndex-coins",
                description = "Collect $coinCount plates",
                goal = MissionGoal.CollectPickupCount(MissionPickupKind.PLATES_COIN, coinCount),
            ),
            Mission(
                id = "mission-$setIndex-powerups",
                description = "Collect $powerupCount powerups",
                goal = MissionGoal.CollectPickupCount(MissionPickupKind.ANY_POWERUP, powerupCount),
            ),
        )
    }
}
