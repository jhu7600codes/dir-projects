package app.lineagifier.install

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import app.lineagifier.catalog.Entry
import app.lineagifier.microg.Microg
import app.lineagifier.shell.ShellRunner
import app.lineagifier.shell.safeToken
import app.lineagifier.shell.shQuote
import app.lineagifier.system.Pm
import app.lineagifier.undo.KSUD
import app.lineagifier.undo.Kind
import app.lineagifier.undo.UndoLog
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** what a downloaded apk turned out to be */
data class ApkInfo(
    val file: File,
    val packageId: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val certSha256: List<String>,
    val sharedUserId: String?,
)

/**
 * lineage apps pulled from lineage builds (apkmirror). the ones with their own package id install normally;
 * the ones that share a package id with a stock app (trebuchet = com.android.launcher3, clock, calculator,
 * messaging) can't install over the system app with a different signature, so with root they replace it
 * through a kernelsu module: the lineage apk is mounted at the stock app's own path at boot. nothing on the
 * real /system changes, and removing the module (restore) brings the stock app back.
 */
object SystemSwap {
    fun inspect(ctx: Context, f: File): ApkInfo {
        val pm = ctx.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
        @Suppress("DEPRECATION")
        val info = pm.getPackageArchiveInfo(f.path, flags) ?: throw IOException("that file isn't a readable apk")
        val sigs = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners?.toList().orEmpty() else @Suppress("DEPRECATION") info.signatures.orEmpty().toList()
        val md = MessageDigest.getInstance("SHA-256")
        val minSdk = info.applicationInfo?.minSdkVersion ?: 0
        @Suppress("DEPRECATION")
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        @Suppress("DEPRECATION")
        return ApkInfo(f, info.packageName, info.versionName ?: "?", code, minSdk,
            sigs.map { s -> md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) } }, info.sharedUserId)
    }

    /** checks before anything is installed. returns a reason to refuse, or null */
    fun refuse(entry: Entry, a: ApkInfo, sdk: Int = Build.VERSION.SDK_INT): String? = when {
        a.packageId !in entry.lineageIds -> "this apk is ${a.packageId}, not ${entry.name} (${entry.lineageIds.joinToString()})"
        a.minSdk > sdk -> "this build needs android sdk ${a.minSdk}, twoyi is $sdk. pick the lineage 20 / android 13 release on apkmirror"
        a.certSha256.isEmpty() -> "the apk isn't signed"
        // aosp 13 ReconcilePackageUtils: a system package in a shared uid with a mismatched signature throws
        // IllegalStateException in system_server, i.e. a bootloop. never swap those.
        a.sharedUserId != null -> "this apk uses sharedUserId ${a.sharedUserId}. swapping it in with a different signature would crash android at boot, so lineagifier won't"
        else -> null
    }

    fun moduleId(pkg: String) = "lineagifier_" + pkg.replace('.', '_')

    /**
     * the stock apk path (`pm path`) -> where it goes inside a kernelsu module. kernelsu mounts a module's
     * system/ over /system, and /product, /system_ext, /vendor through system/product etc.
     */
    fun modulePath(stockApk: String): String {
        val p = stockApk.trim()
        require(p.startsWith("/") && p.endsWith(".apk")) { "unexpected stock path $p" }
        return when {
            p.startsWith("/system/") -> p.removePrefix("/")
            p.startsWith("/product/") || p.startsWith("/system_ext/") || p.startsWith("/vendor/") -> "system$p"
            else -> throw IOException("stock app lives in $p, which a kernelsu module can't overlay")
        }
    }

    /** builds the module zip: the lineage apk at the stock path, its native libs, and privapp permissions if it's a priv-app */
    fun buildModule(ctx: Context, a: ApkInfo, apkPathInModule: String, out: File, extraSysconfig: String? = null) {
        val abi = Build.SUPPORTED_ABIS.first()
        val dir = apkPathInModule.substringBeforeLast('/')
        val priv = "/priv-app/" in "/$apkPathInModule"
        ZipOutputStream(out.outputStream().buffered()).use { z ->
            fun text(path: String, s: String) { z.putNextEntry(ZipEntry(path)); z.write(s.toByteArray()); z.closeEntry() }
            text("module.prop", """
                id=${moduleId(a.packageId)}
                name=${a.packageId} (lineage, via lineagifier)
                version=${a.versionName}
                versionCode=1
                author=lineagifier
                description=replaces ${a.packageId} with the lineageos build systemlessly. remove the module to get the stock app back.
            """.trimIndent() + "\n")
            z.putNextEntry(ZipEntry(apkPathInModule)); a.file.inputStream().use { it.copyTo(z) }; z.closeEntry()
            ZipFile(a.file).use { apk ->
                apk.entries().asSequence().filter { it.name.startsWith("lib/$abi/") && it.name.endsWith(".so") }.forEach { e ->
                    z.putNextEntry(ZipEntry("$dir/lib/${Microg.libDir(abi)}/${e.name.substringAfterLast('/')}"))
                    apk.getInputStream(e).use { it.copyTo(z) }; z.closeEntry()
                }
            }
            if (priv) {
                // a priv-app asking for a privileged permission that isn't allowlisted can stop the image from booting
                val perms = Microg.privilegedPermissions(ctx, a.file)
                val partition = apkPathInModule.removePrefix("system/").substringBefore("/priv-app")   // "", "product", "system_ext"
                val etc = if (partition.isEmpty()) "system/etc" else "system/$partition/etc"
                text("$etc/permissions/privapp-permissions-lineagifier-${a.packageId}.xml", Microg.privappXml(mapOf(a.packageId to perms)))
            }
            if (extraSysconfig != null) text("system/etc/sysconfig/lineagifier-${a.packageId}.xml", extraSysconfig)
        }
    }

    /** seedvault: allow its transport, like lineage's own sysconfig does */
    fun seedvaultSysconfig(pkg: String) = """
        <?xml version="1.0" encoding="utf-8"?>
        <config>
            <backup-transport-whitelisted-service service="$pkg/.transport.ConfigurableBackupTransportService"/>
        </config>
    """.trimIndent() + "\n"

    data class Plan(val swap: Boolean, val stockPath: String?, val reason: String)

    /** decide how [a] gets in: normal install, swap over a stock system app, or a new privileged system app */
    suspend fun plan(shell: ShellRunner, entry: Entry, a: ApkInfo): Plan {
        val pkgs = Pm.read(shell).getOrThrow()
        return when {
            pkgs.isSystem(a.packageId) -> {
                val shared = shell.run("dumpsys package ${safeToken(a.packageId)} | grep -m1 'sharedUser='").out.trim()
                if (shared.isNotEmpty()) throw IOException("the stock ${a.packageId} is in a shared user ($shared). a signature change there crashes android at boot, so it can't be swapped")
                val path = shell.run("pm path ${safeToken(a.packageId)}").out.lines().firstOrNull { it.startsWith("package:") }?.removePrefix("package:")?.trim()
                    ?: throw IOException("can't find where ${a.packageId} lives")
                Plan(true, path, "${a.packageId} is a system app here, so the lineage build replaces it through a kernelsu module (after a reboot)")
            }
            entry.status.name == "system_only" -> Plan(true, null, "${entry.name} has to be a privileged system app, so it goes in through a kernelsu module (after a reboot)")
            else -> Plan(false, null, "installs as a normal app")
        }
    }

    /** the launcher swap only goes ahead with a backup launcher, which becomes home before the reboot */
    fun backupLauncher(homePackages: Set<String>, swapping: String): String? = (homePackages - swapping - "com.android.settings").firstOrNull()

    suspend fun installSwap(ctx: Context, shell: ShellRunner, log: UndoLog, entry: Entry, a: ApkInfo, plan: Plan): String {
        if (!(shell.run("[ -x $KSUD ] && echo yes").out.trim() == "yes")) throw IOException("no kernelsu (ksud) found")
        val path = if (plan.stockPath != null) modulePath(plan.stockPath) else "system/priv-app/${entry.name.replace(Regex("[^A-Za-z0-9]"), "")}/${entry.name.replace(Regex("[^A-Za-z0-9]"), "")}.apk"
        val zip = File(ctx.cacheDir, "${moduleId(a.packageId)}.zip")
        buildModule(ctx, a, path, zip, if (a.packageId == "com.stevesoltys.seedvault") seedvaultSysconfig(a.packageId) else null)
        val id = moduleId(a.packageId)
        val e = log.add(Kind.module, safeToken(id))
        val r = shell.run("$KSUD module install ${shQuote(zip.absolutePath)}")
        zip.delete()
        val there = shell.run("[ -d /data/adb/modules_update/$id ] || [ -d /data/adb/modules/$id ] && echo yes").out.trim() == "yes"
        if (!there) { log.markRestored(e.id); throw IOException("ksud module install failed: ${r.message}") }
        return "module $id installed: ${a.packageId} ${a.versionName} at /${path.removePrefix("system/").let { if (path.startsWith("system/product") || path.startsWith("system/system_ext") || path.startsWith("system/vendor")) it else "system/$it" }}. reboot to switch."
    }
}
