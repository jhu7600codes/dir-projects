package com.vanbank.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanbank.app.data.local.entity.TransactionEntity
import com.vanbank.app.data.repository.AuthRepository
import com.vanbank.app.data.repository.TransactionRepository
import com.vanbank.core.model.TransactionCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class TransactionsUiState(
    val all: List<TransactionEntity> = emptyList(),
    val filtered: List<TransactionEntity> = emptyList(),
    val selectedCategory: TransactionCategory? = null,
)

class TransactionsViewModel(
    authRepository: AuthRepository,
    transactionRepository: TransactionRepository,
) : ViewModel() {
    private val selectedCategory = MutableStateFlow<TransactionCategory?>(null)

    private val allTransactions: kotlinx.coroutines.flow.Flow<List<TransactionEntity>> = authRepository.currentUserId
        .filterNotNull()
        .flatMapLatest { transactionRepository.observeForUser(it) }

    val uiState: StateFlow<TransactionsUiState> = combine(allTransactions, selectedCategory) { all, category ->
        TransactionsUiState(
            all = all,
            filtered = category?.let { c -> all.filter { it.category == c } } ?: all,
            selectedCategory = category,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    fun selectCategory(category: TransactionCategory?) {
        selectedCategory.value = category
    }
}
