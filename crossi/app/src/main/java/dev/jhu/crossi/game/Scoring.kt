package dev.jhu.crossi.game

import java.time.LocalDate

object Scoring {
    /** 3 stars: at most 1 check and no power ups. 2: up to 3 checks and 1 power up. */
    fun stars(state: GameState): Int = when {
        state.status != Status.WON -> 0
        state.checksUsed <= 1 && state.powerUpsUsed == 0 -> 3
        state.checksUsed <= 3 && state.powerUpsUsed <= 1 -> 2
        else -> 1
    }

    /** Free roam / daily score. Bigger grids and harder difficulties count more, speed adds a bonus. */
    fun score(state: GameState, difficulty: Difficulty): Int {
        if (state.status != Status.WON) return 0
        val cells = state.geometry.whiteCells.size
        val mult = difficulty.scoreMultiplier
        val base = cells * 10 * mult
        val parMs = cells * 8_000L
        val timeBonus = ((parMs - state.elapsedMs).coerceAtLeast(0) / 1000 * mult).toInt()
        val checkFactor = (1.0 - 0.1 * (state.checksUsed - 1).coerceAtLeast(0)).coerceAtLeast(0.5)
        val powerFactor = (1.0 - 0.15 * state.powerUpsUsed).coerceAtLeast(0.2)
        return ((base + timeBonus) * checkFactor * powerFactor).toInt()
    }
}

object Rewards {
    val BOSS_BUNDLE = listOf(
        PowerUpType.REVEAL_LETTER, PowerUpType.REVEAL_LETTER, PowerUpType.REVEAL_WORD, PowerUpType.EXTRA_CHECK,
    )
    val ALL_DAILIES_BONUS = listOf(PowerUpType.EXTRA_CHECK)

    /** Free roam run score thresholds, each one crossed gives a power up. */
    val RUN_THRESHOLDS = listOf(2_000, 5_000, 10_000, 20_000, 40_000, 80_000, 160_000)

    fun streakMilestone(days: Int): List<PowerUpType>? = when {
        days == 3 -> listOf(PowerUpType.REVEAL_LETTER)
        days == 7 -> listOf(PowerUpType.REVEAL_LETTER, PowerUpType.EXTRA_CHECK)
        days == 14 -> listOf(PowerUpType.REVEAL_LETTER, PowerUpType.REVEAL_WORD, PowerUpType.EXTRA_CHECK)
        days >= 30 && days % 30 == 0 -> PowerUpType.entries.flatMap { listOf(it, it) }
        else -> null
    }

    fun nextMilestone(days: Int): Int = listOf(3, 7, 14).firstOrNull { it > days } ?: ((days / 30) + 1) * 30

    fun runRewards(before: Int, after: Int): List<PowerUpType> =
        RUN_THRESHOLDS.withIndex().filter { (_, t) -> before < t && after >= t }
            .map { (i, _) -> PowerUpType.entries[i % PowerUpType.entries.size] }
}

data class Streak(val current: Int = 0, val best: Int = 0, val lastDate: String? = null)

object Streaks {
    /** Clearing any daily on [today] keeps or grows the streak, once per day. */
    fun onDailyCleared(streak: Streak, today: LocalDate): Streak {
        val last = streak.lastDate?.let(LocalDate::parse)
        val current = when {
            last == today -> return streak
            last == today.minusDays(1) -> streak.current + 1
            else -> 1
        }
        return Streak(current, maxOf(streak.best, current), today.toString())
    }

    /** What the streak shows right now: a missed day means it's gone. */
    fun displayed(streak: Streak, today: LocalDate): Int {
        val last = streak.lastDate?.let(LocalDate::parse) ?: return 0
        return if (last == today || last == today.minusDays(1)) streak.current else 0
    }
}
