package app.lineagifier.tweaks

import app.lineagifier.shell.ShellRunner
import app.lineagifier.shell.safeToken
import app.lineagifier.shell.shQuote
import app.lineagifier.system.OverlayInfo
import app.lineagifier.system.Overlays
import app.lineagifier.system.Services
import app.lineagifier.undo.Kind
import app.lineagifier.undo.UndoLog
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

data class Option(val label: String, val value: String)

data class Tweak(
    val id: String,
    val title: String,
    val available: Boolean,
    val reason: String,
    val current: String?,
    val options: List<Option>,
)

/** probes every tweak's commands on this image first; anything that doesn't exist is shown as unavailable */
class Tweaks(private val shell: ShellRunner, private val log: UndoLog, private val sdk: Int) {

    companion object {
        const val DARK = "dark"
        const val ANIM = "anim"
        const val NAV = "nav"
        const val ACCENT = "accent"
        const val SHAPE = "shape"

        const val THEME_KEY = "theme_customization_overlay_packages"
        const val NAV_PREFIX = "com.android.internal.systemui.navbar."
        const val SHAPE_PREFIX = "com.android.theme.icon."
        const val COLOR_PREFIX = "com.android.theme.color."
        val ANIM_KEYS = listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")

        /** lineage's brand teal plus a few neutral presets */
        val ACCENTS = listOf(
            Option("from wallpaper (aosp default)", "wallpaper"),
            Option("lineage teal", "167C80"),
            Option("blue", "1A73E8"),
            Option("purple", "7E57C2"),
            Option("orange", "F57C00"),
        )

        /** merges color/shape choices into the json systemui reads on android 12+ */
        fun mergeTheme(current: String?, accent: String?, shape: String?): String {
            val base = try {
                if (current.isNullOrBlank() || current == "null") JsonObject(emptyMap()) else Json.parseToJsonElement(current).jsonObject
            } catch (t: Throwable) { JsonObject(emptyMap()) }
            val m = base.toMutableMap()
            when (accent) {
                null -> {}
                "wallpaper" -> {
                    m.remove("android.theme.customization.system_palette")
                    m.remove("android.theme.customization.accent_color")
                    m["android.theme.customization.color_source"] = JsonPrimitive("home_wallpaper")
                }
                else -> {
                    m["android.theme.customization.color_source"] = JsonPrimitive("preset")
                    m["android.theme.customization.system_palette"] = JsonPrimitive(accent)
                    m["android.theme.customization.accent_color"] = JsonPrimitive(accent)
                }
            }
            when (shape) {
                null -> {}
                "default" -> m.remove("android.theme.customization.adaptive_icon_shape")
                else -> m["android.theme.customization.adaptive_icon_shape"] = JsonPrimitive(shape)
            }
            return JsonObject(m).toString()
        }

        fun prettyOverlay(pkg: String, prefix: String) = pkg.removePrefix(prefix).replace('_', ' ')
    }

    private var overlays: List<OverlayInfo> = emptyList()
    private var services: Set<String> = emptySet()
    private var exclusiveCategory = false

    suspend fun probe(): List<Tweak> {
        services = Services.list(shell)
        overlays = if ("overlay" in services) Overlays.list(shell) else emptyList()
        exclusiveCategory = "overlay" in services && shell.run("cmd overlay help").let { (it.out + it.err).contains("--category") }
        return listOf(probeDark(), probeAnim(), probeNav(), probeAccent(), probeShape())
    }

    private suspend fun probeDark(): Tweak {
        if ("uimode" !in services) return Tweak(DARK, "dark mode", false, "no `cmd uimode` on this image", null, emptyList())
        val r = shell.run("cmd uimode night")
        val cur = Regex("(?i)night mode:\\s*(\\w+)").find(r.out)?.groupValues?.get(1)?.lowercase()
        if (!r.ok || cur == null) return Tweak(DARK, "dark mode", false, "`cmd uimode night` didn't answer: ${r.message}", null, emptyList())
        return Tweak(DARK, "dark mode", true, "cmd uimode night", cur, listOf(Option("dark", "yes"), Option("light", "no"), Option("auto", "auto")))
    }

    private suspend fun probeAnim(): Tweak {
        val r = shell.run("settings get global window_animation_scale")
        if (!r.ok) return Tweak(ANIM, "animation speed", false, "`settings` can't read global: ${r.message}", null, emptyList())
        val cur = r.out.trim().let { if (it == "null") "1.0" else it }
        return Tweak(ANIM, "animation speed", true, "settings put global *_animation_scale", cur,
            listOf(Option("snappy (0.5x)", "0.5"), Option("normal (1x)", "1.0"), Option("off", "0")))
    }

    private fun probeNav(): Tweak {
        val navs = overlays.filter { it.pkg.startsWith(NAV_PREFIX) && it.loadable }
        if (navs.isEmpty()) return Tweak(NAV, "navigation", false, "no navbar overlays on this image", null, emptyList())
        val cur = navs.firstOrNull { it.enabled }?.pkg
        return Tweak(NAV, "navigation", true, "cmd overlay enable-exclusive", cur,
            navs.map { Option(prettyOverlay(it.pkg, NAV_PREFIX), it.pkg) })
    }

    private suspend fun probeAccent(): Tweak {
        if (sdk >= 31) {
            val r = shell.run("settings get secure $THEME_KEY")
            if (!r.ok) return Tweak(ACCENT, "accent color", false, "can't read secure/$THEME_KEY: ${r.message}", null, emptyList())
            return Tweak(ACCENT, "accent color", true, "settings put secure $THEME_KEY (monet)", r.out.trim(), ACCENTS)
        }
        val colors = overlays.filter { it.pkg.startsWith(COLOR_PREFIX) && it.loadable }
        if (colors.isEmpty()) return Tweak(ACCENT, "accent color", false, "no accent overlays on this image", null, emptyList())
        return Tweak(ACCENT, "accent color", true, "cmd overlay (accent overlays)", colors.firstOrNull { it.enabled }?.pkg,
            colors.map { Option(prettyOverlay(it.pkg, COLOR_PREFIX), it.pkg) })
    }

    private suspend fun probeShape(): Tweak {
        val shapes = overlays.filter { it.pkg.startsWith(SHAPE_PREFIX) && it.loadable }
        if (shapes.isEmpty()) return Tweak(SHAPE, "icon shape", false, "no icon shape overlays on this image", null, emptyList())
        val opts = listOf(Option("default", "default")) + shapes.map { Option(prettyOverlay(it.pkg, SHAPE_PREFIX), it.pkg) }
        if (sdk >= 31) {
            val r = shell.run("settings get secure $THEME_KEY")
            if (!r.ok) return Tweak(SHAPE, "icon shape", false, "can't read secure/$THEME_KEY: ${r.message}", null, emptyList())
        }
        return Tweak(SHAPE, "icon shape", true, if (sdk >= 31) "theme json + shape overlays" else "cmd overlay", shapes.firstOrNull { it.enabled }?.pkg ?: "default", opts)
    }

    /** applies one choice, logs the old value first, then reads it back to confirm */
    suspend fun apply(id: String, value: String): Result<String> = runCatching {
        when (id) {
            DARK -> {
                val before = Regex("(?i)night mode:\\s*(\\w+)").find(shell.run("cmd uimode night").out)?.groupValues?.get(1)?.lowercase() ?: "no"
                log.add(Kind.uimode, "night", previous = before)
                val r = shell.run("cmd uimode night ${safeToken(value)}")
                val after = shell.run("cmd uimode night").out
                if (!after.contains(value, ignoreCase = true)) error("uimode still says: ${after.trim()} (${r.message})")
                "dark mode: $value"
            }
            ANIM -> {
                for (k in ANIM_KEYS) putSetting("global", k, value)
                "animation scales: $value"
            }
            NAV -> {
                enableExclusive(value, overlays.filter { it.pkg.startsWith(NAV_PREFIX) })
                "navigation: ${prettyOverlay(value, NAV_PREFIX)}"
            }
            ACCENT -> if (sdk >= 31) {
                val cur = shell.run("settings get secure $THEME_KEY").out.trim()
                putSetting("secure", THEME_KEY, mergeTheme(cur, value, null))
                "accent: $value"
            } else {
                enableExclusive(value, overlays.filter { it.pkg.startsWith(COLOR_PREFIX) })
                "accent: ${prettyOverlay(value, COLOR_PREFIX)}"
            }
            SHAPE -> if (sdk >= 31) {
                val cur = shell.run("settings get secure $THEME_KEY").out.trim()
                putSetting("secure", THEME_KEY, mergeTheme(cur, null, value))
                "icon shape: ${prettyOverlay(value, SHAPE_PREFIX)}"
            } else {
                val group = overlays.filter { it.pkg.startsWith(SHAPE_PREFIX) }
                if (value == "default") group.filter { it.enabled }.forEach { setOverlay(it, false) }
                else enableExclusive(value, group)
                "icon shape: ${prettyOverlay(value, SHAPE_PREFIX)}"
            }
            else -> error("unknown tweak $id")
        }
    }

    private suspend fun putSetting(ns: String, key: String, value: String) {
        val before = shell.run("settings get ${safeToken(ns)} ${safeToken(key)}")
        if (!before.ok) error("can't read $ns/$key: ${before.message}")
        log.add(Kind.setting, "$ns/$key", previous = before.out.trim().takeIf { it != "null" })
        val r = shell.run("settings put ${safeToken(ns)} ${safeToken(key)} ${shQuote(value)}")
        val after = shell.run("settings get ${safeToken(ns)} ${safeToken(key)}").out.trim()
        if (after != value) error("$ns/$key didn't stick (now '$after'): ${r.message}")
    }

    private suspend fun setOverlay(o: OverlayInfo, enable: Boolean) {
        log.add(Kind.overlay, o.pkg, previous = if (o.enabled) "enabled" else "disabled")
        val r = shell.run("cmd overlay ${if (enable) "enable" else "disable"} --user 0 ${safeToken(o.pkg)}")
        if (!r.okStrict) error("overlay ${o.pkg}: ${r.message}")
    }

    private suspend fun enableExclusive(pkg: String, group: List<OverlayInfo>) {
        if (group.none { it.pkg == pkg }) error("$pkg isn't on this image")
        // log every member of the group so undo can put the exact old combination back
        group.forEach { log.add(Kind.overlay, it.pkg, previous = if (it.enabled) "enabled" else "disabled") }
        val r = if (exclusiveCategory) shell.run("cmd overlay enable-exclusive --user 0 --category ${safeToken(pkg)}")
        else {
            group.filter { it.enabled && it.pkg != pkg }.forEach { shell.run("cmd overlay disable --user 0 ${safeToken(it.pkg)}") }
            shell.run("cmd overlay enable --user 0 ${safeToken(pkg)}")
        }
        val now = Overlays.list(shell)
        if (now.none { it.pkg == pkg && it.enabled }) error("$pkg didn't enable: ${r.message}")
        overlays = now
    }
}
