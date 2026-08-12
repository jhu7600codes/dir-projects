package com.orbitalsurf.core.progression

/**
 * Owns today's daily challenge set and their completion state. Regenerates exactly once per
 * calendar day (the first [ensureUpToDate] call after the epoch day changes), and evaluates
 * finished runs against the still-incomplete ones.
 */
class DailyChallengeSystem(
    initialLastResetEpochDay: Long = NEVER_RESET,
    initialChallenges: List<DailyChallenge> = emptyList(),
) {
    var lastResetEpochDay: Long = initialLastResetEpochDay
        private set

    var challenges: List<DailyChallenge> = initialChallenges
        private set

    /** Call with "today" (e.g. at run start); regenerates the day's set the first time a new epoch day is seen. */
    fun ensureUpToDate(todayEpochDay: Long) {
        if (todayEpochDay != lastResetEpochDay) {
            lastResetEpochDay = todayEpochDay
            challenges = DailyChallengePool.draw(todayEpochDay)
        }
    }

    /** Marks any not-yet-completed challenge this run's stats satisfy as completed; returns the newly-completed ones (for reward granting). */
    fun evaluateRun(stats: DailyRunStats): List<DailyChallenge> {
        val newlyCompleted = mutableListOf<DailyChallenge>()
        challenges = challenges.map { challenge ->
            if (challenge.completed) return@map challenge
            val satisfied = when (val goal = challenge.goal) {
                is DailyChallengeGoal.ScoreAtLeastWithoutPowerups ->
                    stats.finalScore >= goal.targetScore && !stats.usedAnyPowerup
                is DailyChallengeGoal.CollectPickupCountInSingleRun ->
                    (stats.pickupCounts[goal.kind] ?: 0) >= goal.count
                is DailyChallengeGoal.TravelDistanceInSingleRun ->
                    stats.distanceTraveled >= goal.meters
            }
            if (satisfied) {
                val completedChallenge = challenge.copy(completed = true)
                newlyCompleted += completedChallenge
                completedChallenge
            } else {
                challenge
            }
        }
        return newlyCompleted
    }

    companion object {
        /** Sentinel meaning "never regenerated yet" -- guaranteed to differ from any real epoch day. */
        const val NEVER_RESET = Long.MIN_VALUE
    }
}
