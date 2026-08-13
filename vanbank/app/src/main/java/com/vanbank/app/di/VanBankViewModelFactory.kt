package com.vanbank.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.vanbank.app.ui.admin.AdminViewModel
import com.vanbank.app.ui.ai.AiRequestViewModel
import com.vanbank.app.ui.auth.AuthViewModel
import com.vanbank.app.ui.billpay.BillPayViewModel
import com.vanbank.app.ui.budgeting.BudgetViewModel
import com.vanbank.app.ui.home.HomeViewModel
import com.vanbank.app.ui.loans.LoansViewModel
import com.vanbank.app.ui.statements.StatementsViewModel
import com.vanbank.app.ui.transactions.TransactionsViewModel
import com.vanbank.app.ui.transfer.TransferViewModel
import com.vanbank.app.ui.vaults.VaultsViewModel

/** One factory, one `when` -- simplest possible way to hand every ViewModel its repositories without a DI framework. */
class VanBankViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when (modelClass) {
            AuthViewModel::class.java -> AuthViewModel(container.authRepository)
            HomeViewModel::class.java -> HomeViewModel(
                container.authRepository,
                container.accountRepository,
                container.cardRepository,
                container.transactionRepository,
                container.aiRequestRepository,
            )
            TransferViewModel::class.java -> TransferViewModel(
                container.authRepository,
                container.accountRepository,
                container.transferRepository,
            )
            AiRequestViewModel::class.java -> AiRequestViewModel(container.authRepository, container.aiRequestRepository)
            TransactionsViewModel::class.java -> TransactionsViewModel(container.authRepository, container.transactionRepository)
            StatementsViewModel::class.java -> StatementsViewModel(container.authRepository, container.statementRepository)
            LoansViewModel::class.java -> LoansViewModel(container.authRepository, container.accountRepository, container.loanRepository)
            VaultsViewModel::class.java -> VaultsViewModel(container.authRepository, container.accountRepository, container.vaultRepository)
            BudgetViewModel::class.java -> BudgetViewModel(container.authRepository, container.budgetRepository)
            BillPayViewModel::class.java -> BillPayViewModel(container.authRepository, container.accountRepository, container.billRepository)
            AdminViewModel::class.java -> AdminViewModel(
                container.authRepository,
                container.accountRepository,
                container.aiRequestRepository,
                container.database,
            )
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        } as T
    }
}
