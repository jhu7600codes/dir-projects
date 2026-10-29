package app.venderian.srs

import app.venderian.data.Progress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SrsTest {
    @Test
    fun `good answers grow the interval`() {
        var s = SrsState()
        val intervals = List(5) { s = Srs.review(s, Grade.GOOD); s.intervalDays }
        assertEquals(listOf(1, 3, 8, 20, 50), intervals)
    }

    @Test
    fun `again resets and lowers ease, never below the minimum`() {
        var s = SrsState(intervalDays = 30, reps = 5)
        repeat(20) { s = Srs.review(s, Grade.AGAIN) }
        assertEquals(0, s.intervalDays)
        assertEquals(0, s.reps)
        assertEquals(20, s.lapses)
        assertEquals(Srs.MIN_EASE, s.ease, 1e-9)
    }

    @Test
    fun `hard grows slowly, easy grows fast`() {
        val base = SrsState(intervalDays = 10, reps = 3)
        val hard = Srs.review(base, Grade.HARD).intervalDays
        val good = Srs.review(base, Grade.GOOD).intervalDays
        val easy = Srs.review(base, Grade.EASY).intervalDays
        assertTrue("$hard < $good < $easy", hard < good && good < easy)
        assertEquals(4, Srs.review(SrsState(), Grade.EASY).intervalDays)
    }

    @Test
    fun `missed cards come back in minutes`() {
        val now = 1_000_000L
        assertEquals(now + Srs.AGAIN_DELAY_MS, Srs.dueAt(now, Srs.review(SrsState(), Grade.AGAIN)))
        assertEquals(now + 3 * Srs.DAY_MS, Srs.dueAt(now, SrsState(intervalDays = 3)))
    }

    @Test
    fun streaks() {
        assertEquals(3, Streaks.current(setOf(8, 9, 10), today = 10))
        assertEquals(3, Streaks.current(setOf(8, 9, 10), today = 11)) // not lost until the day is over
        assertEquals(0, Streaks.current(setOf(8, 9, 10), today = 12))
        assertEquals(0, Streaks.current(emptySet(), today = 12))
        assertEquals(4, Streaks.longest(setOf(1, 2, 3, 4, 7, 8, 10)))
    }

    @Test
    fun levels() {
        assertEquals(1, Progress(totalXp = 0).level)
        assertEquals(1, Progress(totalXp = 99).level)
        assertEquals(2, Progress(totalXp = 100).level)
        assertEquals(220, Progress.xpFor(3))
        assertEquals(0.5f, Progress(totalXp = 160).levelProgress, 1e-6f)
    }
}
