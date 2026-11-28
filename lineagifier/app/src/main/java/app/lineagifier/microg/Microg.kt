package app.lineagifier.microg

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import app.lineagifier.catalog.Download
import app.lineagifier.install.Downloader
import app.lineagifier.shell.ShellRunner
import app.lineagifier.shell.safeToken
import app.lineagifier.shell.shQuote
import app.lineagifier.undo.KSUD
import app.lineagifier.undo.Kind
import app.lineagifier.undo.UndoLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * microg with root, no rom support needed:
 *  1. microg's own apks (pinned, verified) go into /system/priv-app through a kernelsu module
 *  2. a zygisk provider (neozygisk) + an xposed framework (vector, the maintained lsposed fork)
 *  3. fakegapps, an xposed module that adds signature spoofing to the framework
 * every step is logged so restore can take it all back out.
 */
object Microg {
    const val MODULE_ID = "lineagifier_microg"
    const val GMS = "com.google.android.gms"
    const val COMPANION = "com.android.vending"
    const val GSF = "com.google.android.gsf"
    const val FAKEGAPPS = "inc.whew.android.fakegapps"

    /**
     * from microg's f-droid repo (https://microg.org/fdroid/repo). on 2026-11-28 index-v1.jar was checked:
     * signed by the repo key 9bd06727e62796c0130eb6dab39b73157451582cbd138e86c468acc395d14165, signature valid,
     * and the sha1 chain .SF -> MANIFEST.MF -> index-v1.json matched. the hashes below come from that json.
     */
    data class MicrogApk(val pkg: String, val dirName: String, val label: String, val download: Download, val required: Boolean)

    val APKS = listOf(
        MicrogApk(GMS, "GmsCore", "microG services (GmsCore)", Download("0.3.18.252432", 252432036, 19,
            "https://microg.org/fdroid/repo/com.google.android.gms-252432036.apk", null,
            "1094fa60ace2367ac52bd08d1bd4f33fa8144ba930e355121b28234b8ebbf382", 108520063), required = true),
        MicrogApk(COMPANION, "Phonesky", "microG companion (fake store)", Download("0.3.18.40226", 84022636, 19,
            "https://microg.org/fdroid/repo/com.android.vending-84022636.apk", null,
            "3f1bde2f72bca746fbb53b61a32c2a85fcb026f886ee96d6fe9a05de9a6c6d5b", 4212484), required = true),
        MicrogApk(GSF, "GsfProxy", "GsfProxy (only for very old apps using c2dm)", Download("v0.1.0", 8, 10,
            "https://microg.org/fdroid/repo/com.google.android.gsf-8.apk", null,
            "86891b174301f06a1c84187b545a0a2a57044c6b768f3e84e865908743349692", 21872), required = false),
    )

    /** github repos for the parts that only ship as github releases. checked to exist on 2026-11-28. */
    data class GhPart(val repo: String, val label: String, val assetPattern: Regex, val prefer: Regex?)

    val NEOZYGISK = GhPart("JingMatrix/NeoZygisk", "NeoZygisk (zygisk for kernelsu)", Regex("(?i)\\.zip$"), Regex("(?i)release"))
    val VECTOR = GhPart("JingMatrix/Vector", "Vector (maintained lsposed)", Regex("(?i)\\.zip$"), Regex("(?i)release"))
    val FAKEGAPPS_PART = GhPart("Xposed-Modules-Repo/$FAKEGAPPS", "FakeGApps (signature spoofing)", Regex("(?i)\\.apk$"), null)

    data class Asset(val name: String, val url: String, val sha256: String, val size: Long, val tag: String)

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * latest release from the github api. github publishes a sha256 "digest" for every release asset;
     * no digest means we can't verify it, so we refuse instead of installing it blind.
     */
    suspend fun latestAsset(part: GhPart): Asset = withContext(Dispatchers.IO) {
        val conn = URL("https://api.github.com/repos/${part.repo}/releases/latest").openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.connectTimeout = 20_000; conn.readTimeout = 30_000
        try {
            if (conn.responseCode != 200) throw IOException("github api ${conn.responseCode} for ${part.repo}")
            val rel = json.parseToJsonElement(conn.inputStream.bufferedReader().readText()).jsonObject
            val tag = rel["tag_name"]?.jsonPrimitive?.contentOrNull ?: "?"
            val assets = rel["assets"]!!.jsonArray.map { it.jsonObject }.filter {
                val n = it["name"]!!.jsonPrimitive.content
                part.assetPattern.containsMatchIn(n) && !n.contains("debug", ignoreCase = true)
            }
            val pick = part.prefer?.let { p -> assets.firstOrNull { p.containsMatchIn(it["name"]!!.jsonPrimitive.content) } } ?: assets.firstOrNull()
                ?: throw IOException("no matching asset in ${part.repo} $tag")
            val digest = pick["digest"]?.jsonPrimitive?.contentOrNull
                ?: throw IOException("github published no sha256 for ${pick["name"]} in ${part.repo} $tag, not installing it unverified")
            if (!digest.startsWith("sha256:")) throw IOException("unexpected digest $digest")
            Asset(
                name = pick["name"]!!.jsonPrimitive.content,
                url = pick["browser_download_url"]!!.jsonPrimitive.content,
                sha256 = digest.removePrefix("sha256:"),
                size = pick["size"]?.jsonPrimitive?.longOrNull ?: 0,
                tag = tag,
            )
        } finally {
            conn.disconnect()
        }
    }

    /** id= from a kernelsu/magisk module zip */
    fun moduleId(zip: File): String = ZipFile(zip).use { z ->
        val e = z.getEntry("module.prop") ?: throw IOException("${zip.name} has no module.prop, not a module")
        z.getInputStream(e).bufferedReader().readLines().firstOrNull { it.startsWith("id=") }?.removePrefix("id=")?.trim()
            ?: throw IOException("${zip.name} module.prop has no id")
    }

    /** android abi -> the folder name system apps use for their libs */
    fun libDir(abi: String) = when (abi) {
        "arm64-v8a" -> "arm64"; "armeabi-v7a" -> "arm"; "x86_64" -> "x86_64"; "x86" -> "x86"; else -> abi
    }

    /** privileged permissions the apk asks for, as this exact image defines them */
    fun privilegedPermissions(ctx: Context, apk: File): List<String> {
        val pm = ctx.packageManager
        @Suppress("DEPRECATION")
        val info = pm.getPackageArchiveInfo(apk.path, PackageManager.GET_PERMISSIONS) ?: throw IOException("can't read ${apk.name}")
        return info.requestedPermissions.orEmpty().filter { name ->
            try {
                val p = pm.getPermissionInfo(name, 0)
                val flags = if (Build.VERSION.SDK_INT >= 28) p.protectionFlags else @Suppress("DEPRECATION") (p.protectionLevel and PermissionInfo.PROTECTION_MASK_FLAGS)
                flags and PermissionInfo.PROTECTION_FLAG_PRIVILEGED != 0
            } catch (e: PackageManager.NameNotFoundException) { false }
        }.sorted()
    }

    fun privappXml(perms: Map<String, List<String>>): String = buildString {
        appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        appendLine("<!-- generated by lineagifier from what each apk requests and what this image marks privileged -->")
        appendLine("<permissions>")
        for ((pkg, list) in perms) {
            appendLine("    <privapp-permissions package=\"$pkg\">")
            list.forEach { appendLine("        <permission name=\"$it\"/>") }
            appendLine("    </privapp-permissions>")
        }
        appendLine("</permissions>")
    }

    val SYSCONFIG = """
        <?xml version="1.0" encoding="utf-8"?>
        <!-- lets microg keep push and location working in doze, like lineageos for microg does -->
        <config>
            <allow-in-power-save package="$GMS"/>
            <allow-in-data-usage-save package="$GMS"/>
        </config>
    """.trimIndent() + "\n"

    /** builds the kernelsu module zip that puts microg in /system/priv-app */
    fun buildModule(ctx: Context, apks: List<Pair<MicrogApk, File>>, out: File) {
        val abi = Build.SUPPORTED_ABIS.first()
        val perms = apks.associate { (a, f) -> a.pkg to privilegedPermissions(ctx, f) }
        ZipOutputStream(out.outputStream().buffered()).use { z ->
            fun text(path: String, s: String) { z.putNextEntry(ZipEntry(path)); z.write(s.toByteArray()); z.closeEntry() }
            text("module.prop", """
                id=$MODULE_ID
                name=microG (lineagifier)
                version=${apks.first().first.download.versionName}
                versionCode=1
                author=lineagifier
                description=microG GmsCore + companion as privileged system apps. remove it from lineagifier's restore or from kernelsu.
            """.trimIndent() + "\n")
            text("system/etc/permissions/privapp-permissions-lineagifier-microg.xml", privappXml(perms))
            text("system/etc/sysconfig/lineagifier-microg.xml", SYSCONFIG)
            for ((a, f) in apks) {
                val base = "system/priv-app/${a.dirName}"
                z.putNextEntry(ZipEntry("$base/${a.dirName}.apk")); f.inputStream().use { it.copyTo(z) }; z.closeEntry()
                // system apps don't get their native libs extracted, so ship them next to the apk
                ZipFile(f).use { apk ->
                    apk.entries().asSequence().filter { it.name.startsWith("lib/$abi/") && it.name.endsWith(".so") }.forEach { e ->
                        z.putNextEntry(ZipEntry("$base/lib/${libDir(abi)}/${e.name.substringAfterLast('/')}"))
                        apk.getInputStream(e).use { it.copyTo(z) }
                        z.closeEntry()
                    }
                }
            }
        }
    }

    data class State(
        val ksud: Boolean,
        val modules: Map<String, String>,   // id -> name
        val gmsInstalled: Boolean,
        val gmsOurs: Boolean,
        val zygisk: String?,
        val xposed: String?,
        val fakegapps: Boolean,
        val spoofing: Boolean?,
    )

    suspend fun state(ctx: Context, shell: ShellRunner): State {
        val ksud = shell.run("[ -x $KSUD ] && echo yes").out.trim() == "yes"
        // name= lines from every installed module, so we see zygisk/lsposed the user may already have
        val r = shell.run("for d in /data/adb/modules/*/; do [ -f \"\$d/module.prop\" ] && echo \"\$(basename \$d)|\$(grep -m1 '^name=' \$d/module.prop | cut -d= -f2-)\"; done")
        val mods = r.out.lines().filter { '|' in it }.associate { it.substringBefore('|') to it.substringAfter('|') }
        val pkgs = shell.run("pm list packages").out
        val gms = "package:$GMS\n" in pkgs + "\n" || pkgs.lines().any { it.trim() == "package:$GMS" }
        val zygisk = mods.entries.firstOrNull { (id, n) -> "zygisk" in id.lowercase() || "zygisk" in n.lowercase() }?.value
        val xposed = mods.entries.firstOrNull { (id, n) -> listOf("lsposed", "vector", "xposed").any { k -> k in id.lowercase() || k in n.lowercase() } }?.value
        val fake = pkgs.lines().any { it.trim() == "package:$FAKEGAPPS" }
        return State(ksud, mods, gms, MODULE_ID in mods, zygisk, xposed, fake, if (gms) spoofingWorks(ctx) else null)
    }

    /**
     * microg declares google's certificate in its manifest; with working spoofing the framework hands that
     * out as gms' signature instead of microg's own.
     */
    fun spoofingWorks(ctx: Context): Boolean = try {
        @Suppress("DEPRECATION")
        val sigs = ctx.packageManager.getPackageInfo(GMS, PackageManager.GET_SIGNATURES).signatures.orEmpty()
        val google = java.security.MessageDigest.getInstance("SHA-256")
        sigs.any { s ->
            google.digest(s.toByteArray()).joinToString("") { "%02x".format(it) } == GOOGLE_CERT_SHA256
        }
    } catch (t: Throwable) { false }

    /**
     * sha256 of the certificate microg fakes. read from gmscore 0.3.18's own <meta-data fake-signature> resource
     * (sha1 38918a453d07199354f8b19af05ec6562ced5788, google's play services release key).
     */
    const val GOOGLE_CERT_SHA256 = "f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83"

    data class Step(val label: String, val ok: Boolean, val detail: String)

    /** runs every missing step. returns what happened; nothing here reboots on its own. */
    suspend fun install(
        ctx: Context, shell: ShellRunner, log: UndoLog, withGsf: Boolean,
        progress: (String, Float?) -> Unit,
    ): List<Step> {
        val steps = mutableListOf<Step>()
        val st = state(ctx, shell)
        if (!st.ksud) return listOf(Step("kernelsu", false, "no $KSUD, lineagifier only knows how to install kernelsu modules"))
        if (st.gmsInstalled && !st.gmsOurs) return listOf(Step("google services", false, "$GMS is already installed (real gapps or another microg). not touching it"))
        val dir = File(ctx.cacheDir, "microg").apply { mkdirs() }

        if (!st.gmsOurs) {
            val chosen = APKS.filter { it.required || withGsf }
            val files = mutableListOf<Pair<MicrogApk, File>>()
            for (a in chosen) {
                val f = File(dir, "${a.pkg}.apk")
                try {
                    Downloader.fetch(a.download, f) { progress("downloading ${a.label}", it) }
                    @Suppress("DEPRECATION")
                    val pkg = ctx.packageManager.getPackageArchiveInfo(f.path, 0)?.packageName
                    if (pkg != a.pkg) throw IOException("apk is $pkg, expected ${a.pkg}")
                    files += a to f
                } catch (t: Throwable) {
                    return steps + Step(a.label, false, t.message ?: "$t")
                }
            }
            progress("building the microg module", null)
            val zip = File(dir, "$MODULE_ID.zip")
            try {
                buildModule(ctx, files, zip)
            } catch (t: Throwable) {
                return steps + Step("microg module", false, t.message ?: "$t")
            }
            steps += installModule(shell, log, zip, "microg module (${files.joinToString { it.first.dirName }})")
            files.forEach { it.second.delete() }
            zip.delete()
            if (!steps.last().ok) return steps
        } else steps += Step("microg module", true, "already installed")

        for (part in listOf(NEOZYGISK, VECTOR)) {
            val have = if (part === NEOZYGISK) st.zygisk else st.xposed
            if (have != null) { steps += Step(part.label, true, "already have: $have"); continue }
            steps += try {
                progress("asking github for ${part.label}", null)
                val a = latestAsset(part)
                val f = File(dir, safeToken(a.name))
                Downloader.fetch(Download(a.tag, 0, 0, a.url, null, a.sha256, a.size), f) { progress("downloading ${a.name}", it) }
                installModule(shell, log, f, "${part.label} ${a.tag}").also { f.delete() }
            } catch (t: Throwable) {
                Step(part.label, false, t.message ?: "$t")
            }
            if (!steps.last().ok) return steps
        }

        if (!st.fakegapps) {
            steps += try {
                progress("asking github for fakegapps", null)
                val a = latestAsset(FAKEGAPPS_PART)
                val f = File(dir, "fakegapps.apk")
                Downloader.fetch(Download(a.tag, 0, 0, a.url, null, a.sha256, a.size), f) { progress("downloading fakegapps", it) }
                @Suppress("DEPRECATION")
                val pkg = ctx.packageManager.getPackageArchiveInfo(f.path, 0)?.packageName
                if (pkg != FAKEGAPPS) throw IOException("apk is $pkg, expected $FAKEGAPPS")
                log.add(Kind.app, FAKEGAPPS)
                val r = shell.installApk(f)
                f.delete()
                if (!r.out.contains("Success")) throw IOException("pm install: ${r.message}")
                Step("${FAKEGAPPS_PART.label} ${a.tag}", true, "installed")
            } catch (t: Throwable) {
                Step(FAKEGAPPS_PART.label, false, t.message ?: "$t")
            }
        } else steps += Step(FAKEGAPPS_PART.label, true, "already installed")
        return steps
    }

    private suspend fun installModule(shell: ShellRunner, log: UndoLog, zip: File, label: String): Step {
        val id = try { moduleId(zip) } catch (t: Throwable) { return Step(label, false, t.message ?: "$t") }
        // log before installing so restore can always find it
        val e = log.add(Kind.module, safeToken(id))
        val r = shell.run("$KSUD module install ${shQuote(zip.absolutePath)}")
        val there = shell.run("[ -d /data/adb/modules_update/$id ] || [ -d /data/adb/modules/$id ] && echo yes").out.trim() == "yes"
        if (!there) {
            log.markRestored(e.id)
            return Step(label, false, "ksud module install failed: ${r.message}")
        }
        return Step(label, true, "installed module $id (active after reboot)")
    }
}
