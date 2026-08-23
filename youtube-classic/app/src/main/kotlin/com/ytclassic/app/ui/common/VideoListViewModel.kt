package com.ytclassic.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ytclassic.app.data.model.VideoUi
import com.ytclassic.app.data.model.toVideoUi
import com.ytclassic.app.extractor.YouTubeRepository
import com.ytclassic.app.network.InnertubeFeedClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.Page

data class VideoListUiState(
    val items: List<VideoUi> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val requiresSignIn: Boolean = false,
    val canLoadMore: Boolean = false,
)

class VideoListViewModel(private val source: VideoListSource) : ViewModel() {

    private val _state = MutableStateFlow(VideoListUiState())
    val state: StateFlow<VideoListUiState> = _state

    private var nextPage: Page? = null
    private var searchQuery: String? = (source as? VideoListSource.Search)?.query

    /** Cookie header from [com.ytclassic.app.auth.SessionManager], or null if signed out. */
    var cookieProvider: (() -> String?)? = null

    fun refresh() {
        _state.value = _state.value.copy(isLoading = true, error = null, requiresSignIn = false)
        viewModelScope.launch {
            try {
                when (source) {
                    VideoListSource.Trending -> {
                        val page = YouTubeRepository.trending()
                        nextPage = page.nextPage
                        _state.value = VideoListUiState(
                            items = page.items.map { it.toVideoUi() },
                            canLoadMore = page.nextPage != null,
                        )
                    }
                    is VideoListSource.Search -> {
                        val page = YouTubeRepository.search(source.query)
                        nextPage = page.nextPage
                        _state.value = VideoListUiState(
                            items = page.items.map { it.toVideoUi() },
                            canLoadMore = page.nextPage != null,
                        )
                    }
                    VideoListSource.Home -> loadFeed(isHome = true)
                    VideoListSource.Subscriptions -> loadFeed(isHome = false)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "error")
            }
        }
    }

    private suspend fun loadFeed(isHome: Boolean) {
        val cookie = cookieProvider?.invoke()
        if (cookie.isNullOrBlank()) {
            _state.value = if (isHome) {
                // No personalized feed without a session - Trending is the
                // closest thing NewPipeExtractor can offer while signed out.
                val page = YouTubeRepository.trending()
                nextPage = page.nextPage
                VideoListUiState(items = page.items.map { it.toVideoUi() }, canLoadMore = page.nextPage != null)
            } else {
                VideoListUiState(requiresSignIn = true)
            }
            return
        }

        val items = if (isHome) InnertubeFeedClient.home(cookie) else InnertubeFeedClient.subscriptionsFeed(cookie)
        _state.value = VideoListUiState(items = items, canLoadMore = false)
    }

    fun loadMore() {
        val page = nextPage ?: return
        if (_state.value.isLoadingMore) return
        _state.value = _state.value.copy(isLoadingMore = true)

        viewModelScope.launch {
            try {
                val result = when (source) {
                    VideoListSource.Trending -> null // Trending kiosk has no further pagination in practice.
                    is VideoListSource.Search -> YouTubeRepository.searchMore(source.query, page)
                    else -> null
                }
                if (result == null) {
                    _state.value = _state.value.copy(isLoadingMore = false, canLoadMore = false)
                    return@launch
                }
                nextPage = result.nextPage
                _state.value = _state.value.copy(
                    items = _state.value.items + result.items.map { it.toVideoUi() },
                    isLoadingMore = false,
                    canLoadMore = result.nextPage != null,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoadingMore = false)
            }
        }
    }
}
