package dev.jhu.crossi.generator

import org.junit.Test
import kotlin.test.assertTrue

class ValidatorTest {
    private val words = setOf("CAT", "ARE", "TEN", "CAT", "ART", "TEN", "ACE", "RAN", "ETA", "ATE", "NET")

    private fun puzzle(vararg rows: String) =
        Puzzle("t", rows.size, rows.toList(), Numbering.starts(rows.toList()).map {
            Entry(it.number, it.direction, it.row, it.col, it.answer, "clue")
        })

    @Test
    fun `valid 3x3 passes`() {
        // C A T / A T E / T E N : across CAT ATE TEN, down CAT ATE TEN
        val p = puzzle("CAT", "ATE", "TEN")
        assertTrue(PuzzleValidator.validate(p) { it in words }.filterNot { it.startsWith("duplicate") }.isEmpty())
    }

    @Test
    fun `two letter runs are caught`() {
        val p = puzzle("CAT##", "ATE##", "TEN##", "#####", "#####")
        assertTrue(PuzzleValidator.validate(p) { true }.any { "symmetric" in it })
        val q = puzzle("AB#", "###", "#BA")
        assertTrue(PuzzleValidator.validate(q) { true }.any { "run of 2" in it || "run of 1" in it })
    }

    @Test
    fun `non words and duplicates are caught`() {
        val p = puzzle("CAT", "ATE", "TEN")
        val errors = PuzzleValidator.validate(p) { it == "CAT" }
        assertTrue(errors.any { it.startsWith("not a word") })
        assertTrue(errors.any { it.startsWith("duplicate") })
    }

    @Test
    fun `disconnected grids are caught`() {
        val p = puzzle("ABC##", "DEF##", "GHI##", "##JKL", "##MNO")
        assertTrue(PuzzleValidator.validate(p) { true }.isNotEmpty())
    }
}
