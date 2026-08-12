package com.orbitalsurf.core.economy

import com.orbitalsurf.core.world.CheckpointSchedule
import kotlinx.serialization.Serializable

/** Which checkpoints have been reached at least once, account-wide (first-reach-only, permanent). */
@Serializable
data class CheckpointUnlocks(val unlocked: Set<Int> = emptySet()) {
    fun markReached(checkpointIndex: Int): CheckpointUnlocks =
        if (checkpointIndex in unlocked) this else copy(unlocked = unlocked + checkpointIndex)

    fun isUnlocked(checkpointIndex: Int): Boolean = checkpointIndex in unlocked

    /** Where a run should start if the player spends a Headstart ticket for this checkpoint -- null if it isn't unlocked. */
    fun headstartStartDistance(checkpointIndex: Int): Double? =
        if (isUnlocked(checkpointIndex)) CheckpointSchedule.checkpointDistance(checkpointIndex) else null
}
