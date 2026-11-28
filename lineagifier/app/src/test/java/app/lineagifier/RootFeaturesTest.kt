package app.lineagifier

import app.lineagifier.microg.Microg
import app.lineagifier.statusbar.ControlCenter
import app.lineagifier.statusbar.Sizing
import app.lineagifier.undo.Kind
import app.lineagifier.undo.Restore
import app.lineagifier.undo.UndoLog
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SizingTest {
    @Test fun pxEncodingMatchesTheValueTestedOnAosp13() {
        // `fabricate ... 0x05 38400` read back as 150.0px on the aosp 13 emulator
        assertEquals(150L shl 8, Sizing.pxComplex(150))
        assertEquals(38400L, Sizing.pxComplex(150))
    }

    @Test fun iphone13Scaling() {
        // native iphone 13 pixels: 1170 x 2532 (3x of 390 x 844 pt)
        val s = Sizing.iphone13(1170, 2532)
        assertEquals(141, s.heightPx)   // 47 pt * 3
        assertEquals(90, s.padStartPx)  // 30 pt * 3
        // orientation doesn't matter, it scales off the long side
        assertEquals(s, Sizing.iphone13(2532, 1170))
    }

    @Test fun commandsAreRootFabricateThenEnable() {
        val c = Sizing.commands(Sizing.HEIGHT, 141)
        assertEquals("cmd overlay fabricate --target android --name lgf_sb_height android:dimen/status_bar_height_default 0x05 36096", c[0])
        assertEquals("cmd overlay enable --user 0 com.android.shell:lgf_sb_height", c[1])
    }
}

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

class ControlCenterTest {
    @Test fun toggleCommandsFlipTheState() {
        assertEquals("cmd wifi set-wifi-enabled disabled", ControlCenter.toggleCommand("wifi", true))
        assertEquals("cmd wifi set-wifi-enabled enabled", ControlCenter.toggleCommand("wifi", false))
        assertEquals("cmd notification set_dnd on", ControlCenter.toggleCommand("dnd", false))
        assertEquals("cmd location set-location-enabled false", ControlCenter.toggleCommand("location", true))
        assertEquals("settings put system accelerometer_rotation 1", ControlCenter.toggleCommand("rotation", false))
        assertNull(ControlCenter.toggleCommand("torch", false))
    }
}

class BarLogicTest {
    @Test fun readsSystemuiIconColorFromRealAosp13Dumps() {
        // both captured from the aosp 13 emulator: lineagifier (dark) then settings (light) in front
        val dark = "    mLastStatusBarAppearanceRegions=\n      AppearanceRegion{ bounds=[0,0][1080,2340]}\n"
        val light = "    mLastStatusBarAppearanceRegions=\n      AppearanceRegion{LIGHT_STATUS_BARS bounds=[0,0][1080,2340]}\n"
        assertEquals(false, app.lineagifier.statusbar.BarLogic.lightStatusBar(dark))
        assertEquals(true, app.lineagifier.statusbar.BarLogic.lightStatusBar(light))
        assertNull(app.lineagifier.statusbar.BarLogic.lightStatusBar(""))
    }
}
