package com.vanbank.app.ui.vaults

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanbank.app.data.local.entity.AccountEntity
import com.vanbank.app.data.local.entity.SavingsVaultEntity
import com.vanbank.app.data.repository.AccountRepository
import com.vanbank.app.data.repository.AuthRepository
import com.vanbank.app.data.repository.VaultRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultsViewModel(
    authRepository: AuthRepository,
    accountRepository: AccountRepository,
    private val vaultRepository: VaultRepository,
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> = authRepository.currentUserId
        .filterNotNull()
        .flatMapLatest { accountRepository.observeForUser(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val vaults: StateFlow<List<SavingsVaultEntity>> = authRepository.currentUserId
        .filterNotNull()
        .flatMapLatest { vaultRepository.observeForUser(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createVault(
        accountId: Long,
        name: String,
        emoji: String,
        targetAmountMinor: Long,
        onResult: (Result<Long>) -> Unit,
    ) {
        viewModelScope.launch {
            onResult(vaultRepository.createVault(accountId, name, emoji, targetAmountMinor, targetDateEpochDay = null))
        }
    }

    fun contribute(vaultId: Long, fromAccountId: Long, amountMinor: Long, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onResult(vaultRepository.contribute(vaultId, fromAccountId, amountMinor)) }
    }

    fun withdraw(vaultId: Long, toAccountId: Long, amountMinor: Long, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onResult(vaultRepository.withdraw(vaultId, toAccountId, amountMinor)) }
    }

    fun delete(vault: SavingsVaultEntity) {
        viewModelScope.launch { vaultRepository.delete(vault) }
    }
}
