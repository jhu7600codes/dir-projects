package app.krala

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import app.krala.data.db.MessageDao
import app.krala.data.memory.MemoryScheduler
import app.krala.data.provider.ProviderRepository
import app.krala.data.settings.SettingsRepository
import app.krala.data.sync.AuthRepository
import app.krala.data.sync.SyncScheduler
import app.krala.data.sync.SyncService
import app.krala.di.AppScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class KralaApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var providers: ProviderRepository
    @Inject lateinit var messages: MessageDao
    @Inject lateinit var memoryScheduler: MemoryScheduler
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var auth: AuthRepository
    @Inject lateinit var sync: SyncService
    @Inject lateinit var activeChat: ActiveChat
    @Inject @AppScope lateinit var scope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            // a reply that was streaming when the process died can't continue
            messages.closeDanglingStreams(System.currentTimeMillis())
            providers.seedIfNeeded()
            memoryScheduler.sweep(exceptChatId = null)
        }
        scope.launch { auth.ensureGuest() }
        scope.launch {
            settings.settings.map { it.syncEnabled }.distinctUntilChanged().collect { on ->
                if (on) {
                    SyncScheduler.periodic(this@KralaApp)
                    SyncScheduler.soon(this@KralaApp)
                } else {
                    SyncScheduler.cancel(this@KralaApp)
                }
            }
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                sync.startLive()
                SyncScheduler.soon(this@KralaApp)
            }

            override fun onStop(owner: LifecycleOwner) {
                // the app went to the background: the open chat has ended for now
                activeChat.id?.let { memoryScheduler.chatEnded(it) }
                sync.stopLive()
                SyncScheduler.soon(this@KralaApp)
            }
        })
    }
}
