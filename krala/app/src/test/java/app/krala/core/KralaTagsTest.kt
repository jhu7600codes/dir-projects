package app.krala.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KralaTagsTest {
    @Test
    fun buildsTheExactLayoutWithEmptySections() {
        val block = KralaTags.build(KralaTags.Block(memory = "", systemPrompt = "be nice", userInstructions = ""))
        assertEquals(
            "--krala tags--\n--memory--\n--system prompt--\nbe nice\n--user instructions--\n--krala tags end--",
            block,
        )
    }

    @Test
    fun memoryLinesUseChatNameBrackets() {
        val mem = KralaTags.formatMemory(listOf(KralaTags.MemoryLine("pizza talk", "likes pineapple"), KralaTags.MemoryLine("a [weird] name", "x")))
        assertEquals("[pizza talk] likes pineapple\n[a (weird) name] x", mem)
    }

    @Test
    fun composePrependsTheBlock() {
        assertEquals("BLOCK\n\nhi", KralaTags.compose("BLOCK", "hi"))
        assertEquals("hi", KralaTags.compose(null, "hi"))
    }

    @Test
    fun parsesCaseInsensitively() {
        val text = "--KRALA TAGS--\n--Memory--\n[a] b\n--SYSTEM PROMPT--\nyou are krala\n--user Instructions--\ncall me jhu\n--Krala Tags End--\n\nhello"
        val b = KralaTags.parse(text)
        assertNotNull(b)
        assertEquals("[a] b", b!!.memory)
        assertEquals("you are krala", b.systemPrompt)
        assertEquals("call me jhu", b.userInstructions)
        assertNull(b.recall)
    }

    @Test
    fun stripRemovesBlocksAndEchoes() {
        val echoed = "--Krala Tags--\n--memory--\nsecret notes\n--krala tags end--\nhey there"
        assertEquals("hey there", KralaTags.strip(echoed))
        assertEquals("partial", KralaTags.strip("partial\n--krala tags--\n--memory--\nhalf streamed"))
        assertEquals("plain text", KralaTags.strip("plain text"))
        assertFalse(KralaTags.strip("a\n--recall--\nb").contains("--recall--"))
    }

    @Test
    fun recallGoesInsideAnExistingBlock() {
        val first = KralaTags.compose(KralaTags.build(KralaTags.Block("", "p", "")), "question")
        val withRecall = KralaTags.withRecall(first, "[old chat] excerpt")
        val parsed = KralaTags.parse(withRecall)!!
        assertEquals("[old chat] excerpt", parsed.recall)
        assertEquals("p", parsed.systemPrompt)
        assertTrue(withRecall.endsWith("question"))
        assertEquals(1, Regex("--krala tags--").findAll(withRecall).count())
    }

    @Test
    fun recallOnlyBlockForLaterMessages() {
        val out = KralaTags.withRecall("later message", "[x] y")
        assertTrue(out.startsWith("--krala tags--\n--recall--\n[x] y\n--krala tags end--"))
        assertTrue(out.endsWith("later message"))
    }

    @Test
    fun updatePromptsKeepsMemory() {
        val prefix = KralaTags.build(KralaTags.Block("[a] b", "old", "old i"))
        val updated = KralaTags.parse(KralaTags.updatePrompts(prefix, "new", "new i"))!!
        assertEquals("[a] b", updated.memory)
        assertEquals("new", updated.systemPrompt)
        assertEquals("new i", updated.userInstructions)
    }
}
