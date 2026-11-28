package app.lineagifier.install

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import app.lineagifier.catalog.Entry
import app.lineagifier.catalog.Installable
import app.lineagifier.shell.ShellRunner
import app.lineagifier.shell.safeToken
import app.lineagifier.system.Pm
import app.lineagifier.system.Roles
import app.lineagifier.system.Services
import app.lineagifier.undo.Kind
import app.lineagifier.undo.UndoLog
import kotlinx.coroutines.delay
import java.io.File

/** the real side effects: install, set default, disable/remove stock. every step reports back. */
class Actions(private val ctx: Context, private val shell: ShellRunner, private val log: UndoLog) {

    fun homePackages(): Set<String> {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        @Suppress("DEPRECATION")
        return ctx.packageManager.queryIntentActivities(i, 0).map { it.activityInfo.packageName }
            // "com.android.settings" exposes a fallback home on some images, it doesn't count as a launcher
            .filter { it != "com.android.settings" }.toSet()
    }

    fun currentHome(): String? {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        @Suppress("DEPRECATION")
        val r = ctx.packageManager.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY) ?: return null
        val p = r.activityInfo?.packageName ?: return null
        // "android" means the resolver/chooser is showing, i.e. no default yet
        return p.takeIf { it != "android" }
    }

    suspend fun defaults(roles: List<String>): DefaultsState {
        val services = Services.list(shell)
        val holders = if (Roles.available(shell, services)) roles.distinct().associateWith { Roles.holders(shell, it).getOrDefault(emptyList()) } else null
        return DefaultsState(holders, homePackages(), currentHome())
    }

    /** download -> verify sha256 -> check it's the package we expect -> pm install -> confirm */
    suspend fun install(item: Installable, progress: (String, Float) -> Unit): Result<String> = runCatching {
        val d = item.download
        if (Build.VERSION.SDK_INT < d.minSdk) error("${item.name} needs android sdk ${d.minSdk}, this image is ${Build.VERSION.SDK_INT}")
        val dir = File(ctx.cacheDir, "apks").apply { mkdirs() }
        val apk = File(dir, "${safeToken(item.packageId)}_${d.versionCode}.apk")

        progress("downloading", 0f)
        Downloader.fetch(d, apk) { progress("downloading", it) }

        progress("checking apk", 1f)
        @Suppress("DEPRECATION")
        val info = ctx.packageManager.getPackageArchiveInfo(apk.path, 0)
            ?: error("downloaded file isn't a readable apk")
        if (info.packageName != item.packageId) error("apk is ${info.packageName}, expected ${item.packageId}")

        progress("installing", 1f)
        val r = shell.installApk(apk)
        if (!r.out.contains("Success")) error("pm install failed: ${r.message}")

        val check = Pm.read(shell).getOrThrow()
        if (!check.isInstalled(item.packageId)) error("pm said success but ${item.packageId} isn't listed")
        apk.delete()
        "installed ${item.packageId} ${d.versionName}"
    }

    /**
     * make [pkg] the default for the entry's role. home also gets set-home-activity as a fallback,
     * which matters on images where the role service is missing or refuses shell.
     */
    suspend fun setDefault(entry: Entry, pkg: String): Result<String> = runCatching {
        val role = entry.role ?: return@runCatching "${entry.category} has no default-app role, pick it once from the app chooser"
        val services = Services.list(shell)
        val msgs = mutableListOf<String>()
        var done = false
        if (Roles.available(shell, services)) {
            val before = Roles.holders(shell, role).getOrDefault(emptyList())
            if (pkg in before) return@runCatching "$pkg is already the default"
            // log before: on a slow device the change can land after cmd already gave up with a timeout
            val entry = log.add(Kind.role, role, previous = before.joinToString(";"), added = pkg)
            val r = shell.run("cmd role add-role-holder --user 0 ${safeToken(role)} ${safeToken(pkg)}")
            // aosp 13's role controller often answers "TimeoutException" and applies the change seconds later,
            // so the exit code means nothing: poll the real holders for up to ~20s
            var after = Roles.holders(shell, role).getOrDefault(emptyList())
            var tries = 0
            while (pkg !in after && tries++ < 10) {
                delay(2_000)
                after = Roles.holders(shell, role).getOrDefault(emptyList())
            }
            if (pkg in after) {
                done = true; msgs += "role $role -> $pkg"
            } else {
                log.markRestored(entry.id)
                msgs += "cmd role didn't take: ${r.message}"
            }
        } else msgs += "no `cmd role` on this image"

        if (role == Safety.HOME && !done) {
            val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(pkg)
            @Suppress("DEPRECATION")
            val act = ctx.packageManager.queryIntentActivities(i, 0).firstOrNull()?.activityInfo
                ?: error("$pkg has no home activity")
            val r = shell.run("cmd package set-home-activity --user 0 ${safeToken("$pkg/${act.name}")}")
            if (currentHome() == pkg) { done = true; msgs += "set-home-activity -> $pkg" }
            else msgs += "set-home-activity didn't take: ${r.message}"
        }
        if (!done) error(msgs.joinToString("; "))
        msgs.joinToString("; ")
    }

    enum class Removal { DISABLE, UNINSTALL }

    /** disables or removes (for user 0, data kept) a stock app, only if Safety allows it right now */
    suspend fun removeStock(entry: Entry, stock: String, how: Removal): Result<String> = runCatching {
        val pkgs = Pm.read(shell).getOrThrow()
        val decision = Safety.canRemove(stock, entry, pkgs, defaults(listOfNotNull(entry.role)))
        if (!decision.allowed) error(decision.reason)

        val p = safeToken(stock)
        val wasHome = stock in homePackages()
        val (cmd, kind) = when (how) {
            Removal.DISABLE -> "pm disable-user --user 0 $p" to Kind.disable
            Removal.UNINSTALL -> "pm uninstall -k --user 0 $p" to Kind.uninstall
        }
        // log first: if we die halfway, restore still knows what to put back
        val e = log.add(kind, stock)
        val r = shell.run(cmd)
        val after = Pm.read(shell).getOrThrow()
        if (after.isEnabled(stock)) {
            log.markRestored(e.id)
            error("$cmd failed: ${r.message}")
        }
        // belt and braces: if this somehow left no launcher, put the stock one straight back
        if (wasHome && homePackages().isEmpty()) {
            shell.run(if (how == Removal.DISABLE) "pm enable --user 0 $p" else "cmd package install-existing --user 0 $p")
            log.markRestored(e.id)
            error("no launcher left after removing $stock, it was put back")
        }
        if (how == Removal.DISABLE) "disabled $stock" else "removed $stock for user 0 (restorable)"
    }

}
