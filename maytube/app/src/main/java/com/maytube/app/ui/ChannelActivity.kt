package com.maytube.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.maytube.app.MaytubeApp
import com.maytube.app.R
import com.maytube.app.browse.HairlineDividerDecoration
import com.maytube.app.browse.VideoSummary
import com.maytube.app.browse.VideoSummaryAdapter
import com.maytube.app.browse.Yt2009Api
import kotlinx.coroutines.launch

/**
 * Native channel page: channel's video list only for this first cut
 * (banner/avatar/subscriber count/description are parsed by Yt2009Api
 * already -- see ChannelSummary -- just not surfaced in this UI yet).
 */
class ChannelActivity : AppCompatActivity() {

    private lateinit var titleView: TextView
    private lateinit var list: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var adapter: VideoSummaryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channel)

        titleView = findViewById(R.id.channelTitle)
        list = findViewById(R.id.channelList)
        progress = findViewById(R.id.channelProgress)
        findViewById<ImageButton>(R.id.channelBackButton).setOnClickListener { finish() }

        adapter = VideoSummaryAdapter(onClick = ::openVideo, onLoadMore = {})
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
        list.addItemDecoration(HairlineDividerDecoration(this))

        val channelPath = intent.getStringExtra(EXTRA_CHANNEL_PATH)
        val config = (application as MaytubeApp).serverConfigRepository.get()
        if (channelPath == null || config == null) {
            finish()
            return
        }

        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val page = Yt2009Api.fetchChannel(config, channelPath)
                titleView.text = page.channel.name
                adapter.submit(page.videos, hasMorePages = false)
            } catch (e: Exception) {
                titleView.text = channelPath
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun openVideo(video: VideoSummary) {
        startActivity(WatchActivity.intent(this, video.videoId))
    }

    companion object {
        private const val EXTRA_CHANNEL_PATH = "channel_path"

        fun intent(context: android.content.Context, channelPath: String): Intent =
            Intent(context, ChannelActivity::class.java).putExtra(EXTRA_CHANNEL_PATH, channelPath)
    }
}
