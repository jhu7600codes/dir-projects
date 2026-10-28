package dev.jhu.crossi.generator

/**
 * SplitMix64. Our own implementation (not kotlin.random / java.util.Random) so a
 * seed gives the exact same puzzle on every device and app version forever.
 * The daily challenge depends on this.
 */
class Rng(seed: Long) {
    private var state = seed

    fun nextLong(): Long {
        state += GOLDEN_GAMMA
        var z = state
        z = (z xor (z ushr 30)) * MIX_1
        z = (z xor (z ushr 27)) * MIX_2
        return z xor (z ushr 31)
    }

    /** Uniform-ish int in [0, bound). The modulo bias is irrelevant at our bound sizes. */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive" }
        return ((nextLong() ushr 1) % bound).toInt()
    }

    fun nextInt(from: Int, until: Int): Int = from + nextInt(until - from)

    fun nextDouble(): Double = (nextLong() ushr 11) * DOUBLE_UNIT

    /** A new independent stream, so sub-steps can't shift each other's randomness. */
    fun fork(): Rng = Rng(nextLong())

    fun <T> shuffle(list: MutableList<T>) {
        for (i in list.lastIndex downTo 1) {
            val j = nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
    }

    fun <T> pick(list: List<T>): T = list[nextInt(list.size)]

    companion object {
        private const val GOLDEN_GAMMA = -7046029254386353131L // 0x9E3779B97F4A7C15
        private const val MIX_1 = -4658895280553007687L // 0xBF58476D1CE4E5B9
        private const val MIX_2 = -7723592293110705685L // 0x94D049BB133111EB
        private const val DOUBLE_UNIT = 1.0 / (1L shl 53)

        /** Stable 64-bit hash of a string (FNV-1a over UTF-8), for date-based seeds. */
        fun seedOf(text: String): Long {
            var h = -3750763034362895579L // FNV offset basis 0xCBF29CE484222325
            for (b in text.encodeToByteArray()) {
                h = (h xor (b.toLong() and 0xFF)) * 1099511628211L
            }
            return Rng(h).nextLong()
        }
    }
}
