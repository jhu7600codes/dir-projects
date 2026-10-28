package dev.jhu.crossi.game

import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProgressionTest {
    @Test
    fun `levels get bigger and bosses come every 10`() {
        var last = 0
        for (n in 1..LevelCatalog.COUNT) {
            val spec = LevelCatalog.spec(n)
            assertTrue(spec.size >= last)
            last = spec.size
            assertEquals(n % LevelCatalog.BOSS_EVERY == 0, spec.isBoss, "level $n")
        }
        assertEquals(5, LevelCatalog.spec(1).size)
        assertEquals(15, LevelCatalog.spec(100).size)
    }

    @Test
    fun `streak grows on consecutive days and resets after a gap`() {
        val d = LocalDate.of(2026, 10, 1)
        var s = Streaks.onDailyCleared(Streak(), d)
        s = Streaks.onDailyCleared(s, d) // same day twice
        assertEquals(1, s.current)
        s = Streaks.onDailyCleared(s, d.plusDays(1))
        s = Streaks.onDailyCleared(s, d.plusDays(2))
        assertEquals(3, s.current)
        assertEquals(3, Streaks.displayed(s, d.plusDays(3)))
        assertEquals(0, Streaks.displayed(s, d.plusDays(4)))
        s = Streaks.onDailyCleared(s, d.plusDays(5))
        assertEquals(1, s.current)
        assertEquals(3, s.best)
    }

    @Test
    fun `streak milestones pay out`() {
        assertTrue(Rewards.streakMilestone(3)!!.isNotEmpty())
        assertTrue(Rewards.streakMilestone(4) == null)
        assertTrue(Rewards.streakMilestone(60)!!.size == 8)
        assertEquals(7, Rewards.nextMilestone(3))
        assertEquals(60, Rewards.nextMilestone(30))
    }

    @Test
    fun `run thresholds give one reward each`() {
        assertEquals(2, Rewards.runRewards(1_000, 6_000).size)
        assertEquals(0, Rewards.runRewards(6_000, 7_000).size)
    }

    @Test
    fun `daily seeds are the same for everyone and differ by day and difficulty`() {
        val a = Seeds.daily("2026-10-28", Difficulty.HARD, Language.EN, 1)
        assertEquals(a, Seeds.daily("2026-10-28", Difficulty.HARD, Language.EN, 1))
        assertTrue(a != Seeds.daily("2026-10-29", Difficulty.HARD, Language.EN, 1))
        assertTrue(a != Seeds.daily("2026-10-28", Difficulty.EASY, Language.EN, 1))
    }
}
