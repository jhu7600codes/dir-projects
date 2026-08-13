package com.vaultgame.core.save

import com.vaultgame.core.economy.Inventory
import com.vaultgame.core.economy.Wallet
import com.vaultgame.core.leaderboard.LeaderboardEntry
import com.vaultgame.core.math.SeededRandom
import com.vaultgame.core.powerups.PowerupType
import com.vaultgame.core.progression.DailyChallenge
import com.vaultgame.core.progression.MissionTargetType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** GameSave is the one object the app's DataStore serializer round-trips to/from JSON, so its
 * whole nested graph -- including enum-keyed maps and nullable fields -- has to actually survive
 * (de)serialization, not just compile with @Serializable. */
class GameSaveSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun defaultSaveRoundTrips() {
        val save = GameSaveDefaults.new(worldSeed = 123L)
        val encoded = json.encodeToString(GameSave.serializer(), save)
        val decoded = json.decodeFromString(GameSave.serializer(), encoded)
        assertEquals(save, decoded)
    }

    @Test
    fun fullyPopulatedSaveRoundTrips() {
        val save = GameSaveDefaults.new(worldSeed = 456L).copy(
            wallet = Wallet(9_999),
            inventory = Inventory(
                ownedSkins = setOf("skin_default", "skin_neon_runner"),
                equippedSkin = "skin_neon_runner",
                powerupUpgradeLevels = mapOf(PowerupType.MAGNET to 2, PowerupType.SHIELD to 0),
                missionSkipVouchers = 3,
                unlockedHeadstarts = setOf(250.0, 1_000.0),
            ),
            dailyChallenge = DailyChallenge(
                dayKey = 42L,
                description = "Collect 200 coins today",
                targetType = MissionTargetType.COLLECT_COINS,
                targetValue = 200,
                progress = 50,
            ),
            achievementsUnlockedIds = setOf("dist_1k", "coins_500"),
            leaderboard = listOf(LeaderboardEntry(score = 500, distanceMeters = 300.0, coinsCollected = 40, timestampEpochMillis = 1_000L)),
            pendingHeadstartDistance = 500.0,
        )

        val encoded = json.encodeToString(GameSave.serializer(), save)
        val decoded = json.decodeFromString(GameSave.serializer(), encoded)
        assertEquals(save, decoded)
    }

    @Test
    fun missionStateWithPowerupTargetRoundTrips() {
        val state = com.vaultgame.core.progression.MissionPool.rollSet(SeededRandom(7L), 0)
        val encoded = json.encodeToString(com.vaultgame.core.progression.MissionSet.serializer(), state)
        val decoded = json.decodeFromString(com.vaultgame.core.progression.MissionSet.serializer(), encoded)
        assertEquals(state, decoded)
    }
}
