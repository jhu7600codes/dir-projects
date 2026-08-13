package com.vanbank.core.finance

import com.vanbank.core.model.TransactionCategory
import com.vanbank.core.model.TransactionDirection
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class StatementSummary(
    val from: LocalDate,
    val to: LocalDate,
    val totalInMinor: Long,
    val totalOutMinor: Long,
    val netMinor: Long,
    val transactionCount: Int,
    val byCategory: Map<TransactionCategory, Long>,
)

/** Builds an in-app statement: totals in/out for a date range, plus a category breakdown of spending. */
object StatementCalculator {
    fun summarize(
        transactions: List<TransactionRecord>,
        from: LocalDate,
        to: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): StatementSummary {
        require(!to.isBefore(from)) { "'to' must not be before 'from'" }

        val inRange = transactions.filter { it.dateIn(zoneId) in from..to }

        val totalIn = inRange.filter { it.direction == TransactionDirection.IN }.sumOf { it.amountMinor }
        val totalOut = inRange.filter { it.direction == TransactionDirection.OUT }.sumOf { it.amountMinor }

        val byCategory = inRange
            .filter { it.direction == TransactionDirection.OUT }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amountMinor } }

        return StatementSummary(
            from = from,
            to = to,
            totalInMinor = totalIn,
            totalOutMinor = totalOut,
            netMinor = totalIn - totalOut,
            transactionCount = inRange.size,
            byCategory = byCategory,
        )
    }

    private fun TransactionRecord.dateIn(zoneId: ZoneId): LocalDate =
        timestamp.atZone(zoneId).toLocalDate()
}

/** Convenience wrapper so app code doesn't need to construct Instant/ZoneId itself for common ranges. */
fun statementRangeForLastDays(days: Long, now: Instant = Instant.now(), zoneId: ZoneId = ZoneId.systemDefault()): Pair<LocalDate, LocalDate> {
    val today = now.atZone(zoneId).toLocalDate()
    return today.minusDays(days) to today
}
