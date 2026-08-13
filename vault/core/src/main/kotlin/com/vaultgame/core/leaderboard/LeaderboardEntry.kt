package com.vaultgame.core.leaderboard

import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardEntry(
    val score: Long,
    val distanceMeters: Double,
    val coinsCollected: Int,
    val timestampEpochMillis: Long,
)
