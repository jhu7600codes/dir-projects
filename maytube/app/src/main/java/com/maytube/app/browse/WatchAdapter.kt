package com.maytube.app.browse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.maytube.app.R

/**
 * Everything below the player on a watch screen -- title/channel/
 * description, related videos, comments -- as one flat, single-scroll
 * RecyclerView instead of nested scrolling containers (which are a
 * long-standing source of scroll-conflict bugs, especially hard to get
 * right without a real device to test on). The player itself lives outside
 * this adapter entirely (see activity_watch.xml's kdoc).
 */
sealed class WatchRow {
    data class Header(
        val title: String,
        val meta: String,
        val channelName: String?,
        val channelUrl: String?,
        val channelAvatarUrl: String?,
        val description: String?
    ) : WatchRow()

    data class SectionHeader(val title: String) : WatchRow()
    data class RelatedItem(val video: VideoSummary) : WatchRow()
    data class CommentRow(val comment: CommentItem) : WatchRow()
    object CommentsEmpty : WatchRow()
    object LoadMoreComments : WatchRow()
}

class WatchAdapter(
    private val onRelatedClick: (VideoSummary) -> Unit,
    private val onChannelClick: (String) -> Unit,
    private val onLoadMoreComments: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val rows = mutableListOf<WatchRow>()

    fun submit(newRows: List<WatchRow>) {
        rows.clear()
        rows.addAll(newRows)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = rows.size

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is WatchRow.Header -> TYPE_HEADER
        is WatchRow.SectionHeader -> TYPE_SECTION_HEADER
        is WatchRow.RelatedItem -> TYPE_RELATED
        is WatchRow.CommentRow -> TYPE_COMMENT
        WatchRow.CommentsEmpty -> TYPE_COMMENTS_EMPTY
        WatchRow.LoadMoreComments -> TYPE_LOAD_MORE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.item_watch_header, parent, false))
            TYPE_SECTION_HEADER -> SectionHeaderViewHolder(inflater.inflate(R.layout.item_section_header, parent, false))
            TYPE_RELATED -> RelatedViewHolder(inflater.inflate(R.layout.item_video_summary, parent, false))
            TYPE_COMMENT -> CommentViewHolder(inflater.inflate(R.layout.item_comment, parent, false))
            TYPE_COMMENTS_EMPTY -> EmptyViewHolder(inflater.inflate(R.layout.item_empty_text, parent, false))
            else -> LoadMoreViewHolder(inflater.inflate(R.layout.item_load_more, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is WatchRow.Header -> (holder as HeaderViewHolder).bind(row, onChannelClick)
            is WatchRow.SectionHeader -> (holder as SectionHeaderViewHolder).bind(row)
            is WatchRow.RelatedItem -> (holder as RelatedViewHolder).bind(row.video, onRelatedClick)
            is WatchRow.CommentRow -> (holder as CommentViewHolder).bind(row.comment)
            WatchRow.CommentsEmpty ->
                (holder.itemView as TextView).setText(R.string.watch_comments_empty)
            WatchRow.LoadMoreComments -> onLoadMoreComments()
        }
    }

    private class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.watchHeaderTitle)
        private val meta: TextView = itemView.findViewById(R.id.watchHeaderMeta)
        private val channelRow: View = itemView.findViewById<View>(R.id.watchHeaderChannelAvatar).parent as View
        private val channelAvatar: ImageView = itemView.findViewById(R.id.watchHeaderChannelAvatar)
        private val channelName: TextView = itemView.findViewById(R.id.watchHeaderChannelName)
        private val description: TextView = itemView.findViewById(R.id.watchHeaderDescription)
        private val descriptionToggle: TextView = itemView.findViewById(R.id.watchHeaderDescriptionToggle)
        private var expanded = false

        fun bind(row: WatchRow.Header, onChannelClick: (String) -> Unit) {
            title.text = row.title
            meta.text = row.meta
            channelName.text = row.channelName
            channelAvatar.load(row.channelAvatarUrl) { crossfade(true) }
            val channelUrl = row.channelUrl
            channelRow.isClickable = channelUrl != null
            channelRow.setOnClickListener { channelUrl?.let(onChannelClick) }
            description.text = row.description
            val hasDescription = !row.description.isNullOrBlank()
            description.visibility = if (hasDescription) View.VISIBLE else View.GONE
            descriptionToggle.visibility = if (hasDescription) View.VISIBLE else View.GONE
            expanded = false
            description.maxLines = 3
            descriptionToggle.setText(R.string.watch_description_more)
            descriptionToggle.setOnClickListener {
                expanded = !expanded
                description.maxLines = if (expanded) Int.MAX_VALUE else 3
                descriptionToggle.setText(
                    if (expanded) R.string.watch_description_less else R.string.watch_description_more
                )
            }
        }
    }

    private class SectionHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(row: WatchRow.SectionHeader) {
            (itemView as TextView).text = row.title
        }
    }

    private class RelatedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail: ImageView = itemView.findViewById(R.id.summaryThumbnail)
        private val duration: TextView = itemView.findViewById(R.id.summaryDuration)
        private val title: TextView = itemView.findViewById(R.id.summaryTitle)
        private val meta: TextView = itemView.findViewById(R.id.summaryMeta)

        fun bind(video: VideoSummary, onClick: (VideoSummary) -> Unit) {
            thumbnail.load(video.thumbnailUrl) { crossfade(true) }
            duration.text = video.durationText
            duration.visibility = if (video.durationText.isNullOrBlank()) View.GONE else View.VISIBLE
            title.text = video.title
            meta.text = listOfNotNull(video.channelName, video.viewCountText).joinToString(" • ")
            itemView.setOnClickListener { onClick(video) }
        }
    }

    private class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatar: ImageView = itemView.findViewById(R.id.commentAvatar)
        private val author: TextView = itemView.findViewById(R.id.commentAuthor)
        private val text: TextView = itemView.findViewById(R.id.commentText)
        private val meta: TextView = itemView.findViewById(R.id.commentMeta)

        fun bind(comment: CommentItem) {
            avatar.load(comment.authorAvatarUrl) { crossfade(true) }
            author.text = comment.author
            text.text = comment.text
            meta.text = listOfNotNull(comment.timeText, comment.scoreText?.let { "$it points" })
                .joinToString(" • ")
        }
    }

    private class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    private class LoadMoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_SECTION_HEADER = 1
        private const val TYPE_RELATED = 2
        private const val TYPE_COMMENT = 3
        private const val TYPE_COMMENTS_EMPTY = 4
        private const val TYPE_LOAD_MORE = 5
    }
}
