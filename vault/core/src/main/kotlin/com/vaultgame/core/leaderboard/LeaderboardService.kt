package com.vaultgame.core.leaderboard

/** Local top-N runs by score, highest first. No backend -- entries live in GameSave. */
object LeaderboardService {
    const val MAX_ENTRIES = 20

    fun withEntryAdded(entries: List<LeaderboardEntry>, newEntry: LeaderboardEntry): List<LeaderboardEntry> =
        (entries + newEntry).sortedByDescending { it.score }.take(MAX_ENTRIES)
}
