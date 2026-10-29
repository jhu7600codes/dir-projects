package app.venderian.convert

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class SoundsTest {
    @Test
    fun `table matches docs alphabet json`() {
        val json = Json.parseToJsonElement(File("../docs/alphabet.json").readText()).jsonObject
        for (group in listOf("consonants", "vowels")) {
            for (letter in json.getValue(group).jsonArray) {
                val id = letter.jsonObject.getValue("id").jsonPrimitive.content
                val cp = letter.jsonObject.getValue("cp").jsonPrimitive.content.toInt(16)
                assertEquals("codepoint of $id", cp, Sounds.glyph(id).code)
            }
        }
        assertEquals(28, Sounds.all.size)
    }

    @Test
    fun `parses digraphs greedily`() {
        assertEquals(listOf("sh", "e", "v", "a", "n"), Sounds.parseRoman("shevan"))
        assertEquals(listOf("o", "kh"), Sounds.parseRoman("okh"))
        assertEquals(listOf("th", "e", "l", "o", "r"), Sounds.parseRoman("thelor"))
        assertEquals(listOf("k", "i", "t", "ë"), Sounds.parseRoman("kitë"))
    }

    @Test
    fun `apostrophe splits a digraph and roman round trips`() {
        assertEquals(listOf("n", "g"), Sounds.parseRoman("n'g"))
        val tricky = listOf("a", "n", "g", "a", "t", "h", "o", "s", "h", "i", "n", "ng")
        val roman = Sounds.toRoman(tricky)
        assertEquals("an'gat'hos'hinng", roman)
        assertEquals(tricky, Sounds.parseRoman(roman))
    }

    @Test
    fun `script and back`() {
        val script = Sounds.toScript(Sounds.parseRoman("vela"))
        assertEquals("", script)
        assertEquals("vela mi.", Sounds.scriptToRoman(script + " " + Sounds.toScript(listOf("m", "i")) + "."))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects letters venderian does not have`() {
        Sounds.parseRoman("jump")
    }
}
