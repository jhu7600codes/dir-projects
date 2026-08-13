package com.vanbank.core.numbers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LuhnTest {
    @Test
    fun `known valid number passes`() {
        // A well-known test PAN with a valid Luhn checksum.
        assertTrue(Luhn.isValid("4111111111111111"))
    }

    @Test
    fun `tampering with a digit fails validation`() {
        assertFalse(Luhn.isValid("4111111111111112"))
    }

    @Test
    fun `checkDigitFor produces a digit that validates`() {
        val payload = "811122223333444"
        val checkDigit = Luhn.checkDigitFor(payload)
        assertTrue(Luhn.isValid(payload + checkDigit))
    }

    @Test
    fun `non-digit input is invalid`() {
        assertFalse(Luhn.isValid("abcd"))
        assertFalse(Luhn.isValid(""))
    }
}
