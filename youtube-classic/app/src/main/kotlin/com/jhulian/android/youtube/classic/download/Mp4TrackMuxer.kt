package com.jhulian.android.youtube.classic.download

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/**
 * Combines a downloaded video-only file and a downloaded audio-only file
 * into a single playable MP4 using Android's built-in MediaExtractor/
 * MediaMuxer - no ffmpeg, no native code. This is the standard way to
 * assemble YouTube's adaptive (DASH-style) streams, which is what
 * NewPipeExtractor hands back as separate `videoOnlyStreams` and
 * `audioStreams` for anything above the lowest progressive qualities.
 */
object Mp4TrackMuxer {

    class MuxException(message: String, cause: Throwable? = null) : Exception(message, cause)

    fun mux(videoOnly: File, audioOnly: File, output: File) {
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var videoTrack: SelectedTrack? = null
        var audioTrack: SelectedTrack? = null
        var started = false
        try {
            videoTrack = openTrack(videoOnly, "video/")
            audioTrack = openTrack(audioOnly, "audio/")

            val videoOutTrack = muxer.addTrack(videoTrack.extractor.getTrackFormat(videoTrack.trackIndex))
            val audioOutTrack = muxer.addTrack(audioTrack.extractor.getTrackFormat(audioTrack.trackIndex))

            muxer.start()
            started = true

            copySamples(videoTrack.extractor, muxer, videoOutTrack, bufferSizeFor(videoTrack))
            copySamples(audioTrack.extractor, muxer, audioOutTrack, bufferSizeFor(audioTrack))
        } catch (e: Exception) {
            throw MuxException("failed to mux video+audio into ${output.name}: ${e.message}", e)
        } finally {
            try {
                if (started) muxer.stop()
            } catch (_: Exception) {
            }
            muxer.release()
            videoTrack?.extractor?.release()
            audioTrack?.extractor?.release()
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
        val bufferInfo = MediaCodec.BufferInfo()
        val buffer = ByteBuffer.allocate(bufferSize)

        while (true) {
            buffer.clear()
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break

            bufferInfo.offset = 0
            bufferInfo.size = sampleSize
            bufferInfo.presentationTimeUs = extractor.sampleTime
            // MediaExtractor.sampleFlags (SAMPLE_FLAG_*) and
            // MediaCodec.BufferInfo.flags (BUFFER_FLAG_*) overlap only on
            // the sync/keyframe bit - map explicitly instead of passing
            // extractor flags straight through.
            var muxerFlags = 0
            if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                muxerFlags = muxerFlags or MediaCodec.BUFFER_FLAG_KEY_FRAME
            }
            bufferInfo.flags = muxerFlags

            muxer.writeSampleData(outTrackIndex, buffer, bufferInfo)
            extractor.advance()
        }
    }
}
