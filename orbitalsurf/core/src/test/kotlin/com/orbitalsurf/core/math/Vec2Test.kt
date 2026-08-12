package com.orbitalsurf.core.math

import org.junit.Assert.assertEquals
import org.junit.Test

class Vec2Test {
    @Test
    fun `plus, minus and scale behave componentwise`() {
        val a = Vec2(1.0, 2.0)
        val b = Vec2(3.0, -4.0)
        assertEquals(Vec2(4.0, -2.0), a + b)
        assertEquals(Vec2(-2.0, 6.0), a - b)
        assertEquals(Vec2(2.0, 4.0), a * 2.0)
    }

    @Test
    fun `normalizing the zero vector returns zero`() {
        assertEquals(Vec2.ZERO, Vec2.ZERO.normalized())
    }

    @Test
    fun `normalized vector has unit length`() {
        assertEquals(1.0, Vec2(3.0, 4.0).normalized().length(), 1e-9)
    }

    @Test
    fun `lerp interpolates linearly`() {
        val a = Vec2(0.0, 0.0)
        val b = Vec2(10.0, 20.0)
        assertEquals(Vec2(5.0, 10.0), Vec2.lerp(a, b, 0.5))
    }
}
