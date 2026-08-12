package com.orbitalsurf.app.render

import android.opengl.Matrix
import com.orbitalsurf.core.physics.BallState

/**
 * A smoothed third-person chase camera. World axes throughout this project: x = lateral
 * (steering axis), y = up, z = forward (the ball's constant direction of travel -- see
 * `BallState`/`SurfaceSampler`'s kdocs in `:core`). Forward position tracks the ball with no
 * lag at all ("the ball can't stop", and neither does the camera); lateral/vertical position
 * lerp toward the ball for a bit of weight instead of rigidly snapping every frame.
 */
class Camera {
    private var eyeX = 0.0
    private var eyeY = FOLLOW_HEIGHT
    private var eyeZ = -FOLLOW_DISTANCE

    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    val viewProjectionMatrix = FloatArray(16)

    var aspectRatio: Float = 1f
        set(value) {
            field = value
            updateProjection()
        }

    init {
        updateProjection()
    }

    fun update(ball: BallState, dtSeconds: Double) {
        val targetX = ball.lateral
        val targetY = ball.height + FOLLOW_HEIGHT

        // Framerate-independent exponential smoothing: converges toward the target at the
        // same rate regardless of the tick's dt, unlike a fixed-fraction-per-frame lerp.
        val smoothing = 1.0 - Math.pow(LAG_PER_SECOND, dtSeconds)
        eyeX += (targetX - eyeX) * smoothing
        eyeY += (targetY - eyeY) * smoothing
        eyeZ = ball.distance - FOLLOW_DISTANCE

        val lookX = ball.lateral
        val lookY = ball.height + LOOK_HEIGHT_OFFSET
        val lookZ = ball.distance + LOOK_AHEAD

        Matrix.setLookAtM(
            viewMatrix, 0,
            eyeX.toFloat(), eyeY.toFloat(), eyeZ.toFloat(),
            lookX.toFloat(), lookY.toFloat(), lookZ.toFloat(),
            0f, 1f, 0f,
        )
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    }

    fun position(): FloatArray = floatArrayOf(eyeX.toFloat(), eyeY.toFloat(), eyeZ.toFloat())

    private fun updateProjection() {
        Matrix.perspectiveM(projectionMatrix, 0, FOV_DEGREES, aspectRatio, NEAR_PLANE, FAR_PLANE)
    }

    companion object {
        private const val FOLLOW_DISTANCE = 9.0
        private const val FOLLOW_HEIGHT = 4.5
        private const val LOOK_HEIGHT_OFFSET = 1.0
        private const val LOOK_AHEAD = 6.0

        // Very close to 0: per Camera.update's smoothing formula, this converges quickly
        // (~85%+ of the way to target within one frame at 60fps) without being a rigid snap.
        private const val LAG_PER_SECOND = 0.0001

        private const val FOV_DEGREES = 62f
        const val NEAR_PLANE = 0.5f
        const val FAR_PLANE = 220f
    }
}
