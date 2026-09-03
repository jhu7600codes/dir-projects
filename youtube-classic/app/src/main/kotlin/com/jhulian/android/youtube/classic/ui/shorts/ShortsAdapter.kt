package com.jhulian.android.youtube.classic.ui.shorts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.Player
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.jhulian.android.youtube.classic.R
import com.jhulian.android.youtube.classic.data.model.VideoUi
import com.jhulian.android.youtube.classic.databinding.ItemShortBinding

/**
 * Each item shows a static thumbnail by default; only the currently
 * centered item ever gets the shared [Player] attached (see
 * [ShortsFragment]) - running one ExoPlayer instance per visible item
 * instead of one shared instance would be needlessly expensive for a
 * vertical feed where at most one is ever playing.
 */
class ShortsAdapter(
    private val onLike: (VideoUi) -> Unit,
    private val onComment: (VideoUi) -> Unit,
    private val onShare: (VideoUi) -> Unit,
    private val onChannel: (VideoUi) -> Unit,
) : ListAdapter<VideoUi, ShortsAdapter.ShortViewHolder>(DIFF_CALLBACK) {

    private var activePosition = -1
    private var activePlayer: Player? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortViewHolder {
        val binding = ItemShortBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.root.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, parent.height)
        return ShortViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShortViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onLike, onComment, onShare, onChannel)
        holder.setActive(position == activePosition, if (position == activePosition) activePlayer else null)
    }

    /** Attaches [player] to [position]'s PlayerView and detaches it from whatever was active before. */
    fun setActivePosition(position: Int, player: Player?, recyclerView: RecyclerView) {
        val previous = activePosition
        activePosition = position
        activePlayer = player

        if (previous in 0 until itemCount) {
            (recyclerView.findViewHolderForAdapterPosition(previous) as? ShortViewHolder)?.setActive(false, null)
        }
        (recyclerView.findViewHolderForAdapterPosition(position) as? ShortViewHolder)?.setActive(true, player)
    }

    class ShortViewHolder(private val binding: ItemShortBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            video: VideoUi,
            onLike: (VideoUi) -> Unit,
            onComment: (VideoUi) -> Unit,
            onShare: (VideoUi) -> Unit,
            onChannel: (VideoUi) -> Unit,
        ) {
            binding.channelName.text = video.channelName
            binding.title.text = video.title
            Glide.with(binding.thumbnail).load(video.thumbnailUrl).into(binding.thumbnail)
            Glide.with(binding.channelAvatar)
                .load(video.channelAvatarUrl)
                .placeholder(R.drawable.ic_account_circle)
                .transform(CircleCrop())
                .into(binding.channelAvatar)

            binding.root.setOnClickListener {
                val player = binding.playerView.player ?: return@setOnClickListener
                if (player.isPlaying) {
                    player.pause()
                    binding.pauseOverlayIcon.visibility = View.VISIBLE
                } else {
                    player.play()
                    binding.pauseOverlayIcon.visibility = View.GONE
                }
            }
            binding.likeButton.setOnClickListener { onLike(video) }
            binding.commentButton.setOnClickListener { onComment(video) }
            binding.shareButton.setOnClickListener { onShare(video) }
            binding.channelAvatar.setOnClickListener { onChannel(video) }
            binding.channelName.setOnClickListener { onChannel(video) }
            binding.subscribeButton.setOnClickListener { onChannel(video) }
        }

        fun setActive(active: Boolean, player: Player?) {
            binding.pauseOverlayIcon.visibility = View.GONE
            if (active && player != null) {
                binding.playerView.player = player
                binding.playerView.visibility = View.VISIBLE
            } else {
                binding.playerView.player = null
                binding.playerView.visibility = View.GONE
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<VideoUi>() {
            override fun areItemsTheSame(oldItem: VideoUi, newItem: VideoUi) = oldItem.videoId == newItem.videoId
            override fun areContentsTheSame(oldItem: VideoUi, newItem: VideoUi) = oldItem == newItem
        }
    }
}
