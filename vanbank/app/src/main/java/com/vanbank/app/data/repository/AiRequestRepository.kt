package com.vanbank.app.data.repository

import androidx.room.withTransaction
import com.vanbank.app.data.local.VanBankDatabase
import com.vanbank.app.data.local.entity.AiPaymentRequestEntity
import com.vanbank.app.data.local.entity.TransactionEntity
import com.vanbank.app.notifications.NotificationHelper
import com.vanbank.core.model.AiRequestStatus
import com.vanbank.core.model.TransactionCategory
import com.vanbank.core.model.TransactionDirection
import com.vanbank.core.model.TransactionStatus
import kotlinx.coroutines.flow.Flow

/**
 * The DIR AI Assistant: it sends legitimate-looking payment requests for
 * completed work ("research summary compilation — ₽240"), and the user
 * approves (balance deducted, transaction logged COMPLETED) or declines (no
 * deduction, transaction logged DECLINED so it's still visible in history).
 */
class AiRequestRepository(
    private val db: VanBankDatabase,
    private val notificationHelper: NotificationHelper?,
) {
    private val aiDao = db.aiRequestDao()
    private val accountDao = db.accountDao()
    private val txDao = db.transactionDao()

    fun observeForUser(userId: Long): Flow<List<AiPaymentRequestEntity>> = aiDao.observeForUser(userId)

    fun observePendingForUser(userId: Long): Flow<List<AiPaymentRequestEntity>> = aiDao.observePendingForUser(userId)

    /** Used by the admin panel (and could be wired to a background trigger) to spawn a new request. */
    suspend fun createRequest(
        userId: Long,
        accountId: Long,
        title: String,
        detail: String,
        amountMinor: Long,
    ): Result<Long> = runCatching {
        requireOrThrow(title.isNotBlank()) { "Title is required." }
        requireOrThrow(amountMinor > 0) { "Amount must be positive." }
        val id = aiDao.insert(
            AiPaymentRequestEntity(
                userId = userId,
                accountId = accountId,
                title = title.trim(),
                detail = detail.trim(),
                amountMinor = amountMinor,
            ),
        )
        val inserted = aiDao.getById(id)
        if (inserted != null) notificationHelper?.showAiPaymentRequest(inserted)
        id
    }

    suspend fun approve(requestId: Long): Result<Unit> = runCatching {
        db.withTransaction {
            val request = pendingRequestOrThrow(requestId)
            val account = accountDao.getById(request.accountId) ?: throw VanBankException("Account not found.")
            requireOrThrow(account.balanceMinor >= request.amountMinor) { "Insufficient funds to approve this request." }

            accountDao.adjustBalance(request.accountId, -request.amountMinor)
            txDao.insert(
                TransactionEntity(
                    accountId = request.accountId,
                    category = TransactionCategory.AI_SERVICES,
                    description = request.title,
                    amountMinor = request.amountMinor,
                    direction = TransactionDirection.OUT,
                    status = TransactionStatus.COMPLETED,
                    counterparty = "DIR AI Assistant",
                ),
            )
            aiDao.update(request.copy(status = AiRequestStatus.APPROVED, resolvedAt = System.currentTimeMillis()))
        }
    }

    suspend fun decline(requestId: Long): Result<Unit> = runCatching {
        db.withTransaction {
            val request = pendingRequestOrThrow(requestId)
            txDao.insert(
                TransactionEntity(
                    accountId = request.accountId,
                    category = TransactionCategory.AI_SERVICES,
                    description = request.title,
                    amountMinor = request.amountMinor,
                    direction = TransactionDirection.OUT,
                    status = TransactionStatus.DECLINED,
                    counterparty = "DIR AI Assistant",
                ),
            )
            aiDao.update(request.copy(status = AiRequestStatus.DECLINED, resolvedAt = System.currentTimeMillis()))
        }
    }

    private suspend fun pendingRequestOrThrow(requestId: Long): AiPaymentRequestEntity {
        val request = aiDao.getById(requestId) ?: throw VanBankException("Request not found.")
        requireOrThrow(request.status == AiRequestStatus.PENDING) { "This request was already resolved." }
        return request
    }
}
