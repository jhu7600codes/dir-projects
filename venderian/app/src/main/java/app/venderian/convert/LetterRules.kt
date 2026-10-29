package app.venderian.convert

/**
 * the fallback for english words cmudict doesn't know: a small set of spelling rules,
 * longest match first. it's a guess, but a consistent one.
 */
object LetterRules {
    // checked in order at each position, so longer spellings come first
    private val rules: List<Pair<String, List<String>>> = listOf(
        "tch" to listOf("ch"), "igh" to listOf("a", "y"), "sch" to listOf("s", "k"),
        "ch" to listOf("ch"), "sh" to listOf("sh"), "th" to listOf("th"), "ph" to listOf("f"),
        "wh" to listOf("w"), "ck" to listOf("k"), "ng" to listOf("ng"), "qu" to listOf("k", "w"),
        "gh" to emptyList(),
        "ee" to listOf("i"), "ea" to listOf("i"), "ie" to listOf("i"), "oo" to listOf("u"),
        "ou" to listOf("a", "w"), "ow" to listOf("o", "w"), "ai" to listOf("e", "y"),
        "ay" to listOf("e", "y"), "ey" to listOf("e", "y"), "oa" to listOf("o", "w"),
        "oi" to listOf("o", "y"), "oy" to listOf("o", "y"), "au" to listOf("o"), "aw" to listOf("o"),
        "er" to listOf("ë", "r"), "ur" to listOf("ë", "r"), "ir" to listOf("ë", "r"),
        "a" to listOf("a"), "b" to listOf("b"), "d" to listOf("d"), "e" to listOf("e"),
        "f" to listOf("f"), "g" to listOf("g"), "h" to listOf("h"), "i" to listOf("i"),
        "j" to listOf("ch"), "k" to listOf("k"), "l" to listOf("l"), "m" to listOf("m"),
        "n" to listOf("n"), "o" to listOf("o"), "p" to listOf("p"), "q" to listOf("k"),
        "r" to listOf("r"), "s" to listOf("s"), "t" to listOf("t"), "u" to listOf("u"),
        "v" to listOf("v"), "w" to listOf("w"), "x" to listOf("k", "s"), "z" to listOf("z"),
    )
    private const val VOWEL_LETTERS = "aeiouy"

    fun sounds(english: String): List<String> {
        var word = english.lowercase().filter { it in 'a'..'z' }
        if (word.isEmpty()) return emptyList()
        // silent letters at the edges
        if (word.length > 2 && word.startsWith("kn")) word = word.drop(1)
        if (word.length > 2 && word.startsWith("wr")) word = word.drop(1)
        val silentE = word.length > 3 && word.endsWith("e") && word[word.length - 2] !in VOWEL_LETTERS &&
            word.dropLast(1).any { it in VOWEL_LETTERS }
        if (silentE) word = word.dropLast(1)

        val out = mutableListOf<String>()
        var i = 0
        while (i < word.length) {
            val c = word[i]
            val next = word.getOrNull(i + 1)
            when {
                // c and g soften before e, i, y
                c == 'c' && next != 'k' && next != 'h' -> {
                    out += if (next != null && next in "eiy") "s" else "k"
                    i += if (next == 'c') 2 else 1
                    continue
                }
                c == 'y' -> {
                    out += if (i == 0 || (next != null && next in VOWEL_LETTERS)) "y" else "i"
                    i++
                    continue
                }
                c == 'g' && next == 'h' && i == 0 -> {
                    out += "g"
                    i += 2
                    continue
                }
                // a doubled consonant letter is one sound in english
                next == c && c !in VOWEL_LETTERS -> {
                    i++
                    continue
                }
            }
            val (spelling, sounds) = rules.first { word.startsWith(it.first, i) }
            out += sounds
            i += spelling.length
        }
        return out
    }
}
