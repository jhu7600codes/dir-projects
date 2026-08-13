package com.vaultgame.core.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyLoginSystemTest {
    private val oneDayMillis = 86_400_000L

    @Test
    fun firstEverLoginStartsStreakAtOne() {
        val result = DailyLoginSystem.claim(DailyLoginState(), nowEpochMillis = 0L)
        assertEquals(1, result.streakDay)
        assertEquals(DailyLoginSystem.BASE_REWARD, result.platesAwarded)
        assertFalse(result.alreadyClaimedToday)
    }

    @Test
    fun claimingTwiceInSameDayGrantsNothingTheSecondTime() {
        val first = DailyLoginSystem.claim(DailyLoginState(), nowEpochMillis = 0L)
        val second = DailyLoginSystem.claim(first.newState, nowEpochMillis = 12_345L)
        assertTrue(second.alreadyClaimedToday)
        assertEquals(0, second.platesAwarded)
        assertEquals(first.streakDay, second.streakDay)
    }

    @Test
    fun consecutiveDayExtendsStreakAndReward() {
        val day1 = DailyLoginSystem.claim(DailyLoginState(), nowEpochMillis = 0L)
        val day2 = DailyLoginSystem.claim(day1.newState, nowEpochMillis = oneDayMillis)
        assertEquals(2, day2.streakDay)
        assertTrue(day2.platesAwarded > day1.platesAwarded)
    }

    @Test
    fun skippedDayResetsStreakToOne() {
        val day1 = DailyLoginSystem.claim(DailyLoginState(), nowEpochMillis = 0L)
        val day3 = DailyLoginSystem.claim(day1.newState, nowEpochMillis = 2 * oneDayMillis)
        assertEquals(1, day3.streakDay)
    }

    @Test
    fun rewardIsCappedAtStreakCapDays() {
        val atCap = DailyLoginSystem.rewardForStreakDay(DailyLoginSystem.STREAK_CAP_DAYS)
        val beyondCap = DailyLoginSystem.rewardForStreakDay(DailyLoginSystem.STREAK_CAP_DAYS + 5)
        assertEquals(atCap, beyondCap)
    }
}
