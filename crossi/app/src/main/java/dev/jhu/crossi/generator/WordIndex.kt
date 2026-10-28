package dev.jhu.crossi.generator

/**
 * Words grouped by length with a bitset per (position, letter), so finding every
 * word matching a pattern like `C?T??` is a handful of ANDs.
 */
internal class WordIndex(words: List<DictWord>, val alphabet: Alphabet) {

    class Bucket(val length: Int, entries: List<DictWord>, alphabet: Alphabet) {
        val words: Array<String> = Array(entries.size) { entries[it].word }
        val scores: IntArray = IntArray(entries.size) { entries[it].commonness }
        val codes: Array<ByteArray> = Array(entries.size) { i ->
            ByteArray(length) { p -> alphabet.code(words[i][p]).toByte() }
        }
        val longs: Int = (words.size + 63) ushr 6
        val all = LongArray(longs).also { bits -> words.indices.forEach { Bits.set(bits, it) } }

        private val alphabetSize = alphabet.size
        private val byLetter = Array(length * alphabetSize) { LongArray(longs) }
        private val position = HashMap<String, Int>(words.size * 2)

        init {
            for (i in words.indices) {
                position[words[i]] = i
                for (p in 0 until length) Bits.set(byLetter[p * alphabetSize + codes[i][p]], i)
            }
        }

        fun letterBits(pos: Int, code: Int): LongArray = byLetter[pos * alphabetSize + code]

        fun indexOf(word: String): Int = position[word] ?: -1
    }

    private val buckets = arrayOfNulls<Bucket>(Dictionary.MAX_WORD + 1)

    init {
        words.groupBy { it.word.length }.forEach { (len, list) -> buckets[len] = Bucket(len, list, alphabet) }
    }

    fun bucket(length: Int): Bucket? = buckets.getOrNull(length)
}
