package dev.jhu.crossi.generator

/** The letters a grid can hold. Words are stored as letter codes (index into [letters]). */
class Alphabet(val letters: String) {
    val size: Int get() = letters.length

    fun code(c: Char): Int = letters.indexOf(c)

    fun letter(code: Int): Char = letters[code]

    /** Uppercases, folds Ё into Е, returns null if the word has anything outside the alphabet. */
    fun normalize(word: String): String? {
        val up = word.trim().uppercase().replace('Ё', 'Е')
        if (up.isEmpty() || up.any { code(it) < 0 }) return null
        return up
    }

    companion object {
        val EN = Alphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
        val RU = Alphabet("АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ")
    }
}
