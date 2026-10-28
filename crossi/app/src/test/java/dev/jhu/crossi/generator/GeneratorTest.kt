package dev.jhu.crossi.generator

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeneratorTest {
    private fun assertValid(puzzle: Puzzle, dictionary: Dictionary) {
        val errors = PuzzleValidator.validate(puzzle) { dictionary.contains(it) }
        assertTrue(errors.isEmpty(), "puzzle ${puzzle.id}: $errors\n${puzzle.grid.joinToString("\n")}")
        assertTrue(puzzle.entries.all { it.clue.isNotBlank() }, "missing clue in ${puzzle.id}")
    }

    private fun sweep(generator: PuzzleGenerator, dictionary: Dictionary, seeds: Map<Int, Int>) {
        for ((size, count) in seeds) {
            var ok = 0
            for (seed in 0L until count) {
                try {
                    assertValid(generator.generate(GenConfig(size, seed)), dictionary)
                    ok++
                } catch (_: GenerationException) {
                }
            }
            // the app retries the next seed when one fails, but almost all should work first time
            assertTrue(ok >= count * 0.8, "only $ok/$count ${size}x$size fills")
        }
    }

    @Test
    fun `english puzzles are valid at every size`() =
        sweep(Fixtures.englishGenerator, Fixtures.english, mapOf(5 to 40, 7 to 30, 9 to 20, 11 to 12, 13 to 8, 15 to 6))

    @Test
    fun `russian puzzles are valid at every size`() =
        sweep(Fixtures.russianGenerator, Fixtures.russian, mapOf(5 to 40, 7 to 30, 9 to 20, 11 to 12, 13 to 8, 15 to 6))

    @Test
    fun `same config same puzzle`() {
        val a = Fixtures.englishGenerator.generate(GenConfig(9, 1234))
        val b = PuzzleGenerator(Fixtures.english).generate(GenConfig(9, 1234))
        assertEquals(a, b)
    }

    @Test
    fun `different seeds give different puzzles`() {
        val a = Fixtures.englishGenerator.generate(GenConfig(7, 1))
        val b = Fixtures.englishGenerator.generate(GenConfig(7, 2))
        assertTrue(a.grid != b.grid)
    }

    @Test
    fun `long words never show up in small grids`() {
        val long = Fixtures.english.longWords.map { it.word }.toSet()
        for (size in listOf(5, 7, 9)) for (seed in 0L until 10) {
            val puzzle = Fixtures.englishGenerator.generate(GenConfig(size, seed))
            assertTrue(puzzle.entries.none { it.answer in long }, "long word in ${size}x$size")
            assertTrue(puzzle.entries.all { it.length <= size })
        }
    }

    @Test
    fun `long words do show up in big grids sometimes`() {
        val long = Fixtures.english.longWords.map { it.word }.toSet()
        val hits = (0L until 10).count { seed ->
            Fixtures.englishGenerator.generate(GenConfig(15, seed)).entries.any { it.answer in long }
        }
        assertTrue(hits > 0, "no long word in ten 15x15 grids")
    }

    @Test
    fun `trick fest puts letter tricks in`() {
        val puzzle = Fixtures.englishGenerator.generate(GenConfig(9, 5, trickFest = true))
        assertTrue(puzzle.entries.any { it.clueKind == ClueKind.TRICK }, puzzle.grid.joinToString("\n"))
    }

    @Test
    fun `entries match the grid`() {
        val puzzle = Fixtures.englishGenerator.generate(GenConfig(11, 77))
        for (e in puzzle.entries) {
            val letters = (0 until e.length).map { i -> e.cellAt(i).let { (r, c) -> puzzle.solution(r, c) } }
            assertEquals(e.answer, letters.joinToString(""))
        }
    }
}
