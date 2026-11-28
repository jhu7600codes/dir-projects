package app.krala.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThinkTest {
    @Test
    fun splitsThinkTags() {
        val s = Think.split("<think>hmm, pizza or sushi</think>\n\nsushi, obviously")
        assertEquals("hmm, pizza or sushi", s.thinking)
        assertEquals("sushi, obviously", s.answer)
        assertTrue(s.thinkingDone)
    }

    @Test
    fun unclosedWhileStreaming() {
        val s = Think.split("<think>still going")
        assertEquals("still going", s.thinking)
        assertEquals("", s.answer)
        assertFalse(s.thinkingDone)
    }

    @Test
    fun plainRepliesUntouched() {
        assertEquals("hi <think> in the middle", Think.answer("hi <think> in the middle"))
    }

    @Test
    fun reasoningFieldIsReadSeparately() {
        assertEquals("let me think", ChoiceContent.reasoningFromChunk("""{"choices":[{"delta":{"reasoning_content":"let me think"}}]}"""))
        assertEquals("r", ChoiceContent.reasoningFromChunk("""{"choices":[{"delta":{"reasoning":"r","content":"x"}}]}"""))
        assertEquals(null, ChoiceContent.fromChunk("""{"choices":[{"delta":{"reasoning_content":"let me think"}}]}"""))
    }
}
