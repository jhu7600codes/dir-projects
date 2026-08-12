package com.orbitalsurf.core.math

import kotlin.math.sqrt

/**
 * Immutable 3D vector. World-space convention used throughout this project:
 * x = lateral (steering axis), y = up, z = forward (the ball's constant direction of travel).
 */
data class Vec3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Double) = Vec3(x * scalar, y * scalar, z * scalar)
    operator fun unaryMinus() = Vec3(-x, -y, -z)

    fun dot(other: Vec3): Double = x * other.x + y * other.y + z * other.z

    fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )

    fun lengthSquared(): Double = x * x + y * y + z * z
    fun length(): Double = sqrt(lengthSquared())

    /** Zero vector normalizes to zero rather than throwing/NaN -- callers never need a defensive check. */
    fun normalized(): Vec3 {
        val len = length()
        return if (len < EPSILON) ZERO else Vec3(x / len, y / len, z / len)
    }

    fun withY(newY: Double): Vec3 = Vec3(x, newY, z)

    companion object {
        const val EPSILON = 1e-9
        val ZERO = Vec3(0.0, 0.0, 0.0)
        val UP = Vec3(0.0, 1.0, 0.0)

        fun lerp(a: Vec3, b: Vec3, t: Double): Vec3 = Vec3(
            MathUtils.lerp(a.x, b.x, t),
            MathUtils.lerp(a.y, b.y, t),
            MathUtils.lerp(a.z, b.z, t),
        )
    }
}
