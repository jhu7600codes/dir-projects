package com.orbitalsurf.core.math

/**
 * Deterministic, stateless hashing used everywhere generation needs to be reproducible:
 * world generation (`ChunkGenerator.generate(seed, chunkIndex)`), daily-challenge
 * selection (seeded by the calendar date), and mission-pool draws. The defining property
 * this whole project leans on is: the SAME (seed, index) pair always produces the SAME
 * output, with no mutable state carried between calls -- so re-deriving chunk N (e.g. after
 * a Headstart skips ahead to it, or after it's evicted and later regenerated) always
 * reconstructs identical content.
 */
object SeededRandom {
    /** splitmix64 finalizer -- a fast, well-distributed 64-bit mix with no external dependency. */
    fun hash64(seed: Long, index: Long): Long {
        var z = seed + index * GOLDEN_GAMMA
        z = (z xor (z ushr 30)) * MULTIPLIER_1
        z = (z xor (z ushr 27)) * MULTIPLIER_2
        return z xor (z ushr 31)
    }

    // Standard splitmix64 constants, written as their two's-complement negative-hex form
    // (Kotlin's `const val` rejects hex Long literals above Long.MAX_VALUE even though the
    // bit pattern is valid) -- e.g. -0x61c8864680b583ebL is bit-for-bit 0x9E3779B97F4A7C15.
    private const val GOLDEN_GAMMA = -0x61c8864680b583ebL
    private const val MULTIPLIER_1 = -0x40a7b892e31b1a47L
    private const val MULTIPLIER_2 = -0x6b2fb644ecceee15L
}

/**
 * A convenience draw-many-values-from-one-(seed,streamIndex) helper. Internally it's just
 * repeated calls to [SeededRandom.hash64] with an incrementing draw counter, so it stays
 * fully deterministic and stateless from the outside: constructing `SeededSequence(seed, i)`
 * twice and drawing the same number of values from each always yields identical sequences.
 */
class SeededSequence(private val seed: Long, private val streamIndex: Long) {
    private var draw = 0L

    private fun nextRaw(): Long = SeededRandom.hash64(seed, streamIndex * STREAM_STRIDE + draw++)

    /** Uniform double in [0, 1). */
    fun nextDouble(): Double = (nextRaw() ushr 11).toDouble() * DOUBLE_UNIT

    /** Uniform double in [min, max). max <= min returns min. */
    fun nextInRange(min: Double, max: Double): Double =
        if (max <= min) min else min + nextDouble() * (max - min)

    /** Uniform int in [0, bound). bound <= 0 returns 0. */
    fun nextInt(bound: Int): Int =
        if (bound <= 0) 0 else (nextRaw() ushr 33).mod(bound.toLong()).toInt()

    /** True with the given probability (0.0..1.0). */
    fun nextBool(probability: Double): Boolean = nextDouble() < probability

    /** Picks one element deterministically; throws on an empty list, same as List.random(). */
    fun <T> pick(items: List<T>): T {
        require(items.isNotEmpty()) { "cannot pick from an empty list" }
        return items[nextInt(items.size)]
    }

    private companion object {
        // Large odd stride so different stream indices never collide in draw-counter space
        // for any realistic number of draws per chunk/stream.
        const val STREAM_STRIDE = 1_000_000L
        const val DOUBLE_UNIT = 1.0 / (1L shl 53)
    }
}
