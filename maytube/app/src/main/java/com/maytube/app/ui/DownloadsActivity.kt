package com.maytube.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maytube.app.R
import com.maytube.app.download.VideoDownloader
import java.io.File

/** Lists videos downloaded via VideoDownloader and lets you play/delete them. */
class DownloadsActivity : AppCompatActivity() {

    private lateinit var adapter: DownloadsAdapter
    private lateinit var emptyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloads)

        setSupportActionBar(findViewById(R.id.downloadsToolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        emptyView = findViewById(R.id.emptyView)
        val list = findViewById<RecyclerView>(R.id.downloadsList)
        list.layoutManager = LinearLayoutManager(this)
        adapter = DownloadsAdapter(emptyList(), ::openFile, ::confirmDelete)
        list.adapter = adapter

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun refresh() {
        val files = VideoDownloader.listDownloaded(this)
        adapter.submitList(files)
        emptyView.visibility = if (files.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun openFile(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun confirmDelete(file: File) {
        MaterialAlertDialogBuilder(this)
            .setTitle(file.nameWithoutExtension)
            .setMessage(R.string.download_delete)
            .setPositiveButton(R.string.download_delete) { _, _ ->
                file.delete()
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
