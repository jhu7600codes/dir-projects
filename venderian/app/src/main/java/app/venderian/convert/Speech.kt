package app.venderian.convert

/**
 * venderian for the phone's text to speech. there's no venderian voice, so the text is
 * respelled for one that exists: spanish voices get it almost exactly right (same five
 * vowels, a tapped r, and spanish j is venderian kh), english ones get a respelling.
 */
object Speech {
    private val WORD = Regex("[a-zë]+(?:'[a-zë]+)*")

    fun spanish(roman: String): String = WORD.replace(roman.lowercase()) { m -> spanishWord(Sounds.parseRoman(m.value)) }

    fun english(roman: String): String = WORD.replace(roman.lowercase()) { m -> englishWord(Sounds.parseRoman(m.value)) }

    private fun spanishWord(s: List<String>): String = buildString {
        for ((i, sound) in s.withIndex()) {
            val next = s.getOrNull(i + 1)
            val afterVowel = s.getOrNull(i - 1)?.let(Sounds::isVowel) == true
            val beforeVowel = next != null && Sounds.isVowel(next)
            append(
                when (sound) {
                    "ë" -> "e"
                    "k" -> if (next == "e" || next == "i" || next == "ë") "qu" else "c"
                    "g" -> if (next == "e" || next == "i" || next == "ë") "gu" else "g"
                    "kh", "h" -> "j"
                    "th" -> "z"
                    "z" -> "s"
                    // the second half of a diphthong (ay, aw) is a vowel in spanish spelling
                    "y" -> if (afterVowel && !beforeVowel) "i" else "y"
                    "w" -> "u"
                    else -> sound
                },
            )
        }
    }

    private fun englishWord(s: List<String>): String = buildString {
        for ((i, sound) in s.withIndex()) {
            val next = s.getOrNull(i + 1)
            append(
                when (sound) {
                    "a" -> if (next == "y" || next == "w") "a" else "ah"
                    "e" -> if (next == "y") "e" else "eh"
                    "i" -> "ee"
                    "o" -> if (next == "y" || next == "w") "o" else "oh"
                    "u" -> "oo"
                    "ë" -> "uh"
                    else -> sound
                },
            )
        }
    }
}
