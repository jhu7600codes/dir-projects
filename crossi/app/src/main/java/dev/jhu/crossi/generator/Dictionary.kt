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
) {
    val words: List<DictWord> = normalized(words)
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
