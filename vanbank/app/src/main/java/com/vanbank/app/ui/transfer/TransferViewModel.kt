package com.vanbank.app.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanbank.app.data.local.entity.AccountEntity
import com.vanbank.app.data.repository.AccountRepository
import com.vanbank.app.data.repository.AuthRepository
import com.vanbank.app.data.repository.TransferRepository
import com.vanbank.core.model.Money
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

enum class TransferMode { INTERNAL, TO_USER, BY_CARD }

data class TransferUiState(
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

class TransferViewModel(
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository,
    private val transferRepository: TransferRepository,
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> = authRepository.currentUserId
        .filterNotNull()
        .flatMapLatest { accountRepository.observeForUser(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun submit(
        mode: TransferMode,
        fromAccountId: Long,
        recipient: String,
        toAccountId: Long?,
        amountText: String,
        note: String,
    ) {
        val amountMinor = Money.parseToMinor(amountText)
        if (amountMinor == null || amountMinor <= 0) {
            _uiState.update { it.copy(errorMessage = "Enter a valid amount.") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val userId = authRepository.currentUserId.first()
            if (userId == null) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = "Not signed in.") }
                return@launch
            }

            val result = when (mode) {
                TransferMode.INTERNAL -> {
                    if (toAccountId == null) {
                        _uiState.update { it.copy(isSubmitting = false, errorMessage = "Choose a destination account.") }
                        return@launch
                    }
                    transferRepository.transferInternal(userId, fromAccountId, toAccountId, amountMinor, note)
                }
                TransferMode.TO_USER -> transferRepository.transferToUser(userId, fromAccountId, recipient, amountMinor, note)
                TransferMode.BY_CARD -> transferRepository.transferByCardNumber(userId, fromAccountId, recipient, amountMinor, note)
            }

            result
                .onSuccess {
                    _uiState.update {
                        it.copy(isSubmitting = false, successMessage = "Sent ${Money.format(amountMinor)}.")
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = error.message ?: "Transfer failed.") }
                }
        }
    }
}
