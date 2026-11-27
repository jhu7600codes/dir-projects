package app.lineagifier.system

import app.lineagifier.shell.ShellRunner
import app.lineagifier.shell.safeToken

/** what's really on the image, read from pm. never guessed. */
data class PackageState(
    /** pm list packages -s -u : every system package on the image, even ones removed for the user */
    val systemAll: Set<String>,
    /** pm list packages : installed for the current user */
    val installed: Set<String>,
    /** pm list packages -d : installed but disabled */
    val disabled: Set<String>,
) {
    fun isOnImage(p: String) = p in systemAll
    fun isInstalled(p: String) = p in installed
    fun isEnabled(p: String) = p in installed && p !in disabled
    fun isSystem(p: String) = p in systemAll
}

object Pm {
    fun parseList(out: String): Set<String> = out.lineSequence()
        .map { it.trim() }
        .filter { it.startsWith("package:") }
        .map { it.removePrefix("package:").substringBefore(' ').trim() }
        .filter { it.isNotEmpty() }
        .toSet()

    suspend fun read(shell: ShellRunner): Result<PackageState> {
        val sys = shell.run("pm list packages -s -u")
        if (!sys.ok) return Result.failure(IllegalStateException("pm list packages -s -u failed: ${sys.message}"))
        val inst = shell.run("pm list packages")
        if (!inst.ok) return Result.failure(IllegalStateException("pm list packages failed: ${inst.message}"))
        val dis = shell.run("pm list packages -d")
        return Result.success(
            PackageState(
                systemAll = parseList(sys.out),
                installed = parseList(inst.out),
                disabled = if (dis.ok) parseList(dis.out) else emptySet(),
            )
        )
    }
}

/** shell services that exist on this image (`cmd -l`). on very old builds `cmd -l` may be missing. */
object Services {
    suspend fun list(shell: ShellRunner): Set<String> {
        val r = shell.run("cmd -l")
        if (!r.ok) return emptySet()
        return r.out.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.contains(' ') }.toSet()
    }
}

object Roles {
    suspend fun available(shell: ShellRunner, services: Set<String>): Boolean = "role" in services

    fun parseHolders(out: String): List<String> =
        out.split(';', '\n', ' ', ',').map { it.trim() }.filter { it.isNotEmpty() && it.contains('.') }

    suspend fun holders(shell: ShellRunner, role: String): Result<List<String>> {
        val r = shell.run("cmd role get-role-holders --user 0 ${safeToken(role)}")
        return if (r.okStrict) Result.success(parseHolders(r.out)) else Result.failure(IllegalStateException(r.message))
    }
}

data class OverlayInfo(val pkg: String, val target: String, val enabled: Boolean, val loadable: Boolean)

object Overlays {
    /**
     * `cmd overlay list` prints a target line, then one line per overlay:
     *   "[x] pkg" enabled, "[ ] pkg" disabled, "--- pkg" can't be enabled
     */
    fun parse(out: String): List<OverlayInfo> {
        val res = mutableListOf<OverlayInfo>()
        var target = ""
        for (raw in out.lines()) {
            val line = raw.trimEnd()
            if (line.isBlank()) continue
            val t = line.trim()
            when {
                t.startsWith("[x]") -> res += OverlayInfo(t.substring(3).trim(), target, enabled = true, loadable = true)
                t.startsWith("[ ]") -> res += OverlayInfo(t.substring(3).trim(), target, enabled = false, loadable = true)
                t.startsWith("---") -> res += OverlayInfo(t.substring(3).trim(), target, enabled = false, loadable = false)
                !line.startsWith(" ") && !line.startsWith("\t") -> target = t
            }
        }
        return res
    }

    suspend fun list(shell: ShellRunner): List<OverlayInfo> {
        val r = shell.run("cmd overlay list")
        return if (r.ok) parse(r.out) else emptyList()
    }
}
