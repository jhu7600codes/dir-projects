package com.vanbank.app.data.repository

import androidx.room.withTransaction
import com.vanbank.app.data.local.VanBankDatabase
import com.vanbank.app.data.local.entity.SavingsVaultEntity
import com.vanbank.app.data.local.entity.TransactionEntity
import com.vanbank.core.model.TransactionCategory
import com.vanbank.core.model.TransactionDirection
import com.vanbank.core.model.TransactionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Named savings goals ("Emergency fund", "Trip to Sochi"). A vault's money
 * is earmarked out of whichever account backs it -- contributing moves cash
 * out of that account into the vault's progress total; withdrawing moves it
 * back.
 */
class VaultRepository(private val db: VanBankDatabase) {
    private val vaultDao = db.vaultDao()
    private val accountDao = db.accountDao()
    private val txDao = db.transactionDao()

    fun observeForUser(userId: Long): Flow<List<SavingsVaultEntity>> = vaultDao.observeForUser(userId)

    fun observeById(vaultId: Long): Flow<SavingsVaultEntity?> = vaultDao.observeById(vaultId)

    suspend fun createVault(
        accountId: Long,
        name: String,
        emoji: String,
        targetAmountMinor: Long,
        targetDateEpochDay: Long?,
    ): Result<Long> = runCatching {
        requireOrThrow(name.isNotBlank()) { "Give the goal a name." }
        requireOrThrow(targetAmountMinor > 0) { "Target amount must be positive." }
        vaultDao.insert(
            SavingsVaultEntity(
                accountId = accountId,
                name = name.trim(),
                emoji = emoji.ifBlank { "🎯" },
                targetAmountMinor = targetAmountMinor,
                targetDateEpochDay = targetDateEpochDay,
            ),
        )
    }

    suspend fun contribute(vaultId: Long, fromAccountId: Long, amountMinor: Long): Result<Unit> = runCatching {
        requireOrThrow(amountMinor > 0) { "Enter an amount greater than zero." }
        db.withTransaction {
            val vault = vaultDao.getById(vaultId) ?: throw VanBankException("Vault not found.")
            val from = accountDao.getById(fromAccountId) ?: throw VanBankException("Account not found.")
            requireOrThrow(from.balanceMinor >= amountMinor) { "Insufficient funds." }

            accountDao.adjustBalance(fromAccountId, -amountMinor)
            vaultDao.adjustAmount(vaultId, amountMinor)
            txDao.insert(
                TransactionEntity(
                    accountId = fromAccountId,
                    category = TransactionCategory.SAVINGS,
                    description = "Vault contribution: ${vault.emoji} ${vault.name}",
                    amountMinor = amountMinor,
                    direction = TransactionDirection.OUT,
                    status = TransactionStatus.COMPLETED,
                ),
            )
        }
    }

    suspend fun withdraw(vaultId: Long, toAccountId: Long, amountMinor: Long): Result<Unit> = runCatching {
        requireOrThrow(amountMinor > 0) { "Enter an amount greater than zero." }
        db.withTransaction {
            val vault = vaultDao.getById(vaultId) ?: throw VanBankException("Vault not found.")
            requireOrThrow(vault.currentAmountMinor >= amountMinor) { "That's more than the vault holds." }

            vaultDao.adjustAmount(vaultId, -amountMinor)
            accountDao.adjustBalance(toAccountId, amountMinor)
            txDao.insert(
                TransactionEntity(
                    accountId = toAccountId,
                    category = TransactionCategory.SAVINGS,
                    description = "Vault withdrawal: ${vault.emoji} ${vault.name}",
                    amountMinor = amountMinor,
                    direction = TransactionDirection.IN,
                    status = TransactionStatus.COMPLETED,
                ),
            )
        }
    }

    suspend fun delete(vault: SavingsVaultEntity) {
        vaultDao.delete(vault)
    }
}
