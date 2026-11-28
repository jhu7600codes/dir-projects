package app.lineagifier

import app.lineagifier.microg.Microg
import app.lineagifier.qs.Backend
import app.lineagifier.qs.Backends
import app.lineagifier.qs.Tiles
import app.lineagifier.undo.Kind
import app.lineagifier.undo.Restore
import app.lineagifier.undo.UndoLog
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MicrogTest {
    @Test fun privappXml() {
        val x = Microg.privappXml(mapOf("com.google.android.gms" to listOf("android.permission.INSTALL_LOCATION_PROVIDER")))
        assertTrue(x.contains("<privapp-permissions package=\"com.google.android.gms\">"))
        assertTrue(x.contains("<permission name=\"android.permission.INSTALL_LOCATION_PROVIDER\"/>"))
    }

    @Test fun libDirs() {
        assertEquals("arm64", Microg.libDir("arm64-v8a"))
        assertEquals("arm", Microg.libDir("armeabi-v7a"))
        assertEquals("x86_64", Microg.libDir("x86_64"))
    }

    @Test fun moduleIdFromZip() {
        val f = File(Files.createTempDirectory("m").toFile(), "m.zip")
        ZipOutputStream(f.outputStream()).use { z ->
            z.putNextEntry(ZipEntry("module.prop")); z.write("name=Zygisk\nid=zygisksu\nversion=1\n".toByteArray()); z.closeEntry()
        }
        assertEquals("zygisksu", Microg.moduleId(f))
    }

    @Test fun pinnedApksAreMicrogsOwnRepo() {
        for (a in Microg.APKS) {
            assertTrue(a.download.url.startsWith("https://microg.org/fdroid/repo/${a.pkg}-${a.download.versionCode}.apk"))
            assertTrue(Regex("^[0-9a-f]{64}$").matches(a.download.sha256))
        }
        assertEquals(64, Microg.GOOGLE_CERT_SHA256.length)
    }
}

class NewUndoKindsTest {
    private val l = UndoLog(File(Files.createTempDirectory("u").toFile(), "u.json"))

    @Test fun moduleAppFabricatedAppop() {
        assertEquals(listOf("/data/adb/ksud module uninstall lineagifier_microg"), Restore.commands(l.add(Kind.module, "lineagifier_microg")))
        assertEquals(listOf("pm uninstall inc.whew.android.fakegapps"), Restore.commands(l.add(Kind.app, "inc.whew.android.fakegapps")))
        assertEquals(listOf("cmd overlay disable --user 0 com.android.shell:lgf_sb_height"), Restore.commands(l.add(Kind.fabricated, "com.android.shell:lgf_sb_height")))
        assertEquals(listOf("appops set app.lineagifier SYSTEM_ALERT_WINDOW default"), Restore.commands(l.add(Kind.appop, "app.lineagifier/SYSTEM_ALERT_WINDOW", "default")))
    }
}

class QsTest {
    @Test fun toggleCommandsFlipTheState() {
        assertEquals("cmd wifi set-wifi-enabled disabled", Backends.toggleCommand(Backend.WIFI, true))
        assertEquals("cmd wifi set-wifi-enabled enabled", Backends.toggleCommand(Backend.WIFI, false))
        assertEquals("cmd notification set_dnd on", Backends.toggleCommand(Backend.DND, false))
        assertEquals("cmd location set-location-enabled false", Backends.toggleCommand(Backend.LOCATION, true))
        assertEquals("settings put system accelerometer_rotation 1", Backends.toggleCommand(Backend.ROTATION, false))
        assertEquals("settings put global heads_up_notifications_enabled 0", Backends.toggleCommand(Backend.HEADS_UP, true))
        assertNull(Backends.toggleCommand(Backend.TORCH, false))
    }

    @Test fun caffeineCyclesLikeLineage() {
        // off -> tap -> 5 min
        assertEquals(0, Tiles.caffeineClick(false, -1, -1, 100_000))
        // tapping again within 5s steps through 10, 30, infinity, then off
        assertEquals(1, Tiles.caffeineClick(true, 0, 100_000, 102_000))
        assertEquals(2, Tiles.caffeineClick(true, 1, 102_000, 103_000))
        assertEquals(3, Tiles.caffeineClick(true, 2, 103_000, 104_000))
        assertEquals(-1, Tiles.caffeineClick(true, 3, 104_000, 105_000))
        // a slow tap while on just turns it off
        assertEquals(-1, Tiles.caffeineClick(true, 1, 100_000, 120_000))
        assertEquals("05:00", Tiles.caffeineLabel(300))
        assertEquals("\u221E", Tiles.caffeineLabel(-1))
    }

    @Test fun brightnessGammaRoundTrips() {
        for (v in listOf(1, 10, 50, 128, 200, 255)) {
            val g = Tiles.linearToGamma(v, 1, 255)
            assertTrue(g in 0..Tiles.GAMMA_SPACE_MAX)
            assertEquals(v.toDouble(), Tiles.gammaToLinear(g, 1, 255).toDouble(), 1.0)
        }
        // the curve gives the low end most of the slider, like aosp
        assertTrue(Tiles.linearToGamma(26, 1, 255) > Tiles.GAMMA_SPACE_MAX / 3)
    }

    @Test fun defaultTilesExistAndStartWithLineagesOrder() {
        assertEquals(listOf("internet", "bt", "flashlight", "dnd"), Tiles.DEFAULT.take(4))
        assertTrue(Tiles.DEFAULT.all { Tiles.bySpec(it) != null })
    }
}

class SystemSwapTest {
    private val cat = app.lineagifier.catalog.Catalog.parse(File("src/main/assets/catalog.json").readText())
    private fun apk(pkg: String, minSdk: Int = 33, shared: String? = null, certs: List<String> = listOf("ab")) =
        app.lineagifier.install.ApkInfo(File("x.apk"), pkg, "1", 1, minSdk, certs, shared)

    @Test fun modulePathsFollowKernelsuLayout() {
        assertEquals("system/product/app/DeskClock/DeskClock.apk", app.lineagifier.install.SystemSwap.modulePath("/product/app/DeskClock/DeskClock.apk"))
        assertEquals("system/system_ext/priv-app/Launcher3QuickStep/Launcher3QuickStep.apk", app.lineagifier.install.SystemSwap.modulePath("/system_ext/priv-app/Launcher3QuickStep/Launcher3QuickStep.apk"))
        assertEquals("system/app/Calculator/Calculator.apk", app.lineagifier.install.SystemSwap.modulePath("/system/app/Calculator/Calculator.apk"))
    }

    @Test(expected = java.io.IOException::class)
    fun refusesPathsAModuleCantOverlay() { app.lineagifier.install.SystemSwap.modulePath("/data/app/x/base.apk") }

    @Test fun refusals() {
        val clock = cat.entries.first { it.id == "clock" }
        assertNull(app.lineagifier.install.SystemSwap.refuse(clock, sdk = 33, a = apk("com.android.deskclock")))
        assertNotNull(app.lineagifier.install.SystemSwap.refuse(clock, sdk = 33, a = apk("com.evil.clock")))
        assertNotNull(app.lineagifier.install.SystemSwap.refuse(clock, sdk = 33, a = apk("com.android.deskclock", minSdk = 99)))
        // shared uid + new signature = system_server IllegalStateException at boot on 13
        assertNotNull(app.lineagifier.install.SystemSwap.refuse(clock, sdk = 33, a = apk("com.android.deskclock", shared = "android.uid.system")))
        assertNotNull(app.lineagifier.install.SystemSwap.refuse(clock, sdk = 33, a = apk("com.android.deskclock", certs = emptyList())))
    }

    @Test fun launcherNeedsABackup() {
        assertNull(app.lineagifier.install.SystemSwap.backupLauncher(setOf("com.android.launcher3"), "com.android.launcher3"))
        assertEquals("app.lawnchair", app.lineagifier.install.SystemSwap.backupLauncher(setOf("com.android.launcher3", "app.lawnchair"), "com.android.launcher3"))
    }

    @Test fun everyApkmirrorEntryHasLineageIds() {
        for (e in cat.entries) if (e.apkmirror != null) {
            assertTrue(e.apkmirror!!.startsWith("https://www.apkmirror.com/apk/lineageos/"))
            assertTrue(e.id, e.lineageIds.isNotEmpty())
        }
    }
}
