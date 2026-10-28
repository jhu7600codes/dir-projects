package dev.jhu.crossi.game

import dev.jhu.crossi.generator.Direction
import dev.jhu.crossi.generator.Entry
import dev.jhu.crossi.generator.Numbering
import dev.jhu.crossi.generator.Puzzle
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameEngineTest {
    // C A T
    // A T E
    // T E N
    private val puzzle = Puzzle("t", 3, listOf("CAT", "ATE", "TEN"), Numbering.starts(listOf("CAT", "ATE", "TEN")).map {
        Entry(it.number, it.direction, it.row, it.col, it.answer, "clue ${it.number}", altClues = listOf("other ${it.number}"))
    })

    private fun typeAll(state: GameState, text: String): GameState =
        text.fold(state) { s, ch -> GameEngine.type(s, ch) }

    private fun solved(): GameState {
        var s = GameEngine.start(puzzle)
        for (row in 0 until 3) {
            s = GameEngine.selectEntry(s, row)
            s = typeAll(s, puzzle.grid[row])
        }
        return s
    }

    @Test
    fun `typing fills and auto advances`() {
        val s = typeAll(GameEngine.start(puzzle), "CA")
        assertEquals('C', s.letterAt(0))
        assertEquals('A', s.letterAt(1))
        assertEquals(2, s.cursor)
    }

    @Test
    fun `tapping the cursor toggles direction`() {
        val s = GameEngine.start(puzzle)
        assertEquals(Direction.ACROSS, s.direction)
        assertEquals(Direction.DOWN, GameEngine.tap(s, s.cursor).direction)
    }

    @Test
    fun `backspace clears then steps back`() {
        var s = typeAll(GameEngine.start(puzzle), "CA")
        s = GameEngine.backspace(s) // cursor on empty cell 2 -> clears cell 1
        assertNull(s.letterAt(1))
        assertEquals(1, s.cursor)
    }

    @Test
    fun `finish on a right board wins with one check`() {
        val (s, result) = GameEngine.finish(solved())
        assertTrue(result.solved)
        assertEquals(Status.WON, s.status)
        assertEquals(4, s.checksLeft)
        assertEquals(3, Scoring.stars(s))
    }

    @Test
    fun `finish marks wrong letters and costs a check`() {
        var s = solved()
        s = GameEngine.tap(s, 4)
        s = GameEngine.type(s, 'X')
        val (checked, result) = GameEngine.finish(s)
        assertEquals(1, result.wrong)
        assertEquals(setOf(4), checked.wrong)
        assertEquals(Status.PLAYING, checked.status)
        // fixing the cell clears the red mark
        val fixed = GameEngine.type(GameEngine.tap(checked, 4), 'T')
        assertTrue(4 !in fixed.wrong)
    }

    @Test
    fun `out of checks means finish is off`() {
        var s = GameEngine.start(puzzle)
        repeat(5) { s = GameEngine.finish(s).first }
        assertEquals(0, s.checksLeft)
        assertFalse(GameEngine.canFinish(s))
        val extra = assertNotNull(GameEngine.use(s, PowerUpType.EXTRA_CHECK))
        assertTrue(GameEngine.canFinish(extra))
    }

    @Test
    fun `give up reveals and fails`() {
        val s = GameEngine.giveUp(GameEngine.start(puzzle))
        assertEquals(Status.FAILED, s.status)
        assertEquals(FailReason.GAVE_UP, s.failReason)
        assertEquals("CATATETEN", s.letters)
    }

    @Test
    fun `reveal word fills the active entry and locks it`() {
        val s = assertNotNull(GameEngine.use(GameEngine.start(puzzle), PowerUpType.REVEAL_WORD))
        assertEquals("CAT", (0..2).map { s.letterAt(it) }.joinToString(""))
        assertEquals(setOf(0, 1, 2), s.revealed)
        assertEquals(1, s.powerUpsUsed)
        assertFalse(s.isEditable(0))
    }

    @Test
    fun `skip clue swaps the clue`() {
        val s = assertNotNull(GameEngine.use(GameEngine.start(puzzle), PowerUpType.SKIP_CLUE))
        assertEquals("other 1", s.clueFor(s.activeEntry))
    }

    @Test
    fun `one shot boss has one check and no extra checks`() {
        val s = GameEngine.start(puzzle, Rules(listOf(BossTwist.ONE_SHOT)))
        assertEquals(1, s.checksLeft)
        assertNull(GameEngine.use(s, PowerUpType.EXTRA_CHECK))
    }

    @Test
    fun `timer boss fails when time runs out`() {
        val s = GameEngine.start(puzzle, Rules(listOf(BossTwist.TIMER)))
        val limit = assertNotNull(s.timeLimitMs)
        val after = GameEngine.tick(s, limit)
        assertEquals(Status.FAILED, after.status)
        assertEquals(FailReason.TIME_UP, after.failReason)
    }

    @Test
    fun `limited mistakes boss loses hearts on bad finishes`() {
        var s = GameEngine.start(puzzle, Rules(listOf(BossTwist.LIMITED_MISTAKES)))
        repeat(3) { s = GameEngine.finish(s).first }
        assertEquals(Status.FAILED, s.status)
        assertEquals(FailReason.OUT_OF_HEARTS, s.failReason)
    }

    @Test
    fun `fog only shows the first word at the start`() {
        val s = GameEngine.start(puzzle, Rules(listOf(BossTwist.FOG)))
        assertEquals(setOf(0, 1, 2), s.visible)
        val typed = typeAll(s, "C")
        assertTrue(3 in typed.visible) // below the C
    }

    @Test
    fun `locked cells never lock a whole word and open when the rest is right`() {
        val big = dev.jhu.crossi.generator.PuzzleGenerator(dev.jhu.crossi.generator.Fixtures.english)
            .generate(dev.jhu.crossi.generator.GenConfig(9, 3))
        var s = GameEngine.start(big, Rules(listOf(BossTwist.LOCKED_CELLS)), seed = 5)
        assertTrue(s.locked.isNotEmpty())
        for (cells in s.geometry.entryCells) assertTrue(cells.count { it !in s.locked } >= 2)
        // fill everything that's open with the right letters: every lock has to open
        repeat(4) {
            for (cell in s.geometry.whiteCells) if (s.isEditable(cell)) {
                s = GameEngine.type(GameEngine.tap(s, cell).let { if (it.cursor != cell) GameEngine.tap(it, cell) else it }, s.solution(cell))
            }
        }
        assertTrue(s.locked.isEmpty(), "still locked: ${s.locked}")
    }

    @Test
    fun `pickups only count for words you filled yourself`() {
        var s = GameEngine.start(puzzle, Rules(pickups = mapOf(0 to PowerUpType.REVEAL_WORD, 3 to PowerUpType.EXTRA_CHECK)))
        s = GameEngine.selectEntry(s, 3)
        s = assertNotNull(GameEngine.use(s, PowerUpType.REVEAL_LETTER)) // cell 0, in across 0 and down 3
        val prefs = InputPrefs(autoAdvance = false)
        for (cell in 1 until 9) {
            s = GameEngine.tap(s, cell)
            s = GameEngine.type(s, puzzle.grid[cell / 3][cell % 3], prefs)
        }
        val won = GameEngine.finish(s).first
        assertEquals(Status.WON, won.status)
        // entry 0 (CAT) has the revealed cell, so only... none of 0/3 count; give entry 1 a pickup to check the positive case
        assertEquals(emptyList(), GameEngine.earnedPickups(won))
        val other = won.copy(rules = Rules(pickups = mapOf(1 to PowerUpType.EXTRA_CHECK)))
        assertEquals(listOf(PowerUpType.EXTRA_CHECK), GameEngine.earnedPickups(other))
    }

    @Test
    fun `scrambled keyboard keeps every key`() {
        val normal = Keyboards.rows(Language.EN).joinToString("").toSet()
        val scrambled = Keyboards.rows(Language.EN, 99)
        assertEquals(normal, scrambled.joinToString("").toSet())
        assertEquals(Keyboards.rows(Language.EN).map { it.length }, scrambled.map { it.length })
    }
}
