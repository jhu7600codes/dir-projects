package app.venderian.convert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class WordLookupTest {
    private val lookup = WordLookup(Lexicon.parse(File("src/main/assets/dictionary.json").readText()).entries)

    private fun parts(token: String) = lookup.analyze(token)?.let { listOf(it.entry.word) + it.endings.map { e -> e.word } }

    @Test
    fun `takes endings off`() {
        assertEquals(listOf("gol"), parts("gol"))
        assertEquals(listOf("gol", "-ta"), parts("golta"))
        assertEquals(listOf("kitë", "-n"), parts("kitën"))
        assertEquals(listOf("vuf", "-an"), parts("Vufan!"))
        assertEquals(listOf("gran", "-esh"), parts("granesh"))
        assertEquals(listOf("luda", "-ri", "-n"), parts("ludarin"))
        assertEquals(listOf("pot", "-ta"), parts("potta"))
        assertNull(lookup.analyze("blorptax"))
    }

    @Test
    fun `search by english, roman, inflected form or script`() {
        assertEquals("kar", lookup.search("house").first().word)
        assertEquals("vuf", lookup.search("dog").first().word)
        assertEquals("kitë", lookup.search("kitën").first().word)
        assertEquals("vela", lookup.search(Sounds.scriptOf("vela")).first().word)
        assertEquals("sipa", lookup.search("drink").first().word)
        assertEquals(lookup.search("").size, lookup.search("   ").size)
    }
}
