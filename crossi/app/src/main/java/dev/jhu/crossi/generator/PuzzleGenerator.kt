package dev.jhu.crossi.generator

data class GenConfig(
    val size: Int,
    val seed: Long,
    /**
     * Preferred max word hardness (1..3). Harder words are still allowed but pushed to the
     * back of the line, a hard cutoff starves big grids of words.
     */
    val maxHardness: Int = 3,
    /** Long fun words as seed entries. Ignored below [PuzzleGenerator.LONG_WORD_MIN_GRID]. */
    val allowLongWords: Boolean = true,
    /** Chance that an answer which can be a letter trick gets the trick clue. */
    val trickChance: Double = 0.5,
    /** Trick fest boss: the filler prefers trick answers and they always get trick clues. */
    val trickFest: Boolean = false,
)

class GenerationException(message: String) : RuntimeException(message)

/**
 * pattern -> fill -> number -> clue. Fully deterministic: the same dictionary and
 * config always give the same puzzle.
 */
class PuzzleGenerator(private val dictionary: Dictionary) {
    private val indexes = HashMap<Pair<Int, Boolean>, WordIndex>()

    fun generate(config: GenConfig): Puzzle {
        val spec = PatternSpec.forSize(config.size)
        val index = indexFor(config.maxHardness, config.trickFest)
        val rng = Rng(config.seed)
        val longWordsOk = config.allowLongWords && config.size >= LONG_WORD_MIN_GRID

        for (attempt in 0 until MAX_ATTEMPTS) {
            val r = rng.fork()
            // First half of the attempts try to get a long word in, then give up on it.
            val seeds = if (longWordsOk && attempt < MAX_ATTEMPTS / 2) pickSeeds(config.size, r) else emptyList()
            val shape = PatternGenerator(spec, r).generate(seeds) ?: continue
            val rows = Filler(index, shape, r, nodeBudget(config.size), MAX_BRANCH).fill(seeds) ?: continue
            return build("${config.size}-${config.seed}", rows, r, config)
        }
        throw GenerationException("no ${config.size}x${config.size} fill for seed ${config.seed}")
    }

    private fun indexFor(maxHardness: Int, trickFest: Boolean): WordIndex = synchronized(indexes) {
        indexes.getOrPut(maxHardness to trickFest) {
            val tricks = dictionary.tricks
            val words = dictionary.words.map {
                when {
                    trickFest && tricks?.isTrick(it.word) == true -> it.copy(commonness = TRICK_FEST_SCORE)
                    it.hardness > maxHardness -> it.copy(commonness = it.commonness - HARDNESS_PENALTY * (it.hardness - maxHardness))
                    else -> it
                }
            }
            WordIndex(words, dictionary.alphabet)
        }
    }

    /**
     * One long word centered in the middle row (needs size - length to be even), or two
     * long words of the same length in symmetric rows. Only words longer than the
     * grid's normal max run count as "long".
     */
    private fun pickSeeds(size: Int, rng: Rng): List<SeedPlacement> {
        val maxRun = PatternSpec.forSize(size).maxRun
        val pool = dictionary.longWords.filter { it.word.length in (maxRun + 1)..size }.map { it.word }.toMutableList()
        if (pool.isEmpty()) return emptyList()
        rng.shuffle(pool)
        if (rng.nextInt(2) == 0) {
            pool.firstOrNull { (size - it.length) % 2 == 0 }?.let {
                return listOf(SeedPlacement(it, size / 2, (size - it.length) / 2))
            }
        }
        for (first in pool) {
            val second = pool.firstOrNull { it != first && it.length == first.length } ?: continue
            val row = rng.nextInt(2, 4)
            return listOf(
                SeedPlacement(first, row, 0),
                SeedPlacement(second, size - 1 - row, size - first.length),
            )
        }
        return emptyList()
    }

    private fun build(id: String, rows: List<String>, rng: Rng, config: GenConfig): Puzzle {
        val tricks = dictionary.tricks
        val entries = Numbering.starts(rows).map { start ->
            val normal = dictionary[start.answer]?.clues.orEmpty()
            val trick = tricks?.allCluesFor(start.answer, rng).orEmpty().shuffledWith(rng)
            val useTrick = trick.isNotEmpty() &&
                (normal.isEmpty() || config.trickFest || rng.nextDouble() < config.trickChance)
            val primary = if (useTrick) trick.first() else pickNormal(normal, config.maxHardness, rng)
            Entry(
                number = start.number,
                direction = start.direction,
                row = start.row,
                col = start.col,
                answer = start.answer,
                clue = primary,
                clueKind = if (useTrick) ClueKind.TRICK else ClueKind.NORMAL,
                altClues = (normal + trick).filter { it != primary }.distinct().take(MAX_ALT_CLUES),
            )
        }
        return Puzzle(id, rows.size, rows, entries)
    }

    /** Clue 1 is the plainest. Easy puzzles always use it, harder ones mix in the rest. */
    private fun pickNormal(clues: List<String>, maxHardness: Int, rng: Rng): String = when {
        clues.isEmpty() -> ""
        maxHardness <= 1 -> clues.first()
        maxHardness == 2 -> clues[rng.nextInt(minOf(2, clues.size))]
        else -> rng.pick(clues)
    }

    private fun <T> List<T>.shuffledWith(rng: Rng): List<T> = toMutableList().also(rng::shuffle)

    companion object {
        const val LONG_WORD_MIN_GRID = 11
        private const val MAX_ATTEMPTS = 60
        private const val MAX_BRANCH = 10
        private const val MAX_ALT_CLUES = 3
        private const val TRICK_FEST_SCORE = 9
        private const val HARDNESS_PENALTY = 3

        private fun nodeBudget(size: Int) = when (size) {
            5 -> 2_000
            7 -> 4_000
            9 -> 8_000
            11 -> 12_000
            13 -> 16_000
            else -> 25_000
        }
    }
}
