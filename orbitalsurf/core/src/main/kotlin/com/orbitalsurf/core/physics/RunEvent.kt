package com.orbitalsurf.core.physics

import com.orbitalsurf.core.world.Obstacle
import com.orbitalsurf.core.world.PickupPlacement

/** Something that happened during one [BallSimulator.step] call, for `GameSession` to react to. */
sealed class RunEvent {
    data object Landed : RunEvent()
    data object LeftGround : RunEvent()

    /** The ball has gone well below any plausible rooftop with nothing underneath -- a run-ending fall unless shielded. */
    data object FellOff : RunEvent()

    /** Overlapped an obstacle's footprint at a height that isn't cleared by a jump -- a run-ending hit unless shielded. */
    data class HitObstacle(val obstacle: Obstacle) : RunEvent()

    data class CollectedPickup(val pickup: PickupPlacement) : RunEvent()

    /**
     * Emitted every tick the ball is inside checkpoint N's interior room (not just once) --
     * deliberately stateless here so `BallSimulator` doesn't need to remember anything
     * between calls. `GameSession` is the layer that dedupes this into a one-time unlock.
     */
    data class CheckpointReached(val checkpointIndex: Int) : RunEvent()
}
