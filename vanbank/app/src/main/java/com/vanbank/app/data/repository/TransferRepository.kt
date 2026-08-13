package com.vanbank.app.data.repository

import androidx.room.withTransaction
import com.vanbank.app.data.local.VanBankDatabase
import com.vanbank.app.data.local.entity.AccountEntity
import com.vanbank.app.data.local.entity.TransactionEntity
import com.vanbank.core.model.TransactionCategory
import com.vanbank.core.model.TransactionDirection
import com.vanbank.core.model.TransactionStatus

/**
 * Moves money between accounts. Every transfer -- internal, to another
 * VANBank user, or by card number -- ends the same way: one OUT
 * transaction logged on the sender's account, one IN transaction logged on
 * the recipient's, both inside a single Room transaction so a balance
 * can never update without a matching transaction record.
 */
class TransferRepository(private val db: VanBankDatabase) {
    private val accountDao = db.accountDao()
    private val cardDao = db.cardDao()
    private val userDao = db.userDao()
    private val txDao = db.transactionDao()

    /** Between two accounts the signed-in user owns themself. */
    suspend fun transferInternal(
        userId: Long,
        fromAccountId: Long,
        toAccountId: Long,
        amountMinor: Long,
        note: String?,
    ): Result<Unit> = runCatching {
        requireOrThrow(fromAccountId != toAccountId) { "Choose two different accounts." }
        db.withTransaction {
            val from = ownedAccountOrThrow(userId, fromAccountId)
            val to = accountDao.getById(toAccountId) ?: throw VanBankException("Destination account not found.")
            requireOrThrow(to.userId == userId) { "Destination account isn't yours." }
            requireAmountAndFunds(amountMinor, from)

            moveMoney(
                from = from,
                to = to,
                amountMinor = amountMinor,
                category = TransactionCategory.TRANSFER,
                senderDescription = note?.takeIf { it.isNotBlank() } ?: "Transfer to ${to.nickname}",
                recipientDescription = note?.takeIf { it.isNotBlank() } ?: "Transfer from ${from.nickname}",
                counterpartyForSender = to.accountNumber,
                counterpartyForRecipient = from.accountNumber,
            )
        }
    }

    /** To another VANBank user, resolved by @username or by their account number. */
    suspend fun transferToUser(
        userId: Long,
        fromAccountId: Long,
        recipientIdentifier: String,
        amountMinor: Long,
        note: String?,
    ): Result<Unit> = runCatching {
        val identifier = recipientIdentifier.trim().removePrefix("@")
        requireOrThrow(identifier.isNotBlank()) { "Enter a username or account number." }

        db.withTransaction {
            val from = ownedAccountOrThrow(userId, fromAccountId)
            requireAmountAndFunds(amountMinor, from)

            val to = if (identifier.all { it.isDigit() }) {
                accountDao.getByAccountNumber(identifier)
                    ?: throw VanBankException("No account found with that number.")
            } else {
                val recipientUser = userDao.getByUsername(identifier)
                    ?: throw VanBankException("No VANBank user found for \"$identifier\".")
                requireOrThrow(recipientUser.id != userId) { "You can't send to yourself this way -- use an internal transfer." }
                accountDao.getForUser(recipientUser.id).firstOrNull { it.type == com.vanbank.core.model.AccountType.CHECKING }
                    ?: accountDao.getForUser(recipientUser.id).firstOrNull()
                    ?: throw VanBankException("That user has no accounts to receive funds.")
            }
            requireOrThrow(to.id != from.id) { "Choose a different destination." }

            val recipientUsername = userDao.getById(to.userId)?.username ?: "user"
            moveMoney(
                from = from,
                to = to,
                amountMinor = amountMinor,
                category = TransactionCategory.TRANSFER,
                senderDescription = note?.takeIf { it.isNotBlank() } ?: "Transfer to @$recipientUsername",
                recipientDescription = note?.takeIf { it.isNotBlank() } ?: "Transfer received",
                counterpartyForSender = "@$recipientUsername",
                counterpartyForRecipient = accountDao.getById(from.id)?.accountNumber,
            )
        }
    }

    /** To whichever VANBank account the given DIR card number is attached to. */
    suspend fun transferByCardNumber(
        userId: Long,
        fromAccountId: Long,
        cardNumber: String,
        amountMinor: Long,
        note: String?,
    ): Result<Unit> = runCatching {
        val cleanedNumber = cardNumber.filter { it.isDigit() }
        requireOrThrow(cleanedNumber.length == 16) { "Card number must be 16 digits." }

        db.withTransaction {
            val from = ownedAccountOrThrow(userId, fromAccountId)
            requireAmountAndFunds(amountMinor, from)

            val card = cardDao.getByCardNumber(cleanedNumber)
                ?: throw VanBankException("No DIR card found with that number.")
            requireOrThrow(card.status == com.vanbank.core.model.CardStatus.ACTIVE) {
                "That card is frozen and can't receive funds."
            }
            val to = accountDao.getById(card.accountId) ?: throw VanBankException("Card's account not found.")
            requireOrThrow(to.id != from.id) { "That's one of your own accounts -- use an internal transfer." }

            val recipientUsername = userDao.getById(to.userId)?.username ?: "user"
            moveMoney(
                from = from,
                to = to,
                amountMinor = amountMinor,
                category = TransactionCategory.TRANSFER,
                senderDescription = note?.takeIf { it.isNotBlank() }
                    ?: "Transfer to card •••• ${cleanedNumber.takeLast(4)}",
                recipientDescription = note?.takeIf { it.isNotBlank() } ?: "Transfer received",
                counterpartyForSender = "@$recipientUsername",
                counterpartyForRecipient = from.accountNumber,
            )
        }
    }

    private suspend fun ownedAccountOrThrow(userId: Long, accountId: Long): AccountEntity {
        val account = accountDao.getById(accountId) ?: throw VanBankException("Source account not found.")
        requireOrThrow(account.userId == userId) { "That's not one of your accounts." }
        return account
    }

    private fun requireAmountAndFunds(amountMinor: Long, from: AccountEntity) {
        requireOrThrow(amountMinor > 0) { "Enter an amount greater than zero." }
        requireOrThrow(from.balanceMinor >= amountMinor) { "Insufficient funds." }
    }

    private suspend fun moveMoney(
        from: AccountEntity,
        to: AccountEntity,
        amountMinor: Long,
        category: TransactionCategory,
        senderDescription: String,
        recipientDescription: String,
        counterpartyForSender: String?,
        counterpartyForRecipient: String?,
    ) {
        accountDao.adjustBalance(from.id, -amountMinor)
        accountDao.adjustBalance(to.id, amountMinor)

        val now = System.currentTimeMillis()
        txDao.insert(
            TransactionEntity(
                accountId = from.id,
                category = category,
                description = senderDescription,
                amountMinor = amountMinor,
                direction = TransactionDirection.OUT,
                status = TransactionStatus.COMPLETED,
                counterparty = counterpartyForSender,
                timestamp = now,
            ),
        )
        txDao.insert(
            TransactionEntity(
                accountId = to.id,
                category = category,
                description = recipientDescription,
                amountMinor = amountMinor,
                direction = TransactionDirection.IN,
                status = TransactionStatus.COMPLETED,
                counterparty = counterpartyForRecipient,
                timestamp = now,
            ),
        )
    }
}
