package com.orbitalsurf.core.progression

import com.orbitalsurf.core.math.SeededSequence

/**
 * Draws 1-3 daily challenges deterministically from a calendar day, represented as a plain
 * epoch-day `Long` rather than `java.time.LocalDate` -- `:core` stays free of `java.time` so
 * it never needs Android's core-library-desugaring story; `:app` is responsible for turning
 * "today" into an epoch day however it likes before calling in.
 */
object DailyChallengePool {
    private const val MAX_CHALLENGES = 3

    private const val SCORE_TARGET_BASE = 8_000L
    private const val SCORE_TARGET_JITTER = 6_000

    private const val MAGNET_COUNT_BASE = 15
    private const val MAGNET_COUNT_JITTER = 10

    private const val DISTANCE_BASE = 2_000.0
    private const val DISTANCE_JITTER = 1_500.0

    fun draw(epochDay: Long): List<DailyChallenge> {
        val seq = SeededSequence(seed = epochDay, streamIndex = 0)
        val count = 1 + seq.nextInt(MAX_CHALLENGES)

        val templates: List<() -> DailyChallenge> = listOf(
            {
                val target = SCORE_TARGET_BASE + seq.nextInt(SCORE_TARGET_JITTER)
                DailyChallenge(
                    id = "daily-$epochDay-score",
                    description = "Score $target+ in a single run without using any powerups",
                    goal = DailyChallengeGoal.ScoreAtLeastWithoutPowerups(target),
                    plateReward = 150,
                )
            },
            {
                val count2 = MAGNET_COUNT_BASE + seq.nextInt(MAGNET_COUNT_JITTER)
                DailyChallenge(
                    id = "daily-$epochDay-magnet",
                    description = "Collect $count2 magnets in a single run",
                    goal = DailyChallengeGoal.CollectPickupCountInSingleRun(MissionPickupKind.MAGNET, count2),
                    plateReward = 120,
                )
            },
            {
                val meters = DISTANCE_BASE + seq.nextInRange(0.0, DISTANCE_JITTER)
                DailyChallenge(
                    id = "daily-$epochDay-distance",
                    description = "Travel ${meters.toInt()}m in a single run",
                    goal = DailyChallengeGoal.TravelDistanceInSingleRun(meters),
                    plateReward = 130,
                )
            },
        )

        return templates.take(count).map { it() }
    }
}
