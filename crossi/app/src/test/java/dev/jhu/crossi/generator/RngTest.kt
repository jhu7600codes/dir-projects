package dev.jhu.crossi.generator

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RngTest {
    @Test
    fun `matches the splitmix64 reference output`() {
        val rng = Rng(0)
        assertEquals(-2152535657050944081L, rng.nextLong())
        assertEquals(7960286522194355700L, rng.nextLong())
        assertEquals(487617019471545679L, rng.nextLong())
    }

    @Test
    fun `same seed same sequence`() {
        val a = Rng(42)
        val b = Rng(42)
        repeat(1000) { assertEquals(a.nextLong(), b.nextLong()) }
    }

    @Test
    fun `nextInt stays in bounds`() {
        val rng = Rng(7)
        repeat(10_000) {
            val v = rng.nextInt(3, 9)
            assert(v in 3 until 9)
        }
    }

    @Test
    fun `seedOf is stable and spreads`() {
        assertEquals(Rng.seedOf("crossi-daily-2026-10-28"), Rng.seedOf("crossi-daily-2026-10-28"))
        assertNotEquals(Rng.seedOf("crossi-daily-2026-10-28"), Rng.seedOf("crossi-daily-2026-10-29"))
    }
}
