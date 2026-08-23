package com.ytclassic.app.ui.channel

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.ytclassic.app.R
import com.ytclassic.app.YtClassicApp
import com.ytclassic.app.data.model.toVideoUi
import com.ytclassic.app.databinding.ActivityChannelBinding
import com.ytclassic.app.extractor.YouTubeRepository
import com.ytclassic.app.network.InnertubeActions
import com.ytclassic.app.ui.common.VideoListAdapter
import com.ytclassic.app.ui.player.PlayerActivity
import kotlinx.coroutines.launch

class ChannelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChannelBinding
    private lateinit var adapter: VideoListAdapter
    private var isSubscribed = false
    private var channelId: String? = null

    private val sessionManager get() = (application as YtClassicApp).sessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChannelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val channelUrl = intent.getStringExtra(EXTRA_CHANNEL_URL)
        if (channelUrl == null) {
            finish()
            return
        }
        channelId = channelUrl.substringAfter("/channel/").substringBefore("/")

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = VideoListAdapter(
            onClick = { video -> PlayerActivity.start(this, video.url) },
            onOverflowClick = { _, _ -> },
        )
        binding.videosRecycler.layoutManager = LinearLayoutManager(this)
        binding.videosRecycler.adapter = adapter

        binding.subscribeButton.setOnClickListener {
            val cookie = sessionManager.cookie
            val id = channelId
            if (cookie.isNullOrBlank() || id == null) return@setOnClickListener
            isSubscribed = !isSubscribed
            updateSubscribeButton()
            lifecycleScope.launch {
                runCatching { InnertubeActions.setSubscribed(id, isSubscribed, cookie) }
            }
        }

        loadChannel(channelUrl)
    }

    private fun loadChannel(url: String) {
        lifecycleScope.launch {
            try {
                val data = YouTubeRepository.channel(url)
                binding.channelName.text = data.name
                binding.channelSubs.text = if (data.subscriberCount > 0) {
                    "${com.ytclassic.app.util.Formatters.compactCount(data.subscriberCount)} subscribers"
                } else {
                    ""
                }
                Glide.with(binding.avatar)
                    .load(data.avatarUrl)
                    .placeholder(R.drawable.ic_account_circle)
                    .transform(CircleCrop())
                    .into(binding.avatar)
                Glide.with(binding.banner).load(data.bannerUrl).into(binding.banner)

                adapter.submitList(data.videos.map { it.toVideoUi() })
            } catch (e: Exception) {
                // Leave the header blank and the video list empty on failure;
                // no dedicated error state for this screen yet.
            } finally {
                binding.progress.visibility = View.GONE
            }
        }
    }

    private fun updateSubscribeButton() {
        binding.subscribeButton.text = if (isSubscribed) getString(R.string.subscribed) else getString(R.string.subscribe)
    }

    companion object {
        const val EXTRA_CHANNEL_URL = "channel_url"
    }
}
