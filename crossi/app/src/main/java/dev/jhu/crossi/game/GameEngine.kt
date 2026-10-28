package dev.jhu.crossi.game

import dev.jhu.crossi.generator.Direction
import dev.jhu.crossi.generator.Puzzle
import dev.jhu.crossi.generator.Rng

data class InputPrefs(
    val autoAdvance: Boolean = true,
    val skipFilled: Boolean = true,
    val jumpToNextClue: Boolean = true,
)

/** What a finish tap found. */
data class CheckResult(val wrong: Int, val missing: Int) {
    val solved: Boolean get() = wrong == 0 && missing == 0
}

/** All game rules as pure functions over [GameState]. */
object GameEngine {
    const val STARTING_CHECKS = 5
    const val BOSS_HEARTS = 3
    private const val TIMER_MS_PER_CELL = 6_000L
    private const val LOCKED_SHARE = 0.18

    fun start(puzzle: Puzzle, rules: Rules = Rules(), seed: Long = 0): GameState {
        val rng = Rng(seed)
        val letters = puzzle.grid.joinToString("") { row ->
            row.map { if (it == Puzzle.BLOCK) GameState.BLOCK else GameState.EMPTY }.joinToString("")
        }
        var state = GameState(
            puzzle = puzzle,
            rules = rules,
            letters = letters,
            checksLeft = if (rules.has(BossTwist.ONE_SHOT)) 1 else STARTING_CHECKS,
            hearts = if (rules.has(BossTwist.LIMITED_MISTAKES)) BOSS_HEARTS else null,
        )
        val white = state.geometry.whiteCells
        if (rules.has(BossTwist.TIMER)) state = state.copy(timeLimitMs = white.size * TIMER_MS_PER_CELL)
        if (rules.has(BossTwist.LOCKED_CELLS)) state = state.copy(locked = pickLocked(state, rng))
        if (rules.has(BossTwist.SCRAMBLED_CLUES)) {
            state = state.copy(clueOrder = puzzle.entries.indices.toMutableList().also(rng::shuffle))
        }
        if (rules.has(BossTwist.SCRAMBLED_KEYBOARD)) state = state.copy(keyboardSeed = rng.nextLong())
        val first = state.geometry.entryCells.firstOrNull()?.firstOrNull { it !in state.locked } ?: white.first()
        return state.copy(cursor = first, direction = puzzle.entries.firstOrNull()?.direction ?: Direction.ACROSS)
    }

    /** ~18% of cells, but every entry keeps at least 2 open cells so nothing deadlocks. */
    private fun pickLocked(state: GameState, rng: Rng): Set<Int> {
        val geo = state.geometry
        val candidates = geo.whiteCells.toMutableList().also(rng::shuffle)
        val target = (geo.whiteCells.size * LOCKED_SHARE).toInt()
        val locked = HashSet<Int>()
        for (cell in candidates) {
            if (locked.size >= target) break
            val ok = geo.entriesThrough(cell).all { e -> geo.entryCells[e].count { it !in locked } - 1 >= 2 }
            if (ok) locked += cell
        }
        return locked
    }

    // ---- input ----

    fun type(state: GameState, letter: Char, prefs: InputPrefs = InputPrefs()): GameState {
        if (state.status != Status.PLAYING) return state
        val ch = letter.uppercaseChar().let { if (it == 'Ё') 'Е' else it }
        val cell = state.cursor
        if (!state.isEditable(cell)) {
            return if (cell in state.revealed) advance(state, prefs) else state
        }
        val letters = StringBuilder(state.letters).apply { setCharAt(cell, ch) }.toString()
        val next = state.copy(
            letters = letters,
            wrong = state.wrong - cell,
            missing = state.missing - cell,
            pencil = if (state.pencilMode) state.pencil + cell else state.pencil - cell,
        )
        return advance(refreshLocks(next), prefs)
    }

    fun backspace(state: GameState): GameState {
        if (state.status != Status.PLAYING) return state
        val cell = state.cursor
        if (state.isEditable(cell) && state.letterAt(cell) != null) return clear(state, cell)
        val cells = state.geometry.entryCells[state.activeEntry]
        val pos = cells.indexOf(cell)
        val prev = (pos - 1 downTo 0).map { cells[it] }.firstOrNull { state.isEditable(it) } ?: return state
        return clear(state.copy(cursor = prev), prev)
    }

    private fun clear(state: GameState, cell: Int): GameState = state.copy(
        letters = StringBuilder(state.letters).apply { setCharAt(cell, GameState.EMPTY) }.toString(),
        wrong = state.wrong - cell,
        pencil = state.pencil - cell,
    )

    fun tap(state: GameState, cell: Int): GameState {
        if (cell !in state.geometry.whiteCells || cell !in state.visible) return state
        if (cell == state.cursor) return toggleDirection(state)
        val direction = if (state.geometry.hasEntry(cell, state.direction)) state.direction else state.direction.other()
        return state.copy(cursor = cell, direction = direction)
    }

    fun toggleDirection(state: GameState): GameState {
        val other = state.direction.other()
        return if (state.geometry.hasEntry(state.cursor, other)) state.copy(direction = other) else state
    }

    fun selectEntry(state: GameState, entry: Int): GameState {
        val cells = state.geometry.entryCells[entry]
        val target = cells.firstOrNull { state.isEditable(it) && state.letterAt(it) == null }
            ?: cells.firstOrNull { it in state.visible } ?: return state
        return state.copy(cursor = target, direction = state.puzzle.entries[entry].direction)
    }

    /** Next / previous clue in list order, skipping fogged-out ones. */
    fun moveEntry(state: GameState, delta: Int): GameState {
        val count = state.puzzle.entries.size
        val order = state.clueOrder ?: (0 until count).toList()
        var i = order.indexOf(state.activeEntry)
        repeat(count) {
            i = (i + delta + count) % count
            val entry = order[i]
            if (state.geometry.entryCells[entry].any { it in state.visible }) return selectEntry(state, entry)
        }
        return state
    }

    fun togglePencil(state: GameState): GameState = state.copy(pencilMode = !state.pencilMode)

    private fun advance(state: GameState, prefs: InputPrefs): GameState {
        if (!prefs.autoAdvance) return state
        val cells = state.geometry.entryCells[state.activeEntry]
        val pos = cells.indexOf(state.cursor)
        val after = (pos + 1 until cells.size).map { cells[it] }
        val target = if (prefs.skipFilled) {
            after.firstOrNull { state.isEditable(it) && state.letterAt(it) == null }
                ?: cells.firstOrNull { state.isEditable(it) && state.letterAt(it) == null && it != state.cursor }
        } else {
            after.firstOrNull { state.isEditable(it) || it in state.revealed }
        }
        if (target != null) return state.copy(cursor = target)
        return if (prefs.jumpToNextClue) nextOpenEntry(state) ?: state else state
    }

    private fun nextOpenEntry(state: GameState): GameState? {
        val count = state.puzzle.entries.size
        val order = state.clueOrder ?: (0 until count).toList()
        val start = order.indexOf(state.activeEntry)
        for (k in 1..count) {
            val entry = order[(start + k) % count]
            val open = state.geometry.entryCells[entry].any { state.isEditable(it) && state.letterAt(it) == null }
            if (open) return selectEntry(state, entry)
        }
        return null
    }

    /** Locked cells open once the rest of a word through them is right. */
    private fun refreshLocks(state: GameState): GameState {
        if (state.locked.isEmpty()) return state
        val geo = state.geometry
        val opened = HashSet<Int>()
        for (cell in state.locked) {
            val open = geo.entriesThrough(cell).any { e ->
                geo.entryCells[e].all { it in state.locked || state.letterAt(it) == state.solution(it) }
            }
            if (open) opened += cell
        }
        return if (opened.isEmpty()) state else state.copy(locked = state.locked - opened)
    }

    // ---- finishing ----

    fun canFinish(state: GameState) = state.status == Status.PLAYING && state.checksLeft > 0

    /** One check on the whole board. Wrong letters go red, empty cells get flagged. */
    fun finish(state: GameState): Pair<GameState, CheckResult> {
        if (!canFinish(state)) return state to CheckResult(0, 0)
        val geo = state.geometry
        val wrong = geo.whiteCells.filter { state.letterAt(it).let { l -> l != null && l != state.solution(it) } }.toSet()
        val missing = geo.whiteCells.filter { state.letterAt(it) == null }.toSet()
        val result = CheckResult(wrong.size, missing.size)
        var next = state.copy(
            checksLeft = state.checksLeft - 1,
            checksUsed = state.checksUsed + 1,
            wrong = wrong,
            missing = missing,
            pencil = state.pencil - wrong,
        )
        if (result.solved) return next.copy(status = Status.WON, pencil = emptySet(), locked = emptySet()) to result
        if (next.hearts != null) {
            val hearts = next.hearts!! - 1
            next = next.copy(hearts = hearts)
            if (hearts <= 0) return fail(next, FailReason.OUT_OF_HEARTS) to result
        }
        if (state.rules.has(BossTwist.SCRAMBLED_KEYBOARD)) next = next.copy(keyboardSeed = Rng(next.keyboardSeed).nextLong())
        return next to result
    }

    fun giveUp(state: GameState): GameState = fail(state, FailReason.GAVE_UP)

    private fun fail(state: GameState, reason: FailReason): GameState {
        val solution = state.puzzle.grid.joinToString("")
        return state.copy(letters = solution, status = Status.FAILED, failReason = reason, locked = emptySet())
    }

    fun tick(state: GameState, deltaMs: Long): GameState {
        if (state.status != Status.PLAYING) return state
        val elapsed = state.elapsedMs + deltaMs
        val limit = state.timeLimitMs
        if (limit != null && elapsed >= limit) return fail(state.copy(elapsedMs = limit), FailReason.TIME_UP)
        return state.copy(elapsedMs = elapsed)
    }

    // ---- power ups ----

    fun canUse(state: GameState, type: PowerUpType): Boolean {
        if (state.status != Status.PLAYING) return false
        val entryCells = state.geometry.entryCells[state.activeEntry]
        return when (type) {
            PowerUpType.REVEAL_LETTER -> state.cursor !in state.revealed && state.cursor in state.visible
            PowerUpType.REVEAL_WORD -> entryCells.any { it !in state.revealed }
            PowerUpType.EXTRA_CHECK -> !state.rules.has(BossTwist.ONE_SHOT)
            PowerUpType.SKIP_CLUE -> entryCells.any { it !in state.revealed }
        }
    }

    /** Returns null if the power up can't be used right now (so it isn't spent). */
    fun use(state: GameState, type: PowerUpType): GameState? {
        if (!canUse(state, type)) return null
        val used = state.copy(powerUpsUsed = state.powerUpsUsed + 1)
        return when (type) {
            PowerUpType.REVEAL_LETTER -> reveal(used, listOf(state.cursor))
            PowerUpType.REVEAL_WORD -> reveal(used, state.geometry.entryCells[state.activeEntry].toList())
            PowerUpType.EXTRA_CHECK -> used.copy(checksLeft = state.checksLeft + 1)
            PowerUpType.SKIP_CLUE -> skipClue(used)
        }
    }

    private fun reveal(state: GameState, cells: List<Int>): GameState {
        val letters = StringBuilder(state.letters)
        cells.forEach { letters.setCharAt(it, state.solution(it)) }
        val set = cells.toSet()
        return refreshLocks(
            state.copy(
                letters = letters.toString(),
                revealed = state.revealed + set,
                wrong = state.wrong - set,
                missing = state.missing - set,
                pencil = state.pencil - set,
                locked = state.locked - set,
            ),
        )
    }

    /** Swaps in a different clue for the active word. No other clue left = reveals one empty letter instead. */
    private fun skipClue(state: GameState): GameState {
        val entryIndex = state.activeEntry
        val entry = state.puzzle.entries[entryIndex]
        val options = listOf(entry.clue) + entry.altClues
        val current = state.clueFor(entryIndex)
        val next = options.getOrNull(options.indexOf(current) + 1)
        if (next != null) return state.copy(clueOverrides = state.clueOverrides + (entryIndex to next))
        val cell = state.geometry.entryCells[entryIndex].firstOrNull { it !in state.revealed && state.letterAt(it) != state.solution(it) }
            ?: state.geometry.entryCells[entryIndex].first { it !in state.revealed }
        return reveal(state, listOf(cell))
    }

    /** Pickups you earned: entries you filled in yourself (no revealed cells). Only counts on a win. */
    fun earnedPickups(state: GameState): List<PowerUpType> {
        if (state.status != Status.WON) return emptyList()
        return state.rules.pickups.filter { (entry, _) ->
            state.geometry.entryCells.getOrNull(entry)?.none { it in state.revealed } == true
        }.values.toList()
    }

    private fun Direction.other() = if (this == Direction.ACROSS) Direction.DOWN else Direction.ACROSS
}
