package com.orbitalsurf.core.physics

/** One tick's worth of player input. `lateralAxis` is continuous (no lanes), clamped to -1..1 by [BallSimulator]. */
data class SteerInput(val lateralAxis: Double, val jumpPressed: Boolean) {
    companion object {
        val NEUTRAL = SteerInput(lateralAxis = 0.0, jumpPressed = false)
    }
}
