package dev.jhu.crossi.generator

/** How dense a grid of a given size should be and how long its runs may get. */
data class PatternSpec(val size: Int, val blocks: IntRange, val maxRun: Int) {
    companion object {
        val SIZES = listOf(5, 7, 9, 11, 13, 15)

        fun forSize(size: Int): PatternSpec = when (size) {
            5 -> PatternSpec(5, 0..4, 5)
            7 -> PatternSpec(7, 8..12, 7)
            9 -> PatternSpec(9, 14..20, 7)
            // big grids: short max runs, common-word dictionaries can't fill long stacks
            11 -> PatternSpec(11, 26..34, 6)
            13 -> PatternSpec(13, 36..46, 6)
            15 -> PatternSpec(15, 46..58, 6)
            else -> throw IllegalArgumentException("unsupported grid size $size")
        }
    }
}

/** A word that must sit across at a fixed spot (the long fun words in big grids). */
data class SeedPlacement(val word: String, val row: Int, val col: Int)

/**
 * Builds a nyt-style block layout: 180° symmetric, every run 3+, all white cells
 * connected, long runs broken up so common words can fill it.
 *
 * Blocks are always placed in symmetric pairs. Placing one cascades: any run it
 * leaves at 1-2 cells becomes blocks too, so the shape never has unchecked cells.
 */
class PatternGenerator(private val spec: PatternSpec, private val rng: Rng) {
    private val n = spec.size

    /** Up to [tries] fresh layouts; random block placement sometimes paints itself into a corner. */
    fun generate(seeds: List<SeedPlacement> = emptyList(), tries: Int = 60): Shape? {
        repeat(tries) { tryOnce(seeds)?.let { return it } }
        return null
    }

    private fun tryOnce(seeds: List<SeedPlacement>, maxSteps: Int = 500): Shape? {
        val blocks = BooleanArray(n * n)
        val locked = BooleanArray(n * n)
        val ends = ArrayList<Int>()
        for (seed in seeds) {
            val len = seed.word.length
            if (seed.row !in 0 until n || seed.col < 0 || seed.col + len > n) return null
            for (i in 0 until len) locked[seed.row * n + seed.col + i] = true
            if (seed.col > 0) ends += seed.row * n + seed.col - 1
            if (seed.col + len < n) ends += seed.row * n + seed.col + len
        }
        for (cell in ends) if (!place(blocks, locked, cell)) return null

        val target = rng.nextInt(spec.blocks.first, spec.blocks.last + 1)
        var steps = 0
        while (steps++ < maxSteps) {
            val long = Shape(n, blocks).slots.filter { slot ->
                slot.length > spec.maxRun && slot.cells.none { locked[it] }
            }
            if (long.isNotEmpty()) {
                val slot = rng.pick(long)
                val offsets = (MIN_RUN until slot.length - MIN_RUN).toMutableList()
                rng.shuffle(offsets)
                offsets.firstOrNull { place(blocks, locked, slot.cells[it]) }
            } else if (blocks.count { it } < target) {
                place(blocks, locked, rng.nextInt(n * n))
            } else {
                break
            }
        }
        return Shape(n, blocks).takeIf { isValid(it, locked) }
    }

    private fun isValid(shape: Shape, locked: BooleanArray): Boolean =
        shape.blockCount in spec.blocks &&
            shape.isSymmetric() &&
            shape.isConnected() &&
            shape.slots.all { slot ->
                slot.length >= MIN_RUN && (slot.length <= spec.maxRun || slot.cells.any { locked[it] })
            }

    /** Tries to block [cell] (and its partner, plus any cascade). Commits only if the result is legal. */
    private fun place(blocks: BooleanArray, locked: BooleanArray, cell: Int): Boolean {
        val trial = blocks.copyOf()
        val queue = ArrayDeque<Int>().apply { add(cell) }
        while (queue.isNotEmpty()) {
            val c = queue.removeFirst()
            for (x in intArrayOf(c, n * n - 1 - c)) {
                if (trial[x]) continue
                if (locked[x]) return false
                trial[x] = true
                queue.addAll(shortRunsThrough(trial, x / n, x % n))
            }
        }
        if (trial.count { it } > spec.blocks.last) return false
        if (!Shape(n, trial).isConnected()) return false
        trial.copyInto(blocks)
        return true
    }

    /** Cells of white runs shorter than [MIN_RUN] in the row and column of (row, col). */
    private fun shortRunsThrough(blocks: BooleanArray, row: Int, col: Int): List<Int> {
        val out = ArrayList<Int>()
        fun scan(cellAt: (Int) -> Int) {
            var start = -1
            for (i in 0..n) {
                val white = i < n && !blocks[cellAt(i)]
                if (white && start < 0) start = i
                if (!white && start >= 0) {
                    if (i - start < MIN_RUN) for (j in start until i) out += cellAt(j)
                    start = -1
                }
            }
        }
        scan { row * n + it }
        scan { it * n + col }
        return out
    }

    companion object {
        const val MIN_RUN = 3
    }
}
