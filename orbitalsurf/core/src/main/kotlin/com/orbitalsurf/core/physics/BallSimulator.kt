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
 * a speed- and grounded-state-aware accelerate-toward-target-velocity model (see
 * [PhysicsConfig.airControlFactor]/[PhysicsConfig.highSpeedTurnFloor]), and vertical movement
 * is gravity + discrete jump impulses with a snap-to-surface landing check each tick.
 *
 * Riding a sloped [com.orbitalsurf.core.world.SurfaceSegment.Ramp] sets vertical velocity to
 * that slope's instantaneous rate (`slope * forwardSpeed`) rather than always zeroing it out on
 * landing -- a fast ball climbing a steep ramp is already moving upward as it leaves the ramp's
 * end, so it carries that same vertical velocity into free flight and traces a real ballistic
 * arc afterward (gravity integration below is unchanged) instead of just dropping straight down.
 * That's what lets speed translate into reaching a higher platform off a ramp. On flat ground
 * (slope 0) this reduces to exactly the old zero-it-out behavior, so nothing changes there.
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

        var verticalVelocity = state.verticalVelocity - config.gravity * dtSeconds
        var grounded = state.grounded
        if (input.jumpPressed && grounded) {
            verticalVelocity = config.jumpImpulse
            grounded = false
            events += RunEvent.LeftGround
        }

        // Momentum resists a sharp turn: full authority only below highSpeedTurnStart,
        // tapering down to highSpeedTurnFloor by highSpeedTurnStart + highSpeedTurnRange, and
        // airborne authority is a further flat fraction of whatever's left -- a real rolling
        // ball can't redirect hard with no wheels on the ground.
        val speedTurnFactor = 1.0 - MathUtils.clamp(
            (forwardSpeed - config.highSpeedTurnStart) / config.highSpeedTurnRange,
            0.0,
            1.0,
        ) * (1.0 - config.highSpeedTurnFloor)
        val groundOrAirFactor = if (grounded) 1.0 else config.airControlFactor
        val effectiveMaxLateralSpeed = config.maxLateralSpeed * speedTurnFactor * groundOrAirFactor
        val targetLateralVelocity = MathUtils.clamp(input.lateralAxis, -1.0, 1.0) * effectiveMaxLateralSpeed
        val lateralVelocity = moveToward(state.lateralVelocity, targetLateralVelocity, config.lateralAccel * dtSeconds)

        val newDistance = state.distance + forwardSpeed * dtSeconds
        val newLateral = state.lateral + lateralVelocity * dtSeconds
        var newHeight = state.height + verticalVelocity * dtSeconds

        val sample = SurfaceSampler.sampleHeight(chunks, newDistance, newLateral)
        if (sample != null) {
            val feetHeight = newHeight - config.ballRadius
            val withinReach = feetHeight <= sample.height + config.groundSnapTolerance
            // Already riding the ground/a ramp this tick -> keep following the surface
            // regardless of vertical velocity's sign (climbing a ramp means positive vertical
            // velocity while still very much grounded). Airborne (jumping/falling) -> only
            // land while actually descending onto the surface, not rising through it right
            // after a jump impulse.
            val shouldLand = if (grounded) withinReach else withinReach && verticalVelocity <= 0.0
            if (shouldLand) {
                newHeight = sample.height + config.ballRadius
                verticalVelocity = sample.segment.slopeAt(newDistance) * forwardSpeed
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
