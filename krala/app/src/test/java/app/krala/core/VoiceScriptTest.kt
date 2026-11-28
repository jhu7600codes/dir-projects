package app.krala.core

import app.krala.core.VoiceScript.LaughKind
import app.krala.core.VoiceScript.Segment
import app.krala.core.VoiceScript.Tone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceScriptTest {
    @Test
    fun laughsTonesAndPauses() {
        val r = VoiceScript.parse("ok [laughs] that's wild... [whisper] i stole it")
        assertEquals(
            listOf(
                Segment.Speak("ok", Tone.NEUTRAL),
                Segment.Laugh(LaughKind.LAUGH),
                Segment.Speak("that's wild", Tone.NEUTRAL),
                Segment.Pause(380),
                Segment.Speak("i stole it", Tone.WHISPER),
            ),
            r.segments,
        )
        assertEquals(Tone.WHISPER, r.endTone)
    }

    @Test
    fun toneCarriesAcrossChunks() {
        val first = VoiceScript.parse("[excited] yes.")
        val second = VoiceScript.parse("still hyped", first.endTone)
        assertEquals(Segment.Speak("still hyped", Tone.EXCITED), second.segments.single())
    }

    @Test
    fun heldAndHypeWords() {
        val s = VoiceScript.parse("sooo good, that is AMAZING").segments
        assertEquals(Segment.Speak("so", Tone.HOLD), s[0])
        assertEquals(Segment.Speak("good, that is", Tone.NEUTRAL), s[1])
        assertEquals(Segment.Speak("amazing", Tone.HYPE), s[2])
        assertEquals(Segment.Speak("ну", Tone.HOLD), VoiceScript.parse("нууу").segments.single())
    }

    @Test
    fun unknownDirectionsAreNotReadAloud() {
        val s = VoiceScript.parse("hi *waves dramatically* there").segments
        assertEquals(listOf(Segment.Speak("hi there", Tone.NEUTRAL)), s)
    }

    @Test
    fun cleansMarkdownLinksAndEmoji() {
        val t = VoiceScript.clean("## title\n- **bold** item\n[site](https://x.y) and `code` 😀")
        assertFalse(t.contains("#") || t.contains("*") || t.contains("http") || t.contains("😀"))
        assertTrue(t.contains("bold item") && t.contains("site") && t.contains("code"))
    }

    @Test
    fun sentenceEnds() {
        assertEquals(4, VoiceScript.sentenceEnd("hey. how are", 0))
        assertEquals(-1, VoiceScript.sentenceEnd("no end yet", 0))
        assertEquals(-1, VoiceScript.sentenceEnd("done.", 0))
        assertEquals(9, VoiceScript.sentenceEnd("one. two! three", 0))
    }

    @Test
    fun language() {
        assertTrue(VoiceScript.isCyrillic("привет как дела"))
        assertFalse(VoiceScript.isCyrillic("hello there"))
    }
}
