package com.vanbank.core.finance

import java.time.LocalDate
import kotlin.math.pow
import kotlin.math.roundToLong

/** One line of a loan's repayment schedule. */
data class LoanInstallment(
    val number: Int,
    val dueDate: LocalDate,
    val principalMinor: Long,
    val interestMinor: Long,
    val totalDueMinor: Long,
    val remainingBalanceMinor: Long,
)

data class AmortizationSchedule(
    val principalMinor: Long,
    val annualRatePercent: Double,
    val termMonths: Int,
    val monthlyPaymentMinor: Long,
    val installments: List<LoanInstallment>,
) {
    val totalRepaymentMinor: Long get() = installments.sumOf { it.totalDueMinor }
    val totalInterestMinor: Long get() = totalRepaymentMinor - principalMinor
}

/**
 * Standard fixed-payment (annuity) amortization: equal monthly payments,
 * with the interest/principal split shifting over time as the balance
 * shrinks. Pure math, no persistence -- everything here is fake/simulated,
 * same as the rest of VANBank, but the arithmetic itself is real.
 */
object InterestCalculator {
    fun buildSchedule(
        principalMinor: Long,
        annualRatePercent: Double,
        termMonths: Int,
        startDate: LocalDate,
    ): AmortizationSchedule {
        require(principalMinor > 0) { "principal must be positive" }
        require(termMonths > 0) { "term must be positive" }
        require(annualRatePercent >= 0) { "rate can't be negative" }

        val monthlyRate = annualRatePercent / 100.0 / 12.0
        val paymentMinor = monthlyPayment(principalMinor, monthlyRate, termMonths)

        val installments = mutableListOf<LoanInstallment>()
        var remaining = principalMinor
        for (n in 1..termMonths) {
            val interestForPeriod = (remaining * monthlyRate).roundToLong()
            val isLast = n == termMonths
            // Last installment clears whatever remains exactly, absorbing rounding drift.
            val principalForPeriod = if (isLast) remaining else (paymentMinor - interestForPeriod).coerceAtMost(remaining)
            val totalForPeriod = principalForPeriod + interestForPeriod
            remaining -= principalForPeriod
            installments += LoanInstallment(
                number = n,
                dueDate = startDate.plusMonths(n.toLong()),
                principalMinor = principalForPeriod,
                interestMinor = interestForPeriod,
                totalDueMinor = totalForPeriod,
                remainingBalanceMinor = remaining,
            )
        }

        return AmortizationSchedule(
            principalMinor = principalMinor,
            annualRatePercent = annualRatePercent,
            termMonths = termMonths,
            monthlyPaymentMinor = paymentMinor,
            installments = installments,
        )
    }

    private fun monthlyPayment(principalMinor: Long, monthlyRate: Double, termMonths: Int): Long {
        if (monthlyRate == 0.0) return (principalMinor.toDouble() / termMonths).roundToLong()
        val factor = (1 + monthlyRate).pow(termMonths)
        val payment = principalMinor * monthlyRate * factor / (factor - 1)
        return payment.roundToLong()
    }
}
