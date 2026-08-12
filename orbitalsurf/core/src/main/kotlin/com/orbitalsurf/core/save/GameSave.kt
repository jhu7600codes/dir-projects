package com.orbitalsurf.core.save

import com.orbitalsurf.core.economy.CheckpointUnlocks
import com.orbitalsurf.core.economy.Inventory
import com.orbitalsurf.core.economy.Wallet
import com.orbitalsurf.core.progression.DailyChallengeSystem
import com.orbitalsurf.core.progression.PlayerStats
import kotlinx.serialization.Serializable

/**
 * Everything persisted between sessions, as one JSON-serializable aggregate.
 *
 * Deliberately does NOT include the active mission set/escalation tier (missions reset fresh
 * every run -- see `MissionSystem`) or the full daily-challenge objects (only
 * [dailyLastResetEpochDay] and [dailyCompletedChallengeIds] are stored; the actual
 * `DailyChallenge` list -- descriptions, targets -- is always re-derived from
 * `DailyChallengePool.draw(dailyLastResetEpochDay)`, which is a pure function of that one
 * Long). That sidesteps needing polymorphic serialization for any of the sealed
 * goal/mission/achievement types in this codebase -- the save file only ever stores plain
 * data (numbers, strings, sets, maps), never a sealed hierarchy.
 */
@Serializable
data class GameSave(
    val bestScore: Long = 0L,
    val wallet: Wallet = Wallet(),
    val inventory: Inventory = Inventory(),
    val checkpointUnlocks: CheckpointUnlocks = CheckpointUnlocks(),
    val visitedExternalLinkAchievementIds: Set<String> = emptySet(),
    val playerStats: PlayerStats = PlayerStats(),
    val dailyLastResetEpochDay: Long = DailyChallengeSystem.NEVER_RESET,
    val dailyCompletedChallengeIds: Set<String> = emptySet(),
)
