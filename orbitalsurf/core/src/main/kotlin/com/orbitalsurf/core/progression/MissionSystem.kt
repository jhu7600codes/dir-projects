package com.orbitalsurf.core.progression

/**
 * Holds the run's active 3 missions and the escalating multiplier those sets build.
 * Completing all 3 immediately rolls in a new (harder -- see [MissionPool]) set and bumps the
 * multiplier one step up [MULTIPLIER_STEPS], capping at the last step rather than growing
 * without bound.
 */
class MissionSystem(private val seed: Long) {
    var tier: Int = 0
        private set

    var multiplier: Double = MULTIPLIER_STEPS[0]
        private set

    private var setIndex: Long = 0L

    var activeMissions: List<Mission> = MissionPool.draw(seed, tier, setIndex)
        private set

    fun onDistanceTraveled(deltaMeters: Double) {
        if (deltaMeters <= 0.0) return
        activeMissions = activeMissions.map { mission ->
            if (mission.goal is MissionGoal.TravelDistance && !mission.isComplete) {
                mission.withProgress(mission.progress + deltaMeters)
            } else {
                mission
            }
        }
        checkCompletion()
    }

    fun onPickupCollected(kind: MissionPickupKind) {
        activeMissions = activeMissions.map { mission ->
            val goal = mission.goal
            if (goal is MissionGoal.CollectPickupCount && !mission.isComplete && matches(goal.kind, kind)) {
                mission.withProgress(mission.progress + 1.0)
            } else {
                mission
            }
        }
        checkCompletion()
    }

    /** Instantly finishes one currently-active, not-yet-complete mission -- what a Mission Skip Voucher spends. */
    fun forceComplete(missionId: String): Boolean {
        val index = activeMissions.indexOfFirst { it.id == missionId && !it.isComplete }
        if (index < 0) return false
        activeMissions = activeMissions.toMutableList().also { list ->
            list[index] = list[index].withProgress(list[index].target)
        }
        checkCompletion()
        return true
    }

    private fun matches(goalKind: MissionPickupKind, actualKind: MissionPickupKind): Boolean =
        goalKind == actualKind || (goalKind == MissionPickupKind.ANY_POWERUP && actualKind != MissionPickupKind.PLATES_COIN)

    private fun checkCompletion() {
        if (activeMissions.isNotEmpty() && activeMissions.all { it.isComplete }) {
            tier += 1
            multiplier = MULTIPLIER_STEPS.getOrElse(tier) { MULTIPLIER_STEPS.last() }
            setIndex += 1
            activeMissions = MissionPool.draw(seed, tier, setIndex)
        }
    }

    companion object {
        val MULTIPLIER_STEPS = listOf(1.0, 1.2, 1.5, 2.0, 2.5, 3.0)
    }
}
