package com.vanbank.core.finance

import com.vanbank.core.model.TransactionCategory
import com.vanbank.core.model.TransactionDirection

data class CategorySpend(
    val category: TransactionCategory,
    val totalMinor: Long,
    val percentage: Double, // 0.0..100.0
)

/** Category breakdown of spending, for the budgeting pie/bar chart. */
object BudgetCalculator {
    fun categoryBreakdown(
        transactions: List<TransactionRecord>,
        direction: TransactionDirection = TransactionDirection.OUT,
    ): List<CategorySpend> {
        val filtered = transactions.filter { it.direction == direction }
        val total = filtered.sumOf { it.amountMinor }
        if (total <= 0L) return emptyList()

        return filtered
            .groupBy { it.category }
            .map { (category, txs) ->
                val sum = txs.sumOf { it.amountMinor }
                CategorySpend(
                    category = category,
                    totalMinor = sum,
                    percentage = sum.toDouble() / total.toDouble() * 100.0,
                )
            }
            .sortedByDescending { it.totalMinor }
    }
}
