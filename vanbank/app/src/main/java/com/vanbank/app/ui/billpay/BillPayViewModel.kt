package com.vanbank.app.ui.billpay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanbank.app.data.local.entity.AccountEntity
import com.vanbank.app.data.local.entity.BillEntity
import com.vanbank.app.data.repository.AccountRepository
import com.vanbank.app.data.repository.AuthRepository
import com.vanbank.app.data.repository.BillRepository
import com.vanbank.core.model.BillFrequency
import com.vanbank.core.model.TransactionCategory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BillPayViewModel(
    private val authRepository: AuthRepository,
    accountRepository: AccountRepository,
    private val billRepository: BillRepository,
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> = authRepository.currentUserId
        .filterNotNull()
        .flatMapLatest { accountRepository.observeForUser(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bills: StateFlow<List<BillEntity>> = authRepository.currentUserId
        .filterNotNull()
        .flatMapLatest { billRepository.observeForUser(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createBill(
        accountId: Long,
        name: String,
        category: TransactionCategory,
        amountMinor: Long,
        frequency: BillFrequency,
        firstDueAtEpochMillis: Long,
        onResult: (Result<Long>) -> Unit,
    ) {
        viewModelScope.launch {
            val userId = authRepository.currentUserId.first() ?: return@launch
            onResult(billRepository.createBill(userId, accountId, name, category, amountMinor, frequency, firstDueAtEpochMillis))
        }
    }

    fun setActive(bill: BillEntity, active: Boolean) {
        viewModelScope.launch { billRepository.setActive(bill, active) }
    }

    fun delete(bill: BillEntity) {
        viewModelScope.launch { billRepository.delete(bill) }
    }
}
