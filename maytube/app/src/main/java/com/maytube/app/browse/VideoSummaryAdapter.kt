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
 * Renders a flat list of [VideoSummary] -- used by home, search results,
 * related videos, and channel video grids alike, since they're all "a list
 * of videos" once parsed. A "load more" footer row is appended whenever the
 * page being shown has a next page (infinite-scroll style, matching how
 * yt2009's own pagination works -- see Yt2009Api).
 */
class VideoSummaryAdapter(
    private val onClick: (VideoSummary) -> Unit,
    private val onLoadMore: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<VideoSummary>()
    private var hasMore = false
    private var loadingMore = false

    fun submit(videos: List<VideoSummary>, hasMorePages: Boolean) {
        items.clear()
        items.addAll(videos)
        hasMore = hasMorePages
        loadingMore = false
        notifyDataSetChanged()
    }

    fun append(videos: List<VideoSummary>, hasMorePages: Boolean) {
        val start = items.size
        items.addAll(videos)
        hasMore = hasMorePages
        loadingMore = false
        notifyItemRangeInserted(start, videos.size)
        if (!hasMore) notifyItemChanged(items.size)
    }

    fun setLoadingMore() {
        if (loadingMore || !hasMore) return
        loadingMore = true
        notifyItemChanged(items.size)
    }

    override fun getItemCount(): Int = items.size + if (hasMore) 1 else 0

    override fun getItemViewType(position: Int): Int =
        if (position >= items.size) VIEW_TYPE_FOOTER else VIEW_TYPE_VIDEO

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_FOOTER) {
            FooterViewHolder(inflater.inflate(R.layout.item_load_more, parent, false))
        } else {
            VideoViewHolder(inflater.inflate(R.layout.item_video_summary, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is VideoViewHolder) {
            holder.bind(items[position], onClick)
        } else if (holder is FooterViewHolder) {
            // getting bound at all means we're near the end of the list --
            // ask for the next page (idempotent: setLoadingMore no-ops once
            // a fetch is already in flight for this page)
            onLoadMore()
        }
    }

    private class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail: ImageView = itemView.findViewById(R.id.summaryThumbnail)
        private val duration: TextView = itemView.findViewById(R.id.summaryDuration)
        private val title: TextView = itemView.findViewById(R.id.summaryTitle)
        private val meta: TextView = itemView.findViewById(R.id.summaryMeta)

        fun bind(video: VideoSummary, onClick: (VideoSummary) -> Unit) {
            thumbnail.load(video.thumbnailUrl) {
                crossfade(true)
            }
            duration.text = video.durationText
            duration.visibility = if (video.durationText.isNullOrBlank()) View.GONE else View.VISIBLE
            title.text = video.title
            meta.text = listOfNotNull(video.channelName, video.viewCountText, video.uploadedText)
                .joinToString(" • ")
            itemView.setOnClickListener { onClick(video) }
        }
    }

    private class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    companion object {
        private const val VIEW_TYPE_VIDEO = 0
        private const val VIEW_TYPE_FOOTER = 1
    }
}
