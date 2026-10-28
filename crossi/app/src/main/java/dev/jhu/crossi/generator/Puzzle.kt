package dev.jhu.crossi.generator

import kotlinx.serialization.Serializable

@Serializable
enum class ClueKind { NORMAL, TRICK }

@Serializable
data class Entry(
    val number: Int,
    val direction: Direction,
    val row: Int,
    val col: Int,
    val answer: String,
    val clue: String,
    val clueKind: ClueKind = ClueKind.NORMAL,
    /** Other clues for the same answer, the skip clue power up swaps one in. */
    val altClues: List<String> = emptyList(),
) {
    val length: Int get() = answer.length

    fun cellAt(i: Int): Pair<Int, Int> =
        if (direction == Direction.ACROSS) row to col + i else row + i to col
}

/**
 * A finished puzzle. [grid] holds the solution row by row, '#' for blocks.
 * Serializable so levels can be frozen to json and games saved.
 */
@Serializable
data class Puzzle(
    val id: String,
    val size: Int,
    val grid: List<String>,
    val entries: List<Entry>,
) {
    fun isBlock(row: Int, col: Int): Boolean = grid[row][col] == BLOCK

    fun solution(row: Int, col: Int): Char = grid[row][col]

    companion object {
        const val BLOCK = '#'
    }
}

/** Standard crossword numbering for a solved grid: row-major, a cell gets a number if it starts a word. */
object Numbering {
    data class Start(val number: Int, val direction: Direction, val row: Int, val col: Int, val answer: String)

    fun starts(grid: List<String>): List<Start> {
        val n = grid.size
        fun white(r: Int, c: Int) = r in 0 until n && c in 0 until n && grid[r][c] != Puzzle.BLOCK
        val across = ArrayList<Start>()
        val down = ArrayList<Start>()
        var number = 0
        for (r in 0 until n) for (c in 0 until n) {
            if (!white(r, c)) continue
            val startsAcross = !white(r, c - 1) && white(r, c + 1)
            val startsDown = !white(r - 1, c) && white(r + 1, c)
            if (!startsAcross && !startsDown) continue
            number++
            if (startsAcross) {
                var end = c
                while (white(r, end)) end++
                across += Start(number, Direction.ACROSS, r, c, grid[r].substring(c, end))
            }
            if (startsDown) {
                var end = r
                while (white(end, c)) end++
                down += Start(number, Direction.DOWN, r, c, (r until end).map { grid[it][c] }.joinToString(""))
            }
        }
        return across + down
    }
}
