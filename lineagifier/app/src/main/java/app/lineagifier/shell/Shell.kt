package app.lineagifier.shell

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

data class ShellResult(val code: Int, val out: String, val err: String) {
    val ok get() = code == 0
    /** pm and cmd sometimes exit 0 but print "Failure"/"Error"/"Exception", so check text too */
    val okStrict get() = ok && !Regex("(?i)^(failure|error|exception)", RegexOption.MULTILINE).containsMatchIn(out + "\n" + err)
    val message get() = (err.trim().ifEmpty { out.trim() }).ifEmpty { "exit $code" }
}

/** anything that can run a shell command. the real one is root (su), tests use a fake. */
interface ShellRunner {
    suspend fun run(cmd: String): ShellResult
    /** streams [file] into `pm install -r -S size` */
    suspend fun installApk(file: File): ShellResult = ShellResult(-1, "", "this shell can't install apks")
}

enum class RootStatus { NO_SU, DENIED, READY }

/**
 * root through `su -c`. on twoyi that's kernelsu's su; the first call makes kernelsu ask
 * the user to allow lineagifier (superuser tab).
 */
object Root : ShellRunner {
    @Volatile private var cached: RootStatus? = null

    /** looks for su and checks it really gives uid 0. blocking, call off the main thread. */
    fun check(): RootStatus {
        val r = exec(listOf("su", "-c", "id -u"), null, 30)
        val st = when {
            r.code == -2 -> RootStatus.NO_SU
            r.ok && r.out.trim() == "0" -> RootStatus.READY
            else -> RootStatus.DENIED
        }
        cached = st
        return st
    }

    fun lastStatus() = cached

    override suspend fun run(cmd: String): ShellResult = withContext(Dispatchers.IO) {
        exec(listOf("su", "-c", cmd), null, 180)
    }

    override suspend fun installApk(file: File): ShellResult = withContext(Dispatchers.IO) {
        exec(listOf("su", "-c", "pm install -r -S ${file.length()}"), file, 600)
    }

    /** code -2 = su doesn't exist, -1 = other failure / timeout */
    private fun exec(argv: List<String>, stdin: File?, timeoutSec: Long): ShellResult {
        val p = try {
            ProcessBuilder(argv).start()
        } catch (t: Throwable) {
            return ShellResult(-2, "", "no su binary: ${t.message}")
        }
        var out = ""
        var err = ""
        val t1 = thread { out = p.inputStream.readAll() }
        val t2 = thread { err = p.errorStream.readAll() }
        try {
            p.outputStream.use { os -> stdin?.inputStream()?.use { it.copyTo(os) } }
        } catch (t: Throwable) {
            err += "stdin: ${t.message}\n"
        }
        if (!p.waitFor(timeoutSec, TimeUnit.SECONDS)) {
            p.destroyForcibly()
            t1.join(1000); t2.join(1000)
            return ShellResult(-1, out, "timed out after ${timeoutSec}s")
        }
        t1.join(); t2.join()
        return ShellResult(p.exitValue(), out, err)
    }

    /** never throws: a killed process (timeout) closes the pipe under the reader, which must not crash the app */
    private fun InputStream.readAll(): String {
        val sb = StringBuilder()
        try { bufferedReader().use { r -> val buf = CharArray(8192); while (true) { val n = r.read(buf); if (n < 0) break; sb.append(buf, 0, n) } } }
        catch (t: Throwable) { }
        return sb.toString()
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
