package com.jhulian.android.youtube.classic.download

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

enum class DownloadStatus { QUEUED, DOWNLOADING, MUXING, COMPLETE, FAILED }

data class DownloadEntry(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String?,
    val status: DownloadStatus,
    val progressPercent: Int = 0,
    val filePath: String? = null,
    val fileSizeBytes: Long = 0,
)

/**
 * In-memory + on-disk index of downloads, so [ui.downloads.DownloadsActivity]
 * has something to show across process restarts and [DownloadService] has
 * somewhere to report progress that isn't tied to any particular Activity
 * being alive.
 */
object DownloadsStore {

    private val _entries = MutableStateFlow<List<DownloadEntry>>(emptyList())
    val entries: StateFlow<List<DownloadEntry>> = _entries

    private lateinit var indexFile: File
    private var initialized = false

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        indexFile = File(context.filesDir, "downloads_index.json")
        _entries.value = load()
    }

    fun upsert(entry: DownloadEntry) {
        _entries.update { current ->
            val without = current.filterNot { it.videoId == entry.videoId }
            without + entry
        }
        persist()
    }

    fun remove(videoId: String) {
        _entries.update { current -> current.filterNot { it.videoId == videoId } }
        persist()
    }

    fun get(videoId: String): DownloadEntry? = _entries.value.firstOrNull { it.videoId == videoId }

    private fun persist() {
        if (!initialized) return
        val array = JSONArray()
        for (entry in _entries.value) {
            array.put(
                JSONObject().apply {
                    put("videoId", entry.videoId)
                    put("title", entry.title)
                    put("thumbnailUrl", entry.thumbnailUrl)
                    put("status", entry.status.name)
                    put("progressPercent", entry.progressPercent)
                    put("filePath", entry.filePath)
                    put("fileSizeBytes", entry.fileSizeBytes)
                },
            )
        }
        runCatching { indexFile.writeText(array.toString()) }
    }

    private fun load(): List<DownloadEntry> {
        if (!indexFile.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(indexFile.readText())
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                DownloadEntry(
                    videoId = obj.getString("videoId"),
                    title = obj.optString("title"),
                    thumbnailUrl = obj.optString("thumbnailUrl").takeIf { it.isNotBlank() },
                    status = runCatching { DownloadStatus.valueOf(obj.getString("status")) }
                        .getOrDefault(DownloadStatus.FAILED),
                    progressPercent = obj.optInt("progressPercent"),
                    filePath = obj.optString("filePath").takeIf { it.isNotBlank() },
                    fileSizeBytes = obj.optLong("fileSizeBytes"),
                )
            }
        }.getOrDefault(emptyList())
    }
}
