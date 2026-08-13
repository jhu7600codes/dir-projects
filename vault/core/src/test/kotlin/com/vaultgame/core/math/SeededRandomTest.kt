package com.vaultgame.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeededRandomTest {
    @Test
    fun sameSeedProducesIdenticalSequence() {
        val a = SeededRandom(42L)
        val b = SeededRandom(42L)
        val seqA = (1..200).map { a.nextInt(1000) }
        val seqB = (1..200).map { b.nextInt(1000) }
        assertEquals(seqA, seqB)
    }

    @Test
    fun differentSeedsDiverge() {
        val a = SeededRandom(1L)
        val b = SeededRandom(2L)
        val seqA = (1..50).map { a.nextInt(1_000_000) }
        val seqB = (1..50).map { b.nextInt(1_000_000) }
        assertTrue(seqA != seqB)
    }

    @Test
    fun nextIntStaysInBounds() {
        val rng = SeededRandom(7L)
        repeat(5_000) {
            val v = rng.nextInt(37)
            assertTrue(v in 0 until 37)
        }
    }

    @Test
    fun nextIntRangeStaysInBounds() {
        val rng = SeededRandom(99L)
        repeat(5_000) {
            val v = rng.nextInt(10, 20)
            assertTrue(v in 10 until 20)
        }
    }

    @Test
    fun nextDoubleStaysInUnitRange() {
        val rng = SeededRandom(5L)
        repeat(5_000) {
            val v = rng.nextDouble()
            assertTrue(v >= 0.0 && v < 1.0)
        }
    }

    @Test
    fun shuffleIsAPermutation() {
        val rng = SeededRandom(3L)
        val original = (1..20).toList()
        val shuffled = rng.shuffle(original)
        assertEquals(original.toSet(), shuffled.toSet())
        assertEquals(original.size, shuffled.size)
    }
}
