package com.orbitalsurf.core.math

import org.junit.Assert.assertEquals
import org.junit.Test

class Vec3Test {
    private val a = Vec3(1.0, 2.0, 3.0)
    private val b = Vec3(4.0, -5.0, 6.0)

    @Test
    fun `plus and minus are componentwise`() {
        assertEquals(Vec3(5.0, -3.0, 9.0), a + b)
        assertEquals(Vec3(-3.0, 7.0, -3.0), a - b)
    }

    @Test
    fun `scalar multiply scales every component`() {
        assertEquals(Vec3(2.0, 4.0, 6.0), a * 2.0)
    }

    @Test
    fun `dot product matches definition`() {
        assertEquals(1.0 * 4.0 + 2.0 * -5.0 + 3.0 * 6.0, a.dot(b), 1e-9)
    }

    @Test
    fun `cross product is perpendicular to both inputs`() {
        val c = a.cross(b)
        assertEquals(0.0, c.dot(a), 1e-9)
        assertEquals(0.0, c.dot(b), 1e-9)
    }

    @Test
    fun `normalized vector has unit length`() {
        assertEquals(1.0, a.normalized().length(), 1e-9)
    }

    @Test
    fun `normalizing the zero vector returns zero instead of NaN`() {
        val result = Vec3.ZERO.normalized()
        assertEquals(Vec3.ZERO, result)
        assertEquals(0.0, result.length(), 1e-9)
    }

    @Test
    fun `withY replaces only the y component`() {
        assertEquals(Vec3(1.0, 99.0, 3.0), a.withY(99.0))
    }

    @Test
    fun `lerp at t=0 and t=1 returns the endpoints, t=0_5 the midpoint`() {
        assertEquals(a, Vec3.lerp(a, b, 0.0))
        assertEquals(b, Vec3.lerp(a, b, 1.0))
        assertEquals(Vec3(2.5, -1.5, 4.5), Vec3.lerp(a, b, 0.5))
    }
}
