package app.venderian.srs

import kotlin.math.max
import kotlin.math.roundToInt

enum class Grade { AGAIN, HARD, GOOD, EASY }

data class SrsState(
    val ease: Double = 2.5,
    val intervalDays: Int = 0,
    val reps: Int = 0,
    val lapses: Int = 0,
)

/** a small sm-2: intervals grow by the card's ease, a miss sends it back to the start. */
object Srs {
    const val MIN_EASE = 1.3
    const val AGAIN_DELAY_MS = 10 * 60 * 1000L
    const val DAY_MS = 24 * 60 * 60 * 1000L

    fun review(s: SrsState, grade: Grade): SrsState = when (grade) {
        Grade.AGAIN -> s.copy(
            ease = max(MIN_EASE, s.ease - 0.2), intervalDays = 0, reps = 0, lapses = s.lapses + 1,
        )
        Grade.HARD -> s.copy(
            ease = max(MIN_EASE, s.ease - 0.15),
            intervalDays = max(1, (s.intervalDays * 1.2).roundToInt()),
            reps = s.reps + 1,
        )
        Grade.GOOD -> s.copy(intervalDays = next(s, s.ease), reps = s.reps + 1)
        Grade.EASY -> s.copy(
            ease = s.ease + 0.15,
            intervalDays = if (s.reps == 0) 4 else max(next(s, s.ease) + 1, (next(s, s.ease) * 1.3).roundToInt()),
            reps = s.reps + 1,
        )
    }

    private fun next(s: SrsState, ease: Double) = when (s.reps) {
        0 -> 1
        1 -> 3
        else -> max(s.intervalDays + 1, (s.intervalDays * ease).roundToInt())
    }

    /** when a card reviewed at [now] with the new state [s] is due again */
    fun dueAt(now: Long, s: SrsState): Long =
        if (s.intervalDays == 0) now + AGAIN_DELAY_MS else now + s.intervalDays * DAY_MS
}

/** consecutive days with any xp, counting back from today (or yesterday, so the streak survives until tonight) */
object Streaks {
    fun current(days: Set<Long>, today: Long): Int {
        var day = if (today in days) today else today - 1
        var n = 0
        while (day in days) {
            n++
            day--
        }
        return n
    }

    fun longest(days: Set<Long>): Int {
        var best = 0
        for (d in days) {
            if (d - 1 in days) continue
            var n = 0
            while (d + n in days) n++
            best = max(best, n)
        }
        return best
    }
}
