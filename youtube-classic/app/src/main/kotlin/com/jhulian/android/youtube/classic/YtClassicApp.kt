package com.jhulian.android.youtube.classic

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.jhulian.android.youtube.classic.auth.SessionManager
import com.jhulian.android.youtube.classic.download.DownloadsStore
import com.jhulian.android.youtube.classic.extractor.OkHttpDownloader
import com.jhulian.android.youtube.classic.network.Innertube
import com.jhulian.android.youtube.classic.network.WebViewInnertubeBridge
import org.schabi.newpipe.extractor.NewPipe

/**
 * Application entry point. Two things have to happen before any other code
 * in the app touches NewPipeExtractor or the auth-gated network layer:
 *
 * 1. [NewPipe.init] with our [OkHttpDownloader] - every extractor call
 *    (search, stream info, comments...) routes its HTTP traffic through
 *    this, and extraction throws NullPointerException deep inside the
 *    library if this hasn't run yet.
 * 2. [SessionManager] loads whatever cookie session was captured by the
 *    WebView login flow, so like/dislike/subscribe/comment calls have it
 *    available immediately.
 * 3. [Innertube.init] wires up the [WebViewInnertubeBridge] that
 *    Home/Subscriptions/Shorts route their browse() calls through - see
 *    that class's kdoc for why they need a real WebView rather than a
 *    plain HTTP client.
 */
class YtClassicApp : Application() {

    lateinit var sessionManager: SessionManager
        private set

    override fun onCreate() {
        super.onCreate()

        NewPipe.init(OkHttpDownloader.instance)
        sessionManager = SessionManager(this)
        Innertube.init(WebViewInnertubeBridge(this))
        DownloadsStore.init(this)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PLAYBACK,
                "Playback",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Now playing controls for background/offline playback" },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DOWNLOADS,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Video download progress" },
        )
    }

    companion object {
        const val CHANNEL_PLAYBACK = "playback"
        const val CHANNEL_DOWNLOADS = "downloads"
    }
}
