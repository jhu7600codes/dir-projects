package dev.jhu.crossi.generator

/**
 * Test-only english word list (SCOWL sizes 10-35, see SCOWL-COPYRIGHT.txt), no clues.
 * The real dictionaries with clues land in m2.
 */
object Fixtures {
    val longWords = listOf(
        "onomatopoeia", "serendipity", "flibbertigibbet", "sesquipedalian", "kaleidoscope",
        "procrastinate", "extraordinary", "hippopotamus", "bamboozled", "gobbledygook",
        "shenanigans", "brouhaha", "lollygagging", "discombobulate", "cattywampus",
    ).map { DictWord(it, commonness = 3, hardness = 1, clues = emptyList()) }

    val englishWords: List<DictWord> by lazy {
        val stream = Fixtures::class.java.classLoader!!.getResourceAsStream("fixture_words_en.txt")
        stream.bufferedReader().readLines().map { line ->
            val (word, commonness) = line.split(' ')
            DictWord(word, commonness.toInt(), hardness = 1, clues = listOf("clue for $word"))
        }
    }

    val english: Dictionary by lazy { Dictionary(Alphabet.EN, englishWords, longWords) }

    val generator: PuzzleGenerator by lazy { PuzzleGenerator(english) }
}
