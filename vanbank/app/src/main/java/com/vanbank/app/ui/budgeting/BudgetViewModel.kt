package com.vanbank.app.ui.budgeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanbank.app.data.repository.AuthRepository
import com.vanbank.app.data.repository.BudgetRepository
import com.vanbank.core.finance.CategorySpend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BudgetUiState(
    val breakdown: List<CategorySpend> = emptyList(),
    val selectedDays: Long = 30,
    val isLoading: Boolean = true,
)

class BudgetViewModel(
    private val authRepository: AuthRepository,
    private val budgetRepository: BudgetRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        loadRange(30)
    }

    fun loadRange(days: Long) {
        _uiState.update { it.copy(isLoading = true, selectedDays = days) }
        viewModelScope.launch {
            val userId = authRepository.currentUserId.first() ?: return@launch
            val breakdown = budgetRepository.categoryBreakdownForLastDays(userId, days)
            _uiState.update { it.copy(isLoading = false, breakdown = breakdown) }
        }
    }
}
