package com.vaultgame.core.progression

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementSystemTest {
    @Test
    fun unlocksDistanceMilestoneOnceReached() {
        val stats = PlayerStats(totalDistanceMeters = 1_200.0)
        val unlocked = AchievementSystem.checkNewlyUnlocked(stats, emptySet())
        assertTrue(unlocked.any { it.id == "dist_1k" })
        assertFalse(unlocked.any { it.id == "dist_5k" })
    }

    @Test
    fun alreadyUnlockedAchievementsAreNotReported() {
        val stats = PlayerStats(totalDistanceMeters = 1_200.0)
        val unlocked = AchievementSystem.checkNewlyUnlocked(stats, alreadyUnlockedIds = setOf("dist_1k"))
        assertFalse(unlocked.any { it.id == "dist_1k" })
    }

    @Test
    fun coinAndStreakAndUsageAndGamesCategoriesAllWork() {
        val stats = PlayerStats(
            totalCoinsCollected = 600,
            bestNoHitStreak = 3,
            powerupUsageTotals = mapOf(com.vaultgame.core.powerups.PowerupType.MAGNET to 25),
            gamesPlayed = 10,
        )
        val unlocked = AchievementSystem.checkNewlyUnlocked(stats, emptySet()).map { it.id }.toSet()
        assertTrue("coins_500" in unlocked)
        assertTrue("streak_3" in unlocked)
        assertTrue("powerup_25" in unlocked)
        assertTrue("games_10" in unlocked)
    }

    @Test
    fun everyCatalogEntryHasAUniqueId() {
        val ids = AchievementCatalog.all.map { it.id }
        assertTrue(ids.size == ids.toSet().size)
    }
}
