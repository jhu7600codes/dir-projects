package dev.jhu.crossi.generator

import java.io.File

/** The real bundled dictionaries, read straight from the assets folder (tests run from the app module dir). */
object Fixtures {
    private fun load(code: String, alphabet: Alphabet) = Dictionary(
        alphabet = alphabet,
        words = DictionaryParser.parse(File("src/main/assets/$code/words.tsv").readText()),
        longWords = DictionaryParser.parse(File("src/main/assets/$code/longwords.tsv").readText()),
    )

    val english: Dictionary by lazy { load("en", Alphabet.EN) }
    val russian: Dictionary by lazy { load("ru", Alphabet.RU) }

    val englishGenerator: PuzzleGenerator by lazy { PuzzleGenerator(english) }
    val russianGenerator: PuzzleGenerator by lazy { PuzzleGenerator(russian) }
}
