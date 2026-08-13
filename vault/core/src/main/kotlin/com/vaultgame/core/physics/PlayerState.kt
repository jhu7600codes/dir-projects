package com.vaultgame.core.physics

import com.vaultgame.core.world.Lane

/**
 * Everything about the runner mutated tick-to-tick. [laneOffset] is a continuous -1..1 value
 * (not just the discrete [lane]) so the renderer can draw a smooth glide between lanes instead
 * of a snap; it always converges to `targetLane.index` by the time a lane switch completes.
 */
data class PlayerState(
    val lane: Lane = Lane.CENTER,
    val targetLane: Lane = Lane.CENTER,
    val laneOffset: Double = 0.0,
    val laneSwitchElapsed: Double = 0.0,
    val isJumping: Boolean = false,
    val jumpElapsed: Double = 0.0,
    val isSliding: Boolean = false,
    val slideElapsed: Double = 0.0,
    val distance: Double = 0.0,
    val speed: Double = 0.0,
    val alive: Boolean = true,
) {
    val isChangingLane: Boolean get() = lane != targetLane
    val isGrounded: Boolean get() = !isJumping
}
