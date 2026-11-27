package app.lineagifier.shell

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.ParcelFileDescriptor
import app.lineagifier.BuildConfig
import app.lineagifier.IShellService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import java.io.File
import kotlin.coroutines.resume

data class ShellResult(val code: Int, val out: String, val err: String) {
    val ok get() = code == 0
    /** pm and cmd sometimes exit 0 but print "Failure"/"Error"/"Exception", so check text too */
    val okStrict get() = ok && !Regex("(?i)^(failure|error|exception)", RegexOption.MULTILINE).containsMatchIn(out + "\n" + err)
    val message get() = (err.trim().ifEmpty { out.trim() }).ifEmpty { "exit $code" }
}

/** anything that can run a shell command. the real one is shizuku, tests use a fake. */
interface ShellRunner {
    suspend fun run(cmd: String): ShellResult
}

enum class ShizukuStatus { NOT_RUNNING, TOO_OLD, NO_PERMISSION, READY }

object Shell : ShellRunner {
    private var service: IShellService? = null
    private val lock = Mutex()

    private val args = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, ShellService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("shell")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    fun status(): ShizukuStatus = try {
        when {
            !Shizuku.pingBinder() -> ShizukuStatus.NOT_RUNNING
            Shizuku.isPreV11() -> ShizukuStatus.TOO_OLD
            Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> ShizukuStatus.NO_PERMISSION
            else -> ShizukuStatus.READY
        }
    } catch (t: Throwable) {
        ShizukuStatus.NOT_RUNNING
    }

    /** 0 = root, 2000 = shell (adb). -1 if unknown. */
    fun uid(): Int = try { Shizuku.getUid() } catch (t: Throwable) { -1 }

    private suspend fun connect(): IShellService = lock.withLock {
        service?.takeIf { it.asBinder().pingBinder() }?.let { return it }
        if (status() != ShizukuStatus.READY) error("shizuku isn't running or permission isn't granted")
        val s = withTimeout(15_000) {
            suspendCancellableCoroutine { cont ->
                val conn = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                        val svc = binder?.takeIf { it.pingBinder() }?.let { IShellService.Stub.asInterface(it) }
                        if (cont.isActive) {
                            if (svc != null) cont.resume(svc) else cont.cancel(IllegalStateException("shell service binder is dead"))
                        }
                    }
                    override fun onServiceDisconnected(name: ComponentName?) {
                        service = null
                    }
                }
                Shizuku.bindUserService(args, conn)
            }
        }
        service = s
        s
    }

    override suspend fun run(cmd: String): ShellResult = withContext(Dispatchers.IO) {
        try {
            val r = connect().exec(cmd)
            ShellResult(r[0].toIntOrNull() ?: -1, r[1], r[2])
        } catch (t: Throwable) {
            ShellResult(-1, "", t.message ?: t.toString())
        }
    }

    suspend fun installApk(file: File): ShellResult = withContext(Dispatchers.IO) {
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val r = connect().installApk(pfd, file.length())
            ShellResult(r[0].toIntOrNull() ?: -1, r[1], r[2])
        } catch (t: Throwable) {
            ShellResult(-1, "", t.message ?: t.toString())
        }
    }
}

private val SAFE_TOKEN = Regex("^[A-Za-z0-9._/\\-:]+$")

/** package ids, components, setting keys: refuse anything that isn't a plain token */
fun safeToken(s: String): String {
    require(SAFE_TOKEN.matches(s)) { "refusing unsafe shell token: $s" }
    return s
}

/** single-quote for sh */
fun shQuote(s: String): String = "'" + s.replace("'", "'\\''") + "'"
