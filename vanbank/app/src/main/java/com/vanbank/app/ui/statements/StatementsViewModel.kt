package com.vanbank.app.ui.statements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanbank.app.data.repository.AuthRepository
import com.vanbank.app.data.repository.StatementRepository
import com.vanbank.core.finance.StatementSummary
import com.vanbank.core.finance.statementRangeForLastDays
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatementsUiState(
    val summary: StatementSummary? = null,
    val isLoading: Boolean = true,
    val selectedDays: Long = 30,
)

class StatementsViewModel(
    private val authRepository: AuthRepository,
    private val statementRepository: StatementRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatementsUiState())
    val uiState: StateFlow<StatementsUiState> = _uiState.asStateFlow()

    init {
        loadRange(30)
    }

    fun loadRange(days: Long) {
        _uiState.update { it.copy(isLoading = true, selectedDays = days) }
        viewModelScope.launch {
            val userId = authRepository.currentUserId.first() ?: return@launch
            val (from, to) = statementRangeForLastDays(days)
            val summary = statementRepository.generate(userId, from, to)
            _uiState.update { it.copy(isLoading = false, summary = summary) }
        }
    }
}
