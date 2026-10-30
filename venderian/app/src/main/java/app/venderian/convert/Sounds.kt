package app.venderian.convert

/**
 * the 28 venderian sounds, their romanization and their private use codepoints.
 * mirrors docs/alphabet.json (SoundsTest checks they agree).
 */
object Sounds {
    val consonants = listOf(
        "p", "b", "t", "d", "k", "g", "f", "v", "th", "s", "z",
        "sh", "kh", "ch", "h", "m", "n", "ng", "r", "l", "y", "w",
    )
    val vowels = listOf("a", "e", "i", "o", "u", "ë")
    val all = consonants + vowels

    private val glyphs: Map<String, Char> =
        consonants.withIndex().associate { (i, s) -> s to (0xE000 + i).toChar() } +
            vowels.withIndex().associate { (i, s) -> s to (0xE020 + i).toChar() }
    private val sounds: Map<Char, String> = glyphs.entries.associate { (s, c) -> c to s }

    // longest first, so "sh" wins over "s"
    private val parseOrder = all.sortedByDescending { it.length }
    private val digraphs = all.filter { it.length == 2 }

    fun isVowel(sound: String) = sound in vowels

    fun glyph(sound: String): Char = glyphs[sound] ?: error("not a venderian sound: $sound")

    fun soundOf(c: Char): String? = sounds[c]

    fun isLetter(c: Char) = c in sounds

    /** "shevan" -> [sh, e, v, a, n]. an apostrophe splits a would-be digraph: "n'g" -> [n, g]. */
    fun parseRoman(word: String): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (i < word.length) {
            if (word[i] == '\'') {
                i++
                continue
            }
            val sound = parseOrder.firstOrNull { word.startsWith(it, i) }
                ?: throw IllegalArgumentException("'${word[i]}' is not a venderian letter (in \"$word\")")
            out += sound
            i += sound.length
        }
        return out
    }

    /** [n, g] -> "n'g", everything else just joined. the inverse of [parseRoman]. */
    fun toRoman(sounds: List<String>): String = buildString {
        for (s in sounds) {
            if (isNotEmpty() && digraphs.any { it[0] == last() && it[1] == s[0] }) append('\'')
            append(s)
        }
    }

    fun toScript(sounds: List<String>): String = String(CharArray(sounds.size) { glyph(sounds[it]) })

    /** romanized text (several words, dictionary hyphens allowed) to venderian script */
    fun scriptOf(roman: String): String =
        roman.split(' ').joinToString(" ") { toScript(parseRoman(it.replace("-", ""))) }

    /** a romanized sentence to script, keeping spaces and punctuation: "vela, ana!" */
    fun scriptOfText(text: String): String =
        Regex("[a-zë]+(?:'[a-zë]+)*").replace(text.lowercase()) { toScript(parseRoman(it.value)) }

    /** venderian text back to romanization. non-letters (spaces, digits, punctuation) pass through. */
    fun scriptToRoman(script: String): String = buildString {
        var run = mutableListOf<String>()
        fun flush() {
            if (run.isNotEmpty()) append(toRoman(run))
            run = mutableListOf()
        }
        for (c in script) {
            val s = soundOf(c)
            if (s != null) run += s else {
                flush()
                append(c)
            }
        }
        flush()
    }
}
