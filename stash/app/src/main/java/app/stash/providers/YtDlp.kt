package app.stash.providers

import android.content.Context
import android.util.Log
import app.stash.data.prefs.Settings
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread

/**
 * Wraps youtubedl-android. Python + yt-dlp get unpacked on first start, then the bundled yt-dlp
 * is updated every app start because youtube breaks old versions all the time.
 */
@Singleton
class YtDlp @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: Settings,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ready = CompletableDeferred<Unit>()

    /** held while yt-dlp is replacing itself so nothing runs a half-written binary */
    private val updateLock = Mutex()

    private val _status = MutableStateFlow("starting")
    val status: StateFlow<String> = _status

    fun start() {
        scope.launch {
            try {
                YoutubeDL.getInstance().init(context)
                ready.complete(Unit)
                _status.value = "ready"
            } catch (e: Exception) {
                Log.e(TAG, "yt-dlp init failed", e)
                ready.completeExceptionally(e)
                _status.value = "failed to start: ${e.message}"
                return@launch
            }
            update()
        }
    }

    /** returns a human readable result for the settings screen */
    suspend fun update(): String = withContext(Dispatchers.IO) {
        ready.await()
        updateLock.withLock {
            _status.value = "updating"
            val msg = try {
                val result = YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel._STABLE)
                val version = YoutubeDL.getInstance().version(context) ?: "?"
                settings.setYtDlpUpdated(version)
                if (result == YoutubeDL.UpdateStatus.DONE) "updated to $version" else "already on $version"
            } catch (e: Exception) {
                Log.w(TAG, "yt-dlp update failed", e)
                "update failed: ${e.message}"
            }
            _status.value = "ready"
            msg
        }
    }

    suspend fun version(): String? = withContext(Dispatchers.IO) {
        runCatching { ready.await(); YoutubeDL.getInstance().version(context) }.getOrNull()
    }

    /**
     * Runs yt-dlp. Cancelling the coroutine kills the process. [onProgress] gets 0..100.
     */
    suspend fun run(
        target: String,
        options: List<Pair<String, String?>>,
        onProgress: ((Float) -> Unit)? = null,
    ): YoutubeDLResponse {
        ready.await()
        updateLock.withLock { } // wait out an update in progress
        val processId = UUID.randomUUID().toString()
        val request = YoutubeDLRequest(target).apply {
            addOption("--no-warnings")
            addOption("--no-check-certificates")
            options.forEach { (k, v) -> if (v == null) addOption(k) else addOption(k, v) }
        }
        return suspendCancellableCoroutine { cont ->
            val worker = thread(name = "yt-dlp") {
                try {
                    val response = YoutubeDL.getInstance().execute(request, processId) { progress, _, _ ->
                        onProgress?.invoke(progress)
                    }
                    if (cont.isActive) cont.resumeWith(Result.success(response))
                } catch (e: Throwable) {
                    if (cont.isActive) cont.resumeWith(Result.failure(ProviderException(cleanError(e), e)))
                }
            }
            cont.invokeOnCancellation {
                YoutubeDL.getInstance().destroyProcessById(processId)
                worker.interrupt()
            }
        }
    }

    private fun cleanError(e: Throwable): String {
        val msg = e.message ?: e.javaClass.simpleName
        // yt-dlp dumps a whole stderr; the useful line starts with ERROR:
        return msg.lines().lastOrNull { it.startsWith("ERROR:") }?.removePrefix("ERROR:")?.trim()
            ?: msg.lines().firstOrNull { it.isNotBlank() }?.take(200)
            ?: msg
    }

    companion object {
        private const val TAG = "YtDlp"
    }
}
