package com.vaultgame.core.leaderboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaderboardServiceTest {
    private fun entry(score: Long) = LeaderboardEntry(score, distanceMeters = score.toDouble(), coinsCollected = 0, timestampEpochMillis = 0L)

    @Test
    fun newEntryIsInsertedInDescendingScoreOrder() {
        val entries = LeaderboardService.withEntryAdded(listOf(entry(100), entry(50)), entry(75))
        assertEquals(listOf(100L, 75L, 50L), entries.map { it.score })
    }

    @Test
    fun listIsCappedAtMaxEntries() {
        var entries = emptyList<LeaderboardEntry>()
        for (score in 1..(LeaderboardService.MAX_ENTRIES + 10)) {
            entries = LeaderboardService.withEntryAdded(entries, entry(score.toLong()))
        }
        assertEquals(LeaderboardService.MAX_ENTRIES, entries.size)
        assertTrue(entries.all { it.score > 10 }) // the lowest 10 scores got pushed out
    }

    @Test
    fun emptyListAcceptsFirstEntry() {
        val entries = LeaderboardService.withEntryAdded(emptyList(), entry(42))
        assertEquals(1, entries.size)
        assertEquals(42L, entries.first().score)
    }
}
