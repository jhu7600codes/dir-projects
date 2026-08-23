package com.jhulian.android.youtube.classic.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream

/** Picks playable streams out of a [StreamInfo] and builds the [MediaItem] for them. */
object StreamSelector {

    /**
     * Builds the [MediaItem] to hand to [com.jhulian.android.youtube.classic.playback.PlaybackService]'s
     * player, preferring (in order): an HLS manifest, a progressive
     * (video+audio combined) stream, or a paired video-only + audio-only
     * stream stitched back together by the service's custom
     * `MediaSource.Factory`.
     *
     * The media id and metadata (title/artwork) set here are what
     * [com.jhulian.android.youtube.classic.ui.MainActivity]'s mini player
     * reads to show what's currently playing and to reopen the full player
     * for it - the mediaId is the original watch-page URL rather than the
     * playable stream URL, since that's what `PlayerActivity.start()` needs.
     */
    @UnstableApi
    fun buildMediaItem(info: StreamInfo, maxHeight: Int): MediaItem? {
        val metadata = MediaMetadata.Builder()
            .setTitle(info.name)
            .setArtworkUri(info.thumbnails.maxByOrNull { it.height }?.url?.let { Uri.parse(it) })
            .build()

        info.hlsUrl?.takeIf { it.isNotBlank() }?.let { hlsUrl ->
            return MediaItem.Builder()
                .setUri(hlsUrl)
                .setMediaId(info.url)
                .setMediaMetadata(metadata)
                .build()
        }

        val progressive = info.videoStreams
            ?.filter { it.height <= maxHeight || maxHeight <= 0 }
            ?.maxByOrNull { it.height }
        if (progressive != null) {
            return MediaItem.Builder()
                .setUri(progressive.content)
                .setMediaId(info.url)
                .setMediaMetadata(metadata)
                .build()
        }

        val videoOnly = bestVideoOnly(info, maxHeight) ?: return null
        val audioOnly = bestAudio(info) ?: return null

        return MediaItem.Builder()
            .setUri(videoOnly.content)
            .setMediaId(info.url)
            .setMediaMetadata(metadata)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setExtras(
                        android.os.Bundle().apply {
                            putString(com.jhulian.android.youtube.classic.playback.PlaybackService.EXTRA_AUDIO_URL, audioOnly.content)
                        },
                    )
                    .build(),
            )
            .build()
    }

    fun bestVideoOnly(info: StreamInfo, maxHeight: Int): VideoStream? =
        info.videoOnlyStreams
            ?.filter { it.height <= maxHeight || maxHeight <= 0 }
            ?.maxByOrNull { it.height }

    fun bestAudio(info: StreamInfo): AudioStream? = info.audioStreams?.maxByOrNull { it.averageBitrate }

    fun bestProgressive(info: StreamInfo, maxHeight: Int): VideoStream? =
        info.videoStreams
            ?.filter { it.height <= maxHeight || maxHeight <= 0 }
            ?.maxByOrNull { it.height }

    fun heightForQualityPref(pref: String?): Int = when (pref) {
        "144p" -> 144
        "240p" -> 240
        "360p" -> 360
        "480p" -> 480
        "720p" -> 720
        "1080p" -> 1080
        else -> 0 // "auto" / unrecognized -> no cap.
    }
}
