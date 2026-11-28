package app.krala.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import kotlin.random.Random

class DaytimeTest {
    @Test
    fun neverAtNight() {
        val rnd = Random(42)
        val start = LocalDateTime.of(2026, 11, 28, 0, 0)
        for (i in 0 until 2_000) {
            val now = start.plusMinutes(rnd.nextLong(0, 7 * 24 * 60))
            for (mode in listOf("rare", "sometimes")) {
                val t = Daytime.next(now, mode, rnd)!!
                assertFalse("$now -> $t", Daytime.isNight(t))
                assertTrue(t.isAfter(now))
            }
        }
    }

    @Test
    fun offMeansNever() {
        assertNull(Daytime.next(LocalDateTime.now(), "off"))
    }

    @Test
    fun nightBoundaries() {
        assertTrue(Daytime.isNight(LocalDateTime.of(2026, 1, 1, 0, 0)))
        assertTrue(Daytime.isNight(LocalDateTime.of(2026, 1, 1, 6, 59)))
        assertFalse(Daytime.isNight(LocalDateTime.of(2026, 1, 1, 7, 0)))
        assertFalse(Daytime.isNight(LocalDateTime.of(2026, 1, 1, 23, 59)))
    }
}
