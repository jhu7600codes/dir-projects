package app.stash.recs

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsTest {
    @Test
    fun parsesLrcWithSeveralStamps() {
        val lines = Lyrics.parseLrc("[00:12.50] hello\n[01:02.00][02:00.10] chorus\nnot a line\n[00:05.00]")
        assertEquals(listOf(5_000L to "", 12_500L to "hello", 62_000L to "chorus", 120_100L to "chorus"), lines)
    }
}
