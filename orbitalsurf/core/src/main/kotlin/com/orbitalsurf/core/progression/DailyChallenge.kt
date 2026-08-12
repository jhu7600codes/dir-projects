package com.orbitalsurf.core.progression

/**
 * Harder, single-run goals separate from the rolling mission chain. Evaluated once against a
 * finished run's stats ([DailyRunStats]), not incrementally ticked like missions -- these are
 * "pull off X in one run", not "accumulate X over time".
 */
sealed class DailyChallengeGoal {
    data class ScoreAtLeastWithoutPowerups(val targetScore: Long) : DailyChallengeGoal()
    data class CollectPickupCountInSingleRun(val kind: MissionPickupKind, val count: Int) : DailyChallengeGoal()
    data class TravelDistanceInSingleRun(val meters: Double) : DailyChallengeGoal()
}

data class DailyChallenge(
    val id: String,
    val description: String,
    val goal: DailyChallengeGoal,
    val plateReward: Long,
    val completed: Boolean = false,
)

/** What `DailyChallengeSystem.evaluateRun` needs from a finished run to check goals against. */
data class DailyRunStats(
    val finalScore: Long,
    val usedAnyPowerup: Boolean,
    val pickupCounts: Map<MissionPickupKind, Int>,
    val distanceTraveled: Double,
)
