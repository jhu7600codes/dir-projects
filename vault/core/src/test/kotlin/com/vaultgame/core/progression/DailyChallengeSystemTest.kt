package com.vaultgame.core.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DailyChallengeSystemTest {
    private val oneDayMillis = 86_400_000L

    @Test
    fun rollsAFreshChallengeWhenNoneExists() {
        val challenge = DailyChallengeSystem.ensureCurrent(null, nowEpochMillis = 10_000L, worldSeed = 1L)
        assertEquals(DailyChallengeSystem.dayKeyFor(10_000L), challenge.dayKey)
    }

    @Test
    fun keepsSameChallengeWithinTheSameDay() {
        val first = DailyChallengeSystem.ensureCurrent(null, nowEpochMillis = 1_000L, worldSeed = 1L)
        val laterSameDay = DailyChallengeSystem.ensureCurrent(first, nowEpochMillis = 50_000L, worldSeed = 1L)
        assertEquals(first, laterSameDay)
    }

    @Test
    fun rollsNewChallengeOnNextDay() {
        val day1 = DailyChallengeSystem.ensureCurrent(null, nowEpochMillis = 1_000L, worldSeed = 1L)
        val day2 = DailyChallengeSystem.ensureCurrent(day1, nowEpochMillis = 1_000L + oneDayMillis, worldSeed = 1L)
        assertNotEquals(day1.dayKey, day2.dayKey)
    }

    @Test
    fun sameDayKeyAndSeedAlwaysRollsTheSameChallenge() {
        val a = DailyChallengeSystem.ensureCurrent(null, nowEpochMillis = 5_000L, worldSeed = 42L)
        val b = DailyChallengeSystem.ensureCurrent(null, nowEpochMillis = 5_000L, worldSeed = 42L)
        assertEquals(a, b)
    }

    @Test
    fun completedChallengeStopsAccumulatingProgress() {
        val challenge = DailyChallenge(
            dayKey = 1L, description = "", targetType = MissionTargetType.COLLECT_COINS, targetValue = 10, progress = 10,
        )
        val summary = RunSummary(0.0, 5, emptyMap(), true, 0L)
        val updated = challenge.applyRunResult(summary)
        assertEquals(10, updated.progress)
    }
}
