package com.maytube.app.player

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Reads from a local file another coroutine is actively appending to (see
 * [StreamingPlayer.FetchState]) -- unlike a plain FileDataSource, which
 * snapshots the file's length once at [open] and refuses to read past it,
 * this keeps polling for newly-written bytes instead of reporting
 * end-of-stream the moment it catches up to the writer. ExoPlayer's own MP4
 * extractor handles a growing fragmented-MP4 source (a moof/mdat pair per
 * SABR segment) fine as long as the DataSource keeps handing it bytes
 * rather than ending the stream prematurely -- this is that DataSource.
 */
@UnstableApi
class GrowingFileDataSource(
    private val state: StreamingPlayer.FetchState,
    private val isAudio: Boolean
) : BaseDataSource(/* isNetwork= */ false) {

    private var file: RandomAccessFile? = null
    private var position = 0L
    private var bytesRemaining = C.LENGTH_UNSET.toLong()

    override fun open(dataSpec: DataSpec): Long {
        val path = if (isAudio) state.audioFile else state.videoFile
        val raf = RandomAccessFile(path, "r")
        raf.seek(dataSpec.position)
        file = raf
        position = dataSpec.position
        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            C.LENGTH_UNSET.toLong()
        }
        transferInitializing(dataSpec)
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val raf = file ?: throw IOException("read() called before open()")
        val wantToRead = if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            minOf(length.toLong(), bytesRemaining).toInt()
        } else {
            length
        }
        if (wantToRead <= 0) return C.RESULT_END_OF_INPUT

        while (true) {
            val written = if (isAudio) state.audioBytesWritten else state.videoBytesWritten
            val available = written - position
            if (available > 0) {
                raf.seek(position)
                val n = raf.read(buffer, offset, minOf(wantToRead.toLong(), available).toInt())
                if (n > 0) {
                    position += n
                    if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= n
                    bytesTransferred(n)
                    return n
                }
            }
            val fetchError = state.error
            if (fetchError != null) throw IOException("SABR fragment fetch failed", fetchError)
            val done = if (isAudio) state.audioDone else state.videoDone
            if (done) return C.RESULT_END_OF_INPUT
            // more data is still on its way from StreamingPlayer's fetch
            // loop -- wait for it instead of ending the stream
            try {
                Thread.sleep(POLL_INTERVAL_MS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("interrupted waiting for more data", e)
            }
        }
    }

    override fun getUri() = null

    override fun close() {
        try {
            file?.close()
        } finally {
            file = null
            transferEnded()
        }
    }

    class Factory(private val state: StreamingPlayer.FetchState, private val isAudio: Boolean) : DataSource.Factory {
        override fun createDataSource(): DataSource = GrowingFileDataSource(state, isAudio)
    }

    companion object {
        private const val POLL_INTERVAL_MS = 80L
    }
}
