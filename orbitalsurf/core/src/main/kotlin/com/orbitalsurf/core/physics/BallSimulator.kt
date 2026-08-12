package com.orbitalsurf.core.physics

import com.orbitalsurf.core.math.MathUtils
import com.orbitalsurf.core.world.Chunk
import com.orbitalsurf.core.world.SurfaceSampler
import com.orbitalsurf.core.world.SurfaceSegment
import kotlin.math.abs
import kotlin.math.sign

data class StepResult(val state: BallState, val events: List<RunEvent>)

/**
 * Advances the ball one tick. This is a deliberately simplified simulation, not a full
 * rigid-body engine: forward speed is dictated from outside (by `SpeedCurve`, based on
 * score -- the ball can't be slowed by the player, only steered/jumped), lateral movement is
 * a simple accelerate-toward-target-velocity model, and vertical movement is gravity +
 * discrete jump impulses with a snap-to-surface landing check each tick (so the ball hugs
 * ramps smoothly rather than free-falling down every incline).
 *
 * Stateless by design: everything it needs (collected-pickup bookkeeping, effective pickup
 * radius from an active Magnet) is passed in explicitly rather than remembered internally, so
 * the whole simulation stays a pure function of its inputs.
 */
object BallSimulator {
    fun step(
        state: BallState,
        input: SteerInput,
        dtSeconds: Double,
        forwardSpeed: Double,
        chunks: List<Chunk>,
        collectedPickupIds: Set<String>,
        config: PhysicsConfig = PhysicsConfig.DEFAULT,
        pickupCollectionRadius: Double = config.ballRadius + 0.7,
    ): StepResult {
        val events = mutableListOf<RunEvent>()

        val targetLateralVelocity = MathUtils.clamp(input.lateralAxis, -1.0, 1.0) * config.maxLateralSpeed
        val lateralVelocity = moveToward(state.lateralVelocity, targetLateralVelocity, config.lateralAccel * dtSeconds)

        var verticalVelocity = state.verticalVelocity - config.gravity * dtSeconds
        var grounded = state.grounded
        if (input.jumpPressed && grounded) {
            verticalVelocity = config.jumpImpulse
            grounded = false
            events += RunEvent.LeftGround
        }

        val newDistance = state.distance + forwardSpeed * dtSeconds
        val newLateral = state.lateral + lateralVelocity * dtSeconds
        var newHeight = state.height + verticalVelocity * dtSeconds

        val sample = SurfaceSampler.sampleHeight(chunks, newDistance, newLateral)
        if (sample != null) {
            val feetHeight = newHeight - config.ballRadius
            val onOrBelowSurface = feetHeight <= sample.height + config.groundSnapTolerance
            if (onOrBelowSurface && verticalVelocity <= 0.0) {
                newHeight = sample.height + config.ballRadius
                verticalVelocity = 0.0
                if (!grounded) events += RunEvent.Landed
                grounded = true
            } else {
                if (grounded) events += RunEvent.LeftGround
                grounded = false
            }
        } else {
            if (grounded) events += RunEvent.LeftGround
            grounded = false
            if (newHeight < config.fellOffHeight) {
                events += RunEvent.FellOff
            }
        }

        val obstacle = SurfaceSampler.overlappingObstacle(chunks, newDistance, newLateral, config.ballRadius)
        if (obstacle != null) {
            val groundHeightHere = sample?.height ?: (newHeight - config.ballRadius)
            val heightAboveGround = (newHeight - config.ballRadius) - groundHeightHere
            if (heightAboveGround < obstacle.height) {
                events += RunEvent.HitObstacle(obstacle)
            }
        }

        val checkpointSegment = sample?.segment as? SurfaceSegment.CheckpointInterior
        if (checkpointSegment != null) {
            events += RunEvent.CheckpointReached(checkpointSegment.checkpointIndex)
        }

        SurfaceSampler.nearbyPickups(chunks, newDistance, newLateral, pickupCollectionRadius)
            .filter { it.id !in collectedPickupIds }
            .forEach { events += RunEvent.CollectedPickup(it) }

        val newState = BallState(
            distance = newDistance,
            lateral = newLateral,
            height = newHeight,
            verticalVelocity = verticalVelocity,
            lateralVelocity = lateralVelocity,
            grounded = grounded,
        )
        return StepResult(newState, events)
    }

    private fun moveToward(current: Double, target: Double, maxDelta: Double): Double {
        val delta = target - current
        return if (abs(delta) <= maxDelta) target else current + sign(delta) * maxDelta
    }
}
