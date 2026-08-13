package com.vaultgame.core.world

/**
 * Every obstacle shape the generator can place. [spansAllLanes] obstacles occupy all three
 * lanes at their distance -- the player *must* perform [avoidedBy] to survive them, switching
 * lanes alone never helps. Lane-local obstacles (spans one lane) can also be dodged by moving
 * out of that lane, on top of the listed avoidance action.
 */
enum class ObstacleType(val spansAllLanes: Boolean, val avoidedBy: AvoidAction) {
    /** A knee-high AC vent or duct -- hop it. */
    LOW_VENT(spansAllLanes = false, avoidedBy = AvoidAction.JUMP),

    /** A steam/utility pipe overhead -- duck under it. */
    OVERHEAD_PIPE(spansAllLanes = false, avoidedBy = AvoidAction.SLIDE),

    /** A stack of crates or an AC unit, too tall to jump -- only a lane switch clears it. */
    CRATE_STACK(spansAllLanes = false, avoidedBy = AvoidAction.SWITCH_LANE_ONLY),

    /** A gap between rooftops spanning the whole path -- jump it or fall. */
    ROOF_GAP(spansAllLanes = true, avoidedBy = AvoidAction.JUMP),

    /** A taut clothesline / cable strung across all three lanes at head height -- slide under. */
    CLOTHESLINE(spansAllLanes = true, avoidedBy = AvoidAction.SLIDE),
}

enum class AvoidAction {
    JUMP,
    SLIDE,
    SWITCH_LANE_ONLY,
}
