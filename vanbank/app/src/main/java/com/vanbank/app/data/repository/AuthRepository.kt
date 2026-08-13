package com.vanbank.app.data.repository

import androidx.room.withTransaction
import com.vanbank.app.data.local.VanBankDatabase
import com.vanbank.app.data.local.entity.AccountEntity
import com.vanbank.app.data.local.entity.CardEntity
import com.vanbank.app.data.local.entity.UserEntity
import com.vanbank.app.data.prefs.SessionManager
import com.vanbank.core.model.AccountType
import com.vanbank.core.model.CardType
import com.vanbank.core.numbers.AccountNumberGenerator
import com.vanbank.core.numbers.CardNumberGenerator
import com.vanbank.core.security.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.random.Random

/** Starter checking balance every new signup gets, so the app isn't empty on first login. */
private const val STARTER_BALANCE_MINOR = 5_000_00L // ₽5,000.00

class AuthRepository(
    private val db: VanBankDatabase,
    private val sessionManager: SessionManager,
) {
    private val userDao = db.userDao()
    private val accountDao = db.accountDao()
    private val cardDao = db.cardDao()

    val currentUserId: Flow<Long?> = sessionManager.currentUserId

    suspend fun currentUser(): UserEntity? {
        val id = currentUserId.first() ?: return null
        return userDao.getById(id)
    }

    fun observeUser(userId: Long): Flow<UserEntity?> = userDao.observeById(userId)

    suspend fun signUp(username: String, password: String, fullName: String): Result<Long> = runCatching {
        val trimmedUsername = username.trim()
        requireOrThrow(trimmedUsername.length >= 3) { "Username must be at least 3 characters." }
        requireOrThrow(trimmedUsername.all { it.isLetterOrDigit() || it == '_' || it == '.' }) {
            "Username can only contain letters, numbers, '.' and '_'."
        }
        requireOrThrow(password.length >= 6) { "Password must be at least 6 characters." }
        requireOrThrow(fullName.isNotBlank()) { "Full name is required." }

        db.withTransaction {
            requireOrThrow(!userDao.usernameExists(trimmedUsername)) { "That username is already taken." }

            val hashed = PasswordHasher.hash(password)
            val userId = userDao.insert(
                UserEntity(
                    username = trimmedUsername,
                    passwordHash = hashed.hashHex,
                    passwordSalt = hashed.saltHex,
                    fullName = fullName.trim(),
                ),
            )

            val checkingId = accountDao.insert(
                AccountEntity(
                    userId = userId,
                    accountNumber = generateUniqueAccountNumber(AccountType.CHECKING),
                    type = AccountType.CHECKING,
                    nickname = "Everyday Checking",
                    balanceMinor = STARTER_BALANCE_MINOR,
                ),
            )
            accountDao.insert(
                AccountEntity(
                    userId = userId,
                    accountNumber = generateUniqueAccountNumber(AccountType.SAVINGS),
                    type = AccountType.SAVINGS,
                    nickname = "Savings",
                    balanceMinor = 0L,
                ),
            )

            val issued = LocalDate.now(ZoneOffset.UTC)
            val expiry = issued.plusYears(4)
            cardDao.insert(
                CardEntity(
                    accountId = checkingId,
                    cardNumber = generateUniqueCardNumber(),
                    cvv = CardNumberGenerator.generateCvv(),
                    expiryMonth = expiry.monthValue,
                    expiryYear = expiry.year,
                    cardholderName = fullName.trim().uppercase(),
                    cardType = CardType.DEBIT,
                ),
            )

            userId
        }
    }

    suspend fun login(username: String, password: String): Result<Long> = runCatching {
        val user = userDao.getByUsername(username.trim())
            ?: throw VanBankException("No account found for that username.")
        val ok = PasswordHasher.verify(password, user.passwordSalt, user.passwordHash)
        requireOrThrow(ok) { "Incorrect password." }
        sessionManager.signIn(user.id)
        user.id
    }

    suspend fun logout() {
        sessionManager.signOut()
    }

    private suspend fun generateUniqueAccountNumber(type: AccountType): String {
        var candidate: String
        do {
            candidate = AccountNumberGenerator.generate(type, Random)
        } while (accountDao.getByAccountNumber(candidate) != null)
        return candidate
    }

    private suspend fun generateUniqueCardNumber(): String {
        var candidate: String
        do {
            candidate = CardNumberGenerator.generate(random = Random)
        } while (cardDao.getByCardNumber(candidate) != null)
        return candidate
    }
}
