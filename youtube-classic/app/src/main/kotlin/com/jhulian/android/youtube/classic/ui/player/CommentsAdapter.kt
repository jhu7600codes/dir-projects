package com.jhulian.android.youtube.classic.ui.player

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.jhulian.android.youtube.classic.R
import com.jhulian.android.youtube.classic.data.model.CommentUi
import com.jhulian.android.youtube.classic.databinding.ItemCommentBinding
import com.jhulian.android.youtube.classic.databinding.ItemCommentReplyBinding
import com.jhulian.android.youtube.classic.util.Formatters

/**
 * Flat list mixing top-level comments and (once expanded) their replies,
 * indented below - simpler than a nested RecyclerView-in-RecyclerView and
 * plays nicely with this screen's single outer NestedScrollView.
 */
class CommentsAdapter(
    private val onLike: (CommentUi) -> Unit,
    private val onToggleReplies: (CommentUi) -> Unit,
) : ListAdapter<CommentsAdapter.Row, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    sealed class Row {
        abstract val rowId: String
        data class TopLevel(val comment: CommentUi, val expanded: Boolean) : Row() {
            override val rowId get() = comment.commentId
        }
        data class Reply(val reply: CommentUi, val parentId: String, val index: Int) : Row() {
            override val rowId get() = "$parentId:reply:$index"
        }
    }

    private val expandedIds = mutableSetOf<String>()
    private var topLevelComments: List<CommentUi> = emptyList()
    private val repliesByParent = mutableMapOf<String, List<CommentUi>>()

    fun submitComments(comments: List<CommentUi>) {
        topLevelComments = comments
        rebuild()
    }

    fun setReplies(parentId: String, replies: List<CommentUi>) {
        repliesByParent[parentId] = replies
        expandedIds.add(parentId)
        rebuild()
    }

    fun collapseReplies(parentId: String) {
        expandedIds.remove(parentId)
        rebuild()
    }

    fun isExpanded(parentId: String) = expandedIds.contains(parentId)

    private fun rebuild() {
        val rows = mutableListOf<Row>()
        for (comment in topLevelComments) {
            val expanded = expandedIds.contains(comment.commentId)
            rows.add(Row.TopLevel(comment, expanded))
            if (expanded) {
                repliesByParent[comment.commentId]?.forEachIndexed { index, reply ->
                    rows.add(Row.Reply(reply, comment.commentId, index))
                }
            }
        }
        submitList(rows)
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is Row.TopLevel -> VIEW_TYPE_COMMENT
        is Row.Reply -> VIEW_TYPE_REPLY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_COMMENT) {
            CommentViewHolder(ItemCommentBinding.inflate(inflater, parent, false))
        } else {
            ReplyViewHolder(ItemCommentReplyBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is Row.TopLevel -> (holder as CommentViewHolder).bind(row, onLike, onToggleReplies)
            is Row.Reply -> (holder as ReplyViewHolder).bind(row.reply, onLike)
        }
    }

    class CommentViewHolder(private val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(row: Row.TopLevel, onLike: (CommentUi) -> Unit, onToggleReplies: (CommentUi) -> Unit) {
            val comment = row.comment
            binding.authorAndTime.text = listOfNotNull(comment.authorName, comment.relativeTime)
                .joinToString("  •  ")
            binding.commentText.text = HtmlCompat.fromHtml(comment.text, HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.likeCount.text = if (comment.likeCount > 0) Formatters.compactCount(comment.likeCount.toLong()) else ""
            binding.likeButton.setOnClickListener { onLike(comment) }

            if (comment.replyCount > 0) {
                binding.viewRepliesToggle.visibility = android.view.View.VISIBLE
                binding.viewRepliesToggle.text = binding.root.context.getString(
                    if (row.expanded) R.string.hide_replies else R.string.view_replies,
                    comment.replyCount,
                )
                binding.viewRepliesToggle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    if (row.expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more, 0, 0, 0,
                )
                binding.viewRepliesToggle.setOnClickListener { onToggleReplies(comment) }
            } else {
                binding.viewRepliesToggle.visibility = android.view.View.GONE
            }

            Glide.with(binding.avatar)
                .load(comment.authorAvatarUrl)
                .placeholder(R.drawable.ic_account_circle)
                .transform(CircleCrop())
                .into(binding.avatar)
        }
    }

    class ReplyViewHolder(private val binding: ItemCommentReplyBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: CommentUi, onLike: (CommentUi) -> Unit) {
            binding.authorAndTime.text = listOfNotNull(comment.authorName, comment.relativeTime)
                .joinToString("  •  ")
            binding.commentText.text = HtmlCompat.fromHtml(comment.text, HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.likeCount.text = if (comment.likeCount > 0) Formatters.compactCount(comment.likeCount.toLong()) else ""
            binding.likeButton.setOnClickListener { onLike(comment) }

            Glide.with(binding.avatar)
                .load(comment.authorAvatarUrl)
                .placeholder(R.drawable.ic_account_circle)
                .transform(CircleCrop())
                .into(binding.avatar)
        }
    }

    companion object {
        private const val VIEW_TYPE_COMMENT = 0
        private const val VIEW_TYPE_REPLY = 1

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Row>() {
            override fun areItemsTheSame(oldItem: Row, newItem: Row) = oldItem.rowId == newItem.rowId
            override fun areContentsTheSame(oldItem: Row, newItem: Row) = oldItem == newItem
        }
    }
}
