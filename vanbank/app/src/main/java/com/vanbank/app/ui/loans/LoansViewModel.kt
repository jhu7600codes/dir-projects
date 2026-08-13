package com.vanbank.app.ui.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanbank.app.data.local.entity.AccountEntity
import com.vanbank.app.data.local.entity.LoanEntity
import com.vanbank.app.data.local.entity.LoanInstallmentEntity
import com.vanbank.app.data.repository.AccountRepository
import com.vanbank.app.data.repository.AuthRepository
import com.vanbank.app.data.repository.LoanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoanRequestUiState(
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

class LoansViewModel(
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository,
    private val loanRepository: LoanRepository,
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> = authRepository.currentUserId
        .filterNotNull()
        .flatMapLatest { accountRepository.observeForUser(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val loans: StateFlow<List<LoanEntity>> = authRepository.currentUserId
        .filterNotNull()
        .flatMapLatest { loanRepository.observeForUser(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _requestState = MutableStateFlow(LoanRequestUiState())
    val requestState: StateFlow<LoanRequestUiState> = _requestState.asStateFlow()

    fun quoteRate(principalMinor: Long, termMonths: Int): Double = loanRepository.quoteRate(principalMinor, termMonths)

    fun requestLoan(accountId: Long, purpose: String, principalMinor: Long, termMonths: Int) {
        _requestState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val userId = authRepository.currentUserId.first() ?: return@launch
            loanRepository.requestLoan(userId, accountId, purpose, principalMinor, termMonths)
                .onSuccess {
                    _requestState.update { it.copy(isSubmitting = false, successMessage = "Loan approved and disbursed.") }
                }
                .onFailure { error ->
                    _requestState.update { it.copy(isSubmitting = false, errorMessage = error.message ?: "Loan request failed.") }
                }
        }
    }

    fun loan(loanId: Long): Flow<LoanEntity?> = loanRepository.observeById(loanId)

    fun installments(loanId: Long): Flow<List<LoanInstallmentEntity>> = loanRepository.observeInstallments(loanId)

    fun payNextInstallment(loanId: Long, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onResult(loanRepository.payNextInstallment(loanId)) }
    }
}
