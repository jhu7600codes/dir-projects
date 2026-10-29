package app.venderian.convert

import app.venderian.convert.Converter.Mode.TRANSLATE
import app.venderian.convert.Converter.Source
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ConverterTest {
    private val dict = Pronunciations { word ->
        mapOf(
            "hello" to "HH AH0 L OW1", "world" to "W ER1 L D", "pizza" to "P IY1 T S AH0",
            "i" to "AY1", "like" to "L AY1 K", "book" to "B UH1 K", "keeper" to "K IY1 P ER0",
            "bookkeeper" to "B UH1 K K IY2 P ER0",
        )[word]?.split(' ')
    }
    private val lexicon = Lexicon.parse(File("src/main/assets/dictionary.json").readText())
    private val converter = Converter(dict, lexicon)

    private fun script(roman: String) = roman.split(' ').joinToString(" ") { Sounds.toScript(Sounds.parseRoman(it)) }

    @Test
    fun `sound mode spells every word`() {
        val r = converter.convert("Hello, world!")
        assertEquals("hëlow, wërld!", r.roman)
        assertEquals(script("hëlow") + ", " + script("wërld") + "!", r.script)
        assertEquals(listOf(Source.CMUDICT, Source.PUNCTUATION, Source.CMUDICT, Source.PUNCTUATION), r.pieces.map { it.source })
    }

    @Test
    fun `unknown words fall back to letter rules`() {
        val r = converter.convert("glorp")
        assertEquals("glorp", r.roman)
        assertEquals(Source.RULES, r.pieces.single().source)
    }

    @Test
    fun `numbers and spacing`() {
        assertEquals("ay layk 2026 bukkipër.", converter.convert("I   like 2026 bookkeeper.").roman)
        assertEquals("ay layk 2026 bukkipër.", Sounds.scriptToRoman(converter.convert("I   like 2026 bookkeeper.").script))
        assertEquals("", converter.convert("  ").roman)
        assertEquals("hëlow, hëlow", converter.convert("hello;  hello").roman)
    }

    @Test
    fun `curly apostrophes count as apostrophes`() {
        assertEquals(converter.convert("don't").roman, converter.convert("don’t").roman)
    }

    @Test
    fun `translate uses the dictionary and spells the rest`() {
        assertEquals("mi sipa nami", converter.convert("I drink water", TRANSLATE).roman)
        val r = converter.convert("I like pizza", TRANSLATE)
        assertEquals("mi goda pitsë", r.roman)
        assertEquals(listOf(Source.LEXICON, Source.LEXICON, Source.CMUDICT), r.pieces.map { it.source })
    }

    @Test
    fun `translate inflects`() {
        assertEquals("vufan", converter.convert("the dogs", TRANSLATE).roman)
        assertEquals("kitën", converter.convert("books", TRANSLATE).roman)
        assertEquals("human", converter.convert("people", TRANSLATE).roman)
        assertEquals("mi pasata", converter.convert("I walked", TRANSLATE).roman)
        assertEquals("mi yamata", converter.convert("I ate", TRANSLATE).roman)
        assertEquals("tu yamava ka?", converter.convert("you will eat ka?", TRANSLATE).roman)
        assertEquals("da mi kar", converter.convert("my house", TRANSLATE).roman)
        assertEquals("mi ne goda", converter.convert("I don't like", TRANSLATE).roman)
    }

    @Test
    fun `translate knows phrases`() {
        assertEquals("shevan!", converter.convert("Thank you!", TRANSLATE).roman)
        assertEquals("vela, ishe da mi ana.", converter.convert("Hello, the name of me ana.", TRANSLATE).roman.replace("anë", "ana"))
    }

    @Test
    fun `lexicon plural rule`() {
        assertEquals("karan", Lexicon.plural("kar"))
        assertEquals("namin", Lexicon.plural("nami"))
        assertEquals("kitën", Lexicon.plural("kitë"))
    }
}
