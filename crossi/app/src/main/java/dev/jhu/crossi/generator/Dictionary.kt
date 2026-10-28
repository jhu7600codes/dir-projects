package dev.jhu.crossi.generator

/**
 * One dictionary word. [commonness] 1 (rare-ish) .. 5 (super common) drives fill
 * preference, [hardness] 1..3 is how hard its clues are.
 */
data class DictWord(
    val word: String,
    val commonness: Int,
    val hardness: Int,
    val clues: List<String>,
)

/**
 * Everything the generator needs for one language. [words] fill the grid,
 * [longWords] are the hand-picked fun ones that only show up as seeds in big grids.
 * [version] goes into daily seeds so a dictionary update can't split players.
 */
class Dictionary(
    val alphabet: Alphabet,
    words: List<DictWord>,
    longWords: List<DictWord> = emptyList(),
    val version: Int = 1,
    val tricks: TrickSet? = TrickSet.forAlphabet(alphabet),
) {
    /** Fill words, including the letter-trick answers (AIGH, ELL, ...) that aren't real words. */
    val words: List<DictWord> = normalized(
        words + tricks?.answers.orEmpty().map { DictWord(it, commonness = 3, hardness = 1, clues = emptyList()) },
    )
    val longWords: List<DictWord> = normalized(longWords)

    private val byWord: Map<String, DictWord> = (this.words + this.longWords).associateBy { it.word }

    operator fun get(word: String): DictWord? = byWord[word]

    fun contains(word: String): Boolean = word in byWord

    private fun normalized(list: List<DictWord>): List<DictWord> {
        val seen = HashSet<String>()
        return list.mapNotNull { w ->
            val norm = alphabet.normalize(w.word) ?: return@mapNotNull null
            if (norm.length !in MIN_WORD..MAX_WORD || !seen.add(norm)) null else w.copy(word = norm)
        }
    }

    companion object {
        const val MIN_WORD = 3
        const val MAX_WORD = 15
    }
}

/** Reads the words.tsv / longwords.tsv format: WORD \t commonness \t hardness \t clue|clue|clue */
object DictionaryParser {
    fun parse(text: String): List<DictWord> = text.lineSequence()
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size < 4) return@mapNotNull null
            DictWord(
                word = parts[0].trim(),
                commonness = parts[1].trim().toIntOrNull() ?: 3,
                hardness = parts[2].trim().toIntOrNull() ?: 2,
                clues = parts[3].split('|').map { it.trim() }.filter { it.isNotEmpty() },
            )
        }
        .toList()
}
