package com.orbitalsurf.core.session

import com.orbitalsurf.core.progression.DailyChallengePool
import com.orbitalsurf.core.progression.MissionPickupKind
import com.orbitalsurf.core.save.GameSaveDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunResultApplierTest {
    private fun summary(
        finalScore: Long = 1_000L,
        distanceTraveled: Double = 500.0,
        platesEarned: Long = 100L,
        missionsCompletedThisRun: Int = 0,
        reachedCheckpoints: Set<Int> = emptySet(),
        usedAnyPowerup: Boolean = false,
        pickupCounts: Map<MissionPickupKind, Int> = emptyMap(),
    ) = RunSummary(finalScore, distanceTraveled, platesEarned, missionsCompletedThisRun, reachedCheckpoints, usedAnyPowerup, pickupCounts)

    @Test
    fun `plates earned are credited to the wallet`() {
        val save = GameSaveDefaults.new()
        val result = RunResultApplier.apply(summary(platesEarned = 250L), save, todayEpochDay = 20_000L)
        assertEquals(250L, result.wallet.plates)
    }

    @Test
    fun `best score only updates when the new score is higher`() {
        val save = GameSaveDefaults.new().copy(bestScore = 5_000L)
        val lower = RunResultApplier.apply(summary(finalScore = 1_000L), save, todayEpochDay = 20_000L)
        assertEquals(5_000L, lower.bestScore)

        val higher = RunResultApplier.apply(summary(finalScore = 9_000L), save, todayEpochDay = 20_000L)
        assertEquals(9_000L, higher.bestScore)
    }

    @Test
    fun `reached checkpoints are folded into checkpoint unlocks`() {
        val save = GameSaveDefaults.new()
        val result = RunResultApplier.apply(summary(reachedCheckpoints = setOf(1, 2)), save, todayEpochDay = 20_000L)
        assertTrue(result.checkpointUnlocks.isUnlocked(1))
        assertTrue(result.checkpointUnlocks.isUnlocked(2))
        assertTrue(!result.checkpointUnlocks.isUnlocked(3))
    }

    @Test
    fun `cumulative player stats accumulate across applications`() {
        val save = GameSaveDefaults.new()
        val once = RunResultApplier.apply(summary(distanceTraveled = 300.0, missionsCompletedThisRun = 2), save, todayEpochDay = 20_000L)
        val twice = RunResultApplier.apply(summary(distanceTraveled = 200.0, missionsCompletedThisRun = 1), once, todayEpochDay = 20_000L)

        assertEquals(500.0, twice.playerStats.totalDistance, 1e-6)
        assertEquals(3L, twice.playerStats.missionsCompleted)
    }

    @Test
    fun `a run that satisfies today's daily challenge grants its plate reward and marks it completed`() {
        val save = GameSaveDefaults.new()
        val challenge = DailyChallengePool.draw(20_000L).first()

        val result = RunResultApplier.apply(satisfyingSummary(challenge), save, todayEpochDay = 20_000L)

        assertTrue(result.dailyCompletedChallengeIds.contains(challenge.id))
        assertEquals(challenge.plateReward, result.wallet.plates)
        assertEquals(1L, result.playerStats.dailiesCompleted)
    }

    /** Builds a RunSummary satisfying whichever goal this challenge happens to have. */
    private fun satisfyingSummary(challenge: com.orbitalsurf.core.progression.DailyChallenge): RunSummary =
        when (val goal = challenge.goal) {
            is com.orbitalsurf.core.progression.DailyChallengeGoal.TravelDistanceInSingleRun ->
                summary(distanceTraveled = goal.meters + 1.0, platesEarned = 0L)
            is com.orbitalsurf.core.progression.DailyChallengeGoal.ScoreAtLeastWithoutPowerups ->
                summary(finalScore = goal.targetScore + 1, usedAnyPowerup = false, platesEarned = 0L)
            is com.orbitalsurf.core.progression.DailyChallengeGoal.CollectPickupCountInSingleRun ->
                summary(pickupCounts = mapOf(goal.kind to goal.count + 1), platesEarned = 0L)
        }

    @Test
    fun `crossing a stat achievement threshold auto-grants its reward skin`() {
        val save = GameSaveDefaults.new()
        // distance_1k unlocks at 1,000m total distance.
        val result = RunResultApplier.apply(summary(distanceTraveled = 1_500.0), save, todayEpochDay = 20_000L)
        assertTrue(result.inventory.ownedSkinIds.contains("skin_bronze_roller"))
    }

    @Test
    fun `newlyUnlockedStatAchievements reports only what crossed the threshold in this application`() {
        val before = GameSaveDefaults.new()
        val after = RunResultApplier.apply(summary(distanceTraveled = 1_500.0), before, todayEpochDay = 20_000L)

        val newlyUnlocked = RunResultApplier.newlyUnlockedStatAchievements(before, after)

        assertTrue(newlyUnlocked.any { it.id == "distance_1k" })
    }

    @Test
    fun `applying a run twice in a row does not double-grant an already-unlocked achievement's skin oddly`() {
        val save = GameSaveDefaults.new()
        val once = RunResultApplier.apply(summary(distanceTraveled = 1_500.0), save, todayEpochDay = 20_000L)
        val twice = RunResultApplier.apply(summary(distanceTraveled = 10.0), once, todayEpochDay = 20_000L)

        // Set semantics: still owned, no duplication artifacts, and no re-report as "newly" unlocked.
        assertTrue(twice.inventory.ownedSkinIds.contains("skin_bronze_roller"))
        assertTrue(RunResultApplier.newlyUnlockedStatAchievements(once, twice).none { it.id == "distance_1k" })
    }

    @Test
    fun `a save is restored across applications on a later day without losing completed-daily state from the same day`() {
        val save = GameSaveDefaults.new()
        val challenges = DailyChallengePool.draw(20_000L)
        val challenge = challenges.first()
        val stats = summary(
            distanceTraveled = if (challenge.goal is com.orbitalsurf.core.progression.DailyChallengeGoal.TravelDistanceInSingleRun) {
                (challenge.goal as com.orbitalsurf.core.progression.DailyChallengeGoal.TravelDistanceInSingleRun).meters + 1.0
            } else {
                0.0
            },
            finalScore = if (challenge.goal is com.orbitalsurf.core.progression.DailyChallengeGoal.ScoreAtLeastWithoutPowerups) {
                (challenge.goal as com.orbitalsurf.core.progression.DailyChallengeGoal.ScoreAtLeastWithoutPowerups).targetScore + 1
            } else {
                0L
            },
            pickupCounts = if (challenge.goal is com.orbitalsurf.core.progression.DailyChallengeGoal.CollectPickupCountInSingleRun) {
                val goal = challenge.goal as com.orbitalsurf.core.progression.DailyChallengeGoal.CollectPickupCountInSingleRun
                mapOf(goal.kind to goal.count + 1)
            } else {
                emptyMap()
            },
        )

        val afterDay1 = RunResultApplier.apply(stats, save, todayEpochDay = 20_000L)
        assertTrue(afterDay1.dailyCompletedChallengeIds.contains(challenge.id))

        // Re-applying on the SAME day again should keep that completion.
        val stillDay1 = RunResultApplier.apply(summary(distanceTraveled = 1.0), afterDay1, todayEpochDay = 20_000L)
        assertTrue(stillDay1.dailyCompletedChallengeIds.contains(challenge.id))

        // A new day regenerates the set -- yesterday's completed id shouldn't linger into today's set.
        val day2 = RunResultApplier.apply(summary(distanceTraveled = 1.0), stillDay1, todayEpochDay = 20_001L)
        assertEquals(20_001L, day2.dailyLastResetEpochDay)
    }
}
