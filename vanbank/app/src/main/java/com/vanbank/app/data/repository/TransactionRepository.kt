package com.vanbank.app.data.repository

import com.vanbank.app.data.local.VanBankDatabase
import com.vanbank.app.data.local.entity.TransactionEntity
import com.vanbank.core.finance.TransactionRecord
import com.vanbank.core.model.TransactionCategory
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class TransactionRepository(private val db: VanBankDatabase) {
    private val txDao = db.transactionDao()

    fun observeForUser(userId: Long): Flow<List<TransactionEntity>> = txDao.observeForUser(userId)

    fun observeRecentForUser(userId: Long, limit: Int): Flow<List<TransactionEntity>> =
        txDao.observeRecentForUser(userId, limit)

    fun observeForAccount(accountId: Long): Flow<List<TransactionEntity>> = txDao.observeForAccount(accountId)

    fun observeForCard(cardId: Long): Flow<List<TransactionEntity>> = txDao.observeForCard(cardId)

    suspend fun getForUserBetween(userId: Long, fromMillis: Long, toMillis: Long): List<TransactionEntity> =
        txDao.getForUserBetween(userId, fromMillis, toMillis)
}

/** Maps a Room transaction row down to the plain shape :core's pure math (statements, budgeting) understands. */
fun TransactionEntity.toRecord(): TransactionRecord = TransactionRecord(
    timestamp = Instant.ofEpochMilli(timestamp),
    amountMinor = amountMinor,
    direction = direction,
    category = category,
)

/** Category display metadata the UI reuses everywhere (transaction rows, budget legend, bill form). */
fun TransactionCategory.label(): String = when (this) {
    TransactionCategory.GROCERIES -> "Groceries"
    TransactionCategory.INCOME -> "Income"
    TransactionCategory.SUBSCRIPTIONS -> "Subscriptions"
    TransactionCategory.DINING -> "Dining"
    TransactionCategory.TRANSPORT -> "Transport"
    TransactionCategory.SHOPPING -> "Shopping"
    TransactionCategory.UTILITIES -> "Utilities"
    TransactionCategory.ENTERTAINMENT -> "Entertainment"
    TransactionCategory.RENT_MORTGAGE -> "Rent / Mortgage"
    TransactionCategory.TRANSFER -> "Transfer"
    TransactionCategory.LOAN -> "Loan"
    TransactionCategory.SAVINGS -> "Savings"
    TransactionCategory.AI_SERVICES -> "DIR AI Services"
    TransactionCategory.FEES -> "Fees"
    TransactionCategory.OTHER -> "Other"
}
