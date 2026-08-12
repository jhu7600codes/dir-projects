package com.orbitalsurf.app.render

import com.orbitalsurf.core.physics.BallState

/**
 * A short ribbon trailing behind the ball, rebuilt fresh every frame from a small ring buffer
 * of recent positions. Cheap since the buffer is tiny (a couple dozen points) and, per `Mesh`'s
 * client-array design, rebuilding a `Mesh` every frame has no GL-side allocation cost to worry
 * about (no VBOs to re-upload or leak).
 */
class TrailRenderer(private val maxPoints: Int = 24, private val ribbonHalfWidth: Float = 0.18f) {
    private val positions = ArrayDeque<FloatArray>()

    fun update(ball: BallState) {
        positions.addLast(floatArrayOf(ball.lateral.toFloat(), ball.height.toFloat(), ball.distance.toFloat()))
        while (positions.size > maxPoints) positions.removeFirst()
    }

    fun reset() {
        positions.clear()
    }

    /** This frame's ribbon mesh, fading toward transparent at the tail -- or null with fewer than 2 points buffered. */
    fun buildMesh(color: FloatArray): Mesh? {
        if (positions.size < 2) return null
        val builder = MeshBuilder()
        val points = positions.toList()
        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            val alpha = i.toFloat() / (points.size - 1)
            val a0 = floatArrayOf(a[0] - ribbonHalfWidth, a[1], a[2])
            val a1 = floatArrayOf(a[0] + ribbonHalfWidth, a[1], a[2])
            val b0 = floatArrayOf(b[0] - ribbonHalfWidth, b[1], b[2])
            val b1 = floatArrayOf(b[0] + ribbonHalfWidth, b[1], b[2])
            builder.addQuad(a0, a1, b1, b0, UP, colorWithAlpha(color, alpha))
        }
        return builder.build()
    }

    private fun colorWithAlpha(color: FloatArray, alpha: Float): FloatArray =
        floatArrayOf(color[0], color[1], color[2], color[3] * alpha)

    private companion object {
        val UP = floatArrayOf(0f, 1f, 0f)
    }
}
