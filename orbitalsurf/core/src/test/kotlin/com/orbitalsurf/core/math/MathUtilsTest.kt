package com.orbitalsurf.core.math

import org.junit.Assert.assertEquals
import org.junit.Test

class MathUtilsTest {
    @Test
    fun `clamp bounds values on both sides and passes through the middle`() {
        assertEquals(0.0, MathUtils.clamp(-5.0, 0.0, 10.0), 0.0)
        assertEquals(10.0, MathUtils.clamp(15.0, 0.0, 10.0), 0.0)
        assertEquals(4.0, MathUtils.clamp(4.0, 0.0, 10.0), 0.0)
    }

    @Test
    fun `lerp at the endpoints and midpoint`() {
        assertEquals(0.0, MathUtils.lerp(0.0, 10.0, 0.0), 1e-9)
        assertEquals(10.0, MathUtils.lerp(0.0, 10.0, 1.0), 1e-9)
        assertEquals(5.0, MathUtils.lerp(0.0, 10.0, 0.5), 1e-9)
    }

    @Test
    fun `invLerp is the inverse of lerp and clamps outside the range`() {
        assertEquals(0.5, MathUtils.invLerp(0.0, 10.0, 5.0), 1e-9)
        assertEquals(0.0, MathUtils.invLerp(0.0, 10.0, -5.0), 1e-9)
        assertEquals(1.0, MathUtils.invLerp(0.0, 10.0, 50.0), 1e-9)
    }

    @Test
    fun `invLerp with degenerate range does not divide by zero`() {
        assertEquals(0.0, MathUtils.invLerp(5.0, 5.0, 5.0), 1e-9)
    }

    @Test
    fun `smoothstep is 0 below edge0, 1 above edge1, and monotonic between`() {
        assertEquals(0.0, MathUtils.smoothstep(0.0, 10.0, -5.0), 1e-9)
        assertEquals(1.0, MathUtils.smoothstep(0.0, 10.0, 15.0), 1e-9)
        val low = MathUtils.smoothstep(0.0, 10.0, 3.0)
        val high = MathUtils.smoothstep(0.0, 10.0, 7.0)
        assert(low < high) { "expected smoothstep to be increasing, got low=$low high=$high" }
    }
}
