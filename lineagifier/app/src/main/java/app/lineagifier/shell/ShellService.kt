package app.lineagifier.shell

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.annotation.Keep
import app.lineagifier.IShellService
import java.io.InputStream
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * lives in shizuku's process, so everything here runs as the shell user (uid 2000).
 * shizuku creates it by reflection, hence @Keep and both constructors.
 */
@Keep
class ShellService() : IShellService.Stub() {

    @Keep
    @Suppress("UNUSED_PARAMETER")
    constructor(context: Context) : this()

    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(cmd: String): Array<String> = try {
        val p = ProcessBuilder("sh", "-c", cmd).start()
        p.outputStream.close()
        collect(p)
    } catch (t: Throwable) {
        arrayOf("-1", "", t.toString())
    }

    override fun installApk(apk: ParcelFileDescriptor, size: Long): Array<String> = try {
        val p = ProcessBuilder("pm", "install", "-r", "-S", size.toString()).start()
        // drain output on other threads while we feed stdin, or pm can block on a full pipe
        var out = ""
        var err = ""
        val t1 = thread { out = p.inputStream.readAll() }
        val t2 = thread { err = p.errorStream.readAll() }
        ParcelFileDescriptor.AutoCloseInputStream(apk).use { input ->
            p.outputStream.use { input.copyTo(it) }
        }
        val code = p.waitFor()
        t1.join(); t2.join()
        arrayOf(code.toString(), out, err)
    } catch (t: Throwable) {
        arrayOf("-1", "", t.toString())
    }

    private fun collect(p: Process): Array<String> {
        var out = ""
        var err = ""
        val t1 = thread { out = p.inputStream.readAll() }
        val t2 = thread { err = p.errorStream.readAll() }
        val code = p.waitFor()
        t1.join(); t2.join()
        return arrayOf(code.toString(), out, err)
    }

    private fun InputStream.readAll(): String = bufferedReader().use { it.readText() }
}
