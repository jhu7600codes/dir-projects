package com.vanbank.app.di

import android.content.Context
import com.vanbank.app.data.local.VanBankDatabase
import com.vanbank.app.data.prefs.SessionManager
import com.vanbank.app.data.repository.AccountRepository
import com.vanbank.app.data.repository.AiRequestRepository
import com.vanbank.app.data.repository.AuthRepository
import com.vanbank.app.data.repository.BillRepository
import com.vanbank.app.data.repository.BudgetRepository
import com.vanbank.app.data.repository.CardRepository
import com.vanbank.app.data.repository.LoanRepository
import com.vanbank.app.data.repository.StatementRepository
import com.vanbank.app.data.repository.TransactionRepository
import com.vanbank.app.data.repository.TransferRepository
import com.vanbank.app.data.repository.VaultRepository
import com.vanbank.app.notifications.NotificationHelper

/**
 * A small, hand-rolled dependency container -- no Hilt/Dagger, just one
 * object built once in [com.vanbank.app.VanBankApplication] and handed to
 * ViewModels via [VanBankViewModelFactory]. Everything here is a cheap
 * singleton (a Room database, DataStore-backed session, and thin
 * repositories over both), so there's nothing a real DI framework would buy
 * that's worth the extra annotation processing setup for an app this size.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: VanBankDatabase by lazy { VanBankDatabase.getInstance(appContext) }
    val sessionManager: SessionManager by lazy { SessionManager(appContext) }
    val notificationHelper: NotificationHelper by lazy { NotificationHelper(appContext) }

    val authRepository: AuthRepository by lazy { AuthRepository(database, sessionManager) }
    val accountRepository: AccountRepository by lazy { AccountRepository(database) }
    val cardRepository: CardRepository by lazy { CardRepository(database) }
    val transferRepository: TransferRepository by lazy { TransferRepository(database) }
    val transactionRepository: TransactionRepository by lazy { TransactionRepository(database) }
    val statementRepository: StatementRepository by lazy { StatementRepository(database) }
    val budgetRepository: BudgetRepository by lazy { BudgetRepository(database) }
    val loanRepository: LoanRepository by lazy { LoanRepository(database) }
    val vaultRepository: VaultRepository by lazy { VaultRepository(database) }
    val billRepository: BillRepository by lazy { BillRepository(database) }
    val aiRequestRepository: AiRequestRepository by lazy { AiRequestRepository(database, notificationHelper) }
}
