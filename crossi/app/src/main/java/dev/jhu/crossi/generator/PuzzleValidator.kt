package dev.jhu.crossi.generator

/**
 * Checks every rule a finished puzzle has to follow. Returns human readable
 * problems, empty list = valid. The generator tests lean on this.
 */
object PuzzleValidator {

    fun validate(puzzle: Puzzle, isWord: (String) -> Boolean): List<String> {
        val errors = ArrayList<String>()
        val n = puzzle.size
        if (puzzle.grid.size != n || puzzle.grid.any { it.length != n }) {
            return listOf("grid is not ${n}x$n")
        }
        val blocks = BooleanArray(n * n) { puzzle.grid[it / n][it % n] == Puzzle.BLOCK }
        val shape = Shape(n, blocks)

        if (!shape.isSymmetric()) errors += "blocks are not 180° symmetric"
        if (!shape.isConnected()) errors += "white cells are not connected"
        shape.slots.filter { it.length < PatternGenerator.MIN_RUN }.forEach {
            errors += "${it.direction} run of ${it.length} at ${it.row},${it.col}"
        }

        val expected = Numbering.starts(puzzle.grid)
        val actual = puzzle.entries.map { Numbering.Start(it.number, it.direction, it.row, it.col, it.answer) }
        if (expected.toSet() != actual.toSet() || expected.size != actual.size) {
            errors += "entries don't match the grid's numbering"
        }

        val seen = HashSet<String>()
        for (e in puzzle.entries) {
            if (!isWord(e.answer)) errors += "not a word: ${e.answer}"
            if (!seen.add(e.answer)) errors += "duplicate word: ${e.answer}"
        }
        return errors
    }
}
