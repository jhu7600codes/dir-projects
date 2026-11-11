package com.vanbank.core.numbers

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CardNumberGeneratorTest {
    @Test
    fun `generated numbers start with DIR's prefix 8`() {
        repeat(50) {
            val number = CardNumberGenerator.generate(random = Random(it))
            assertTrue(number.startsWith("8"))
        }
    }

    @Test
    fun `generated numbers are 16 digits`() {
        val number = CardNumberGenerator.generate()
        assertEquals(16, number.length)
        assertTrue(number.all(Char::isDigit))
    }

    @Test
    fun `generated numbers pass Luhn validation`() {
        repeat(100) {
            val number = CardNumberGenerator.generate(random = Random(it))
            assertTrue(Luhn.isValid(number), "expected $number to be Luhn-valid")
        }
    }

    @Test
    fun `isDirCard is true only for valid DIR numbers`() {
        val dirNumber = CardNumberGenerator.generate(random = Random(7))
        assertTrue(CardNumberGenerator.isDirCard(dirNumber))
        assertTrue(!CardNumberGenerator.isDirCard("4111111111111111")) // Visa prefix
    }

    @Test
    fun `formatMasked hides everything but the last four digits`() {
        val masked = CardNumberGenerator.formatMasked("8123456789012345")
        assertEquals("•••• •••• •••• 2345", masked)
    }

    @Test
    fun `cvv is always three digits`() {
        repeat(50) {
            val cvv = CardNumberGenerator.generateCvv(random = Random(it))
            assertEquals(3, cvv.length)
        }
    }
}
