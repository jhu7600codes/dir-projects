package com.vaultgame.core.session

import com.vaultgame.core.leaderboard.LeaderboardEntry
import com.vaultgame.core.leaderboard.LeaderboardService
import com.vaultgame.core.math.SeededRandom
import com.vaultgame.core.progression.Achievement
import com.vaultgame.core.progression.AchievementSystem
import com.vaultgame.core.progression.DailyChallengeSystem
import com.vaultgame.core.progression.MissionSystem
import com.vaultgame.core.progression.RunSummary
import com.vaultgame.core.save.GameSave

/** Turns a finished [GameSession] into an updated [GameSave]: wallet credit, mission/daily
 * progress, lifetime stats, newly unlocked achievements/skins, and a leaderboard entry. */
object RunResultApplier {

    /** Plates earned per coin collected in a run -- the only way to earn the soft currency. */
    const val PLATES_PER_COIN = 2L

    data class Result(
        val updatedSave: GameSave,
        val summary: RunSummary,
        val platesEarned: Long,
        val newlyUnlockedAchievements: List<Achievement>,
    )

    /** Consumes any pending headstart, returning a fresh [GameSession] and the [GameSave] with
     * that headstart cleared. The run's own procedural layout is seeded from [GameSave.worldSeed]
     * combined with the run count so every attempt sees a different, still-deterministic path. */
    fun beginRun(save: GameSave): Pair<GameSession, GameSave> {
        val runSeed = save.worldSeed xor ((save.playerStats.gamesPlayed + 1) * -0x61c8864680b583ebL)
        val startDistance = save.pendingHeadstartDistance ?: 0.0
        val session = GameSession(runSeed, startDistance, save.inventory.powerupUpgradeLevels)
        return session to save.copy(pendingHeadstartDistance = null)
    }

    fun apply(save: GameSave, session: GameSession, nowEpochMillis: Long): Result {
        val (scoreMultiplier, missionStateAfterConsume) = MissionSystem.consumePendingMultiplier(save.missionState)
        val summary = session.buildSummary(scoreMultiplier)

        val rng = SeededRandom(save.worldSeed xor nowEpochMillis)
        val missionState = MissionSystem.applyRunResult(missionStateAfterConsume, summary, rng)

        val dailyChallenge = DailyChallengeSystem
            .ensureCurrent(save.dailyChallenge, nowEpochMillis, save.worldSeed)
            .let { DailyChallengeSystem.applyRunResult(it, summary) }

        val playerStats = save.playerStats.applyRunResult(summary)
        val newlyUnlocked = AchievementSystem.checkNewlyUnlocked(playerStats, save.achievementsUnlockedIds)

        val inventory = newlyUnlocked.fold(save.inventory) { inv, achievement ->
            achievement.unlockSkinId?.let(inv::withSkinUnlocked) ?: inv
        }

        val platesEarned = summary.coinsCollected * PLATES_PER_COIN
        val wallet = save.wallet.credit(platesEarned)

        val leaderboard = LeaderboardService.withEntryAdded(
            save.leaderboard,
            LeaderboardEntry(summary.score, summary.distanceMeters, summary.coinsCollected, nowEpochMillis),
        )

        val updatedSave = save.copy(
            wallet = wallet,
            inventory = inventory,
            playerStats = playerStats,
            missionState = missionState,
            dailyChallenge = dailyChallenge,
            achievementsUnlockedIds = save.achievementsUnlockedIds + newlyUnlocked.map { it.id },
            leaderboard = leaderboard,
        )

        return Result(updatedSave, summary, platesEarned, newlyUnlocked)
    }
}
