package app.stash.recs

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsTest {
    @Test
    fun parsesLrcWithSeveralStamps() {
        val lines = Lyrics.parseLrc("[00:12.50] hello\n[01:02.00][02:00.10] chorus\nnot a line\n[00:05.00]")
        assertEquals(listOf(5_000L to "", 12_500L to "hello", 62_000L to "chorus", 120_100L to "chorus"), lines)
    }

    @Test
    fun `musixmatch token from debug info or bare`() {
        val tok = "26102673bb286cead8fd088c6bc69e04013a2bc05f400742279376"
        org.junit.Assert.assertEquals(tok, Musixmatch.parseToken("---- Debug Info ----\n[Device]: iPhone\n[UserToken]: $tok\n[Other]: x"))
        org.junit.Assert.assertEquals(tok, Musixmatch.parseToken("  $tok \n"))
        org.junit.Assert.assertNull(Musixmatch.parseToken("hello"))
    }
}
