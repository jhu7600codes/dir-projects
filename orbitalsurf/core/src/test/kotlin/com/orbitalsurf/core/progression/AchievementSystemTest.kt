package com.orbitalsurf.core.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementSystemTest {
    @Test
    fun `no stat achievements are unlocked for a fresh player`() {
        val system = AchievementSystem()
        assertTrue(system.unlockedStatThresholds(PlayerStats()).isEmpty())
    }

    @Test
    fun `crossing a stat threshold unlocks the matching achievement`() {
        val system = AchievementSystem()
        val stats = PlayerStats(totalDistance = 1_500.0)
        val unlocked = system.unlockedStatThresholds(stats)
        assertTrue(unlocked.any { it.id == "distance_1k" })
        assertTrue("shouldn't unlock the 10k tier yet", unlocked.none { it.id == "distance_10k" })
    }

    @Test
    fun `external link achievements are locked until visited, and unlocking is idempotent`() {
        val system = AchievementSystem()
        assertFalse(system.isExternalLinkVisited("appteka_follow"))
        assertTrue(system.unlockedExternalLinks().isEmpty())

        system.markExternalLinkVisited("appteka_follow")
        system.markExternalLinkVisited("appteka_follow")

        assertTrue(system.isExternalLinkVisited("appteka_follow"))
        assertEquals(1, system.unlockedExternalLinks().size)
    }

    @Test
    fun `the two Appteka achievements are present in the catalog as external links`() {
        val ids = AchievementCatalog.all.filterIsInstance<Achievement.ExternalLinkAchievement>().map { it.id }
        assertTrue(ids.contains("appteka_follow"))
        assertTrue(ids.contains("appteka_subway_surfers_og"))
    }

    @Test
    fun `allUnlocked combines stat and external-link achievements`() {
        val system = AchievementSystem()
        system.markExternalLinkVisited("appteka_follow")
        val stats = PlayerStats(totalDistance = 1_500.0)

        val all = system.allUnlocked(stats)

        assertTrue(all.any { it.id == "distance_1k" })
        assertTrue(all.any { it.id == "appteka_follow" })
    }

    @Test
    fun `visited ids round-trip through restoreVisited for save-file persistence`() {
        val system = AchievementSystem()
        system.markExternalLinkVisited("appteka_follow")
        system.markExternalLinkVisited("appteka_subway_surfers_og")

        val restored = AchievementSystem()
        restored.restoreVisited(system.visitedIds())

        assertEquals(system.visitedIds(), restored.visitedIds())
    }
}
