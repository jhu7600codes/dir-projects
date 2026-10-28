package dev.jhu.crossi.generator

data class GenConfig(
    val size: Int,
    val seed: Long,
    /** Words (and so clues) up to this hardness are allowed. */
    val maxHardness: Int = 3,
    /** Long fun words as seed entries. Ignored below [PuzzleGenerator.LONG_WORD_MIN_GRID]. */
    val allowLongWords: Boolean = true,
)

class GenerationException(message: String) : RuntimeException(message)

/**
 * pattern -> fill -> number -> clue. Fully deterministic: the same dictionary and
 * config always give the same puzzle.
 */
class PuzzleGenerator(private val dictionary: Dictionary) {
    private val indexes = HashMap<Int, WordIndex>()

    fun generate(config: GenConfig): Puzzle {
        val spec = PatternSpec.forSize(config.size)
        val index = indexFor(config.maxHardness)
        val rng = Rng(config.seed)
        val longWordsOk = config.allowLongWords && config.size >= LONG_WORD_MIN_GRID

        for (attempt in 0 until MAX_ATTEMPTS) {
            val r = rng.fork()
            // First half of the attempts try to get a long word in, then give up on it.
            val seeds = if (longWordsOk && attempt < MAX_ATTEMPTS / 2) pickSeeds(config.size, r) else emptyList()
            val shape = PatternGenerator(spec, r).generate(seeds) ?: continue
            val rows = Filler(index, shape, r, nodeBudget(config.size), MAX_BRANCH).fill(seeds) ?: continue
            return build("${config.size}-${config.seed}", rows, r)
        }
        throw GenerationException("no ${config.size}x${config.size} fill for seed ${config.seed}")
    }

    private fun indexFor(maxHardness: Int): WordIndex = indexes.getOrPut(maxHardness) {
        WordIndex(dictionary.words.filter { it.hardness <= maxHardness }, dictionary.alphabet)
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

    private fun build(id: String, rows: List<String>, rng: Rng): Puzzle {
        val entries = Numbering.starts(rows).map { start ->
            val clues = dictionary[start.answer]?.clues.orEmpty()
            Entry(
                number = start.number,
                direction = start.direction,
                row = start.row,
                col = start.col,
                answer = start.answer,
                clue = if (clues.isEmpty()) "" else rng.pick(clues),
            )
        }
        return Puzzle(id, rows.size, rows, entries)
    }

    companion object {
        const val LONG_WORD_MIN_GRID = 11
        private const val MAX_ATTEMPTS = 40
        private const val MAX_BRANCH = 6

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
