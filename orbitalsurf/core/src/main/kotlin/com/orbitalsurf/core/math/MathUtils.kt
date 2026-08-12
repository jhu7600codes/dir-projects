package com.orbitalsurf.core.math

object MathUtils {
    fun clamp(value: Double, min: Double, max: Double): Double =
        if (value < min) min else if (value > max) max else value

    fun clamp(value: Float, min: Float, max: Float): Float =
        if (value < min) min else if (value > max) max else value

    fun clamp(value: Int, min: Int, max: Int): Int =
        if (value < min) min else if (value > max) max else value

    fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /** Inverse of lerp: for value v between a and b, returns where in [0,1] it sits. b == a returns 0.0. */
    fun invLerp(a: Double, b: Double, v: Double): Double =
        if (kotlin.math.abs(b - a) < 1e-12) 0.0 else clamp((v - a) / (b - a), 0.0, 1.0)

    /** Classic smoothstep: 0 below edge0, 1 above edge1, smoothed (3t^2 - 2t^3) in between. */
    fun smoothstep(edge0: Double, edge1: Double, x: Double): Double {
        val t = invLerp(edge0, edge1, x)
        return t * t * (3.0 - 2.0 * t)
    }
}
