package com.vaultgame.core.progression

/** Checks lifetime stats against [AchievementCatalog] and reports anything newly earned. */
object AchievementSystem {
    fun checkNewlyUnlocked(stats: PlayerStats, alreadyUnlockedIds: Set<String>): List<Achievement> =
        AchievementCatalog.all.filter { achievement ->
            achievement.id !in alreadyUnlockedIds && statValue(stats, achievement.category) >= achievement.threshold
        }

    private fun statValue(stats: PlayerStats, category: AchievementCategory): Long = when (category) {
        AchievementCategory.DISTANCE -> stats.totalDistanceMeters.toLong()
        AchievementCategory.COINS -> stats.totalCoinsCollected
        AchievementCategory.NO_HIT_STREAK -> stats.bestNoHitStreak.toLong()
        AchievementCategory.POWERUP_USAGE -> stats.powerupUsageTotals.values.sumOf { it.toLong() }
        AchievementCategory.GAMES_PLAYED -> stats.gamesPlayed.toLong()
    }
}
