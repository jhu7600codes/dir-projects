package com.vanbank.core.finance

import kotlin.test.Test
import kotlin.test.assertTrue

class LoanRateEngineTest {
    @Test
    fun `larger principal quotes a higher or equal rate for the same term`() {
        val small = LoanRateEngine.quoteAnnualRatePercent(10_000_00, 12)
        val large = LoanRateEngine.quoteAnnualRatePercent(500_000_00, 12)
        assertTrue(large >= small)
    }

    @Test
    fun `longer term quotes a higher or equal rate for the same principal`() {
        val short = LoanRateEngine.quoteAnnualRatePercent(50_000_00, 6)
        val long = LoanRateEngine.quoteAnnualRatePercent(50_000_00, 60)
        assertTrue(long >= short)
    }

    @Test
    fun `rate always stays within the published bounds`() {
        val cases = listOf(
            LoanRateEngine.quoteAnnualRatePercent(1_00, 1),
            LoanRateEngine.quoteAnnualRatePercent(100_000_000_00, 360),
        )
        cases.forEach {
            assertTrue(it in 4.9..24.9, "rate $it out of bounds")
        }
    }

    @Test
    fun `quote is deterministic for the same inputs`() {
        val a = LoanRateEngine.quoteAnnualRatePercent(75_000_00, 24)
        val b = LoanRateEngine.quoteAnnualRatePercent(75_000_00, 24)
        assertTrue(a == b)
    }
}
