package app.venderian.convert

/** cmudict (arpabet) phonemes to venderian sounds, the table in docs/venderian.md section 5. */
object Arpabet {
    private val table = mapOf(
        "AA" to listOf("a"), "AE" to listOf("a"), "AO" to listOf("o"),
        "AW" to listOf("a", "w"), "AY" to listOf("a", "y"),
        "EH" to listOf("e"), "ER" to listOf("ë", "r"), "EY" to listOf("e", "y"),
        "IH" to listOf("i"), "IY" to listOf("i"),
        "OW" to listOf("o", "w"), "OY" to listOf("o", "y"),
        "UH" to listOf("u"), "UW" to listOf("u"),
        "B" to listOf("b"), "CH" to listOf("ch"), "JH" to listOf("ch"), "D" to listOf("d"),
        "DH" to listOf("th"), "TH" to listOf("th"), "F" to listOf("f"), "G" to listOf("g"),
        "HH" to listOf("h"), "K" to listOf("k"), "L" to listOf("l"), "M" to listOf("m"),
        "N" to listOf("n"), "NG" to listOf("ng"), "P" to listOf("p"), "R" to listOf("r"),
        "S" to listOf("s"), "SH" to listOf("sh"), "ZH" to listOf("sh"), "T" to listOf("t"),
        "V" to listOf("v"), "W" to listOf("w"), "Y" to listOf("y"), "Z" to listOf("z"),
    )

    fun toSounds(phones: List<String>): List<String> = phones.flatMap { phone ->
        val base = phone.trimEnd { it.isDigit() }
        val stressed = phone.lastOrNull()?.let { it == '1' || it == '2' } ?: false
        if (base == "AH") listOf(if (stressed) "a" else "ë")
        else table[base] ?: throw IllegalArgumentException("unknown arpabet phoneme: $phone")
    }
}
