package app.venderian.convert

import org.junit.Assert.assertEquals
import org.junit.Test

class LetterRulesTest {
    private fun roman(word: String) = Sounds.toRoman(LetterRules.sounds(word))

    @Test
    fun `common spellings`() {
        assertEquals("fon", roman("phone"))
        assertEquals("nayt", roman("night"))
        assertEquals("shak", roman("shack"))
        assertEquals("kwiz", roman("quiz"))
        assertEquals("siti", roman("city"))
        assertEquals("yelow", roman("yellow"))
        assertEquals("nit", roman("knit"))
        assertEquals("kach", roman("catch"))
        assertEquals("boks", roman("box"))
        assertEquals("chat", roman("chat"))
    }

    @Test
    fun `made up words still come out`() {
        assertEquals("zorblaks", roman("Zorblax"))
        assertEquals("glorp", roman("glorp"))
    }

    @Test
    fun `ignores non letters`() {
        assertEquals(emptyList<String>(), LetterRules.sounds("123"))
    }
}
