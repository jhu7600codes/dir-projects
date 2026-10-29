package app.venderian.convert

import java.io.InputStream
import java.util.zip.GZIPInputStream

fun interface Pronunciations {
    /** arpabet phonemes for a lowercase english word, or null if unknown */
    fun lookup(word: String): List<String>?
}

/**
 * cmudict, as made by tools/make_cmudict.py: sorted `word PHONES...` lines. it stays one
 * byte array and is binary searched, so the 125k words don't become 125k map entries.
 */
class CmuDict(private val bytes: ByteArray) : Pronunciations {
    private val lineStarts: IntArray

    init {
        var count = 0
        for (b in bytes) if (b == NL) count++
        lineStarts = IntArray(count)
        var line = 0
        var start = 0
        for (i in bytes.indices) {
            if (bytes[i] == NL) {
                if (i > start) lineStarts[line++] = start
                start = i + 1
            }
        }
    }

    val size get() = lineStarts.size

    override fun lookup(word: String): List<String>? {
        val key = word.lowercase().toByteArray(Charsets.US_ASCII)
        var lo = 0
        var hi = lineStarts.size - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val cmp = compare(lineStarts[mid], key)
            when {
                cmp < 0 -> lo = mid + 1
                cmp > 0 -> hi = mid - 1
                else -> return phones(lineStarts[mid] + key.size + 1)
            }
        }
        return null
    }

    /** compares the word at the start of a line with key */
    private fun compare(start: Int, key: ByteArray): Int {
        var i = 0
        while (true) {
            val b = bytes[start + i]
            val wordEnded = b == SPACE
            val keyEnded = i == key.size
            if (wordEnded || keyEnded) return if (wordEnded && keyEnded) 0 else if (wordEnded) -1 else 1
            val diff = (b.toInt() and 0xff) - (key[i].toInt() and 0xff)
            if (diff != 0) return diff
            i++
        }
    }

    private fun phones(from: Int): List<String> {
        var end = from
        while (end < bytes.size && bytes[end] != NL) end++
        return String(bytes, from, end - from, Charsets.US_ASCII).split(' ')
    }

    companion object {
        private const val NL = '\n'.code.toByte()
        private const val SPACE = ' '.code.toByte()

        fun fromGzip(input: InputStream) = CmuDict(GZIPInputStream(input).use { it.readBytes() })
    }
}
