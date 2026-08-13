package com.vaultgame.core.math

/**
 * A tiny splitmix64-based PRNG. We deliberately don't use [kotlin.random.Random] directly --
 * its algorithm isn't part of its API contract, so the exact sequence it produces for a given
 * seed could change between Kotlin versions. [SeededRandom] pins the algorithm so that segment
 * generation is reproducibly deterministic for a given world seed forever, which
 * [com.vaultgame.core.world.SegmentGenerator]'s tests rely on.
 */
class SeededRandom(seed: Long) {
    private var state: Long = seed

    private fun nextLong(): Long {
        state += 0x9e3779b97f4a7c15UL.toLong()
        var z = state
        z = (z xor (z ushr 30)) * 0xbf58476d1ce4e5b9UL.toLong()
        z = (z xor (z ushr 27)) * 0x94d049bb133111ebUL.toLong()
        return z xor (z ushr 31)
    }

    /** Uniform int in [0, bound). */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive" }
        val bits = nextLong() ushr 1
        return (bits % bound).toInt()
    }

    /** Uniform int in [from, until). */
    fun nextInt(from: Int, until: Int): Int {
        require(until > from) { "until must be > from" }
        return from + nextInt(until - from)
    }

    /** Uniform double in [0, 1). */
    fun nextDouble(): Double {
        val bits = nextLong() ushr 11
        return bits.toDouble() / (1L shl 53).toDouble()
    }

    /** True with probability [chance] (0..1). */
    fun chance(chance: Double): Boolean = nextDouble() < chance

    fun <T> pick(items: List<T>): T = items[nextInt(items.size)]

    /** Fisher-Yates shuffle using this generator, so shuffling stays part of the deterministic
     * sequence (kotlin.random.Random.shuffled would fork off an undetermined global source). */
    fun <T> shuffle(items: List<T>): List<T> {
        val list = items.toMutableList()
        for (i in list.indices.reversed()) {
            val j = nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
        return list
    }
}
