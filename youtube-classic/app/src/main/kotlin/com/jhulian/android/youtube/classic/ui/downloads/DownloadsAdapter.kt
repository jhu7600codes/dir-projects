package com.jhulian.android.youtube.classic.ui.downloads

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jhulian.android.youtube.classic.R
import com.jhulian.android.youtube.classic.databinding.ItemDownloadBinding
import com.jhulian.android.youtube.classic.download.DownloadEntry
import com.jhulian.android.youtube.classic.download.DownloadStatus

class DownloadsAdapter(
    private val onClick: (DownloadEntry) -> Unit,
    private val onRemove: (DownloadEntry) -> Unit,
) : ListAdapter<DownloadEntry, DownloadsAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onClick, onRemove)
    }

    class ViewHolder(private val binding: ItemDownloadBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: DownloadEntry, onClick: (DownloadEntry) -> Unit, onRemove: (DownloadEntry) -> Unit) {
            binding.title.text = entry.title
            Glide.with(binding.thumbnail).load(entry.thumbnailUrl).into(binding.thumbnail)

            binding.status.text = when (entry.status) {
                DownloadStatus.QUEUED -> "Queued"
                DownloadStatus.DOWNLOADING -> "Downloading ${entry.progressPercent}%"
                DownloadStatus.MUXING -> "Processing…"
                DownloadStatus.COMPLETE -> "%.1f MB".format(entry.fileSizeBytes / 1024.0 / 1024.0)
                DownloadStatus.FAILED -> binding.root.context.getString(R.string.download_failed)
            }

            val inProgress = entry.status == DownloadStatus.DOWNLOADING || entry.status == DownloadStatus.MUXING
            binding.progress.visibility = if (inProgress) View.VISIBLE else View.GONE
            binding.progress.isIndeterminate = entry.status == DownloadStatus.MUXING
            binding.progress.progress = entry.progressPercent

            binding.root.setOnClickListener {
                if (entry.status == DownloadStatus.COMPLETE) onClick(entry)
            }
            binding.actionButton.setOnClickListener { onRemove(entry) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DownloadEntry>() {
            override fun areItemsTheSame(oldItem: DownloadEntry, newItem: DownloadEntry) =
                oldItem.videoId == newItem.videoId
            override fun areContentsTheSame(oldItem: DownloadEntry, newItem: DownloadEntry) = oldItem == newItem
        }
    }
}
