package com.orbitalsurf.core.physics

import com.orbitalsurf.core.world.Chunk
import com.orbitalsurf.core.world.Obstacle
import com.orbitalsurf.core.world.PickupKind
import com.orbitalsurf.core.world.PickupPlacement
import com.orbitalsurf.core.world.SurfaceSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BallSimulatorTest {
    private val config = PhysicsConfig.DEFAULT

    private fun chunkOf(vararg segments: SurfaceSegment, obstacles: List<Obstacle> = emptyList(), pickups: List<PickupPlacement> = emptyList()) =
        Chunk(
            index = 0L,
            startDistance = segments.minOf { it.startDistance },
            endDistance = segments.maxOf { it.endDistance },
            segments = segments.toList(),
            obstacles = obstacles,
            pickups = pickups,
        )

    private val flatChunk = chunkOf(SurfaceSegment.RooftopSlab(0.0, 1000.0, -10.0, 10.0, height = 10.0))

    @Test
    fun `an airborne ball with no ground below falls under gravity`() {
        val state = BallState(distance = 0.0, lateral = 0.0, height = 50.0, grounded = false)
        val emptyChunk = chunkOf(SurfaceSegment.Gap(0.0, 1000.0, -10.0, 10.0))

        val result = BallSimulator.step(state, SteerInput.NEUTRAL, dtSeconds = 0.1, forwardSpeed = 5.0, chunks = listOf(emptyChunk), collectedPickupIds = emptySet(), config = config)

        assertTrue("expected height to drop", result.state.height < state.height)
        assertTrue("expected downward velocity", result.state.verticalVelocity < 0.0)
        assertTrue(!result.state.grounded)
    }

    @Test
    fun `a ball resting on a flat slab stays grounded`() {
        val state = BallState.startingAt(distance = 0.0, height = 10.0 + config.ballRadius)
        val result = BallSimulator.step(state, SteerInput.NEUTRAL, dtSeconds = 0.1, forwardSpeed = 5.0, chunks = listOf(flatChunk), collectedPickupIds = emptySet(), config = config)

        assertTrue(result.state.grounded)
        assertEquals(10.0 + config.ballRadius, result.state.height, 1e-6)
        assertTrue(result.events.none { it is RunEvent.FellOff })
    }

    @Test
    fun `jumping while grounded gives upward velocity and leaves the ground`() {
        val state = BallState.startingAt(distance = 0.0, height = 10.0 + config.ballRadius)
        val result = BallSimulator.step(
            state, SteerInput(lateralAxis = 0.0, jumpPressed = true), dtSeconds = 0.01, forwardSpeed = 5.0,
            chunks = listOf(flatChunk), collectedPickupIds = emptySet(), config = config,
        )

        assertTrue("expected the ball to leave the ground", !result.state.grounded)
        assertTrue("expected upward velocity right after a jump", result.state.verticalVelocity > 0.0)
    }

    @Test
    fun `a jump arc eventually lands back on the same slab`() {
        var state = BallState.startingAt(distance = 0.0, height = 10.0 + config.ballRadius)
        var jumped = false
        var landedAgain = false
        repeat(200) {
            val input = if (!jumped) SteerInput(0.0, jumpPressed = true) else SteerInput.NEUTRAL
            val result = BallSimulator.step(state, input, dtSeconds = 0.02, forwardSpeed = 0.0, chunks = listOf(flatChunk), collectedPickupIds = emptySet(), config = config)
            if (!jumped) jumped = true
            if (result.events.contains(RunEvent.Landed)) landedAgain = true
            state = result.state
        }
        assertTrue("expected the ball to land again after jumping", landedAgain)
        assertEquals(10.0 + config.ballRadius, state.height, 1e-6)
    }

    @Test
    fun `on a ramp the ball's height follows the slope as it moves forward`() {
        val ramp = chunkOf(SurfaceSegment.Ramp(0.0, 20.0, -10.0, 10.0, startHeight = 10.0, endHeight = 20.0))
        var state = BallState.startingAt(distance = 0.0, height = 10.0 + config.ballRadius)
        repeat(100) {
            val result = BallSimulator.step(state, SteerInput.NEUTRAL, dtSeconds = 0.05, forwardSpeed = 3.0, chunks = listOf(ramp), collectedPickupIds = emptySet(), config = config)
            state = result.state
        }
        assertTrue("expected the ball to have climbed the ramp", state.height > 10.0 + config.ballRadius + 1.0)
        assertTrue(state.grounded)
    }

    @Test
    fun `steering never exceeds the configured max lateral speed`() {
        var state = BallState.startingAt(distance = 0.0, height = 10.0 + config.ballRadius)
        repeat(500) {
            val result = BallSimulator.step(state, SteerInput(lateralAxis = 1.0, jumpPressed = false), dtSeconds = 0.02, forwardSpeed = 0.0, chunks = listOf(flatChunk), collectedPickupIds = emptySet(), config = config)
            assertTrue("lateral speed exceeded max", abs(result.state.lateralVelocity) <= config.maxLateralSpeed + 1e-9)
            state = result.state
        }
    }

    @Test
    fun `running off the edge of a slab with nothing below eventually fires FellOff`() {
        val shortSlab = chunkOf(SurfaceSegment.RooftopSlab(0.0, 5.0, -10.0, 10.0, height = 10.0))
        var state = BallState.startingAt(distance = 4.5, height = 10.0 + config.ballRadius)
        var fellOff = false
        repeat(300) {
            val result = BallSimulator.step(state, SteerInput.NEUTRAL, dtSeconds = 0.05, forwardSpeed = 5.0, chunks = listOf(shortSlab), collectedPickupIds = emptySet(), config = config)
            if (result.events.contains(RunEvent.FellOff)) fellOff = true
            state = result.state
        }
        assertTrue("expected the ball to fall off after running past the slab's edge", fellOff)
    }

    @Test
    fun `overlapping a low obstacle at ground level emits HitObstacle`() {
        val obstacle = Obstacle("obs-1", distance = 5.0, lateral = 0.0, halfWidthLateral = 1.0, halfWidthDistance = 1.0, height = 1.5)
        val chunk = chunkOf(SurfaceSegment.RooftopSlab(0.0, 20.0, -10.0, 10.0, height = 10.0), obstacles = listOf(obstacle))
        val state = BallState.startingAt(distance = 4.9, height = 10.0 + config.ballRadius)

        val result = BallSimulator.step(state, SteerInput.NEUTRAL, dtSeconds = 0.05, forwardSpeed = 5.0, chunks = listOf(chunk), collectedPickupIds = emptySet(), config = config)

        assertTrue(result.events.any { it is RunEvent.HitObstacle })
    }

    @Test
    fun `jumping high enough clears an obstacle without a hit`() {
        val obstacle = Obstacle("obs-1", distance = 5.0, lateral = 0.0, halfWidthLateral = 1.0, halfWidthDistance = 1.0, height = 1.5)
        val chunk = chunkOf(SurfaceSegment.RooftopSlab(0.0, 20.0, -10.0, 10.0, height = 10.0), obstacles = listOf(obstacle))
        // Simulate the ball already high in its jump arc, well above the obstacle's height, right on top of it.
        val state = BallState(distance = 4.9, lateral = 0.0, height = 10.0 + config.ballRadius + obstacle.height + 1.0, verticalVelocity = 0.0, grounded = false)

        val result = BallSimulator.step(state, SteerInput.NEUTRAL, dtSeconds = 0.02, forwardSpeed = 5.0, chunks = listOf(chunk), collectedPickupIds = emptySet(), config = config)

        assertTrue(result.events.none { it is RunEvent.HitObstacle })
    }

    @Test
    fun `standing inside a checkpoint interior reports CheckpointReached`() {
        val interior = SurfaceSegment.CheckpointInterior(0.0, 20.0, -10.0, 10.0, height = 10.0, checkpointIndex = 3)
        val chunk = chunkOf(interior)
        val state = BallState.startingAt(distance = 5.0, height = 10.0 + config.ballRadius)

        val result = BallSimulator.step(state, SteerInput.NEUTRAL, dtSeconds = 0.05, forwardSpeed = 5.0, chunks = listOf(chunk), collectedPickupIds = emptySet(), config = config)

        assertTrue(result.events.any { it == RunEvent.CheckpointReached(3) })
    }

    @Test
    fun `walking over an uncollected pickup emits CollectedPickup, but not if already collected`() {
        val pickup = PickupPlacement("pk-1", distance = 5.0, lateral = 0.0, height = 1.0, kind = PickupKind.PlatesCoin)
        val chunk = chunkOf(SurfaceSegment.RooftopSlab(0.0, 20.0, -10.0, 10.0, height = 10.0), pickups = listOf(pickup))
        val state = BallState.startingAt(distance = 4.9, height = 10.0 + config.ballRadius)

        val fresh = BallSimulator.step(state, SteerInput.NEUTRAL, dtSeconds = 0.05, forwardSpeed = 5.0, chunks = listOf(chunk), collectedPickupIds = emptySet(), config = config)
        assertTrue(fresh.events.any { it is RunEvent.CollectedPickup && it.pickup.id == "pk-1" })

        val alreadyCollected = BallSimulator.step(state, SteerInput.NEUTRAL, dtSeconds = 0.05, forwardSpeed = 5.0, chunks = listOf(chunk), collectedPickupIds = setOf("pk-1"), config = config)
        assertTrue(alreadyCollected.events.none { it is RunEvent.CollectedPickup })
    }
}

private fun abs(v: Double) = kotlin.math.abs(v)
