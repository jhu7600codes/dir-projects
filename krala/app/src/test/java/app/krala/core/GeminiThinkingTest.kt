package app.krala.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeminiThinkingTest {
    @Test
    fun gemini3UsesLevels() {
        assertEquals(mapOf("include_thoughts" to true, "thinking_level" to "low"), GeminiThinking.config("gemini-3.1-flash-lite-preview", "fast"))
        assertEquals(mapOf("include_thoughts" to true), GeminiThinking.config("gemini-3.1-flash-lite-preview", "balanced"))
        assertEquals("high", GeminiThinking.config("gemini-3-pro-preview", "deep")!!["thinking_level"])
    }

    @Test
    fun gemini25UsesBudgets() {
        assertEquals(512, GeminiThinking.config("gemini-2.5-flash", "fast")!!["thinking_budget"])
        assertEquals(128, GeminiThinking.config("gemini-2.5-pro", "fast")!!["thinking_budget"])
        assertEquals(-1, GeminiThinking.config("gemini-2.5-flash", "deep")!!["thinking_budget"])
    }

    @Test
    fun oldOrOtherModels() {
        assertNull(GeminiThinking.config("gemini-2.0-flash", "fast"))
        assertNull(GeminiThinking.config("llama-3.3-70b", "fast"))
    }
}
