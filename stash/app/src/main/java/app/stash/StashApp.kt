package app.stash

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import app.stash.download.Downloads
import app.stash.providers.YtDlp
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StashApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var ytDlp: YtDlp
    @Inject lateinit var downloads: Downloads

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DOWNLOADS, "downloads", NotificationManager.IMPORTANCE_LOW)
        )
        ytDlp.start()
        downloads.recoverAfterRestart()
    }

    companion object {
        const val CHANNEL_DOWNLOADS = "downloads"
    }
}
