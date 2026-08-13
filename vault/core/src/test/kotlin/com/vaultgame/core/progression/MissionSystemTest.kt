package com.vaultgame.core.progression

import com.vaultgame.core.math.SeededRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionSystemTest {
    private fun summary(
        distance: Double = 0.0,
        coins: Int = 0,
        powerups: Map<com.vaultgame.core.powerups.PowerupType, Int> = emptyMap(),
        clean: Boolean = true,
    ) = RunSummary(distance, coins, powerups, clean, score = 0L)

    @Test
    fun freshStateHasExactlyThreeMissions() {
        val state = MissionSystem.freshState(SeededRandom(1L))
        assertEquals(3, state.currentSet.missions.size)
        assertEquals(1.0, state.pendingScoreMultiplier, 1e-9)
    }

    @Test
    fun completingAllThreeBanksMultiplierAndRollsNewSet() {
        var state = MissionState(
            currentSet = MissionSet(
                listOf(
                    Mission("m1", "", MissionTargetType.COLLECT_COINS, targetValue = 10),
                    Mission("m2", "", MissionTargetType.COLLECT_COINS, targetValue = 10),
                    Mission("m3", "", MissionTargetType.COLLECT_COINS, targetValue = 10),
                ),
                rewardMultiplier = 2.0,
            ),
        )
        val originalSet = state.currentSet
        state = MissionSystem.applyRunResult(state, summary(coins = 10), SeededRandom(5L))

        assertTrue(state.currentSet !== originalSet)
        assertEquals(2.0, state.pendingScoreMultiplier, 1e-9)
        assertEquals(1, state.setsCompletedTotal)
        assertEquals(3, state.currentSet.missions.size)
    }

    @Test
    fun partialProgressDoesNotCompleteSet() {
        val state = MissionState(
            currentSet = MissionSet(
                listOf(
                    Mission("m1", "", MissionTargetType.COLLECT_COINS, targetValue = 100),
                    Mission("m2", "", MissionTargetType.COLLECT_COINS, targetValue = 100),
                    Mission("m3", "", MissionTargetType.COLLECT_COINS, targetValue = 100),
                ),
            ),
        )
        val updated = MissionSystem.applyRunResult(state, summary(coins = 10), SeededRandom(5L))
        assertEquals(1.0, updated.pendingScoreMultiplier, 1e-9)
        assertTrue(updated.currentSet.missions.all { it.progress == 10 })
        assertTrue(!updated.currentSet.isComplete)
    }

    @Test
    fun consumePendingMultiplierResetsToOne() {
        val state = MissionState(
            currentSet = MissionPool.rollSet(SeededRandom(1L), 0),
            pendingScoreMultiplier = 1.75,
        )
        val (multiplier, newState) = MissionSystem.consumePendingMultiplier(state)
        assertEquals(1.75, multiplier, 1e-9)
        assertEquals(1.0, newState.pendingScoreMultiplier, 1e-9)
    }

    @Test
    fun skipInstantlyCompletesSetAndBanksMultiplier() {
        val state = MissionState(currentSet = MissionPool.rollSet(SeededRandom(2L), 0), pendingScoreMultiplier = 1.0)
        val skipped = MissionSystem.skip(state, SeededRandom(3L))
        assertEquals(MissionSet.DEFAULT_REWARD_MULTIPLIER, skipped.pendingScoreMultiplier, 1e-9)
        assertEquals(1, skipped.setsCompletedTotal)
    }

    @Test
    fun cleanRunStreakMissionResetsOnHit() {
        val mission = Mission("streak", "", MissionTargetType.CLEAN_RUN_STREAK, targetValue = 3, progress = 2)
        val afterHitRun = mission.applyRunResult(summary(clean = false))
        assertEquals(0, afterHitRun.progress)

        val afterCleanRun = mission.applyRunResult(summary(clean = true))
        assertEquals(3, afterCleanRun.progress)
        assertTrue(afterCleanRun.isComplete)
    }

    @Test
    fun runDistanceMissionSnapsRatherThanAccumulates() {
        val mission = Mission("dist", "", MissionTargetType.RUN_DISTANCE_SINGLE_RUN, targetValue = 500, progress = 200)
        val shortRun = mission.applyRunResult(summary(distance = 100.0))
        assertEquals(200, shortRun.progress) // best-so-far kept, not reduced

        val longRun = mission.applyRunResult(summary(distance = 800.0))
        assertEquals(500, longRun.progress) // capped at target, not summed
    }
}
