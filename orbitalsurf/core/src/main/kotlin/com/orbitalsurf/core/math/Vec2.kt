package com.orbitalsurf.core.math

import kotlin.math.sqrt

/** Immutable 2D vector. Used for lateral/ground-plane positions (x = lateral offset, y = distance along the path). */
data class Vec2(val x: Double, val y: Double) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Double) = Vec2(x * scalar, y * scalar)
    operator fun unaryMinus() = Vec2(-x, -y)

    fun dot(other: Vec2): Double = x * other.x + y * other.y
    fun lengthSquared(): Double = x * x + y * y
    fun length(): Double = sqrt(lengthSquared())

    /** Zero vector normalizes to zero rather than throwing/NaN -- callers never need a defensive check. */
    fun normalized(): Vec2 {
        val len = length()
        return if (len < EPSILON) ZERO else Vec2(x / len, y / len)
    }

    companion object {
        const val EPSILON = 1e-9
        val ZERO = Vec2(0.0, 0.0)

        fun lerp(a: Vec2, b: Vec2, t: Double): Vec2 = Vec2(
            MathUtils.lerp(a.x, b.x, t),
            MathUtils.lerp(a.y, b.y, t),
        )
    }
}
