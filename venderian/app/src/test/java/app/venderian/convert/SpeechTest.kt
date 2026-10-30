package app.venderian.convert

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechTest {
    @Test
    fun spanish() {
        assertEquals("vela, ana!", Speech.spanish("vela, ana!"))
        assertEquals("quiten", Speech.spanish("kitën"))
        assertEquals("ojo", Speech.spanish("okho"))
        assertEquals("nai", Speech.spanish("nay"))
        assertEquals("gueraus", Speech.spanish("gëraws"))
        assertEquals("zelor", Speech.spanish("thelor"))
        assertEquals("yama", Speech.spanish("yama"))
    }

    @Test
    fun english() {
        assertEquals("vehlah", Speech.english("vela"))
        assertEquals("keetuh", Speech.english("kitë"))
        assertEquals("nay", Speech.english("nay"))
    }
}
