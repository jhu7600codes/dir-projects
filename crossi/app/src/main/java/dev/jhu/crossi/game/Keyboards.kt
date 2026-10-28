package dev.jhu.crossi.game

import dev.jhu.crossi.generator.Rng

object Keyboards {
    private val EN = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
    private val RU = listOf("ЙЦУКЕНГШЩЗХЪ", "ФЫВАПРОЛДЖЭ", "ЯЧСМИТЬБЮ")

    /** Letter rows for [language]. Scrambled keyboard boss shuffles keys but keeps row lengths. */
    fun rows(language: Language, scrambleSeed: Long = 0): List<String> {
        val rows = if (language == Language.RU) RU else EN
        if (scrambleSeed == 0L) return rows
        val keys = rows.joinToString("").toMutableList().also(Rng(scrambleSeed)::shuffle)
        var i = 0
        return rows.map { row -> String(CharArray(row.length) { keys[i++] }) }
    }
}
