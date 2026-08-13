package com.vaultgame.core.progression

import com.vaultgame.core.math.SeededRandom

/** Rolls and advances the daily challenge. Days are UTC calendar days keyed by epoch millis --
 * deliberately timezone-naive, so "resets every 24h" means the same instant worldwide rather
 * than depending on the device's local timezone. */
object DailyChallengeSystem {
    private const val MILLIS_PER_DAY = 86_400_000L

    fun dayKeyFor(epochMillis: Long): Long = Math.floorDiv(epochMillis, MILLIS_PER_DAY)

    /** Returns [existing] unchanged if it's still today's challenge; otherwise rolls a fresh one
     * for today, seeded so the same day always rolls the same challenge for a given [worldSeed]. */
    fun ensureCurrent(existing: DailyChallenge?, nowEpochMillis: Long, worldSeed: Long): DailyChallenge {
        val dayKey = dayKeyFor(nowEpochMillis)
        if (existing != null && existing.dayKey == dayKey) return existing
        return DailyChallengePool.rollForDay(dayKey, SeededRandom(worldSeed xor dayKey))
    }

    fun applyRunResult(challenge: DailyChallenge, summary: RunSummary): DailyChallenge =
        challenge.applyRunResult(summary)
}
