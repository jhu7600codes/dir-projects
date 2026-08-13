package com.vanbank.core.finance

import com.vanbank.core.model.TransactionCategory
import com.vanbank.core.model.TransactionDirection
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class BudgetCalculatorTest {
    private fun tx(amountMinor: Long, direction: TransactionDirection, category: TransactionCategory) =
        TransactionRecord(Instant.EPOCH, amountMinor, direction, category)

    @Test
    fun `percentages sum to one hundred`() {
        val transactions = listOf(
            tx(3_000_00, TransactionDirection.OUT, TransactionCategory.GROCERIES),
            tx(1_000_00, TransactionDirection.OUT, TransactionCategory.SUBSCRIPTIONS),
            tx(6_000_00, TransactionDirection.OUT, TransactionCategory.RENT_MORTGAGE),
        )
        val breakdown = BudgetCalculator.categoryBreakdown(transactions)
        val totalPercentage = breakdown.sumOf { it.percentage }
        assertEquals(100.0, totalPercentage, 0.001)
    }

    @Test
    fun `incoming transactions are excluded from spend breakdown`() {
        val transactions = listOf(
            tx(10_000_00, TransactionDirection.IN, TransactionCategory.INCOME),
            tx(500_00, TransactionDirection.OUT, TransactionCategory.DINING),
        )
        val breakdown = BudgetCalculator.categoryBreakdown(transactions)
        assertEquals(1, breakdown.size)
        assertEquals(TransactionCategory.DINING, breakdown.first().category)
    }

    @Test
    fun `results are sorted highest spend first`() {
        val transactions = listOf(
            tx(100_00, TransactionDirection.OUT, TransactionCategory.ENTERTAINMENT),
            tx(5_000_00, TransactionDirection.OUT, TransactionCategory.RENT_MORTGAGE),
            tx(900_00, TransactionDirection.OUT, TransactionCategory.GROCERIES),
        )
        val breakdown = BudgetCalculator.categoryBreakdown(transactions)
        assertEquals(
            listOf(TransactionCategory.RENT_MORTGAGE, TransactionCategory.GROCERIES, TransactionCategory.ENTERTAINMENT),
            breakdown.map { it.category },
        )
    }

    @Test
    fun `empty spend returns an empty breakdown`() {
        assertEquals(emptyList(), BudgetCalculator.categoryBreakdown(emptyList()))
    }
}
