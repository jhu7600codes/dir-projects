package com.jhulian.android.youtube.classic.ui.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhulian.android.youtube.classic.data.model.CommentUi
import com.jhulian.android.youtube.classic.data.model.toCommentUi
import com.jhulian.android.youtube.classic.extractor.YouTubeRepository
import com.jhulian.android.youtube.classic.network.InnertubeActions
import com.jhulian.android.youtube.classic.network.LikeStatus
import com.jhulian.android.youtube.classic.network.SponsorBlockClient
import com.jhulian.android.youtube.classic.network.SponsorSegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.stream.StreamInfo

data class PlayerUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val streamInfo: StreamInfo? = null,
    val likeStatus: LikeStatus = LikeStatus.NONE,
    val isSubscribed: Boolean = false,
    val sponsorSegments: List<SponsorSegment> = emptyList(),
    val comments: List<CommentUi> = emptyList(),
    val commentsLoading: Boolean = false,
    val commentsNextPage: Page? = null,
    val commentCount: Long = -1,
)

class PlayerViewModel : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state

    private var currentUrl: String? = null

    /** Reply pages currently expanded, so re-rendering doesn't refetch them. */
    private val expandedReplies = mutableMapOf<String, List<CommentUi>>()

    fun load(url: String, sponsorBlockEnabled: Boolean, sponsorCategories: List<String>) {
        if (currentUrl == url && _state.value.streamInfo != null) return
        currentUrl = url
        _state.value = PlayerUiState(isLoading = true)

        viewModelScope.launch {
            try {
                val info = YouTubeRepository.streamInfo(url)
                _state.value = _state.value.copy(isLoading = false, streamInfo = info)
                loadComments(url)

                if (sponsorBlockEnabled) {
                    val videoId = extractVideoId(url)
                    if (videoId != null) {
                        val segments = runCatching {
                            SponsorBlockClient.fetchSegments(videoId, sponsorCategories)
                        }.getOrDefault(emptyList())
                        _state.value = _state.value.copy(sponsorSegments = segments)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "load($url) failed", e)
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load video")
            }
        }
    }

    private fun loadComments(url: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(commentsLoading = true)
            try {
                val page = YouTubeRepository.comments(url)
                _state.value = _state.value.copy(
                    comments = page.items.map { it.toCommentUi() },
                    commentsNextPage = page.nextPage,
                    commentsLoading = false,
                )
            } catch (e: Exception) {
                Log.e(TAG, "loadComments($url) failed", e)
                _state.value = _state.value.copy(commentsLoading = false)
            }
        }
    }

    fun loadMoreComments() {
        val url = currentUrl ?: return
        val page = _state.value.commentsNextPage ?: return
        if (_state.value.commentsLoading) return

        viewModelScope.launch {
            _state.value = _state.value.copy(commentsLoading = true)
            try {
                val next = YouTubeRepository.commentsMore(url, page)
                _state.value = _state.value.copy(
                    comments = _state.value.comments + next.items.map { it.toCommentUi() },
                    commentsNextPage = next.nextPage,
                    commentsLoading = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(commentsLoading = false)
            }
        }
    }

    fun toggleLike(newStatus: LikeStatus, cookie: String) {
        val info = _state.value.streamInfo ?: return
        val videoId = extractVideoId(info.url) ?: return
        val previous = _state.value.likeStatus
        val target = if (previous == newStatus) LikeStatus.NONE else newStatus
        _state.value = _state.value.copy(likeStatus = target)
        viewModelScope.launch {
            runCatching { InnertubeActions.setLikeStatus(videoId, target, cookie) }
                .onFailure { _state.value = _state.value.copy(likeStatus = previous) }
        }
    }

    fun toggleSubscribe(cookie: String) {
        val channelId = _state.value.streamInfo?.uploaderUrl?.let { extractChannelId(it) } ?: return
        val target = !_state.value.isSubscribed
        _state.value = _state.value.copy(isSubscribed = target)
        viewModelScope.launch {
            runCatching { InnertubeActions.setSubscribed(channelId, target, cookie) }
                .onFailure { _state.value = _state.value.copy(isSubscribed = !target) }
        }
    }

    fun postComment(text: String, cookie: String, onDone: (Boolean) -> Unit) {
        val videoId = currentUrl?.let { extractVideoId(it) } ?: return onDone(false)
        viewModelScope.launch {
            val success = runCatching { InnertubeActions.postComment(videoId, text, cookie) }.getOrDefault(false)
            if (success) loadComments(currentUrl!!)
            onDone(success)
        }
    }

    fun repliesFor(commentId: String): List<CommentUi>? = expandedReplies[commentId]

    fun loadReplies(comment: CommentUi, onLoaded: (List<CommentUi>) -> Unit) {
        val url = currentUrl ?: return
        val page = comment.repliesPage ?: return
        expandedReplies[comment.commentId]?.let { onLoaded(it); return }

        viewModelScope.launch {
            val result = runCatching { YouTubeRepository.replies(url, page) }.getOrNull()
            val replies = result?.items?.map { it.toCommentUi() } ?: emptyList()
            expandedReplies[comment.commentId] = replies
            onLoaded(replies)
        }
    }

    companion object {
        private const val TAG = "PlayerViewModel"

        fun extractVideoId(url: String): String? {
            val shortsMarker = "/shorts/"
            val shortsIdx = url.indexOf(shortsMarker)
            if (shortsIdx >= 0) {
                val start = shortsIdx + shortsMarker.length
                val end = url.indexOf('?', start).let { if (it == -1) url.length else it }
                return url.substring(start, end)
            }
            val marker = "v="
            val idx = url.indexOf(marker)
            if (idx < 0) return null
            val start = idx + marker.length
            val end = url.indexOf('&', start)
            return if (end == -1) url.substring(start) else url.substring(start, end)
        }

        private fun extractChannelId(channelUrl: String): String? {
            val marker = "/channel/"
            val idx = channelUrl.indexOf(marker)
            if (idx < 0) return null
            val start = idx + marker.length
            val end = channelUrl.indexOf('/', start).let { if (it == -1) channelUrl.length else it }
            return channelUrl.substring(start, end)
        }
    }
}
