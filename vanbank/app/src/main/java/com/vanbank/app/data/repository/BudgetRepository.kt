package com.vanbank.app.data.repository

import com.vanbank.app.data.local.VanBankDatabase
import com.vanbank.core.finance.BudgetCalculator
import com.vanbank.core.finance.CategorySpend
import java.time.LocalDate
import java.time.ZoneId

class BudgetRepository(private val db: VanBankDatabase) {
    private val txDao = db.transactionDao()

    /** Category spend breakdown over the last [days] days, for the budgeting chart. */
    suspend fun categoryBreakdownForLastDays(userId: Long, days: Long): List<CategorySpend> {
        val zone = ZoneId.systemDefault()
        val to = LocalDate.now(zone)
        val from = to.minusDays(days)
        val fromMillis = from.atStartOfDay(zone).toInstant().toEpochMilli()
        val toMillis = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val transactions = txDao.getForUserBetween(userId, fromMillis, toMillis).map { it.toRecord() }
        return BudgetCalculator.categoryBreakdown(transactions)
    }
}
