package com.maytube.app.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
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
import com.maytube.app.data.ServerConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Native equivalent of yt2009's /results -- see Yt2009Api.search. */
class SearchActivity : AppCompatActivity() {

    private lateinit var input: EditText
    private lateinit var list: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var adapter: VideoSummaryAdapter

    private var config: ServerConfig? = null
    private var currentQuery: String? = null
    private var nextPage = 1
    private var loadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        config = (application as MaytubeApp).serverConfigRepository.get()

        input = findViewById(R.id.searchInput)
        list = findViewById(R.id.searchList)
        progress = findViewById(R.id.searchProgress)

        findViewById<ImageButton>(R.id.searchBackButton).setOnClickListener { finish() }

        adapter = VideoSummaryAdapter(onClick = ::openVideo, onLoadMore = ::loadMore)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
        list.addItemDecoration(HairlineDividerDecoration(this))

        input.setOnEditorActionListener { _, actionId, event ->
            val triggered = actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            if (triggered) {
                runSearch(input.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }
        input.requestFocus()
    }

    private fun openVideo(video: VideoSummary) {
        startActivity(WatchActivity.intent(this, video.videoId))
    }

    private fun runSearch(query: String) {
        val cfg = config ?: return
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        currentQuery = trimmed
        nextPage = 1
        loadJob?.cancel()
        progress.visibility = View.VISIBLE
        loadJob = lifecycleScope.launch {
            try {
                val result = Yt2009Api.search(cfg, trimmed, page = 1)
                adapter.submit(result.videos, hasMorePages = result.nextPageUrl != null)
                nextPage = 2
            } catch (e: Exception) {
                adapter.submit(emptyList(), hasMorePages = false)
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun loadMore() {
        val cfg = config ?: return
        val query = currentQuery ?: return
        if (loadJob?.isActive == true) return
        adapter.setLoadingMore()
        loadJob = lifecycleScope.launch {
            try {
                val result = Yt2009Api.search(cfg, query, page = nextPage)
                adapter.append(result.videos, hasMorePages = result.nextPageUrl != null)
                nextPage++
            } catch (e: Exception) {
                adapter.append(emptyList(), hasMorePages = false)
            }
        }
    }
}
