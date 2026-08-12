package com.orbitalsurf.core.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyChallengeSystemTest {
    @Test
    fun `the first ensureUpToDate call generates a set for that day`() {
        val system = DailyChallengeSystem()
        system.ensureUpToDate(todayEpochDay = 20_000L)
        assertTrue(system.challenges.isNotEmpty())
        assertEquals(20_000L, system.lastResetEpochDay)
    }

    @Test
    fun `calling ensureUpToDate again on the same day does not regenerate`() {
        val system = DailyChallengeSystem()
        system.ensureUpToDate(20_000L)
        val original = system.challenges
        system.ensureUpToDate(20_000L)
        assertEquals(original, system.challenges)
    }

    @Test
    fun `the same epoch day always regenerates the identical set`() {
        val a = DailyChallengeSystem()
        val b = DailyChallengeSystem()
        a.ensureUpToDate(20_000L)
        b.ensureUpToDate(20_000L)
        assertEquals(a.challenges, b.challenges)
    }

    @Test
    fun `a new day produces a different set and updates lastResetEpochDay`() {
        val system = DailyChallengeSystem()
        system.ensureUpToDate(20_000L)
        val day1 = system.challenges
        system.ensureUpToDate(20_001L)
        assertEquals(20_001L, system.lastResetEpochDay)
        assertTrue(day1.map { it.id } != system.challenges.map { it.id })
    }

    /** Builds run stats that satisfy the given challenge's goal, whichever kind it happens to be (the day's challenge count/mix varies). */
    private fun satisfyingStats(challenge: DailyChallenge): DailyRunStats = when (val goal = challenge.goal) {
        is DailyChallengeGoal.TravelDistanceInSingleRun ->
            DailyRunStats(finalScore = 0, usedAnyPowerup = false, pickupCounts = emptyMap(), distanceTraveled = goal.meters + 1.0)
        is DailyChallengeGoal.ScoreAtLeastWithoutPowerups ->
            DailyRunStats(finalScore = goal.targetScore + 1, usedAnyPowerup = false, pickupCounts = emptyMap(), distanceTraveled = 0.0)
        is DailyChallengeGoal.CollectPickupCountInSingleRun ->
            DailyRunStats(finalScore = 0, usedAnyPowerup = false, pickupCounts = mapOf(goal.kind to goal.count + 1), distanceTraveled = 0.0)
    }

    @Test
    fun `a run satisfying a challenge's goal marks it completed and returns it as newly completed`() {
        val system = DailyChallengeSystem()
        system.ensureUpToDate(20_000L)
        val challenge = system.challenges.first()

        val newlyCompleted = system.evaluateRun(satisfyingStats(challenge))

        assertTrue(newlyCompleted.any { it.id == challenge.id })
        assertTrue(system.challenges.first { it.id == challenge.id }.completed)
    }

    @Test
    fun `evaluating a run again after completion does not re-report it as newly completed`() {
        val system = DailyChallengeSystem()
        system.ensureUpToDate(20_000L)
        val challenge = system.challenges.first()
        val stats = satisfyingStats(challenge)

        system.evaluateRun(stats)
        val secondPass = system.evaluateRun(stats)

        assertTrue(secondPass.none { it.id == challenge.id })
    }

    @Test
    fun `a score challenge is not satisfied if the run used a powerup`() {
        val system = DailyChallengeSystem()
        system.ensureUpToDate(20_000L)
        val scoreChallenge = system.challenges.firstOrNull { it.goal is DailyChallengeGoal.ScoreAtLeastWithoutPowerups } ?: return
        val target = (scoreChallenge.goal as DailyChallengeGoal.ScoreAtLeastWithoutPowerups).targetScore

        val newlyCompleted = system.evaluateRun(
            DailyRunStats(finalScore = target + 1000, usedAnyPowerup = true, pickupCounts = emptyMap(), distanceTraveled = 0.0),
        )

        assertTrue(newlyCompleted.none { it.id == scoreChallenge.id })
    }

    @Test
    fun `state can be restored via the constructor and behaves the same as if it had never reset that day`() {
        val fresh = DailyChallengeSystem()
        fresh.ensureUpToDate(20_000L)

        val restored = DailyChallengeSystem(initialLastResetEpochDay = 20_000L, initialChallenges = fresh.challenges)
        restored.ensureUpToDate(20_000L)

        assertEquals(fresh.challenges, restored.challenges)
    }
}
