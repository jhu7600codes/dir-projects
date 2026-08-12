package com.orbitalsurf.core.session

import com.orbitalsurf.core.physics.BallSimulator
import com.orbitalsurf.core.physics.BallState
import com.orbitalsurf.core.physics.PhysicsConfig
import com.orbitalsurf.core.physics.RunEvent
import com.orbitalsurf.core.physics.SteerInput
import com.orbitalsurf.core.progression.ActivePowerups
import com.orbitalsurf.core.progression.Mission
import com.orbitalsurf.core.progression.MissionPickupKind
import com.orbitalsurf.core.progression.MissionSystem
import com.orbitalsurf.core.progression.RemainingPowerup
import com.orbitalsurf.core.progression.ScoreSystem
import com.orbitalsurf.core.progression.SpeedCurve
import com.orbitalsurf.core.progression.toMissionPickupKind
import com.orbitalsurf.core.world.Chunk
import com.orbitalsurf.core.world.PickupKind
import com.orbitalsurf.core.world.PickupPlacement
import com.orbitalsurf.core.world.SurfaceSampler
import com.orbitalsurf.core.world.WorldStreamer

/** Everything the UI needs to render/react to after one [GameSession.update] tick. */
data class RunFrameResult(
    val ballState: BallState,
    val score: Long,
    val speed: Double,
    val activeMissions: List<Mission>,
    val missionMultiplier: Double,
    val activePowerups: List<RemainingPowerup>,
    val isShielded: Boolean,
    val distanceTraveled: Double,
    val isGameOver: Boolean,
)

/** The final tally handed to [RunResultApplier] once a run ends. */
data class RunSummary(
    val finalScore: Long,
    val distanceTraveled: Double,
    val platesEarned: Long,
    val missionsCompletedThisRun: Int,
    val reachedCheckpoints: Set<Int>,
    val usedAnyPowerup: Boolean,
    val pickupCounts: Map<MissionPickupKind, Int>,
)

/**
 * The per-run orchestrator: owns one run's `WorldStreamer`, ball physics, score, missions, and
 * active powerups, and wires them together every tick. Everything about *this run* lives here;
 * everything that persists *across* runs (the save file) is untouched until [buildRunSummary]
 * hands its result to [RunResultApplier] at the end.
 */
class GameSession(
    private val seed: Long,
    startDistance: Double = 0.0,
    private val physicsConfig: PhysicsConfig = PhysicsConfig.DEFAULT,
    /** Injectable mainly for tests (e.g. starting a scripted run already shielded); real runs always start with a fresh set. */
    private val activePowerups: ActivePowerups = ActivePowerups(),
) {
    private val worldStreamer = WorldStreamer(seed = seed)
    private val scoreSystem = ScoreSystem()
    private val missionSystem = MissionSystem(seed = seed)

    private val collectedPickupIdsInternal = mutableSetOf<String>()
    private val reachedCheckpointsThisRun = mutableSetOf<Int>()
    private val pickupCountsThisRun = mutableMapOf<MissionPickupKind, Int>()
    private var platesEarnedThisRun = 0L
    private var usedAnyPowerupThisRun = false
    private var gameOver = false

    private var ballState: BallState
    private var lastGroundedState: BallState

    init {
        worldStreamer.reset(startDistance)
        val initialSample = SurfaceSampler.sampleHeight(worldStreamer.activeChunks, startDistance, 0.0)
        val initialHeight = (initialSample?.height ?: FALLBACK_START_HEIGHT) + physicsConfig.ballRadius
        val initial = BallState.startingAt(distance = startDistance, height = initialHeight)
        ballState = initial
        lastGroundedState = initial
    }

    val isGameOver: Boolean get() = gameOver

    /** The current streamed window, for the renderer to draw -- mirrors `WorldStreamer.activeChunks`. */
    val activeChunks: List<Chunk> get() = worldStreamer.activeChunks

    /** Pickup ids already collected this run, so the renderer can skip drawing them. */
    val collectedPickupIds: Set<String> get() = collectedPickupIdsInternal

    fun update(dtSeconds: Double, input: SteerInput): RunFrameResult {
        if (gameOver) return currentFrameResult()

        worldStreamer.update(ballState.distance)
        val chunks = worldStreamer.activeChunks

        val combinedScoreMultiplier = missionSystem.multiplier * activePowerups.scoreMultiplier()
        val platesMultiplier = activePowerups.platesMultiplier()
        val pickupRadius = activePowerups.magnetRadius(physicsConfig.ballRadius + DEFAULT_PICKUP_RADIUS_MARGIN)
        val forwardSpeed = SpeedCurve.speedAt(scoreSystem.score)

        val step = BallSimulator.step(
            state = ballState,
            input = input,
            dtSeconds = dtSeconds,
            forwardSpeed = forwardSpeed,
            chunks = chunks,
            collectedPickupIds = collectedPickupIdsInternal,
            config = physicsConfig,
            pickupCollectionRadius = pickupRadius,
        )

        val deltaDistance = (step.state.distance - ballState.distance).coerceAtLeast(0.0)
        ballState = step.state
        activePowerups.tick(dtSeconds)

        scoreSystem.addDistance(deltaDistance, combinedScoreMultiplier)
        missionSystem.onDistanceTraveled(deltaDistance)

        var fellOrHit = false
        for (event in step.events) {
            when (event) {
                is RunEvent.CollectedPickup -> handlePickup(event.pickup, combinedScoreMultiplier, platesMultiplier)
                is RunEvent.CheckpointReached -> reachedCheckpointsThisRun += event.checkpointIndex
                RunEvent.FellOff, is RunEvent.HitObstacle -> fellOrHit = true
                else -> {}
            }
        }

        if (fellOrHit) {
            if (activePowerups.consumeShieldIfAvailable()) {
                val wasFall = step.events.any { it is RunEvent.FellOff }
                if (wasFall) {
                    // The shield rescues the ball back onto the last surface it was actually
                    // standing on -- just cancelling the fall in place would leave it stuck
                    // over the same gap, falling again next tick.
                    ballState = ballState.copy(
                        lateral = lastGroundedState.lateral,
                        height = lastGroundedState.height,
                        verticalVelocity = 0.0,
                        grounded = true,
                    )
                }
                // A shielded HitObstacle needs no repositioning -- the ball just passes through.
            } else {
                gameOver = true
            }
        }

        if (ballState.grounded) lastGroundedState = ballState

        return currentFrameResult()
    }

    /** Spends one Mission Skip Voucher on a specific active mission. Returns whether it found one to complete. */
    fun useMissionSkipVoucher(missionId: String): Boolean = missionSystem.forceComplete(missionId)

    fun buildRunSummary(): RunSummary = RunSummary(
        finalScore = scoreSystem.score,
        distanceTraveled = scoreSystem.totalDistanceTraveled,
        platesEarned = platesEarnedThisRun,
        missionsCompletedThisRun = missionSystem.tier,
        reachedCheckpoints = reachedCheckpointsThisRun.toSet(),
        usedAnyPowerup = usedAnyPowerupThisRun,
        pickupCounts = pickupCountsThisRun.toMap(),
    )

    private fun handlePickup(pickup: PickupPlacement, scoreMultiplier: Double, platesMultiplier: Double) {
        collectedPickupIdsInternal += pickup.id
        when (val kind = pickup.kind) {
            PickupKind.PlatesCoin -> {
                platesEarnedThisRun += (PLATES_PER_COIN * platesMultiplier).toLong()
                scoreSystem.addPlatesPickupPoints(scoreMultiplier)
                recordPickup(MissionPickupKind.PLATES_COIN)
            }
            is PickupKind.Powerup -> {
                activePowerups.collect(kind.type)
                scoreSystem.addPowerupPickupPoints(scoreMultiplier)
                usedAnyPowerupThisRun = true
                recordPickup(kind.type.toMissionPickupKind())
            }
        }
    }

    private fun recordPickup(missionKind: MissionPickupKind) {
        missionSystem.onPickupCollected(missionKind)
        pickupCountsThisRun[missionKind] = (pickupCountsThisRun[missionKind] ?: 0) + 1
    }

    private fun currentFrameResult(): RunFrameResult = RunFrameResult(
        ballState = ballState,
        score = scoreSystem.score,
        speed = SpeedCurve.speedAt(scoreSystem.score),
        activeMissions = missionSystem.activeMissions,
        missionMultiplier = missionSystem.multiplier,
        activePowerups = activePowerups.active,
        isShielded = activePowerups.isShielded,
        distanceTraveled = scoreSystem.totalDistanceTraveled,
        isGameOver = gameOver,
    )

    private companion object {
        const val FALLBACK_START_HEIGHT = 10.0
        const val DEFAULT_PICKUP_RADIUS_MARGIN = 0.7
        const val PLATES_PER_COIN = 10L
    }
}
