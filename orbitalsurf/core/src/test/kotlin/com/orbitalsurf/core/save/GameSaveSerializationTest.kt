package com.orbitalsurf.core.save

import com.orbitalsurf.core.economy.CheckpointUnlocks
import com.orbitalsurf.core.economy.Inventory
import com.orbitalsurf.core.economy.Wallet
import com.orbitalsurf.core.progression.PlayerStats
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Exercises the exact JSON encode/decode logic `:app`'s DataStore `Serializer<GameSave>`
 * will reuse -- if this round-trips correctly here, the DataStore wiring around it (which
 * this sandbox can't compile/run) has nothing left to get wrong on the serialization side.
 */
class GameSaveSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `default GameSave round-trips through JSON`() {
        val original = GameSaveDefaults.new()
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<GameSave>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `a fully populated GameSave round-trips through JSON with every field intact`() {
        val original = GameSave(
            bestScore = 123_456L,
            wallet = Wallet(plates = 7_890L),
            inventory = Inventory(
                missionSkipVouchers = 3,
                headstartTickets = mapOf(1 to 2, 5 to 1),
                ownedSkinIds = setOf("skin_neon_ball", "skin_appteka_follower"),
                equippedSkinId = "skin_neon_ball",
            ),
            checkpointUnlocks = CheckpointUnlocks(unlocked = setOf(1, 2, 3)),
            visitedExternalLinkAchievementIds = setOf("appteka_follow"),
            playerStats = PlayerStats(
                totalDistance = 54_321.5,
                totalPlatesEarned = 10_000L,
                missionsCompleted = 42L,
                dailiesCompleted = 7L,
            ),
            dailyLastResetEpochDay = 20_100L,
            dailyCompletedChallengeIds = setOf("daily-20100-score"),
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<GameSave>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `an empty JSON object decodes to all-defaults`() {
        val decoded = json.decodeFromString<GameSave>("{}")
        assertEquals(GameSaveDefaults.new(), decoded)
    }
}
