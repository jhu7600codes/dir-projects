package com.orbitalsurf.core.session

import com.orbitalsurf.core.progression.Achievement
import com.orbitalsurf.core.progression.AchievementSystem
import com.orbitalsurf.core.progression.DailyChallengePool
import com.orbitalsurf.core.progression.DailyChallengeSystem
import com.orbitalsurf.core.progression.DailyRunStats
import com.orbitalsurf.core.save.GameSave

/**
 * Pure function for "what a finished run does to the save": credits plates, folds in
 * checkpoint unlocks, evaluates today's daily challenges against this run, updates cumulative
 * stats, and auto-grants any cosmetic skin whose stat-threshold achievement this run just
 * crossed. Kept out of `GameSession` itself so all of this stays testable in isolation from
 * the physics/generation loop.
 */
object RunResultApplier {
    fun apply(summary: RunSummary, save: GameSave, todayEpochDay: Long): GameSave {
        var checkpointUnlocks = save.checkpointUnlocks
        summary.reachedCheckpoints.forEach { checkpointUnlocks = checkpointUnlocks.markReached(it) }

        val dailySystem = dailyChallengeSystemFrom(save)
        dailySystem.ensureUpToDate(todayEpochDay)
        val newlyCompletedDailies = dailySystem.evaluateRun(
            DailyRunStats(
                finalScore = summary.finalScore,
                usedAnyPowerup = summary.usedAnyPowerup,
                pickupCounts = summary.pickupCounts,
                distanceTraveled = summary.distanceTraveled,
            ),
        )
        val dailyPlatesReward = newlyCompletedDailies.sumOf { it.plateReward }

        val newStats = save.playerStats.copy(
            totalDistance = save.playerStats.totalDistance + summary.distanceTraveled,
            totalPlatesEarned = save.playerStats.totalPlatesEarned + summary.platesEarned + dailyPlatesReward,
            missionsCompleted = save.playerStats.missionsCompleted + summary.missionsCompletedThisRun,
            dailiesCompleted = save.playerStats.dailiesCompleted + newlyCompletedDailies.size,
        )

        val newlyUnlockedAchievements = AchievementSystem().unlockedStatThresholds(newStats) -
            AchievementSystem().unlockedStatThresholds(save.playerStats)
        val newInventory = save.inventory.copy(
            ownedSkinIds = save.inventory.ownedSkinIds + newlyUnlockedAchievements.map { it.rewardSkinId },
        )

        return save.copy(
            bestScore = maxOf(save.bestScore, summary.finalScore),
            wallet = save.wallet.credit(summary.platesEarned + dailyPlatesReward),
            inventory = newInventory,
            checkpointUnlocks = checkpointUnlocks,
            playerStats = newStats,
            dailyLastResetEpochDay = dailySystem.lastResetEpochDay,
            dailyCompletedChallengeIds = dailySystem.challenges.filter { it.completed }.map { it.id }.toSet(),
        )
    }

    /** Which stat-threshold achievements this call newly unlocked (for a "you just unlocked X!" notice). Re-derives from before/after stats rather than being tracked as extra state. */
    fun newlyUnlockedStatAchievements(before: GameSave, after: GameSave): Set<Achievement.StatThresholdAchievement> {
        val system = AchievementSystem()
        return system.unlockedStatThresholds(after.playerStats) - system.unlockedStatThresholds(before.playerStats)
    }

    /**
     * Rebuilds a live [DailyChallengeSystem] from a save's persisted `(lastResetEpochDay,
     * completedChallengeIds)` pair -- the same reconstruction [apply] uses internally, exposed
     * so UI code (e.g. a Daily Challenges screen) can display today's set + completion state
     * without duplicating this logic.
     */
    fun dailyChallengeSystemFrom(save: GameSave): DailyChallengeSystem {
        val drawn = if (save.dailyLastResetEpochDay == DailyChallengeSystem.NEVER_RESET) {
            emptyList()
        } else {
            DailyChallengePool.draw(save.dailyLastResetEpochDay)
        }
        val withCompletion = drawn.map { challenge ->
            if (challenge.id in save.dailyCompletedChallengeIds) challenge.copy(completed = true) else challenge
        }
        return DailyChallengeSystem(save.dailyLastResetEpochDay, withCompletion)
    }
}
