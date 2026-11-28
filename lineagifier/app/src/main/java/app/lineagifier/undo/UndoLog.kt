package app.lineagifier.undo

import app.lineagifier.shell.ShellRunner
import app.lineagifier.shell.safeToken
import app.lineagifier.shell.shQuote
import app.lineagifier.system.Roles
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
enum class Kind { disable, uninstall, setting, uimode, overlay, role, module, app, fabricated, appop }

/**
 * one reversible change. `target` is a package, an overlay, a role, or "namespace/key" for settings.
 * `previous` is what was there before (null = setting didn't exist / role had no holder).
 * `added` is the role holder we set, so undo knows what to take back.
 */
@Serializable
data class UndoEntry(
    val id: Long,
    val time: Long,
    val kind: Kind,
    val target: String,
    val previous: String? = null,
    val added: String? = null,
    val restored: Boolean = false,
)

@Serializable
private data class UndoFile(val entries: List<UndoEntry> = emptyList())

/** append-only json log of everything lineagifier changed, in filesDir/undo-log.json */
class UndoLog(private val file: File) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Synchronized
    fun all(): List<UndoEntry> = try {
        if (file.exists()) json.decodeFromString(UndoFile.serializer(), file.readText()).entries else emptyList()
    } catch (t: Throwable) {
        // never silently drop the log: keep the broken file around for a human to look at
        file.copyTo(File(file.parentFile, file.name + ".broken-" + System.currentTimeMillis()), overwrite = true)
        emptyList()
    }

    fun pending() = all().filter { !it.restored }

    @Synchronized
    fun add(kind: Kind, target: String, previous: String? = null, added: String? = null): UndoEntry {
        val list = all()
        val e = UndoEntry((list.maxOfOrNull { it.id } ?: 0) + 1, System.currentTimeMillis(), kind, target, previous, added)
        write(list + e)
        return e
    }

    @Synchronized
    fun markRestored(id: Long) = write(all().map { if (it.id == id) it.copy(restored = true) else it })

    private fun write(list: List<UndoEntry>) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(UndoFile.serializer(), UndoFile(list)))
        if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
    }
}

data class RestoreLine(val entry: UndoEntry, val ok: Boolean, val message: String)

const val KSUD = "/data/adb/ksud"

object Restore {
    /** the commands that undo one entry. pure so it can be unit tested. */
    fun commands(e: UndoEntry): List<String> = when (e.kind) {
        Kind.uninstall -> listOf("cmd package install-existing --user 0 ${safeToken(e.target)}")
        Kind.disable -> listOf("pm enable --user 0 ${safeToken(e.target)}")
        Kind.setting -> {
            val ns = safeToken(e.target.substringBefore('/'))
            val key = safeToken(e.target.substringAfter('/'))
            if (e.previous == null || e.previous == "null") listOf("settings delete $ns $key")
            else listOf("settings put $ns $key ${shQuote(e.previous)}")
        }
        Kind.uimode -> listOf("cmd uimode night ${safeToken(e.previous ?: "no")}")
        Kind.overlay -> listOf(
            if (e.previous == "enabled") "cmd overlay enable --user 0 ${safeToken(e.target)}"
            else "cmd overlay disable --user 0 ${safeToken(e.target)}"
        )
        // a kernelsu module lineagifier installed: uninstall it (takes effect after a reboot)
        Kind.module -> listOf("${KSUD} module uninstall ${safeToken(e.target)}")
        // an app lineagifier installed as an extra (not a replacement): remove it
        Kind.app -> listOf("pm uninstall ${safeToken(e.target)}")
        // a fabricated overlay (status bar sizing): unregister it
        Kind.fabricated -> listOf("cmd overlay disable --user 0 ${safeToken(e.target)}")
        // an appop lineagifier set: put the old mode back (target = "pkg/OP")
        Kind.appop -> listOf("appops set ${safeToken(e.target.substringBefore('/'))} ${safeToken(e.target.substringAfter('/'))} ${safeToken(e.previous ?: "default")}")
        Kind.role -> {
            val prev = e.previous?.split(';')?.filter { it.isNotBlank() }.orEmpty()
            if (prev.isNotEmpty()) prev.map { "cmd role add-role-holder --user 0 ${safeToken(e.target)} ${safeToken(it)}" }
            else listOfNotNull(e.added?.let { "cmd role remove-role-holder --user 0 ${safeToken(e.target)} ${safeToken(it)}" })
        }
    }

    /**
     * undo newest first. that order matters: a stock launcher is always re-enabled before
     * the home role is handed back to it, and restore never removes anything, so the
     * device can't end up without a launcher here.
     */
    suspend fun everything(log: UndoLog, shell: ShellRunner): List<RestoreLine> {
        val out = mutableListOf<RestoreLine>()
        for (e in log.pending().sortedByDescending { it.id }) {
            var ok = true
            val msgs = mutableListOf<String>()
            for (cmd in commands(e)) {
                val r = shell.run(cmd)
                if (!r.okStrict) { ok = false; msgs += "$cmd -> ${r.message}" }
            }
            // role commands time out on aosp 13 and still apply later, so judge them by the real holders
            if (!ok && e.kind == Kind.role) {
                val want = e.previous?.split(';')?.filter { it.isNotBlank() }.orEmpty()
                for (i in 0 until 10) {
                    val now = Roles.holders(shell, e.target).getOrDefault(emptyList())
                    if (if (want.isNotEmpty()) want.all { it in now } else e.added !in now) { ok = true; break }
                    delay(2_000)
                }
            }
            if (ok) log.markRestored(e.id)
            out += RestoreLine(e, ok, if (ok) "restored" else msgs.joinToString("\n"))
        }
        return out
    }
}
