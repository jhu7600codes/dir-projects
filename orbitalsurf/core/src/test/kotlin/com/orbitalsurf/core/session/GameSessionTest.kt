package com.orbitalsurf.core.session

import com.orbitalsurf.core.physics.SteerInput
import com.orbitalsurf.core.progression.ActivePowerups
import com.orbitalsurf.core.progression.MissionSystem
import com.orbitalsurf.core.world.PowerupType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSessionTest {
    private val dt = 0.05
    private val neutral = SteerInput.NEUTRAL

    @Test
    fun `score and distance increase over time as the ball runs forward`() {
        val session = GameSession(seed = 1L)
        val first = session.update(dt, neutral)
        var last = first
        repeat(200) { last = session.update(dt, neutral) }

        assertTrue(last.distanceTraveled > first.distanceTraveled)
        assertTrue(last.score >= first.score)
    }

    @Test
    fun `running with no jump input eventually falls off a gap and ends the run`() {
        val session = GameSession(seed = 1L)
        var gameOver = false
        repeat(6_000) {
            if (!gameOver) {
                val frame = session.update(dt, neutral)
                gameOver = frame.isGameOver
            }
        }
        assertTrue("expected the run to end after enough distance with no jumping", gameOver)
    }

    @Test
    fun `a shield banked at the start survives the same fall that ends an unshielded run`() {
        val unshielded = GameSession(seed = 1L)
        val shieldPowerups = ActivePowerups().also { it.collect(PowerupType.Shield) }
        val shielded = GameSession(seed = 1L, activePowerups = shieldPowerups)

        var failTick = -1
        var wasEverShielded = false
        for (tick in 0 until 6_000) {
            val unshieldedFrame = unshielded.update(dt, neutral)
            val shieldedFrame = shielded.update(dt, neutral)
            if (shieldedFrame.isShielded) wasEverShielded = true

            if (failTick < 0 && unshieldedFrame.isGameOver) {
                failTick = tick
                // Same seed, same inputs, same physics -- at the exact tick the unshielded
                // run dies, the shielded one (which banked a shield from tick 0) must still
                // be alive, and must have consumed the shield to survive it.
                assertTrue("expected the shielded run to survive the same fall", !shieldedFrame.isGameOver)
                assertTrue("expected the shield to have been active before the save", wasEverShielded)
                assertTrue("expected the shield to be consumed after saving the run", !shieldedFrame.isShielded)
                break
            }
        }
        assertTrue("expected the unshielded run to have ended within the tick budget", failTick >= 0)
    }

    @Test
    fun `crossing checkpoint 1's distance is recorded in the run summary`() {
        // Start just before checkpoint 1 (500m) and run far enough forward to cross it.
        val session = GameSession(seed = 1L, startDistance = 490.0)
        repeat(500) { session.update(dt, neutral) }
        assertTrue(session.buildRunSummary().reachedCheckpoints.contains(1))
    }

    @Test
    fun `forcing all 3 active missions complete via vouchers raises the score multiplier mid-run`() {
        val session = GameSession(seed = 1L)
        val firstFrame = session.update(dt, neutral)
        assertEquals(1.0, firstFrame.missionMultiplier, 1e-9)

        firstFrame.activeMissions.forEach { mission -> session.useMissionSkipVoucher(mission.id) }

        val afterFrame = session.update(dt, neutral)
        assertEquals(MissionSystem.MULTIPLIER_STEPS[1], afterFrame.missionMultiplier, 1e-9)
    }

    @Test
    fun `buildRunSummary reflects accumulated score and distance`() {
        val session = GameSession(seed = 1L)
        repeat(100) { session.update(dt, neutral) }
        val frame = session.update(dt, neutral)
        val summary = session.buildRunSummary()

        assertEquals(frame.score, summary.finalScore)
        assertEquals(frame.distanceTraveled, summary.distanceTraveled, 1e-6)
    }
}
