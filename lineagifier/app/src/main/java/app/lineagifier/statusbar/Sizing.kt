package app.lineagifier.statusbar

import app.lineagifier.shell.ShellRunner
import app.lineagifier.shell.safeToken
import app.lineagifier.system.Overlays
import app.lineagifier.undo.Kind
import app.lineagifier.undo.UndoLog
import kotlin.math.roundToInt

/**
 * sizes the real systemui status bar for the iphone 13 screen twoyi runs on.
 *
 * on android 13 the bar height is max(cutout safe inset, android:dimen/status_bar_height_default)
 * (frameworks/base SystemBarUtils.getStatusBarHeight), and systemui keeps the clock/icons away from the
 * edges with status_bar_padding_start/end. root can override single resources with
 * `cmd overlay fabricate` (int values only, one resource per overlay, root only), so that's what this does.
 * aosp 13 has no shell command to delete a fabricated overlay, so "off" and undo disable it.
 */
object Sizing {
    /**
     * iphone 13 geometry in points (screen 390 x 844 pt). these are the commonly published numbers, not
     * something lineagifier can measure, so the ui shows them as sliders with these as defaults.
     */
    const val IPHONE_W_PT = 390f
    const val IPHONE_H_PT = 844f
    const val SAFE_TOP_PT = 47f        // ios top safe area on iphone 12/13
    const val NOTCH_W_PT = 162f        // iphone 13 notch width
    const val CORNER_PAD_PT = 30f      // where ios puts the clock: roughly centered in the left "ear"

    const val PREFIX = "lgf_"
    const val SHELL = "com.android.shell"

    /** the whole display in px, including the status and navigation bars (displayMetrics leaves those out) */
    fun realScreen(ctx: android.content.Context): Pair<Int, Int> {
        val wm = ctx.getSystemService(android.view.WindowManager::class.java)
        return if (android.os.Build.VERSION.SDK_INT >= 30) wm.maximumWindowMetrics.bounds.let { it.width() to it.height() }
        else android.util.DisplayMetrics().also { @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(it) }.let { it.widthPixels to it.heightPixels }
    }

    data class Spec(val heightPx: Int, val padStartPx: Int, val padEndPx: Int)

    /** px values for a screen of [wPx] x [hPx], scaled from iphone points */
    fun iphone13(wPx: Int, hPx: Int, safeTopPt: Float = SAFE_TOP_PT, padPt: Float = CORNER_PAD_PT): Spec {
        val portraitH = maxOf(wPx, hPx)
        val scale = portraitH / IPHONE_H_PT
        return Spec((safeTopPt * scale).roundToInt(), (padPt * scale).roundToInt(), (padPt * scale).roundToInt())
    }

    /** TypedValue complex encoding for a dimension in px: mantissa << 8, radix 23p0, unit px (0) */
    fun pxComplex(px: Int): Long = (px.toLong() and 0xFFFFFF) shl 8

    data class Res(val overlay: String, val target: String, val resource: String)

    val HEIGHT = Res("${PREFIX}sb_height", "android", "android:dimen/status_bar_height_default")
    val PAD_START = Res("${PREFIX}sb_pad_start", "com.android.systemui", "com.android.systemui:dimen/status_bar_padding_start")
    val PAD_END = Res("${PREFIX}sb_pad_end", "com.android.systemui", "com.android.systemui:dimen/status_bar_padding_end")
    val ALL = listOf(HEIGHT, PAD_START, PAD_END)

    /** commands for one resource; pure so it can be unit tested */
    fun commands(r: Res, px: Int): List<String> = listOf(
        "cmd overlay fabricate --target ${safeToken(r.target)} --name ${safeToken(r.overlay)} ${safeToken(r.resource)} 0x05 ${pxComplex(px)}",
        "cmd overlay enable --user 0 ${safeToken("$SHELL:${r.overlay}")}",
    )

    /** applies the spec, logs each overlay once (previous state) so restore can turn it off again */
    suspend fun apply(shell: ShellRunner, log: UndoLog, spec: Spec): Result<String> = runCatching {
        val before = Overlays.list(shell).associateBy { it.pkg }
        for ((r, px) in listOf(HEIGHT to spec.heightPx, PAD_START to spec.padStartPx, PAD_END to spec.padEndPx)) {
            val id = "$SHELL:${r.overlay}"
            if (before[id]?.enabled != true && log.pending().none { it.kind == Kind.fabricated && it.target == id })
                log.add(Kind.fabricated, id, previous = if (before[id]?.enabled == true) "enabled" else "disabled")
            for (c in commands(r, px)) {
                val res = shell.run(c)
                if (!res.okStrict) error("$c -> ${res.message}")
            }
            // read it back through the resource system, not just the exit code
            val look = shell.run("cmd overlay lookup ${safeToken(r.target)} ${safeToken(r.resource)}").out.trim()
            if (!look.startsWith("$px.0px") && !look.startsWith("${px}px")) error("${r.resource} reads $look, expected ${px}px")
        }
        "status bar ${spec.heightPx}px tall, padding ${spec.padStartPx}/${spec.padEndPx}px"
    }

    /** used by the overlay bar: pads the stock icons aside so lineagifier's own clock/battery have room */
    suspend fun setPaddings(shell: ShellRunner, log: UndoLog, startPx: Int, endPx: Int): Result<Unit> = runCatching {
        val before = Overlays.list(shell).associateBy { it.pkg }
        for ((r, px) in listOf(PAD_START to startPx, PAD_END to endPx)) {
            val id = "$SHELL:${r.overlay}"
            if (log.pending().none { it.kind == Kind.fabricated && it.target == id })
                log.add(Kind.fabricated, id, previous = if (before[id]?.enabled == true) "enabled" else "disabled")
            for (c in commands(r, px)) shell.run(c)
        }
    }

    suspend fun disable(shell: ShellRunner): Result<String> = runCatching {
        for (r in ALL) shell.run("cmd overlay disable --user 0 ${safeToken("$SHELL:${r.overlay}")}")
        "stock status bar size"
    }

    /** true if our height overlay is on */
    suspend fun active(shell: ShellRunner): Boolean =
        Overlays.list(shell).any { it.pkg == "$SHELL:${HEIGHT.overlay}" && it.enabled }
}
