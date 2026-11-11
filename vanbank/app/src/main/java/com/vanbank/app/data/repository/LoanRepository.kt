package com.vanbank.app.data.repository

import androidx.room.withTransaction
import com.vanbank.app.data.local.VanBankDatabase
import com.vanbank.app.data.local.entity.LoanEntity
import com.vanbank.app.data.local.entity.LoanInstallmentEntity
import com.vanbank.app.data.local.entity.TransactionEntity
import com.vanbank.core.finance.InterestCalculator
import com.vanbank.core.finance.LoanRateEngine
import com.vanbank.core.model.LoanStatus
import com.vanbank.core.model.TransactionCategory
import com.vanbank.core.model.TransactionDirection
import com.vanbank.core.model.TransactionStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneOffset

private const val MIN_PRINCIPAL_MINOR = 1_000_00L // ₽1,000
private const val MAX_PRINCIPAL_MINOR = 5_000_000_00L // ₽5,000,000
private const val MIN_TERM_MONTHS = 3
private const val MAX_TERM_MONTHS = 360

class LoanRepository(private val db: VanBankDatabase) {
    private val loanDao = db.loanDao()
    private val accountDao = db.accountDao()
    private val txDao = db.transactionDao()

    fun observeForUser(userId: Long): Flow<List<LoanEntity>> = loanDao.observeLoansForUser(userId)

    fun observeById(loanId: Long): Flow<LoanEntity?> = loanDao.observeLoanById(loanId)

    fun observeInstallments(loanId: Long): Flow<List<LoanInstallmentEntity>> = loanDao.observeInstallments(loanId)

    fun quoteRate(principalMinor: Long, termMonths: Int): Double =
        LoanRateEngine.quoteAnnualRatePercent(principalMinor, termMonths)

    /** Requests, "approves", and disburses a loan in one step -- this is a simulator, not real underwriting. */
    suspend fun requestLoan(
        userId: Long,
        accountId: Long,
        purpose: String,
        principalMinor: Long,
        termMonths: Int,
    ): Result<Long> = runCatching {
        requireOrThrow(principalMinor in MIN_PRINCIPAL_MINOR..MAX_PRINCIPAL_MINOR) {
            "Loan amount must be between ₽1,000 and ₽5,000,000."
        }
        requireOrThrow(termMonths in MIN_TERM_MONTHS..MAX_TERM_MONTHS) {
            "Term must be between $MIN_TERM_MONTHS and $MAX_TERM_MONTHS months."
        }
        requireOrThrow(purpose.isNotBlank()) { "Tell us what the loan is for." }

        db.withTransaction {
            val account = accountDao.getById(accountId) ?: throw VanBankException("Account not found.")
            requireOrThrow(account.userId == userId) { "That's not one of your accounts." }

            val rate = LoanRateEngine.quoteAnnualRatePercent(principalMinor, termMonths)
            val now = System.currentTimeMillis()
            val loanId = loanDao.insertLoan(
                LoanEntity(
                    userId = userId,
                    accountId = accountId,
                    purpose = purpose.trim(),
                    principalMinor = principalMinor,
                    annualRatePercent = rate,
                    termMonths = termMonths,
                    status = LoanStatus.ACTIVE,
                    requestedAt = now,
                    approvedAt = now,
                ),
            )

            val schedule = InterestCalculator.buildSchedule(
                principalMinor = principalMinor,
                annualRatePercent = rate,
                termMonths = termMonths,
                startDate = LocalDate.now(ZoneOffset.UTC),
            )
            loanDao.insertInstallments(
                schedule.installments.map { line ->
                    LoanInstallmentEntity(
                        loanId = loanId,
                        number = line.number,
                        dueDateEpochDay = line.dueDate.toEpochDay(),
                        principalMinor = line.principalMinor,
                        interestMinor = line.interestMinor,
                        totalDueMinor = line.totalDueMinor,
                        remainingBalanceMinor = line.remainingBalanceMinor,
                    )
                },
            )

            // Disburse the principal into the chosen account.
            accountDao.adjustBalance(accountId, principalMinor)
            txDao.insert(
                TransactionEntity(
                    accountId = accountId,
                    category = TransactionCategory.LOAN,
                    description = "Loan disbursement -- ${purpose.trim()}",
                    amountMinor = principalMinor,
                    direction = TransactionDirection.IN,
                    status = TransactionStatus.COMPLETED,
                    counterparty = "VANBank Lending",
                    timestamp = now,
                ),
            )

            loanId
        }
    }

    /** Pays the next unpaid installment from the loan's own account. */
    suspend fun payNextInstallment(loanId: Long): Result<Unit> = runCatching {
        db.withTransaction {
            val loan = loanDao.getLoanById(loanId) ?: throw VanBankException("Loan not found.")
            val installment = loanDao.nextUnpaidInstallment(loanId)
                ?: throw VanBankException("This loan is already fully paid off.")
            val account = accountDao.getById(loan.accountId) ?: throw VanBankException("Loan's account not found.")
            requireOrThrow(account.balanceMinor >= installment.totalDueMinor) { "Insufficient funds for this installment." }

            accountDao.adjustBalance(loan.accountId, -installment.totalDueMinor)
            loanDao.updateInstallment(installment.copy(isPaid = true, paidAt = System.currentTimeMillis()))
            txDao.insert(
                TransactionEntity(
                    accountId = loan.accountId,
                    category = TransactionCategory.LOAN,
                    description = "Loan repayment #${installment.number} -- ${loan.purpose}",
                    amountMinor = installment.totalDueMinor,
                    direction = TransactionDirection.OUT,
                    status = TransactionStatus.COMPLETED,
                    counterparty = "VANBank Lending",
                ),
            )

            if (installment.number == loan.termMonths) {
                loanDao.updateLoan(loan.copy(status = LoanStatus.PAID_OFF))
            }
        }
    }
}
