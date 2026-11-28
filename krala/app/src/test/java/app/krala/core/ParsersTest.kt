package app.krala.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParsersTest {
    @Test
    fun onlyChoicesZeroMessageContent() {
        val body = """{"id":"x","choices":[{"message":{"role":"assistant","content":"hi","reasoning_content":"secret thoughts"}},{"message":{"content":"second"}}],"usage":{"total_tokens":3}}"""
        assertEquals("hi", ChoiceContent.fromResponse(body))
        assertNull(ChoiceContent.fromResponse("""{"choices":[]}"""))
        assertNull(ChoiceContent.fromResponse("""{"choices":[{"message":{"content":null,"tool_calls":[]}}]}"""))
    }

    @Test
    fun streamChunksReadDeltaContentOnly() {
        assertEquals("he", ChoiceContent.fromChunk("""{"choices":[{"delta":{"content":"he","reasoning":"no"}}]}"""))
        assertNull(ChoiceContent.fromChunk("""{"choices":[{"delta":{"reasoning_content":"thinking"}}]}"""))
        assertNull(ChoiceContent.fromChunk("""{"choices":[{"delta":{"role":"assistant"}}]}"""))
    }

    @Test
    fun errorMessages() {
        assertEquals("bad key", ChoiceContent.errorMessage("""{"error":{"message":"bad key","code":401}}"""))
        assertEquals("nope", ChoiceContent.errorMessage("""[{"error":{"message":"nope"}}]"""))
    }

    @Test
    fun sseLines() {
        val p = SseLineParser()
        assertNull(p.feed(": keep alive"))
        assertEquals("""{"a":1}""", p.feed("""data: {"a":1}"""))
        assertNull(p.feed(""))
        assertEquals("[DONE]", p.feed("data: [DONE]"))
        // a json object split over two data lines
        assertNull(p.feed("""data: {"a":"""))
        assertEquals("{\"a\":\n2}", p.feed("data: 2}"))
    }

    @Test
    fun recallTerms() {
        val t = RecallQuery.terms("hey krala do you remember my cat borscht and the trip to kazan?")
        assertTrue("borscht" in t)
        assertTrue("kazan" in t)
        assertTrue("krala" !in t && "remember" !in t && "the" !in t)
        assertEquals("\"borsch\"* OR \"kaza\"* OR \"cat\"", RecallQuery.fts(listOf("borscht", "kazan", "cat")))
        assertNull(RecallQuery.fts(emptyList()))
    }

    @Test
    fun markdownBlocks() {
        val md = "# title\n\nsome **bold** text\n\n- one\n- [x] two\n\n```kotlin\nval x = 1\n```\n\n> quote\n\n| a | b |\n|---|---|\n| 1 | 2 |\n\n---\n1. first"
        val blocks = Markdown.parse(md)
        assertTrue(blocks[0] is Markdown.Block.Heading)
        assertTrue(blocks[1] is Markdown.Block.Paragraph)
        assertTrue(blocks[2] is Markdown.Block.ListItem)
        assertEquals(true, (blocks[3] as Markdown.Block.ListItem).checked)
        val code = blocks[4] as Markdown.Block.Code
        assertEquals("kotlin", code.language)
        assertEquals("val x = 1", code.code)
        assertTrue(code.closed)
        assertTrue(blocks[5] is Markdown.Block.Quote)
        val table = blocks[6] as Markdown.Block.Table
        assertEquals(listOf("a", "b"), table.header)
        assertEquals(listOf(listOf("1", "2")), table.rows)
        assertTrue(blocks[7] is Markdown.Block.Rule)
        assertTrue((blocks[8] as Markdown.Block.ListItem).ordered)
    }

    @Test
    fun unclosedFenceWhileStreaming() {
        val blocks = Markdown.parse("look:\n```py\nprint(1)\nprint(2")
        val code = blocks.last() as Markdown.Block.Code
        assertEquals("print(1)\nprint(2", code.code)
        assertTrue(!code.closed)
    }

    @Test
    fun inlineSpans() {
        val spans = Markdown.inline("a **b** *c* `d` [e](https://x.y) ~~f~~ snake_case_word")
        assertEquals("a b c d e f snake_case_word", Markdown.plain(spans))
        assertTrue(spans.any { it is Markdown.Span.InlineCode })
        assertTrue(spans.any { it is Markdown.Span.Link && it.url == "https://x.y" })
    }
}
