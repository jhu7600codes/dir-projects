package com.orbitalsurf.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeededRandomTest {
    @Test
    fun `same seed and index always hash to the same value`() {
        assertEquals(SeededRandom.hash64(42L, 7L), SeededRandom.hash64(42L, 7L))
    }

    @Test
    fun `different index changes the hash`() {
        assertNotEquals(SeededRandom.hash64(42L, 7L), SeededRandom.hash64(42L, 8L))
    }

    @Test
    fun `different seed changes the hash for the same index`() {
        assertNotEquals(SeededRandom.hash64(1L, 7L), SeededRandom.hash64(2L, 7L))
    }

    @Test
    fun `two sequences built from the same seed and stream index draw identical values`() {
        val a = SeededSequence(seed = 123L, streamIndex = 5L)
        val b = SeededSequence(seed = 123L, streamIndex = 5L)
        repeat(50) {
            assertEquals(a.nextDouble(), b.nextDouble(), 0.0)
        }
    }

    @Test
    fun `different stream indices from the same seed produce different sequences`() {
        val a = SeededSequence(seed = 123L, streamIndex = 5L)
        val b = SeededSequence(seed = 123L, streamIndex = 6L)
        val drawsA = List(10) { a.nextDouble() }
        val drawsB = List(10) { b.nextDouble() }
        assertNotEquals(drawsA, drawsB)
    }

    @Test
    fun `nextDouble stays within 0 inclusive to 1 exclusive over many draws`() {
        val seq = SeededSequence(seed = 999L, streamIndex = 1L)
        repeat(10_000) {
            val v = seq.nextDouble()
            assertTrue("value $v out of range", v >= 0.0 && v < 1.0)
        }
    }

    @Test
    fun `nextInRange stays within the requested bounds`() {
        val seq = SeededSequence(seed = 1L, streamIndex = 2L)
        repeat(1_000) {
            val v = seq.nextInRange(5.0, 10.0)
            assertTrue("value $v out of range", v >= 5.0 && v < 10.0)
        }
    }

    @Test
    fun `nextInRange with max less than or equal to min returns min`() {
        val seq = SeededSequence(seed = 1L, streamIndex = 2L)
        assertEquals(5.0, seq.nextInRange(5.0, 5.0), 0.0)
        assertEquals(5.0, seq.nextInRange(5.0, 1.0), 0.0)
    }

    @Test
    fun `nextInt stays within 0 inclusive to bound exclusive`() {
        val seq = SeededSequence(seed = 42L, streamIndex = 3L)
        repeat(1_000) {
            val v = seq.nextInt(7)
            assertTrue("value $v out of range", v in 0 until 7)
        }
    }

    @Test
    fun `nextInt with bound 0 returns 0`() {
        val seq = SeededSequence(seed = 42L, streamIndex = 3L)
        assertEquals(0, seq.nextInt(0))
    }

    @Test
    fun `pick always returns one of the supplied items`() {
        val seq = SeededSequence(seed = 7L, streamIndex = 1L)
        val items = listOf("a", "b", "c")
        repeat(100) {
            assertTrue(items.contains(seq.pick(items)))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `pick on an empty list throws`() {
        SeededSequence(seed = 1L, streamIndex = 1L).pick(emptyList<String>())
    }

    @Test
    fun `nextBool respects extreme probabilities deterministically`() {
        val alwaysTrue = SeededSequence(seed = 1L, streamIndex = 1L)
        val alwaysFalse = SeededSequence(seed = 1L, streamIndex = 1L)
        repeat(200) {
            assertTrue(alwaysTrue.nextBool(1.0))
            assertTrue(!alwaysFalse.nextBool(0.0))
        }
    }
}
