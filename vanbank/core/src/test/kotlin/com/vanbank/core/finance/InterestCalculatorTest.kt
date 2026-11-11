package com.vanbank.core.finance

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InterestCalculatorTest {
    @Test
    fun `schedule has exactly termMonths installments`() {
        val schedule = InterestCalculator.buildSchedule(
            principalMinor = 100_000_00, // ₽100,000
            annualRatePercent = 12.0,
            termMonths = 12,
            startDate = LocalDate.of(2026, 1, 1),
        )
        assertEquals(12, schedule.installments.size)
    }

    @Test
    fun `final installment pays the loan down to exactly zero`() {
        val schedule = InterestCalculator.buildSchedule(
            principalMinor = 50_000_00,
            annualRatePercent = 9.5,
            termMonths = 24,
            startDate = LocalDate.of(2026, 3, 15),
        )
        assertEquals(0L, schedule.installments.last().remainingBalanceMinor)
    }

    @Test
    fun `total repayment equals principal plus total interest`() {
        val schedule = InterestCalculator.buildSchedule(
            principalMinor = 20_000_00,
            annualRatePercent = 15.0,
            termMonths = 6,
            startDate = LocalDate.of(2026, 1, 1),
        )
        assertEquals(schedule.principalMinor + schedule.totalInterestMinor, schedule.totalRepaymentMinor)
    }

    @Test
    fun `zero interest rate splits principal evenly with no interest charged`() {
        val schedule = InterestCalculator.buildSchedule(
            principalMinor = 12_000_00,
            annualRatePercent = 0.0,
            termMonths = 12,
            startDate = LocalDate.of(2026, 1, 1),
        )
        assertEquals(0L, schedule.totalInterestMinor)
        assertTrue(schedule.installments.all { it.interestMinor == 0L })
    }

    @Test
    fun `due dates advance one month per installment`() {
        val start = LocalDate.of(2026, 1, 31)
        val schedule = InterestCalculator.buildSchedule(
            principalMinor = 10_000_00,
            annualRatePercent = 10.0,
            termMonths = 3,
            startDate = start,
        )
        assertEquals(start.plusMonths(1), schedule.installments[0].dueDate)
        assertEquals(start.plusMonths(2), schedule.installments[1].dueDate)
        assertEquals(start.plusMonths(3), schedule.installments[2].dueDate)
    }

    @Test
    fun `remaining balance strictly decreases each installment`() {
        val schedule = InterestCalculator.buildSchedule(
            principalMinor = 75_000_00,
            annualRatePercent = 18.0,
            termMonths = 18,
            startDate = LocalDate.of(2026, 1, 1),
        )
        var previous = schedule.principalMinor
        for (installment in schedule.installments) {
            assertTrue(installment.remainingBalanceMinor < previous)
            previous = installment.remainingBalanceMinor
        }
    }
}
