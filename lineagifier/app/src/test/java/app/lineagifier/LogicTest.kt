package app.lineagifier

import app.lineagifier.catalog.Catalog
import app.lineagifier.catalog.Status
import app.lineagifier.install.DefaultsState
import app.lineagifier.install.PROTECTED
import app.lineagifier.install.Safety
import app.lineagifier.shell.ShellResult
import app.lineagifier.shell.ShellRunner
import app.lineagifier.system.Overlays
import app.lineagifier.system.PackageState
import app.lineagifier.system.Pm
import app.lineagifier.system.Roles
import app.lineagifier.tweaks.Tweaks
import app.lineagifier.undo.Kind
import app.lineagifier.undo.Restore
import app.lineagifier.undo.UndoLog
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FakeShell(val answers: (String) -> ShellResult = { ShellResult(0, "", "") }) : ShellRunner {
    val ran = mutableListOf<String>()
    override suspend fun run(cmd: String): ShellResult { ran += cmd; return answers(cmd) }
}

class CatalogTest {
    private val cat = Catalog.parse(File("src/main/assets/catalog.json").readText())

    @Test fun parsesAndHasEveryRequestedSlot() {
        val ids = cat.entries.map { it.id }.toSet()
        for (want in listOf("launcher", "camera", "gallery", "browser", "calendar", "recorder", "clock", "calculator", "files", "contacts", "dialer", "messaging", "backgrounds", "backup"))
            assertTrue("missing $want", want in ids)
    }

    @Test fun everyDownloadIsHttpsFdroidWithRealLookingHash() {
        val all = cat.entries.flatMap { it.installables() }
        assertTrue(all.isNotEmpty())
        for (i in all) {
            val d = i.download
            assertTrue(d.url, d.url.startsWith("https://f-droid.org/repo/${i.packageId}_${d.versionCode}.apk"))
            assertEquals(d.url.replace("/repo/", "/archive/"), d.fallbackUrl)
            assertTrue(Regex("^[0-9a-f]{64}$").matches(d.sha256))
            assertTrue(d.size > 0)
        }
    }

    @Test fun nonPrebuiltEntriesHaveNoDownload() {
        for (e in cat.entries) if (e.status != Status.prebuilt) assertNull(e.id, e.download)
        assertEquals(Status.unsupported, cat.entries.first { it.id == "backup" }.status)
    }

    @Test fun catalogNeverTargetsProtectedPackages() {
        for (e in cat.entries) for (r in e.replaces) assertFalse("${e.id} replaces protected $r", r in PROTECTED)
    }

    @Test fun replacementIdsNeverIncludeTheStockApp() {
        for (e in cat.entries) assertTrue(e.replacementIds().none { it in e.replaces })
    }
}

class ParseTest {
    @Test fun pmList() {
        val out = "package:com.android.camera2\npackage:com.android.launcher3\n\nWARNING junk\npackage:android\n"
        assertEquals(setOf("com.android.camera2", "com.android.launcher3", "android"), Pm.parseList(out))
    }

    @Test fun overlayList() {
        val out = """
            android
            [x] com.android.internal.systemui.navbar.gestural
            [ ] com.android.internal.systemui.navbar.threebutton
            --- com.android.internal.systemui.navbar.twobutton
            [ ] com.android.theme.icon.squircle
            com.android.systemui
            [ ] com.android.theme.icon_pack.circular.systemui
        """.trimIndent()
        val o = Overlays.parse(out)
        assertEquals(5, o.size)
        assertTrue(o.first { it.pkg.endsWith("gestural") }.enabled)
        assertFalse(o.first { it.pkg.endsWith("twobutton") }.loadable)
        assertEquals("com.android.systemui", o.last().target)
    }

    @Test fun roleHolders() {
        assertEquals(listOf("com.android.launcher3"), Roles.parseHolders("com.android.launcher3\n"))
        assertEquals(listOf("a.b", "c.d"), Roles.parseHolders("a.b;c.d"))
        assertEquals(emptyList<String>(), Roles.parseHolders("\n"))
    }
}

class SafetyTest {
    private val cat = Catalog.parse(File("src/main/assets/catalog.json").readText())
    private val gallery = cat.entries.first { it.id == "gallery" }
    private val dialer = cat.entries.first { it.id == "dialer" }
    private val launcher = cat.entries.first { it.id == "launcher" }

    private fun pkgs(installed: Set<String>, system: Set<String> = installed, disabled: Set<String> = emptySet()) =
        PackageState(system, installed, disabled)
    private val noRoles = DefaultsState(roleHolders = emptyMap(), homePackages = setOf("com.android.launcher3"), currentHome = "com.android.launcher3")

    @Test fun refusesWithoutReplacement() {
        val d = Safety.canRemove("com.android.gallery3d", gallery, pkgs(setOf("com.android.gallery3d")), noRoles)
        assertFalse(d.allowed)
    }

    @Test fun allowsWhenReplacementInstalled() {
        val p = pkgs(setOf("com.android.gallery3d", "org.fossify.gallery"), system = setOf("com.android.gallery3d"))
        assertTrue(Safety.canRemove("com.android.gallery3d", gallery, p, noRoles).allowed)
    }

    @Test fun refusesWhenReplacementIsDisabled() {
        val p = pkgs(setOf("com.android.gallery3d", "org.fossify.gallery"), system = setOf("com.android.gallery3d"), disabled = setOf("org.fossify.gallery"))
        assertFalse(Safety.canRemove("com.android.gallery3d", gallery, p, noRoles).allowed)
    }

    @Test fun refusesWhenNotOnImage() {
        val p = pkgs(setOf("org.fossify.gallery"), system = emptySet())
        assertFalse(Safety.canRemove("com.android.gallery3d", gallery, p, noRoles).allowed)
    }

    @Test fun roleMustBeHeldFirst() {
        val p = pkgs(setOf("com.android.dialer", "org.fossify.phone"), system = setOf("com.android.dialer"))
        val notYet = noRoles.copy(roleHolders = mapOf("android.app.role.DIALER" to listOf("com.android.dialer")))
        assertFalse(Safety.canRemove("com.android.dialer", dialer, p, notYet).allowed)
        val done = noRoles.copy(roleHolders = mapOf("android.app.role.DIALER" to listOf("org.fossify.phone")))
        assertTrue(Safety.canRemove("com.android.dialer", dialer, p, done).allowed)
    }

    @Test fun neverStrandsWithoutLauncher() {
        // pretend some other launcher got registered as trebuchet's .dev build
        val p = pkgs(setOf("com.android.launcher3", "com.android.launcher3.dev"), system = setOf("com.android.launcher3"))
        val onlyStock = DefaultsState(mapOf(Safety.HOME to listOf("com.android.launcher3")), setOf("com.android.launcher3"), "com.android.launcher3")
        assertFalse(Safety.canRemove("com.android.launcher3", launcher, p, onlyStock).allowed)
    }

    @Test fun neverRemovesProtected() {
        val files = cat.entries.first { it.id == "files" }.copy(replaces = listOf("com.android.documentsui"))
        val p = pkgs(setOf("com.android.documentsui", "org.fossify.filemanager"), system = setOf("com.android.documentsui"))
        assertFalse(Safety.canRemove("com.android.documentsui", files, p, noRoles).allowed)
    }

    @Test fun homeProviderNeedsAnotherCurrentHome() {
        // an alternative that is also a launcher candidate: stock is home -> refuse until home moved
        val e = gallery.copy(replaces = listOf("com.android.gallery3d"))
        val p = pkgs(setOf("com.android.gallery3d", "org.fossify.gallery"), system = setOf("com.android.gallery3d"))
        val d = DefaultsState(emptyMap(), setOf("com.android.gallery3d"), "com.android.gallery3d")
        assertFalse(Safety.canRemove("com.android.gallery3d", e, p, d).allowed)
    }
}

class UndoTest {
    private fun tmpLog() = UndoLog(File(Files.createTempDirectory("undo").toFile(), "undo-log.json"))

    @Test fun commandsForEachKind() {
        val l = tmpLog()
        assertEquals(listOf("cmd package install-existing --user 0 com.android.camera2"), Restore.commands(l.add(Kind.uninstall, "com.android.camera2")))
        assertEquals(listOf("pm enable --user 0 com.android.camera2"), Restore.commands(l.add(Kind.disable, "com.android.camera2")))
        assertEquals(listOf("settings delete global window_animation_scale"), Restore.commands(l.add(Kind.setting, "global/window_animation_scale", null)))
        assertEquals(listOf("settings put secure theme_customization_overlay_packages '{\"a\":\"b\"}'"),
            Restore.commands(l.add(Kind.setting, "secure/theme_customization_overlay_packages", "{\"a\":\"b\"}")))
        assertEquals(listOf("cmd role add-role-holder --user 0 android.app.role.HOME com.android.launcher3"),
            Restore.commands(l.add(Kind.role, "android.app.role.HOME", "com.android.launcher3", added = "x.y")))
        assertEquals(listOf("cmd role remove-role-holder --user 0 android.app.role.BROWSER x.y"),
            Restore.commands(l.add(Kind.role, "android.app.role.BROWSER", "", added = "x.y")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun refusesInjection() {
        Restore.commands(tmpLog().add(Kind.disable, "com.x; reboot"))
    }

    @Test fun restoresNewestFirstAndMarksDone() = runBlocking {
        val l = tmpLog()
        l.add(Kind.role, "android.app.role.HOME", "com.android.launcher3", added = "x.home")
        l.add(Kind.disable, "com.android.launcher3")
        val sh = FakeShell()
        val res = Restore.everything(l, sh)
        assertTrue(res.all { it.ok })
        // launcher re-enabled before home role goes back to it
        assertEquals("pm enable --user 0 com.android.launcher3", sh.ran[0])
        assertTrue(sh.ran[1].startsWith("cmd role add-role-holder"))
        assertTrue(l.pending().isEmpty())
        // running again does nothing
        assertTrue(Restore.everything(l, sh).isEmpty())
    }

    @Test fun failedRestoreStaysPending() = runBlocking {
        val l = tmpLog()
        l.add(Kind.uninstall, "com.android.camera2")
        val sh = FakeShell { ShellResult(1, "", "Package com.android.camera2 doesn't exist") }
        val res = Restore.everything(l, sh)
        assertFalse(res.single().ok)
        assertEquals(1, l.pending().size)
    }

    @Test fun survivesReload() {
        val dir = Files.createTempDirectory("undo2").toFile()
        UndoLog(File(dir, "u.json")).add(Kind.disable, "com.android.camera2")
        assertEquals("com.android.camera2", UndoLog(File(dir, "u.json")).all().single().target)
    }
}

class TweaksTest {
    @Test fun mergeKeepsOtherKeys() {
        val cur = """{"android.theme.customization.font":"x","android.theme.customization.color_source":"home_wallpaper"}"""
        val j = Json.parseToJsonElement(Tweaks.mergeTheme(cur, "167C80", null)).jsonObject
        assertEquals("x", j["android.theme.customization.font"]!!.jsonPrimitive.content)
        assertEquals("preset", j["android.theme.customization.color_source"]!!.jsonPrimitive.content)
        assertEquals("167C80", j["android.theme.customization.system_palette"]!!.jsonPrimitive.content)
    }

    @Test fun mergeFromNullAndShape() {
        val j = Json.parseToJsonElement(Tweaks.mergeTheme("null", null, "com.android.theme.icon.squircle")).jsonObject
        assertEquals("com.android.theme.icon.squircle", j["android.theme.customization.adaptive_icon_shape"]!!.jsonPrimitive.content)
        val back = Json.parseToJsonElement(Tweaks.mergeTheme(j.toString(), null, "default")).jsonObject
        assertFalse(back.containsKey("android.theme.customization.adaptive_icon_shape"))
    }

    @Test fun probeMarksMissingCommandsUnavailable() = runBlocking {
        // an image with no uimode, no overlay service, and settings that fails
        val sh = FakeShell { cmd ->
            when {
                cmd == "cmd -l" -> ShellResult(0, "package\nsettings\nactivity\n", "")
                cmd.startsWith("settings get") -> ShellResult(255, "", "cmd: Can't find service: settings")
                else -> ShellResult(1, "", "nope")
            }
        }
        val t = Tweaks(sh, UndoLog(File(Files.createTempDirectory("t").toFile(), "u.json")), 33).probe()
        assertTrue(t.none { it.available })
        assertFalse(sh.ran.any { it.startsWith("cmd overlay") })
    }

    @Test fun probeOffersOnlyOverlaysThatExist() = runBlocking {
        val sh = FakeShell { cmd ->
            when {
                cmd == "cmd -l" -> ShellResult(0, "uimode\noverlay\nsettings\n", "")
                cmd == "cmd overlay list" -> ShellResult(0, "android\n[x] com.android.internal.systemui.navbar.threebutton\n[ ] com.android.internal.systemui.navbar.gestural\n[ ] com.android.theme.icon.squircle\n", "")
                cmd == "cmd overlay help" -> ShellResult(0, "enable-exclusive [--user USER_ID] [--category] PACKAGE", "")
                cmd == "cmd uimode night" -> ShellResult(0, "Night mode: no\n", "")
                cmd.startsWith("settings get") -> ShellResult(0, "null\n", "")
                else -> ShellResult(0, "", "")
            }
        }
        val t = Tweaks(sh, UndoLog(File(Files.createTempDirectory("t").toFile(), "u.json")), 33).probe().associateBy { it.id }
        assertTrue(t.values.all { it.available })
        assertEquals(2, t[Tweaks.NAV]!!.options.size)
        assertEquals("com.android.internal.systemui.navbar.threebutton", t[Tweaks.NAV]!!.current)
        assertEquals(listOf("default", "com.android.theme.icon.squircle"), t[Tweaks.SHAPE]!!.options.map { it.value })
        assertEquals("no", t[Tweaks.DARK]!!.current)
    }
}

class DumpsysRoleTest {
    // trimmed from a real aosp 13 emulator (sdk 33)
    private val out = """
        ROLE STATE (dumpsys role):
        {
          user_states={
            user_id=0
            roles=[
              {
                name=android.app.role.SYSTEM_CONTACTS
              }
              {
                name=android.app.role.HOME
                holders=com.android.sdksetup
              }
              {
                name=android.app.role.SYSTEM_SETTINGS_INTELLIGENCE
                holders=com.android.settings.intelligence
                holders=com.android.bluetooth
              }
            ]
            user_id=10
            roles=[
              {
                name=android.app.role.HOME
                holders=other.launcher
              }
            ]
          }
        }
    """.trimIndent()

    @Test fun readsUser0Only() {
        assertEquals(listOf("com.android.sdksetup"), Roles.parseDumpsys(out, "android.app.role.HOME"))
        assertEquals(listOf("other.launcher"), Roles.parseDumpsys(out, "android.app.role.HOME", 10))
        assertEquals(listOf("com.android.settings.intelligence", "com.android.bluetooth"), Roles.parseDumpsys(out, "android.app.role.SYSTEM_SETTINGS_INTELLIGENCE"))
        assertEquals(emptyList<String>(), Roles.parseDumpsys(out, "android.app.role.SYSTEM_CONTACTS"))
    }
}

class RoleTimeoutTest {
    @Test fun restoreTrustsHoldersNotExitCode() = runBlocking {
        val l = UndoLog(File(Files.createTempDirectory("rt").toFile(), "u.json"))
        l.add(Kind.role, "android.app.role.DIALER", previous = "com.android.dialer", added = "org.fossify.phone")
        // what aosp 13 really does: the command "fails" with a timeout, the change lands anyway
        val sh = FakeShell { cmd ->
            when {
                cmd.startsWith("cmd role add-role-holder") -> ShellResult(255, "", "Error: see logcat for details.\njava.util.concurrent.TimeoutException")
                cmd.startsWith("cmd role get-role-holders") -> ShellResult(255, "", "Unknown command: get-role-holders")
                cmd == "dumpsys role" -> ShellResult(0, "user_id=0\n{\nname=android.app.role.DIALER\nholders=com.android.dialer\n}\n", "")
                else -> ShellResult(0, "", "")
            }
        }
        assertTrue(Restore.everything(l, sh).single().ok)
        assertTrue(l.pending().isEmpty())
    }
}
