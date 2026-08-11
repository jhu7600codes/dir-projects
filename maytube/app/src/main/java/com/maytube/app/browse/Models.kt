package com.maytube.app.browse

/**
 * A single video as it appears in a listing (home feed, search results,
 * related videos, channel grid) -- everything the row/card UI needs
 * without opening the watch page itself.
 */
data class VideoSummary(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String?,
    val channelName: String?,
    val channelId: String?,
    val viewCountText: String?,
    val durationText: String?,
    val uploadedText: String?
)

/** A page of [VideoSummary] plus whether another page is available. */
data class VideoPage(
    val videos: List<VideoSummary>,
    val nextPageUrl: String?
)

/**
 * Full metadata for a watch page, minus the comments (fetched/paged
 * separately -- see [CommentPage]). yt2009 has no like/dislike counter; the
 * only rating on a video is YouTube-2009-era's 5-star widget
 * (`button.yt2009-stars`, watch.html:298), hence [ratingText] (e.g. "4.5")
 * / [ratingCountText] (e.g. "42 ratings") rather than like/dislike counts.
 */
data class VideoDetails(
    val videoId: String,
    val title: String,
    val channelName: String?,
    val channelUrl: String?,
    val channelAvatarUrl: String?,
    val viewCountText: String?,
    val uploadedText: String?,
    val ratingText: String?,
    val ratingCountText: String?,
    val description: String?,
    val related: List<VideoSummary>
)

data class CommentItem(
    val id: String?,
    val author: String,
    val authorUrl: String?,
    val authorAvatarUrl: String?,
    val text: String,
    val timeText: String?,
    val scoreText: String?
)

/**
 * [nextContinuation] is yt2009's own opaque continuation token (see
 * Yt2009Api.fetchMoreComments), not a URL -- comment paging goes through a
 * `continuation`/`page` request *header*, not a query string.
 */
data class CommentPage(
    val comments: List<CommentItem>,
    val nextContinuation: String?,
    val nextPage: Int?
)

data class ChannelSummary(
    val channelUrl: String,
    val name: String,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val subscriberCountText: String?,
    val description: String?
)

data class ChannelPage(
    val channel: ChannelSummary,
    val videos: List<VideoSummary>
)
