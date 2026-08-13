package com.vaultgame.core.physics

import com.vaultgame.core.powerups.ActivePowerups
import com.vaultgame.core.powerups.PowerupType
import com.vaultgame.core.world.Lane
import com.vaultgame.core.world.Obstacle
import com.vaultgame.core.world.ObstacleType
import com.vaultgame.core.world.Pickup
import com.vaultgame.core.world.PickupType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunSimulatorTest {
    private val dt = 0.02

    /** Runs [ticks] fixed steps, applying [actionsOnFirstTick] only on the very first one. */
    private fun run(
        start: PlayerState,
        obstacles: List<Obstacle>,
        pickups: List<Pickup>,
        powerups: ActivePowerups,
        ticks: Int,
        actionsOnFirstTick: List<PlayerAction> = emptyList(),
    ): PlayerState {
        var state = start
        for (i in 0 until ticks) {
            val actions = if (i == 0) actionsOnFirstTick else emptyList()
            val result = RunSimulator.step(state, actions, obstacles, pickups, powerups, dt)
            state = result.state
        }
        return state
    }

    @Test
    fun jumpAvoidsLowVent() {
        val obstacle = Obstacle(distance = 1.0, lane = Lane.CENTER, type = ObstacleType.LOW_VENT)
        val end = run(
            PlayerState(), listOf(obstacle), emptyList(), ActivePowerups(),
            ticks = 40, actionsOnFirstTick = listOf(PlayerAction.JUMP),
        )
        assertTrue(end.alive)
    }

    @Test
    fun missingJumpHitsLowVent() {
        val obstacle = Obstacle(distance = 1.0, lane = Lane.CENTER, type = ObstacleType.LOW_VENT)
        val end = run(PlayerState(), listOf(obstacle), emptyList(), ActivePowerups(), ticks = 40)
        assertFalse(end.alive)
    }

    @Test
    fun slideAvoidsOverheadPipe() {
        val obstacle = Obstacle(distance = 1.0, lane = Lane.CENTER, type = ObstacleType.OVERHEAD_PIPE)
        val end = run(
            PlayerState(), listOf(obstacle), emptyList(), ActivePowerups(),
            ticks = 40, actionsOnFirstTick = listOf(PlayerAction.SLIDE),
        )
        assertTrue(end.alive)
    }

    @Test
    fun jumpingDoesNotAvoidOverheadPipe() {
        val obstacle = Obstacle(distance = 1.0, lane = Lane.CENTER, type = ObstacleType.OVERHEAD_PIPE)
        val end = run(
            PlayerState(), listOf(obstacle), emptyList(), ActivePowerups(),
            ticks = 40, actionsOnFirstTick = listOf(PlayerAction.JUMP),
        )
        assertFalse(end.alive)
    }

    @Test
    fun crateStackIsNeverAvoidableInPlace() {
        // Two independent Obstacle instances -- resolveObstacles mutates `resolved` on the
        // instance it's given, so reusing one across sub-cases would make the second a no-op.
        val jumped = run(
            PlayerState(),
            listOf(Obstacle(distance = 1.0, lane = Lane.CENTER, type = ObstacleType.CRATE_STACK)),
            emptyList(), ActivePowerups(), ticks = 40, actionsOnFirstTick = listOf(PlayerAction.JUMP),
        )
        assertFalse(jumped.alive)

        val slid = run(
            PlayerState(),
            listOf(Obstacle(distance = 1.0, lane = Lane.CENTER, type = ObstacleType.CRATE_STACK)),
            emptyList(), ActivePowerups(), ticks = 40, actionsOnFirstTick = listOf(PlayerAction.SLIDE),
        )
        assertFalse(slid.alive)
    }

    @Test
    fun switchingLaneAvoidsCrateStack() {
        val obstacle = Obstacle(distance = 2.0, lane = Lane.CENTER, type = ObstacleType.CRATE_STACK)
        val end = run(
            PlayerState(), listOf(obstacle), emptyList(), ActivePowerups(),
            ticks = 60, actionsOnFirstTick = listOf(PlayerAction.MOVE_LEFT),
        )
        assertTrue(end.alive)
        assertEquals(Lane.LEFT, end.lane)
    }

    @Test
    fun roofGapRequiresJumpRegardlessOfLane() {
        val switchedButDidNotJump = run(
            PlayerState(),
            listOf(Obstacle(distance = 2.0, lane = Lane.CENTER, type = ObstacleType.ROOF_GAP)),
            emptyList(), ActivePowerups(), ticks = 60, actionsOnFirstTick = listOf(PlayerAction.MOVE_RIGHT),
        )
        assertFalse(switchedButDidNotJump.alive)

        val jumped = run(
            PlayerState(),
            listOf(Obstacle(distance = 2.0, lane = Lane.CENTER, type = ObstacleType.ROOF_GAP)),
            emptyList(), ActivePowerups(), ticks = 40, actionsOnFirstTick = listOf(PlayerAction.JUMP),
        )
        assertTrue(jumped.alive)
    }

    @Test
    fun shieldAbsorbsExactlyOneHitThenBreaks() {
        val powerups = ActivePowerups().apply { activate(PowerupType.SHIELD) }
        val firstHit = Obstacle(distance = 1.0, lane = Lane.CENTER, type = ObstacleType.LOW_VENT)
        val secondHit = Obstacle(distance = 2.0, lane = Lane.CENTER, type = ObstacleType.LOW_VENT)

        var state = PlayerState()
        val step1 = RunSimulator.step(state, emptyList(), listOf(firstHit), emptyList(), powerups, dtSeconds = 0.2)
        state = step1.state
        assertTrue("shield should absorb the first hit", state.alive)
        assertTrue(step1.events.any { it is RunEvent.ObstacleHit && it.shieldAbsorbed })
        assertFalse(powerups.isActive(PowerupType.SHIELD))

        repeat(30) {
            val step = RunSimulator.step(state, emptyList(), listOf(secondHit), emptyList(), powerups, dt)
            state = step.state
        }
        assertFalse("second hit with no shield left should end the run", state.alive)
    }

    @Test
    fun jetpackFlightIgnoresObstacles() {
        val powerups = ActivePowerups().apply { activate(PowerupType.JETPACK) }
        val obstacle = Obstacle(distance = 1.0, lane = Lane.CENTER, type = ObstacleType.CRATE_STACK)
        val end = run(PlayerState(), listOf(obstacle), emptyList(), powerups, ticks = 40)
        assertTrue(end.alive)
    }

    @Test
    fun deadPlayerStopsAdvancing() {
        val obstacle = Obstacle(distance = 1.0, lane = Lane.CENTER, type = ObstacleType.LOW_VENT)
        val dead = PlayerState(alive = false, distance = 5.0)
        val result = RunSimulator.step(dead, emptyList(), listOf(obstacle), emptyList(), ActivePowerups(), dtSeconds = 1.0)
        assertEquals(5.0, result.state.distance, 1e-9)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun coinCollectionEmitsEventWithMultiplier() {
        val powerups = ActivePowerups().apply { activate(PowerupType.COIN_MULTIPLIER) }
        val coin = Pickup(distance = 1.0, lane = Lane.CENTER, type = PickupType.Coin)
        var state = PlayerState()
        var collectedValue = 0
        repeat(40) {
            val result = RunSimulator.step(state, emptyList(), emptyList(), listOf(coin), powerups, dt)
            state = result.state
            result.events.filterIsInstance<RunEvent.CoinCollected>().forEach { collectedValue += it.baseValue }
        }
        assertEquals(2, collectedValue) // base coin value 1 * x2 multiplier
        assertTrue(coin.collected)
    }

    @Test
    fun magnetPullsInAdjacentLaneCoin() {
        val powerups = ActivePowerups().apply { activate(PowerupType.MAGNET) }
        val coin = Pickup(distance = 1.0, lane = Lane.RIGHT, type = PickupType.Coin) // player runs CENTER
        var state = PlayerState()
        var collected = false
        repeat(40) {
            val result = RunSimulator.step(state, emptyList(), emptyList(), listOf(coin), powerups, dt)
            state = result.state
            if (result.events.any { it is RunEvent.CoinCollected }) collected = true
        }
        assertTrue("magnet should reach across one lane", collected)
    }

    @Test
    fun withoutMagnetAdjacentLaneCoinIsMissed() {
        val coin = Pickup(distance = 1.0, lane = Lane.RIGHT, type = PickupType.Coin)
        var state = PlayerState()
        var collected = false
        repeat(40) {
            val result = RunSimulator.step(state, emptyList(), emptyList(), listOf(coin), ActivePowerups(), dt)
            state = result.state
            if (result.events.any { it is RunEvent.CoinCollected }) collected = true
        }
        assertFalse(collected)
    }

    @Test
    fun powerupPickupActivatesItself() {
        val powerups = ActivePowerups()
        val pickup = Pickup(distance = 1.0, lane = Lane.CENTER, type = PickupType.Powerup(PowerupType.SPEED_BOOST))
        var state = PlayerState()
        repeat(40) {
            val result = RunSimulator.step(state, emptyList(), emptyList(), listOf(pickup), powerups, dt)
            state = result.state
        }
        assertTrue(powerups.isActive(PowerupType.SPEED_BOOST))
    }
}
