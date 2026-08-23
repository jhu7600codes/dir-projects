package com.ytclassic.app.data.model

import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.comments.CommentsInfoItem

data class CommentUi(
    val commentId: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val text: String,
    val likeCount: Int,
    val relativeTime: String?,
    val replyCount: Int,
    val repliesPage: Page?,
    val isPinned: Boolean,
    val isHeartedByUploader: Boolean,
)

fun CommentsInfoItem.toCommentUi(): CommentUi = CommentUi(
    commentId = commentId.orEmpty(),
    authorName = uploaderName.orEmpty(),
    authorAvatarUrl = uploaderAvatars.bestUrl(),
    text = commentText.content.orEmpty(),
    likeCount = likeCount,
    relativeTime = textualUploadDate,
    replyCount = replyCount,
    repliesPage = replies,
    isPinned = isPinned,
    isHeartedByUploader = isHeartedByUploader,
)
