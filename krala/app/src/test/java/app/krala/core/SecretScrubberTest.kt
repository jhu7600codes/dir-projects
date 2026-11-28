package app.krala.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretScrubberTest {
    private fun scrubbed(s: String) = SecretScrubber.scrub(s)

    @Test
    fun removesApiKeys() {
        listOf(
            "my key is sk-proj-abcdefghijklmnopqrstuvwxyz123456",
            "gsk_abcdefghijklmnopqrstuvwxyz12",
            "AIzaSyA1234567890abcdefghijklmnopqrstu",
            "ghp_abcdefghijklmnopqrstuvwxyz0123456789",
            "nvapi-abcdefghijklmnopqrstuvwxyz",
            "Authorization: Bearer abcdefghijklmnopqrstuvwxyz.123",
        ).forEach { assertTrue(it, scrubbed(it).contains(SecretScrubber.MASK)) }
    }

    @Test
    fun removesPasswords() {
        assertEquals("my password is ${SecretScrubber.MASK}", scrubbed("my password is hunter22"))
        assertTrue(scrubbed("мой пароль: qwerty123").contains(SecretScrubber.MASK))
        assertFalse(scrubbed("мой пароль: qwerty123").contains("qwerty123"))
    }

    @Test
    fun removesValidCardNumbersOnly() {
        assertTrue(scrubbed("card 4111 1111 1111 1111 exp").contains(SecretScrubber.MASK))
        // not luhn valid: a phone number or order id stays
        assertEquals("order 1234 5678 9012 3456", scrubbed("order 1234 5678 9012 3456"))
    }

    @Test
    fun leavesNormalTextAlone() {
        val s = "user likes pizza, has a cat named borscht, plans a trip to kazan on 12 may"
        assertEquals(s, scrubbed(s))
    }

    @Test
    fun luhn() {
        assertTrue(SecretScrubber.luhn("4111111111111111"))
        assertFalse(SecretScrubber.luhn("4111111111111112"))
    }
}
