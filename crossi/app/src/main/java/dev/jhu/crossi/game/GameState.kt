package dev.jhu.crossi.game

import dev.jhu.crossi.generator.ClueKind
import dev.jhu.crossi.generator.Direction
import dev.jhu.crossi.generator.Puzzle
import kotlinx.serialization.Serializable

@Serializable
data class Rules(
    val twists: List<BossTwist> = emptyList(),
    /** entry index -> power up granted for solving that entry yourself */
    val pickups: Map<Int, PowerUpType> = emptyMap(),
) {
    fun has(twist: BossTwist) = twist in twists
}

@Serializable
enum class Status { PLAYING, WON, FAILED }

@Serializable
enum class FailReason { GAVE_UP, TIME_UP, OUT_OF_HEARTS }

/**
 * Everything about a game in progress. Immutable, serializable (saved games),
 * changed only through [GameEngine]. Cells are row-major indices.
 */
@Serializable
data class GameState(
    val puzzle: Puzzle,
    val rules: Rules = Rules(),
    /** size*size chars: '#' block, ' ' empty, otherwise the typed letter */
    val letters: String,
    val pencil: Set<Int> = emptySet(),
    val revealed: Set<Int> = emptySet(),
    val wrong: Set<Int> = emptySet(),
    /** empty cells flagged by the last check */
    val missing: Set<Int> = emptySet(),
    val locked: Set<Int> = emptySet(),
    val cursor: Int = 0,
    val direction: Direction = Direction.ACROSS,
    val checksLeft: Int = GameEngine.STARTING_CHECKS,
    val checksUsed: Int = 0,
    val hearts: Int? = null,
    val elapsedMs: Long = 0,
    val timeLimitMs: Long? = null,
    val powerUpsUsed: Int = 0,
    val clueOverrides: Map<Int, String> = emptyMap(),
    val keyboardSeed: Long = 0,
    /** scrambled clues boss: the order the clue list is shown in */
    val clueOrder: List<Int>? = null,
    val pencilMode: Boolean = false,
    val status: Status = Status.PLAYING,
    val failReason: FailReason? = null,
) {
    val geometry: Geometry by lazy { Geometry.of(puzzle) }

    val size: Int get() = puzzle.size

    fun isBlock(cell: Int): Boolean = letters[cell] == BLOCK

    fun solution(cell: Int): Char = puzzle.grid[cell / size][cell % size]

    fun letterAt(cell: Int): Char? = letters[cell].takeIf { it != EMPTY && it != BLOCK }

    val activeEntry: Int get() = geometry.entryAt(cursor, direction)

    fun clueFor(entry: Int): String = clueOverrides[entry] ?: puzzle.entries[entry].clue

    fun clueKindFor(entry: Int): ClueKind =
        if (entry in clueOverrides) ClueKind.NORMAL else puzzle.entries[entry].clueKind

    /** Fog boss: what you can see. Everything when there's no fog. */
    val visible: Set<Int> by lazy { fogVisible() }

    fun isEditable(cell: Int): Boolean =
        !isBlock(cell) && cell !in revealed && cell !in locked && cell in visible && status == Status.PLAYING

    val isFull: Boolean get() = geometry.whiteCells.all { letterAt(it) != null }

    private fun fogVisible(): Set<Int> {
        if (!rules.has(BossTwist.FOG)) return geometry.whiteCells.toSet()
        val out = HashSet<Int>()
        geometry.entryCells.firstOrNull()?.let { cells -> cells.forEach(out::add) }
        for (cell in geometry.whiteCells) {
            if (letterAt(cell) == null && cell !in revealed) continue
            out += cell
            geometry.neighbors(cell).forEach(out::add)
        }
        if (status != Status.PLAYING) out += geometry.whiteCells
        return out
    }

    companion object {
        const val BLOCK = '#'
        const val EMPTY = ' '
    }
}

/** Cell <-> entry lookups for one puzzle, computed once. */
class Geometry private constructor(puzzle: Puzzle) {
    val size = puzzle.size
    val entryCells: List<IntArray> = puzzle.entries.map { e ->
        IntArray(e.length) { i -> val (r, c) = e.cellAt(i); r * size + c }
    }
    private val across = IntArray(size * size) { -1 }
    private val down = IntArray(size * size) { -1 }
    val numbers = IntArray(size * size)
    val whiteCells: List<Int>

    init {
        puzzle.entries.forEachIndexed { index, e ->
            val owner = if (e.direction == Direction.ACROSS) across else down
            entryCells[index].forEach { owner[it] = index }
            numbers[entryCells[index][0]] = e.number
        }
        whiteCells = (0 until size * size).filter { across[it] >= 0 || down[it] >= 0 }
    }

    /** Entry through [cell] in [direction], falling back to the other direction. */
    fun entryAt(cell: Int, direction: Direction): Int {
        val primary = if (direction == Direction.ACROSS) across[cell] else down[cell]
        return if (primary >= 0) primary else if (direction == Direction.ACROSS) down[cell] else across[cell]
    }

    fun hasEntry(cell: Int, direction: Direction): Boolean =
        (if (direction == Direction.ACROSS) across[cell] else down[cell]) >= 0

    fun entriesThrough(cell: Int): List<Int> = listOf(across[cell], down[cell]).filter { it >= 0 }

    fun neighbors(cell: Int): List<Int> {
        val r = cell / size
        val c = cell % size
        return listOfNotNull(
            if (r > 0) cell - size else null,
            if (r < size - 1) cell + size else null,
            if (c > 0) cell - 1 else null,
            if (c < size - 1) cell + 1 else null,
        ).filter { across[it] >= 0 || down[it] >= 0 }
    }

    companion object {
        private val cache = object : LinkedHashMap<Puzzle, Geometry>(8, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Puzzle, Geometry>?) = size > 8
        }

        fun of(puzzle: Puzzle): Geometry = synchronized(cache) { cache.getOrPut(puzzle) { Geometry(puzzle) } }
    }
}
