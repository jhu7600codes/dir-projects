package com.jhulian.android.youtube.classic.ui.shorts

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhulian.android.youtube.classic.data.model.VideoUi
import com.jhulian.android.youtube.classic.network.InnertubeFeedClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ShortsUiState(
    val items: List<VideoUi> = emptyList(),
    val isLoading: Boolean = false,
    val requiresSignIn: Boolean = false,
    // Distinguishes "haven't tried loading yet" from "loaded successfully
    // but got zero items back" - without this the screen has no way to
    // tell the difference between still-loading and a genuinely empty
    // feed, and shows neither a spinner nor a message for the latter.
    val hasLoaded: Boolean = false,
)

class ShortsViewModel : ViewModel() {

    private val _state = MutableStateFlow(ShortsUiState())
    val state: StateFlow<ShortsUiState> = _state

    private var loadedOnce = false

    fun loadIfNeeded(cookie: String?) {
        if (loadedOnce) return
        loadedOnce = true

        if (cookie.isNullOrBlank()) {
            _state.value = ShortsUiState(requiresSignIn = true, hasLoaded = true)
            return
        }

        _state.value = _state.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val items = InnertubeFeedClient.shorts(cookie)
                _state.value = ShortsUiState(items = items, hasLoaded = true)
            } catch (e: Exception) {
                Log.e(TAG, "loadIfNeeded() failed", e)
                _state.value = ShortsUiState(hasLoaded = true)
            }
        }
    }

    companion object {
        private const val TAG = "ShortsViewModel"
    }
}
