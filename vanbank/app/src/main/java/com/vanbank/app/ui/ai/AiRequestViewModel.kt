package com.vanbank.app.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanbank.app.data.local.entity.AiPaymentRequestEntity
import com.vanbank.app.data.repository.AiRequestRepository
import com.vanbank.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiRequestsUiState(
    val requests: List<AiPaymentRequestEntity> = emptyList(),
    val processingId: Long? = null,
    val errorMessage: String? = null,
)

class AiRequestViewModel(
    private val authRepository: AuthRepository,
    private val aiRequestRepository: AiRequestRepository,
) : ViewModel() {
    private val _errorState = MutableStateFlow<String?>(null)
    private val _processingId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<AiRequestsUiState> = authRepository.currentUserId
        .filterNotNull()
        .flatMapLatest { userId -> aiRequestRepository.observeForUser(userId) }
        .combine(_processingId) { requests, processingId -> requests to processingId }
        .combine(_errorState) { (requests, processingId), error ->
            AiRequestsUiState(requests = requests, processingId = processingId, errorMessage = error)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiRequestsUiState())

    fun approve(requestId: Long) {
        _processingId.value = requestId
        viewModelScope.launch {
            aiRequestRepository.approve(requestId)
                .onFailure { _errorState.value = it.message }
            _processingId.value = null
        }
    }

    fun decline(requestId: Long) {
        _processingId.value = requestId
        viewModelScope.launch {
            aiRequestRepository.decline(requestId)
                .onFailure { _errorState.value = it.message }
            _processingId.value = null
        }
    }

    fun clearError() {
        _errorState.value = null
    }
}
