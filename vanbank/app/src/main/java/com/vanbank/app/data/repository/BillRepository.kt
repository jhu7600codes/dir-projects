package com.vanbank.app.data.repository

import androidx.room.withTransaction
import com.vanbank.app.data.local.VanBankDatabase
import com.vanbank.app.data.local.entity.BillEntity
import com.vanbank.app.data.local.entity.TransactionEntity
import com.vanbank.core.model.BillFrequency
import com.vanbank.core.model.TransactionCategory
import com.vanbank.core.model.TransactionDirection
import com.vanbank.core.model.TransactionStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneOffset

sealed class BillPayOutcome {
    data class Paid(val bill: BillEntity) : BillPayOutcome()
    data class Failed(val bill: BillEntity, val reason: String) : BillPayOutcome()
}

/** Recurring fake bills that auto-deduct on schedule. See [com.vanbank.app.work.BillPayWorker] for the scheduler. */
class BillRepository(private val db: VanBankDatabase) {
    private val billDao = db.billDao()
    private val accountDao = db.accountDao()
    private val txDao = db.transactionDao()

    fun observeForUser(userId: Long): Flow<List<BillEntity>> = billDao.observeForUser(userId)

    suspend fun createBill(
        userId: Long,
        accountId: Long,
        name: String,
        category: TransactionCategory,
        amountMinor: Long,
        frequency: BillFrequency,
        firstDueAtEpochMillis: Long,
    ): Result<Long> = runCatching {
        requireOrThrow(name.isNotBlank()) { "Give the bill a name." }
        requireOrThrow(amountMinor > 0) { "Amount must be positive." }
        billDao.insert(
            BillEntity(
                userId = userId,
                accountId = accountId,
                name = name.trim(),
                category = category,
                amountMinor = amountMinor,
                frequency = frequency,
                nextDueAtEpochMillis = firstDueAtEpochMillis,
            ),
        )
    }

    suspend fun setActive(bill: BillEntity, active: Boolean) {
        billDao.update(bill.copy(isActive = active))
    }

    suspend fun delete(bill: BillEntity) {
        billDao.delete(bill)
    }

    /** Every bill due at or before now, across all users -- what the periodic worker sweeps. */
    suspend fun getDueBills(nowMillis: Long = System.currentTimeMillis()): List<BillEntity> =
        billDao.getDueBills(nowMillis)

    /** Attempts to pay one bill: deducts on success, flags on insufficient funds, and always rolls the due date forward. */
    suspend fun processBill(bill: BillEntity, now: Long = System.currentTimeMillis()): BillPayOutcome =
        db.withTransaction {
            val account = accountDao.getById(bill.accountId)
            val nextDue = advance(bill.nextDueAtEpochMillis, bill.frequency)

            if (account == null || account.balanceMinor < bill.amountMinor) {
                val updated = bill.copy(nextDueAtEpochMillis = nextDue, lastPaymentFailed = true)
                billDao.update(updated)
                return@withTransaction BillPayOutcome.Failed(updated, "Insufficient funds")
            }

            accountDao.adjustBalance(bill.accountId, -bill.amountMinor)
            txDao.insert(
                TransactionEntity(
                    accountId = bill.accountId,
                    category = bill.category,
                    description = "Bill: ${bill.name}",
                    amountMinor = bill.amountMinor,
                    direction = TransactionDirection.OUT,
                    status = TransactionStatus.COMPLETED,
                    counterparty = bill.name,
                    timestamp = now,
                ),
            )
            val updated = bill.copy(nextDueAtEpochMillis = nextDue, lastPaidAt = now, lastPaymentFailed = false)
            billDao.update(updated)
            BillPayOutcome.Paid(updated)
        }

    private fun advance(fromMillis: Long, frequency: BillFrequency): Long {
        val zoned = Instant.ofEpochMilli(fromMillis).atZone(ZoneOffset.UTC)
        val next = when (frequency) {
            BillFrequency.WEEKLY -> zoned.plusWeeks(1)
            BillFrequency.MONTHLY -> zoned.plusMonths(1)
            BillFrequency.YEARLY -> zoned.plusYears(1)
        }
        return next.toInstant().toEpochMilli()
    }
}
