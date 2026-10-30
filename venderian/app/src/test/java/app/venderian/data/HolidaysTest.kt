package app.venderian.data

import app.venderian.convert.Lexicon
import app.venderian.convert.WordLookup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.time.LocalDate

class HolidaysTest {
    private val lookup = WordLookup(Lexicon.parse(File("src/main/assets/dictionary.json").readText()).entries)

    private fun allVenderian(text: String) {
        for (w in text.split(Regex("[^a-zë]+")).filter { it.isNotEmpty() }) assertNotNull("$w in \"$text\"", lookup.analyze(w))
    }

    @Test
    fun dates() {
        assertEquals(Holiday.VENDERI_DAY, Holiday.on(LocalDate.of(2027, 7, 15)))
        assertEquals(Holiday.NEW_YEAR, Holiday.on(LocalDate.of(2027, 1, 1)))
        assertEquals(Holiday.CHRISTMAS, Holiday.on(LocalDate.of(2027, 1, 7)))
        assertEquals(Holiday.CHRISTMAS, Holiday.on(LocalDate.of(2027, 12, 25)))
        assertEquals(Holiday.HALLOWEEN, Holiday.on(LocalDate.of(2026, 10, 31)))
        assertNull(Holiday.on(LocalDate.of(2026, 10, 30)))
    }

    @Test
    fun `greetings and taglines are real venderian`() {
        Holiday.entries.forEach { allVenderian(it.greeting) }
        for (streak in 0..400) for (today in listOf(true, false)) allVenderian(Taglines.forStreak(streak, today).first)
    }
}
