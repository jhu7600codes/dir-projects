package com.vaultgame.core.progression

import com.vaultgame.core.math.SeededRandom

/** Drives mission-set progress and rollover. Pure functions over [MissionState] -- the session
 * layer owns the actual mutable state and persists it via GameSave. */
object MissionSystem {

    fun freshState(rng: SeededRandom): MissionState =
        MissionState(currentSet = MissionPool.rollSet(rng, setIndex = 0))

    /** Applies one run's results to the active set. If the set just became complete, its reward
     * multiplier is banked into [MissionState.pendingScoreMultiplier] and a new set is rolled
     * immediately so there's always an active goal to chase. */
    fun applyRunResult(state: MissionState, summary: RunSummary, rng: SeededRandom): MissionState {
        val updatedSet = state.currentSet.applyRunResult(summary)
        if (!state.currentSet.isComplete && updatedSet.isComplete) {
            val nextIndex = state.setsCompletedTotal + 1L
            return state.copy(
                currentSet = MissionPool.rollSet(rng, nextIndex),
                pendingScoreMultiplier = state.pendingScoreMultiplier * updatedSet.rewardMultiplier,
                setsCompletedTotal = state.setsCompletedTotal + 1,
            )
        }
        return state.copy(currentSet = updatedSet)
    }

    /** Consumes the banked multiplier for one run's score (called once per run by
     * RunResultApplier, after ScoreSystem has used it). Returns 1.0 (no boost) going forward. */
    fun consumePendingMultiplier(state: MissionState): Pair<Double, MissionState> {
        val multiplier = state.pendingScoreMultiplier
        return multiplier to state.copy(pendingScoreMultiplier = 1.0)
    }

    /** Shop mission-skip voucher: instantly completes the active set without playing it out,
     * banking its reward multiplier and rolling a new set, same as a natural completion. */
    fun skip(state: MissionState, rng: SeededRandom): MissionState {
        if (state.currentSet.isComplete) return state
        val completedSet = state.currentSet.copy(
            missions = state.currentSet.missions.map { it.copy(progress = it.targetValue) },
        )
        val nextIndex = state.setsCompletedTotal + 1L
        return state.copy(
            currentSet = MissionPool.rollSet(rng, nextIndex),
            pendingScoreMultiplier = state.pendingScoreMultiplier * completedSet.rewardMultiplier,
            setsCompletedTotal = state.setsCompletedTotal + 1,
        )
    }
}
