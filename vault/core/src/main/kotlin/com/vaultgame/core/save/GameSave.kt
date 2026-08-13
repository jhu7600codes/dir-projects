package com.vaultgame.core.save

import com.vaultgame.core.economy.Inventory
import com.vaultgame.core.economy.Wallet
import com.vaultgame.core.leaderboard.LeaderboardEntry
import com.vaultgame.core.progression.DailyChallenge
import com.vaultgame.core.progression.DailyLoginState
import com.vaultgame.core.progression.MissionState
import com.vaultgame.core.progression.PlayerStats
import kotlinx.serialization.Serializable

/**
 * The whole persisted game state -- one JSON blob written by the app's DataStore repository.
 * [worldSeed] is fixed at first-ever save creation and never changes; per-run variety comes from
 * combining it with [PlayerStats.gamesPlayed] in [com.vaultgame.core.session.RunResultApplier].
 */
@Serializable
data class GameSave(
    val worldSeed: Long,
    val wallet: Wallet = Wallet(),
    val inventory: Inventory = Inventory(),
    val playerStats: PlayerStats = PlayerStats(),
    val missionState: MissionState,
    val dailyChallenge: DailyChallenge? = null,
    val dailyLogin: DailyLoginState = DailyLoginState(),
    val achievementsUnlockedIds: Set<String> = emptySet(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    /** Distance (meters) a purchased headstart should start the next run at; null means start
     * at 0. Consumed (reset to null) the moment a run actually begins. */
    val pendingHeadstartDistance: Double? = null,
)
