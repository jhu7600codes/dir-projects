package app.venderian.convert

/**
 * english text -> venderian. two modes:
 * - [Mode.SOUND]: every word is spelled by sound (cmudict, then [LetterRules] as a fallback)
 * - [Mode.TRANSLATE]: words in the [Lexicon] become venderian words, the rest are spelled by sound
 *
 * the result's [Result.script] is the text to show in the venderian font (private use
 * codepoints for letters, ascii for spaces, digits and punctuation).
 */
class Converter(private val dict: Pronunciations, private val lexicon: Lexicon? = null) {

    enum class Mode { SOUND, TRANSLATE }

    enum class Source { LEXICON, CMUDICT, RULES, NUMBER, PUNCTUATION }

    data class Piece(val english: String, val roman: String, val script: String, val source: Source)

    data class Result(val pieces: List<Piece>) {
        val script: String = join { it.script }
        val roman: String = join { it.roman }

        private fun join(text: (Piece) -> String) = buildString {
            for ((i, p) in pieces.withIndex()) {
                if (i > 0 && p.source != Source.PUNCTUATION) append(' ')
                append(text(p))
            }
        }
    }

    fun convert(text: String, mode: Mode = Mode.SOUND): Result {
        val tokens = TOKEN.findAll(text).map { it.value.replace('’', '\'') }.toList()
        val pieces = mutableListOf<Piece>()
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            when {
                t[0].isDigit() -> pieces += Piece(t, t, t, Source.NUMBER)
                !t[0].isLetter() -> pieces += Piece(t, PUNCTUATION.getValue(t), PUNCTUATION.getValue(t), Source.PUNCTUATION)
                mode == Mode.TRANSLATE && lexicon != null -> {
                    val (used, piece) = translate(tokens, i, lexicon)
                    piece?.let { pieces += it }
                    i += used
                    continue
                }
                else -> pieces += spell(t)
            }
            i++
        }
        return Result(pieces)
    }

    /** spells one english word by sound */
    fun spell(word: String): Piece {
        val phones = dict.lookup(word.lowercase())
        val sounds = phones?.let(Arpabet::toSounds) ?: LetterRules.sounds(word)
        return Piece(word, Sounds.toRoman(sounds), Sounds.toScript(sounds), if (phones != null) Source.CMUDICT else Source.RULES)
    }

    /** returns how many tokens were used, and the piece (null when the word is dropped) */
    private fun translate(tokens: List<String>, at: Int, lexicon: Lexicon): Pair<Int, Piece?> {
        // multi-word meanings first ("thank you", "a little")
        for (n in lexicon.longestPhrase downTo 2) {
            val words = tokens.subList(at, minOf(tokens.size, at + n))
            if (words.size < n || words.any { !it[0].isLetter() }) continue
            val phrase = words.joinToString(" ")
            lexicon.entry(phrase)?.let { return n to venderian(phrase, it.word) }
        }
        val word = tokens[at]
        // "will eat" -> "yamava"
        if (word.lowercase() == "will") {
            val next = tokens.getOrNull(at + 1)
            if (next != null && next[0].isLetter() && lexicon.isVerb(next)) {
                val verb = lexicon.translate(next)
                if (verb != null) return 2 to venderian("$word $next", Lexicon.future(verb))
            }
        }
        val translated = lexicon.translate(word) ?: return 1 to spell(word)
        return 1 to translated.takeIf { it.isNotEmpty() }?.let { venderian(word, it) }
    }

    private fun venderian(english: String, roman: String) = Piece(
        english, roman,
        roman.split(' ').joinToString(" ") { Sounds.toScript(Sounds.parseRoman(it)) },
        Source.LEXICON,
    )

    companion object {
        private val TOKEN = Regex("[A-Za-z]+(?:['’][A-Za-z]+)*|\\d+|[.,?!;:]")
        private val PUNCTUATION = mapOf(
            "." to ".", "," to ",", "?" to "?", "!" to "!", ";" to ",", ":" to ",",
        )
    }
}
