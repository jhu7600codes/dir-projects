package com.vaultgame.core.physics

/** Timings for the three lane-runner moves. All in seconds. */
object PhysicsConfig {
    /** Time to glide from one lane to the adjacent one. */
    const val LANE_SWITCH_DURATION = 0.16

    /** Total airborne time of a jump, start to landing. */
    const val JUMP_DURATION = 0.5

    /** Total time spent in the slide crouch. */
    const val SLIDE_DURATION = 0.45

    /** Peak jump height in world units, for the renderer's arc -- physics only cares whether
     * the player is airborne, not how high. */
    const val JUMP_HEIGHT = 1.6
}
