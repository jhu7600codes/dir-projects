package com.orbitalsurf.core.physics

/**
 * The ball's simulated state. `distance` doubles as both "position along the forward axis"
 * and "how far this run has travelled" -- the two are the same number, since the ball never
 * moves backward.
 */
data class BallState(
    val distance: Double,
    val lateral: Double,
    val height: Double,
    val verticalVelocity: Double = 0.0,
    val lateralVelocity: Double = 0.0,
    val grounded: Boolean = false,
) {
    companion object {
        fun startingAt(distance: Double, height: Double, lateral: Double = 0.0): BallState =
            BallState(distance = distance, lateral = lateral, height = height, grounded = true)
    }
}
