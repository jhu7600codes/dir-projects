package com.maytube.app.download

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/**
 * Combines a video-only fragmented-MP4 file and an audio-only
 * fragmented-MP4 file into a single playable MP4, using Android's built in
 * MediaExtractor/MediaMuxer -- no ffmpeg/native code needed.
 *
 * This mirrors the *second* of the two ffmpeg passes yt2009's own
 * server-side downloader does (back/yt2009sabr.js "download": first
 * concatenates per-track fragments, then muxes the resulting video-only and
 * audio-only files together with `-map 0:a -map 1:v -c copy`). We do the
 * concatenation ourselves as plain byte-appends while fetching (see
 * SabrFragmentDownloader) since consecutive SABR fragments for the same
 * track are just continuation fMP4 fragments of one another; this class
 * only needs to do the "combine two separate tracks into one file" step,
 * which is the part that genuinely requires a real muxer.
 */
object Mp4TrackMuxer {

    class MuxException(message: String, cause: Throwable? = null) : Exception(message, cause)

    fun mux(videoOnly: File, audioOnly: File, output: File) {
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var videoExtractor: SelectedTrack? = null
        var audioExtractor: SelectedTrack? = null
        var started = false
        try {
            videoExtractor = openTrack(videoOnly, "video/")
            audioExtractor = openTrack(audioOnly, "audio/")

            val videoOutTrack = muxer.addTrack(videoExtractor.extractor.getTrackFormat(videoExtractor.trackIndex))
            val audioOutTrack = muxer.addTrack(audioExtractor.extractor.getTrackFormat(audioExtractor.trackIndex))

            muxer.start()
            started = true

            copySamples(videoExtractor.extractor, muxer, videoOutTrack, bufferSizeFor(videoExtractor))
            copySamples(audioExtractor.extractor, muxer, audioOutTrack, bufferSizeFor(audioExtractor))
        } catch (e: Exception) {
            throw MuxException("failed to mux video+audio into ${output.name}: ${e.message}", e)
        } finally {
            try {
                if (started) muxer.stop()
            } catch (_: Exception) {
            }
            muxer.release()
            videoExtractor?.extractor?.release()
            audioExtractor?.extractor?.release()
        }
    }

    private class SelectedTrack(val extractor: MediaExtractor, val trackIndex: Int)

    private fun openTrack(file: File, mimePrefix: String): SelectedTrack {
        val extractor = MediaExtractor()
        extractor.setDataSource(file.absolutePath)
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith(mimePrefix)) {
                extractor.selectTrack(i)
                return SelectedTrack(extractor, i)
            }
        }
        extractor.release()
        throw MuxException("no $mimePrefix* track found in ${file.name}")
    }

    private const val DEFAULT_BUFFER_SIZE = 2 * 1024 * 1024

    private fun bufferSizeFor(track: SelectedTrack): Int {
        val format = track.extractor.getTrackFormat(track.trackIndex)
        val declared = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        } else 0
        return maxOf(DEFAULT_BUFFER_SIZE, declared + (64 * 1024))
    }

    private fun copySamples(extractor: MediaExtractor, muxer: MediaMuxer, outTrackIndex: Int, bufferSize: Int) {
        val bufferInfo = android.media.MediaCodec.BufferInfo()
        val buffer = ByteBuffer.allocate(bufferSize)

        while (true) {
            buffer.clear()
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break

            bufferInfo.offset = 0
            bufferInfo.size = sampleSize
            bufferInfo.presentationTimeUs = extractor.sampleTime
            // MediaExtractor.sampleFlags (SAMPLE_FLAG_*) and
            // MediaCodec.BufferInfo.flags (BUFFER_FLAG_*) are different bit
            // sets that happen to overlap on the sync/keyframe bit -- map
            // explicitly rather than passing extractor flags straight
            // through, which would misinterpret any encrypted/partial-frame
            // bits as unrelated codec-config/other flags.
            var muxerFlags = 0
            if (extractor.sampleFlags and android.media.MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                muxerFlags = muxerFlags or android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME
            }
            bufferInfo.flags = muxerFlags

            muxer.writeSampleData(outTrackIndex, buffer, bufferInfo)
            extractor.advance()
        }
    }
}
