package com.vanbank.core.finance

/**
 * Quotes a fake but deterministic interest rate for a loan request, based
 * on how much is borrowed and for how long -- bigger and longer loans quote
 * higher, the way a real risk-based pricing model would, without any of
 * the actual underwriting.
 */
object LoanRateEngine {
    private const val BASE_RATE_PERCENT = 6.5
    private const val MIN_RATE_PERCENT = 4.9
    private const val MAX_RATE_PERCENT = 24.9

    fun quoteAnnualRatePercent(principalMinor: Long, termMonths: Int): Double {
        require(principalMinor > 0)
        require(termMonths > 0)

        val principalRubles = principalMinor / 100.0
        val amountLoading = (principalRubles / 100_000.0 * 1.5).coerceAtMost(6.0)
        val termLoading = (termMonths / 12.0 * 0.5).coerceAtMost(6.0)

        val rate = BASE_RATE_PERCENT + amountLoading + termLoading
        return rate.coerceIn(MIN_RATE_PERCENT, MAX_RATE_PERCENT)
            .let { Math.round(it * 100.0) / 100.0 } // round to 2 decimals
    }
}
