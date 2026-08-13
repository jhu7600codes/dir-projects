package com.vanbank.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanbank.app.data.local.entity.AccountEntity
import com.vanbank.app.data.local.entity.AiPaymentRequestEntity
import com.vanbank.app.data.local.entity.CardEntity
import com.vanbank.app.data.local.entity.TransactionEntity
import com.vanbank.app.data.repository.AccountRepository
import com.vanbank.app.data.repository.AiRequestRepository
import com.vanbank.app.data.repository.AuthRepository
import com.vanbank.app.data.repository.CardRepository
import com.vanbank.app.data.repository.TransactionRepository
import com.vanbank.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val fullName: String = "",
    val accounts: List<AccountEntity> = emptyList(),
    val cards: List<CardEntity> = emptyList(),
    val totalBalanceMinor: Long = 0L,
    val pendingAiRequests: List<AiPaymentRequestEntity> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
)

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository,
    private val cardRepository: CardRepository,
    private val transactionRepository: TransactionRepository,
    private val aiRequestRepository: AiRequestRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = authRepository.currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            combine(
                authRepository.observeUser(userId),
                accountRepository.observeForUser(userId),
                cardRepository.observeForUser(userId),
                aiRequestRepository.observePendingForUser(userId),
                transactionRepository.observeRecentForUser(userId, 12),
            ) { user: UserEntity?, accounts: List<AccountEntity>, cards: List<CardEntity>,
                pending: List<AiPaymentRequestEntity>, recent: List<TransactionEntity> ->
                HomeUiState(
                    isLoading = false,
                    fullName = user?.fullName.orEmpty(),
                    accounts = accounts,
                    cards = cards,
                    totalBalanceMinor = accounts.sumOf { it.balanceMinor },
                    pendingAiRequests = pending,
                    recentTransactions = recent,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun toggleCardFreeze(cardId: Long, currentlyFrozen: Boolean) {
        viewModelScope.launch { cardRepository.setFrozen(cardId, !currentlyFrozen) }
    }

    fun approveAiRequest(requestId: Long, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onResult(aiRequestRepository.approve(requestId)) }
    }

    fun declineAiRequest(requestId: Long, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onResult(aiRequestRepository.decline(requestId)) }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
