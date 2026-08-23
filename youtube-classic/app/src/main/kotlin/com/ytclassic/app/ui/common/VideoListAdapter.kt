package com.ytclassic.app.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.ytclassic.app.data.model.VideoUi
import com.ytclassic.app.databinding.ItemVideoBinding

class VideoListAdapter(
    private val onClick: (VideoUi) -> Unit,
    private val onOverflowClick: (VideoUi, android.view.View) -> Unit,
) : ListAdapter<VideoUi, VideoListAdapter.VideoViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position), onClick, onOverflowClick)
    }

    class VideoViewHolder(private val binding: ItemVideoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(video: VideoUi, onClick: (VideoUi) -> Unit, onOverflowClick: (VideoUi, android.view.View) -> Unit) {
            binding.title.text = video.title
            binding.metadata.text = listOfNotNull(video.channelName, video.metadataLine)
                .filter { it.isNotBlank() }
                .joinToString("  •  ")

            if (video.isLive) {
                binding.duration.setBackgroundResource(com.ytclassic.app.R.drawable.bg_live_badge)
                binding.duration.text = "LIVE"
                binding.duration.visibility = android.view.View.VISIBLE
            } else if (!video.durationText.isNullOrBlank()) {
                binding.duration.setBackgroundResource(com.ytclassic.app.R.drawable.bg_duration_badge)
                binding.duration.text = video.durationText
                binding.duration.visibility = android.view.View.VISIBLE
            } else {
                binding.duration.visibility = android.view.View.GONE
            }

            Glide.with(binding.thumbnail)
                .load(video.thumbnailUrl)
                .placeholder(com.ytclassic.app.R.color.yt_chip_background)
                .into(binding.thumbnail)

            Glide.with(binding.channelAvatar)
                .load(video.channelAvatarUrl)
                .placeholder(com.ytclassic.app.R.drawable.ic_account_circle)
                .transform(CircleCrop())
                .into(binding.channelAvatar)

            binding.root.setOnClickListener { onClick(video) }
            binding.overflow.setOnClickListener { onOverflowClick(video, it) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<VideoUi>() {
            override fun areItemsTheSame(oldItem: VideoUi, newItem: VideoUi) = oldItem.videoId == newItem.videoId
            override fun areContentsTheSame(oldItem: VideoUi, newItem: VideoUi) = oldItem == newItem
        }
    }
}
