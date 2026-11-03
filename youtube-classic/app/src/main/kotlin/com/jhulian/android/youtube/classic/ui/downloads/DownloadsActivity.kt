package com.jhulian.android.youtube.classic.ui.downloads

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jhulian.android.youtube.classic.databinding.ActivityDownloadsBinding
import com.jhulian.android.youtube.classic.download.DownloadEntry
import com.jhulian.android.youtube.classic.download.DownloadsStore
import com.jhulian.android.youtube.classic.ui.player.PlayerActivity
import java.io.File
import kotlinx.coroutines.launch

class DownloadsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownloadsBinding
    private lateinit var adapter: DownloadsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        DownloadsStore.init(applicationContext)

        adapter = DownloadsAdapter(
            onClick = { entry -> playDownload(entry) },
            onRemove = { entry -> removeDownload(entry) },
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        lifecycleScope.launch {
            DownloadsStore.entries.collect { entries ->
                adapter.submitList(entries)
                binding.emptyText.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun playDownload(entry: DownloadEntry) {
        val path = entry.filePath ?: return
        PlayerActivity.startLocal(this, path, entry.title)
    }

    private fun removeDownload(entry: DownloadEntry) {
        entry.filePath?.let { File(it).delete() }
        DownloadsStore.remove(entry.videoId)
    }
}
