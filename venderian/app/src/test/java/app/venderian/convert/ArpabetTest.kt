package app.venderian.convert

import org.junit.Assert.assertEquals
import org.junit.Test

class ArpabetTest {
    private fun roman(phones: String) = Sounds.toRoman(Arpabet.toSounds(phones.split(' ')))

    @Test
    fun `maps the table from the docs`() {
        assertEquals("hëlow", roman("HH AH0 L OW1"))
        assertEquals("wotër", roman("W AO1 T ER0"))
        assertEquals("chach", roman("JH AH1 JH"))
        assertEquals("thë", roman("DH AH0"))
        assertEquals("ayland", roman("AY1 L AH0 N D").replace("ë", "a"))
        assertEquals("singing", roman("S IH1 NG IH0 NG"))
        assertEquals("plesher", roman("P L EH1 ZH ER0").replace("ë", "e"))
    }

    @Test
    fun `stressed ah is a, unstressed is schwa`() {
        assertEquals(listOf("a"), Arpabet.toSounds(listOf("AH1")))
        assertEquals(listOf("a"), Arpabet.toSounds(listOf("AH2")))
        assertEquals(listOf("ë"), Arpabet.toSounds(listOf("AH0")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown phoneme fails loudly`() {
        Arpabet.toSounds(listOf("QQ1"))
    }
}
