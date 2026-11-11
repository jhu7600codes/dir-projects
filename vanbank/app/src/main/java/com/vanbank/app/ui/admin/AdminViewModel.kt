package com.vanbank.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanbank.app.data.local.VanBankDatabase
import com.vanbank.app.data.local.entity.AccountEntity
import com.vanbank.app.data.local.entity.UserEntity
import com.vanbank.app.data.repository.AccountRepository
import com.vanbank.app.data.repository.AiRequestRepository
import com.vanbank.app.data.repository.AuthRepository
import com.vanbank.core.model.AccountType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A fixed reset balance for the "reset balances" dev action -- matches the signup starter amount. */
private const val RESET_CHECKING_MINOR = 5_000_00L
private const val RESET_SAVINGS_MINOR = 0L

data class AdminUiState(
    val lastSpawnedCredentials: Pair<String, String>? = null, // username to password, shown once
    val message: String? = null,
)

/**
 * A dev/admin panel with zero real authorization behind it -- it's a
 * simulator, so "admin" just means "the screen that pokes the database
 * directly": spawn test users, reset balances, or manually fire an AI
 * Assistant payment request instead of waiting for one to naturally occur.
 */
class AdminViewModel(
    private val authRepository: AuthRepository,
    accountRepository: AccountRepository,
    private val aiRequestRepository: AiRequestRepository,
    private val database: VanBankDatabase,
) : ViewModel() {

    val allUsers: StateFlow<List<UserEntity>> =
        database.userDao().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allAccounts: StateFlow<List<AccountEntity>> =
        database.accountDao().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState

    fun spawnTestUser() {
        viewModelScope.launch {
            val suffix = (1000..9999).random()
            val username = "testuser$suffix"
            val password = "test${(100000..999999).random()}"
            authRepository.signUp(username, password, "Test User $suffix")
                .onSuccess {
                    _uiState.value = AdminUiState(lastSpawnedCredentials = username to password, message = "Spawned @$username")
                }
                .onFailure { _uiState.value = AdminUiState(message = it.message) }
        }
    }

    fun resetBalance(accountId: Long, type: AccountType) {
        viewModelScope.launch {
            val amount = if (type == AccountType.CHECKING) RESET_CHECKING_MINOR else RESET_SAVINGS_MINOR
            database.accountDao().setBalance(accountId, amount)
            _uiState.value = AdminUiState(message = "Balance reset.")
        }
    }

    fun triggerAiRequest(userId: Long, title: String, detail: String, amountMinor: Long) {
        viewModelScope.launch {
            val account = database.accountDao().getForUser(userId).firstOrNull { it.type == AccountType.CHECKING }
                ?: database.accountDao().getForUser(userId).firstOrNull()
            if (account == null) {
                _uiState.value = AdminUiState(message = "That user has no accounts.")
                return@launch
            }
            aiRequestRepository.createRequest(userId, account.id, title, detail, amountMinor)
                .onSuccess { _uiState.value = AdminUiState(message = "AI request sent.") }
                .onFailure { _uiState.value = AdminUiState(message = it.message) }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
