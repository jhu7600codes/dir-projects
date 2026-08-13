package com.vaultgame.core.progression

import kotlinx.serialization.Serializable

/** [currentStreak] is the number of consecutive UTC calendar days the player has opened the app
 * on, including today once claimed. [lastClaimDayKey] is null before the very first login. */
@Serializable
data class DailyLoginState(
    val lastClaimDayKey: Long? = null,
    val currentStreak: Int = 0,
)

data class DailyLoginResult(
    val newState: DailyLoginState,
    val alreadyClaimedToday: Boolean,
    val platesAwarded: Int,
    val streakDay: Int,
)

/** Small login-streak bonus: base plates plus a per-day ramp, capped so very long streaks don't
 * run away, then flat. */
object DailyLoginSystem {
    private const val MILLIS_PER_DAY = 86_400_000L
    const val BASE_REWARD = 50
    const val PER_DAY_BONUS = 25
    const val STREAK_CAP_DAYS = 7

    fun dayKeyFor(epochMillis: Long): Long = Math.floorDiv(epochMillis, MILLIS_PER_DAY)

    fun rewardForStreakDay(streakDay: Int): Int =
        BASE_REWARD + PER_DAY_BONUS * (streakDay.coerceAtMost(STREAK_CAP_DAYS) - 1)

    fun claim(state: DailyLoginState, nowEpochMillis: Long): DailyLoginResult {
        val today = dayKeyFor(nowEpochMillis)
        if (state.lastClaimDayKey == today) {
            return DailyLoginResult(state, alreadyClaimedToday = true, platesAwarded = 0, streakDay = state.currentStreak)
        }
        val newStreak = if (state.lastClaimDayKey == today - 1) state.currentStreak + 1 else 1
        val reward = rewardForStreakDay(newStreak)
        val newState = DailyLoginState(lastClaimDayKey = today, currentStreak = newStreak)
        return DailyLoginResult(newState, alreadyClaimedToday = false, platesAwarded = reward, streakDay = newStreak)
    }
}
