package com.vanbank.core.finance

import com.vanbank.core.model.TransactionCategory
import com.vanbank.core.model.TransactionDirection
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class StatementCalculatorTest {
    private val zone: ZoneId = ZoneOffset.UTC

    private fun tx(date: LocalDate, amountMinor: Long, direction: TransactionDirection, category: TransactionCategory) =
        TransactionRecord(
            timestamp = date.atStartOfDay(zone).toInstant(),
            amountMinor = amountMinor,
            direction = direction,
            category = category,
        )

    @Test
    fun `totals in and out are summed separately`() {
        val transactions = listOf(
            tx(LocalDate.of(2026, 8, 1), 5_000_00, TransactionDirection.IN, TransactionCategory.INCOME),
            tx(LocalDate.of(2026, 8, 2), 1_200_00, TransactionDirection.OUT, TransactionCategory.GROCERIES),
            tx(LocalDate.of(2026, 8, 3), 300_00, TransactionDirection.OUT, TransactionCategory.SUBSCRIPTIONS),
        )
        val summary = StatementCalculator.summarize(
            transactions, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), zone,
        )
        assertEquals(5_000_00, summary.totalInMinor)
        assertEquals(1_500_00, summary.totalOutMinor)
        assertEquals(3_500_00, summary.netMinor)
        assertEquals(3, summary.transactionCount)
    }

    @Test
    fun `transactions outside the date range are excluded`() {
        val transactions = listOf(
            tx(LocalDate.of(2026, 7, 31), 1_000_00, TransactionDirection.IN, TransactionCategory.INCOME),
            tx(LocalDate.of(2026, 8, 15), 1_000_00, TransactionDirection.IN, TransactionCategory.INCOME),
            tx(LocalDate.of(2026, 9, 1), 1_000_00, TransactionDirection.IN, TransactionCategory.INCOME),
        )
        val summary = StatementCalculator.summarize(
            transactions, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), zone,
        )
        assertEquals(1_000_00, summary.totalInMinor)
        assertEquals(1, summary.transactionCount)
    }

    @Test
    fun `byCategory only includes outgoing spend`() {
        val transactions = listOf(
            tx(LocalDate.of(2026, 8, 1), 4_000_00, TransactionDirection.IN, TransactionCategory.INCOME),
            tx(LocalDate.of(2026, 8, 2), 800_00, TransactionDirection.OUT, TransactionCategory.GROCERIES),
            tx(LocalDate.of(2026, 8, 3), 200_00, TransactionDirection.OUT, TransactionCategory.GROCERIES),
        )
        val summary = StatementCalculator.summarize(
            transactions, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), zone,
        )
        assertEquals(mapOf(TransactionCategory.GROCERIES to 1_000_00L), summary.byCategory)
    }
}
