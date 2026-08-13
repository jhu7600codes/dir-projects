package com.vaultgame.core.session

import com.vaultgame.core.physics.PlayerAction
import com.vaultgame.core.physics.PlayerState
import com.vaultgame.core.physics.RunEvent
import com.vaultgame.core.physics.RunSimulator
import com.vaultgame.core.powerups.ActivePowerups
import com.vaultgame.core.powerups.PowerupType
import com.vaultgame.core.progression.RunSummary
import com.vaultgame.core.progression.ScoreSystem
import com.vaultgame.core.world.RoofSegment
import com.vaultgame.core.world.SegmentGenerator
import com.vaultgame.core.world.WorldConstants
import kotlin.math.floor

/**
 * One playthrough: owns the segment buffer, player state, and active powerups, and steps them
 * all together each tick. The app's game loop calls [queueAction] from swipe input and [tick]
 * once per frame (or fixed physics step); it reads [playerState] and [visibleSegments] to render.
 */
class GameSession(
    runSeed: Long,
    startDistance: Double = 0.0,
    powerupUpgradeLevels: Map<PowerupType, Int> = emptyMap(),
) {
    private val generator = SegmentGenerator(runSeed)
    private val segments = mutableListOf<RoofSegment>()
    private val activePowerups = ActivePowerups(powerupUpgradeLevels)
    private val pendingActions = mutableListOf<PlayerAction>()

    var playerState: PlayerState = PlayerState(distance = startDistance)
        private set
    var runEnded: Boolean = false
        private set
    var coinsCollected: Int = 0
        private set
    private var hitsTaken: Int = 0

    val visibleSegments: List<RoofSegment> get() = segments

    init {
        bufferSegmentsAhead()
    }

    fun queueAction(action: PlayerAction) {
        pendingActions += action
    }

    fun tick(dtSeconds: Double): List<RunEvent> {
        if (runEnded) return emptyList()
        bufferSegmentsAhead()

        val obstacles = segments.flatMap(RoofSegment::obstacles)
        val pickups = segments.flatMap(RoofSegment::pickups)
        val result = RunSimulator.step(
            state = playerState,
            queuedActions = pendingActions.toList(),
            activeObstacles = obstacles,
            activePickups = pickups,
            activePowerups = activePowerups,
            dtSeconds = dtSeconds,
        )
        pendingActions.clear()
        playerState = result.state

        for (event in result.events) {
            when (event) {
                is RunEvent.CoinCollected -> coinsCollected += event.baseValue
                is RunEvent.ObstacleHit -> hitsTaken += 1
                is RunEvent.RunEnded -> runEnded = true
                else -> Unit
            }
        }

        pruneSegmentsBehind()
        return result.events
    }

    fun activePowerupsSnapshot(): ActivePowerups = activePowerups

    /** [scoreMultiplier] comes from any mission-set reward banked before this run started --
     * see MissionSystem.consumePendingMultiplier, applied by RunResultApplier. */
    fun buildSummary(scoreMultiplier: Double): RunSummary {
        val score = ScoreSystem.computeScore(playerState.distance, coinsCollected, scoreMultiplier)
        return RunSummary(
            distanceMeters = playerState.distance,
            coinsCollected = coinsCollected,
            powerupActivations = activePowerups.usageCounts.toMap(),
            wasCleanRun = hitsTaken == 0,
            score = score,
        )
    }

    private fun bufferSegmentsAhead() {
        val targetEnd = playerState.distance + WorldConstants.SEGMENT_LENGTH * WorldConstants.SEGMENT_LOOKAHEAD
        while (segments.isEmpty() || segments.last().endDistance < targetEnd) {
            val nextStart = segments.lastOrNull()?.endDistance
                ?: (floor(playerState.distance / WorldConstants.SEGMENT_LENGTH) * WorldConstants.SEGMENT_LENGTH)
            segments += generator.nextSegment(nextStart)
        }
    }

    private fun pruneSegmentsBehind() {
        segments.removeAll { it.endDistance < playerState.distance - WorldConstants.SEGMENT_LENGTH }
    }
}
