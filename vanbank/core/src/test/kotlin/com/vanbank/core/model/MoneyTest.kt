package com.vanbank.core.model

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyTest {
    @Test
    fun `format renders thousands separators and two decimals`() {
        assertEquals("₽1,234.56", Money.format(123_456))
        assertEquals("₽0.00", Money.format(0))
        assertEquals("₽5.00", Money.format(500))
    }

    @Test
    fun `format handles negative amounts with a leading minus`() {
        assertEquals("-₽42.00", Money.format(-4200))
    }

    @Test
    fun `formatSigned prefixes plus for incoming and minus for outgoing`() {
        assertEquals("+₽100.00", Money.formatSigned(10_000, TransactionDirection.IN))
        assertEquals("-₽100.00", Money.formatSigned(10_000, TransactionDirection.OUT))
    }

    @Test
    fun `rublesToMinor and minorToRubles round-trip`() {
        val minor = Money.rublesToMinor(BigDecimal("1234.56"))
        assertEquals(123456L, minor)
        assertEquals(BigDecimal("1234.56"), Money.minorToRubles(minor))
    }

    @Test
    fun `parseToMinor accepts grouped input and rejects garbage`() {
        assertEquals(123456L, Money.parseToMinor("1,234.56"))
        assertEquals(100L, Money.parseToMinor("1"))
        assertNull(Money.parseToMinor("not-a-number"))
        assertNull(Money.parseToMinor(""))
    }
}
