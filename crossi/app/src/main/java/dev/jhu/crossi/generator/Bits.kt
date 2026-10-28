package dev.jhu.crossi.generator

/** Tiny helpers for fixed-size bitsets stored in LongArrays (all arrays of a bucket share a size). */
internal object Bits {
    fun count(bits: LongArray): Int {
        var n = 0
        for (w in bits) n += java.lang.Long.bitCount(w)
        return n
    }

    inline fun forEach(bits: LongArray, action: (Int) -> Unit) {
        for (i in bits.indices) {
            var w = bits[i]
            while (w != 0L) {
                val bit = java.lang.Long.numberOfTrailingZeros(w)
                action((i shl 6) + bit)
                w = w and (w - 1)
            }
        }
    }

    fun set(bits: LongArray, index: Int) {
        bits[index ushr 6] = bits[index ushr 6] or (1L shl (index and 63))
    }

    fun clear(bits: LongArray, index: Int) {
        bits[index ushr 6] = bits[index ushr 6] and (1L shl (index and 63)).inv()
    }
}
