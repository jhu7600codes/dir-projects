package com.vanbank.app.data.repository

import androidx.room.withTransaction
import com.vanbank.app.data.local.VanBankDatabase
import com.vanbank.app.data.local.entity.CardEntity
import com.vanbank.core.model.CardStatus
import com.vanbank.core.model.CardType
import com.vanbank.core.numbers.CardNumberGenerator
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.random.Random

class CardRepository(private val db: VanBankDatabase) {
    private val cardDao = db.cardDao()
    private val accountDao = db.accountDao()

    fun observeForUser(userId: Long): Flow<List<CardEntity>> = cardDao.observeForUser(userId)

    fun observeForAccount(accountId: Long): Flow<List<CardEntity>> = cardDao.observeForAccount(accountId)

    fun observeById(cardId: Long): Flow<CardEntity?> = cardDao.observeById(cardId)

    suspend fun getById(cardId: Long): CardEntity? = cardDao.getById(cardId)

    suspend fun getByCardNumber(cardNumber: String): CardEntity? = cardDao.getByCardNumber(cardNumber)

    suspend fun setFrozen(cardId: Long, frozen: Boolean) {
        cardDao.setStatus(cardId, if (frozen) CardStatus.FROZEN else CardStatus.ACTIVE)
    }

    suspend fun issueCard(
        accountId: Long,
        cardholderName: String,
        cardType: CardType,
        creditLimitMinor: Long? = null,
    ): Result<Long> = runCatching {
        requireOrThrow(accountDao.getById(accountId) != null) { "Account not found." }
        if (cardType == CardType.CREDIT) {
            requireOrThrow(creditLimitMinor != null && creditLimitMinor > 0) { "Credit cards need a positive credit limit." }
        }

        var candidate: String
        do {
            candidate = CardNumberGenerator.generate(random = Random)
        } while (cardDao.getByCardNumber(candidate) != null)

        val issued = LocalDate.now(ZoneOffset.UTC)
        val expiry = issued.plusYears(4)
        cardDao.insert(
            CardEntity(
                accountId = accountId,
                cardNumber = candidate,
                cvv = CardNumberGenerator.generateCvv(),
                expiryMonth = expiry.monthValue,
                expiryYear = expiry.year,
                cardholderName = cardholderName.trim().uppercase(),
                cardType = cardType,
                creditLimitMinor = if (cardType == CardType.CREDIT) creditLimitMinor else null,
                creditBalanceMinor = if (cardType == CardType.CREDIT) 0L else null,
            ),
        )
    }

    /** Pays down a credit card's outstanding balance from one of the user's own accounts. */
    suspend fun payCreditCardBalance(cardId: Long, fromAccountId: Long, amountMinor: Long): Result<Unit> = runCatching {
        requireOrThrow(amountMinor > 0) { "Payment amount must be positive." }
        db.withTransaction {
            val card = cardDao.getById(cardId) ?: throw VanBankException("Card not found.")
            requireOrThrow(card.cardType == CardType.CREDIT) { "Only credit cards carry a balance to pay down." }
            val owed = card.creditBalanceMinor ?: 0L
            requireOrThrow(owed > 0) { "This card has no outstanding balance." }
            val fromAccount = accountDao.getById(fromAccountId) ?: throw VanBankException("Source account not found.")
            requireOrThrow(fromAccount.balanceMinor >= amountMinor) { "Insufficient funds." }

            val applied = amountMinor.coerceAtMost(owed)
            accountDao.adjustBalance(fromAccountId, -applied)
            cardDao.adjustCreditBalance(cardId, -applied)
        }
    }
}
