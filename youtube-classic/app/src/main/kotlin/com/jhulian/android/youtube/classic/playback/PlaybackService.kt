package com.jhulian.android.youtube.classic.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import okhttp3.OkHttpClient

/**
 * Keeps a single ExoPlayer + MediaSession alive independent of any Activity,
 * so playback (audio, or video backgrounded to audio-only) survives the
 * player screen closing, matches system media controls/notification, and
 * keeps going for downloaded files exactly the same way it does for a
 * live stream URL - offline playback is just "the same player, pointed at
 * a file:// Uri instead of an https:// one" (see download/DownloadService.kt
 * for where those files come from).
 *
 * YouTube serves most non-live streams as *separate* video-only and
 * audio-only adaptive tracks rather than one progressive file or an HLS
 * manifest, so a plain [MediaItem] URI isn't enough to play them back in
 * sync. [PlayerActivity][com.jhulian.android.youtube.classic.ui.player.PlayerActivity] stashes
 * the paired audio URL in the MediaItem's session-safe
 * `requestMetadata.extras` (the one part of a MediaItem guaranteed to
 * survive the MediaController IPC boundary), and the custom
 * [MediaSource.Factory] below reassembles it into a synced
 * [MergingMediaSource] on this side, where the real ExoPlayer lives.
 */
@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val dataSourceFactory = DefaultDataSource.Factory(
            this,
            OkHttpDataSource.Factory(OkHttpClient.Builder().build()),
        )
        val defaultFactory = DefaultMediaSourceFactory(dataSourceFactory)

        // MediaSource.Factory has several other abstract members
        // (setDrmSessionManagerProvider, setLoadErrorHandlingPolicy,
        // getSupportedTypes) that aren't relevant here - delegate the whole
        // interface to the default factory and only override the one method
        // that needs the video-only/audio-only merge logic.
        val mediaSourceFactory = object : MediaSource.Factory by defaultFactory {
            override fun createMediaSource(mediaItem: MediaItem): MediaSource {
                val audioUrl = mediaItem.requestMetadata.extras?.getString(EXTRA_AUDIO_URL)
                return if (audioUrl.isNullOrBlank()) {
                    defaultFactory.createMediaSource(mediaItem)
                } else {
                    val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
                    val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(audioUrl))
                    MergingMediaSource(videoSource, audioSource)
                }
            }
        }

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            // Media3 doesn't expose seek-increment XML attrs on PlayerView
            // in this version - the rewind/forward buttons in
            // player_control_view.xml call Player.seekBack()/seekForward(),
            // and these are what those actually seek by.
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val session = mediaSession ?: return
        if (!session.player.isPlaying) {
            stopSelfIfNotPlaying(session.player)
        }
    }

    private fun stopSelfIfNotPlaying(player: Player) {
        if (!player.isPlaying) {
            player.release()
            mediaSession?.release()
            mediaSession = null
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_AUDIO_URL = "audio_url"
    }
}
