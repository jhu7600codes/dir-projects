package com.vanbank.app.data.repository

import com.vanbank.app.data.local.VanBankDatabase
import com.vanbank.core.finance.StatementCalculator
import com.vanbank.core.finance.StatementSummary
import java.time.LocalDate
import java.time.ZoneId

class StatementRepository(private val db: VanBankDatabase) {
    private val txDao = db.transactionDao()

    suspend fun generate(userId: Long, from: LocalDate, to: LocalDate): StatementSummary {
        val zone = ZoneId.systemDefault()
        val fromMillis = from.atStartOfDay(zone).toInstant().toEpochMilli()
        val toMillis = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val transactions = txDao.getForUserBetween(userId, fromMillis, toMillis).map { it.toRecord() }
        return StatementCalculator.summarize(transactions, from, to, zone)
    }
}
