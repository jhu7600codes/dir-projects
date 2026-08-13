package com.vaultgame.core.progression

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreSystemTest {
    @Test
    fun combinesDistanceAndCoinsWithNoMultiplier() {
        val score = ScoreSystem.computeScore(distanceMeters = 100.0, coinsCollected = 10, multiplier = 1.0)
        // 100 * 1.0 + 10 * 5.0 = 150
        assertEquals(150L, score)
    }

    @Test
    fun multiplierScalesTheWholeScore() {
        val base = ScoreSystem.computeScore(500.0, 20, 1.0)
        val boosted = ScoreSystem.computeScore(500.0, 20, 1.5)
        assertEquals((base * 1.5).toLong(), boosted)
    }

    @Test
    fun zeroActivityScoresZero() {
        assertEquals(0L, ScoreSystem.computeScore(0.0, 0, 1.0))
    }
}
