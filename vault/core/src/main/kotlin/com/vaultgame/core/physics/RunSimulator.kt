package com.vaultgame.core.physics

import com.vaultgame.core.powerups.ActivePowerups
import com.vaultgame.core.powerups.PowerupType
import com.vaultgame.core.world.DifficultyCurve
import com.vaultgame.core.world.Obstacle
import com.vaultgame.core.world.ObstacleType
import com.vaultgame.core.world.Pickup
import com.vaultgame.core.world.PickupType
import com.vaultgame.core.world.WorldConstants
import kotlin.math.abs
import kotlin.math.min

data class SimulationResult(val state: PlayerState, val events: List<RunEvent>)

/**
 * Advances the runner one fixed tick. Pure with respect to its inputs (aside from mutating the
 * [ActivePowerups]/[Obstacle]/[Pickup] instances it's handed, which is the deliberate shared
 * mutable state a session owns across ticks) -- given the same state/segments/queued actions it
 * always produces the same result, which is what makes it unit-testable without any Android
 * dependency.
 */
object RunSimulator {

    fun step(
        state: PlayerState,
        queuedActions: List<PlayerAction>,
        activeObstacles: List<Obstacle>,
        activePickups: List<Pickup>,
        activePowerups: ActivePowerups,
        dtSeconds: Double,
    ): SimulationResult {
        if (!state.alive) return SimulationResult(state, emptyList())

        val events = mutableListOf<RunEvent>()
        var s = state

        s = applyActions(s, queuedActions)

        val expired = activePowerups.tick(dtSeconds)
        for (type in expired) events += RunEvent.PowerupExpired(type)

        s = advanceLane(s, dtSeconds)
        s = advanceJump(s, dtSeconds)
        s = advanceSlide(s, dtSeconds)

        val speed = DifficultyCurve.speedForDistance(s.distance) * activePowerups.speedMultiplier()
        val prevDistance = s.distance
        val newDistance = prevDistance + speed * dtSeconds
        s = s.copy(distance = newDistance, speed = speed)

        collectPickups(s, prevDistance, newDistance, activePickups, activePowerups, events)
        resolveObstacles(s, prevDistance, newDistance, activeObstacles, activePowerups, events)?.let {
            s = it
        }

        return SimulationResult(s, events)
    }

    private fun applyActions(state: PlayerState, actions: List<PlayerAction>): PlayerState {
        var s = state
        for (action in actions) {
            s = when (action) {
                PlayerAction.MOVE_LEFT ->
                    if (!s.isChangingLane) s.copy(targetLane = s.lane.shift(-1)) else s
                PlayerAction.MOVE_RIGHT ->
                    if (!s.isChangingLane) s.copy(targetLane = s.lane.shift(1)) else s
                PlayerAction.JUMP ->
                    if (s.isGrounded && !s.isSliding) {
                        s.copy(isJumping = true, jumpElapsed = 0.0)
                    } else s
                PlayerAction.SLIDE ->
                    if (s.isGrounded && !s.isSliding) {
                        s.copy(isSliding = true, slideElapsed = 0.0)
                    } else s
            }
        }
        return s
    }

    private fun advanceLane(state: PlayerState, dt: Double): PlayerState {
        if (!state.isChangingLane) return state
        val elapsed = state.laneSwitchElapsed + dt
        val t = min(1.0, elapsed / PhysicsConfig.LANE_SWITCH_DURATION)
        val fromOffset = state.lane.index.toDouble()
        val toOffset = state.targetLane.index.toDouble()
        val offset = fromOffset + (toOffset - fromOffset) * t
        return if (t >= 1.0) {
            state.copy(
                lane = state.targetLane,
                laneOffset = toOffset,
                laneSwitchElapsed = 0.0,
            )
        } else {
            state.copy(laneOffset = offset, laneSwitchElapsed = elapsed)
        }
    }

    private fun advanceJump(state: PlayerState, dt: Double): PlayerState {
        if (!state.isJumping) return state
        val elapsed = state.jumpElapsed + dt
        return if (elapsed >= PhysicsConfig.JUMP_DURATION) {
            state.copy(isJumping = false, jumpElapsed = 0.0)
        } else {
            state.copy(jumpElapsed = elapsed)
        }
    }

    private fun advanceSlide(state: PlayerState, dt: Double): PlayerState {
        if (!state.isSliding) return state
        val elapsed = state.slideElapsed + dt
        return if (elapsed >= PhysicsConfig.SLIDE_DURATION) {
            state.copy(isSliding = false, slideElapsed = 0.0)
        } else {
            state.copy(slideElapsed = elapsed)
        }
    }

    private fun collectPickups(
        state: PlayerState,
        prevDistance: Double,
        newDistance: Double,
        pickups: List<Pickup>,
        activePowerups: ActivePowerups,
        events: MutableList<RunEvent>,
    ) {
        val magnetRange = activePowerups.magnetRangeLanes()
        for (pickup in pickups) {
            if (pickup.collected) continue
            if (pickup.distance < prevDistance || pickup.distance > newDistance) continue

            val laneDistance = abs(pickup.lane.index - state.laneOffset)
            val inRange = laneDistance < 0.5 || (magnetRange > 0.0 && laneDistance <= magnetRange)
            if (!inRange) continue

            pickup.collected = true
            when (val type = pickup.type) {
                is PickupType.Coin -> {
                    val multiplier = activePowerups.coinMultiplier()
                    events += RunEvent.CoinCollected((WorldConstants.COIN_VALUE * multiplier).toInt().coerceAtLeast(1))
                }
                is PickupType.Powerup -> {
                    activePowerups.activate(type.type)
                    events += RunEvent.PowerupCollected(type.type)
                }
            }
        }
    }

    private fun resolveObstacles(
        state: PlayerState,
        prevDistance: Double,
        newDistance: Double,
        obstacles: List<Obstacle>,
        activePowerups: ActivePowerups,
        events: MutableList<RunEvent>,
    ): PlayerState? {
        var s: PlayerState? = null
        for (obstacle in obstacles) {
            if (obstacle.resolved) continue
            if (obstacle.distance < prevDistance || obstacle.distance > newDistance) continue

            val current = s ?: state
            obstacle.resolved = true
            if (!obstacle.blocksLane(current.lane)) continue
            if (activePowerups.isFlying()) continue
            if (isAvoided(current, obstacle.type)) continue

            if (activePowerups.consumeShield()) {
                events += RunEvent.ObstacleHit(obstacle.type, shieldAbsorbed = true)
            } else {
                events += RunEvent.ObstacleHit(obstacle.type, shieldAbsorbed = false)
                events += RunEvent.RunEnded(current.distance)
                s = current.copy(alive = false)
                return s
            }
        }
        return s
    }

    private fun isAvoided(state: PlayerState, type: ObstacleType): Boolean = when (type) {
        ObstacleType.LOW_VENT, ObstacleType.ROOF_GAP -> state.isJumping
        ObstacleType.OVERHEAD_PIPE, ObstacleType.CLOTHESLINE -> state.isSliding
        ObstacleType.CRATE_STACK -> false
    }
}
