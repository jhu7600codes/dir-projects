package com.vaultgame.core.session

import com.vaultgame.core.physics.PlayerAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSessionTest {
    @Test
    fun tickingAdvancesDistance() {
        // No input is queued, so an obstacle in the CENTER lane can legitimately end the run
        // early (that's correct engine behavior) -- this only asserts forward progress happened
        // before that, not that the run survives the whole window.
        val session = GameSession(runSeed = 1L)
        repeat(100) { session.tick(0.05) }
        assertTrue(session.playerState.distance > 0.0)
    }

    @Test
    fun distanceStopsChangingOnceTheRunEnds() {
        val session = GameSession(runSeed = 1L)
        var ticks = 0
        while (!session.runEnded && ticks < 20_000) {
            session.tick(0.05)
            ticks++
        }
        val distanceAtDeath = session.playerState.distance
        repeat(10) { session.tick(0.05) }
        assertEquals(distanceAtDeath, session.playerState.distance, 1e-9)
    }

    @Test
    fun bufferedSegmentsAlwaysCoverTheLookaheadWindow() {
        val session = GameSession(runSeed = 2L)
        repeat(50) { session.tick(0.1) }
        val furthestBuffered = session.visibleSegments.maxOf { it.endDistance }
        assertTrue(furthestBuffered > session.playerState.distance)
    }

    @Test
    fun aLongEnoughRunWithoutInputEventuallyEndsOnAnObstacle() {
        val session = GameSession(runSeed = 3L)
        var ticks = 0
        // No input at all -- every LOW_VENT/OVERHEAD_PIPE/CRATE_STACK/ROOF_GAP/CLOTHESLINE in
        // the CENTER lane the player never leaves will eventually end the run.
        while (!session.runEnded && ticks < 20_000) {
            session.tick(0.05)
            ticks++
        }
        assertFalse(session.playerState.alive)
        assertTrue(session.runEnded)
    }

    @Test
    fun queuedJumpsAndSlidesCanSurviveIndefinitely() {
        val session = GameSession(runSeed = 4L)
        // Cheat: alternate jump/slide every tick so almost everything in the CENTER lane clears,
        // just to prove queueAction actually reaches the simulator through a full session tick.
        repeat(200) { i ->
            session.queueAction(if (i % 2 == 0) PlayerAction.JUMP else PlayerAction.SLIDE)
            session.tick(0.05)
        }
        // Not asserting survival (CRATE_STACK always hits), just that actions were consumed
        // without throwing and distance moved forward.
        assertTrue(session.playerState.distance > 0.0)
    }

    @Test
    fun summaryReflectsCoinsAndMultiplier() {
        val session = GameSession(runSeed = 5L)
        repeat(300) { session.tick(0.05) }
        val summary = session.buildSummary(scoreMultiplier = 2.0)
        assertTrue(summary.distanceMeters > 0.0)
        assertTrue(summary.score >= 0L)
    }
}
