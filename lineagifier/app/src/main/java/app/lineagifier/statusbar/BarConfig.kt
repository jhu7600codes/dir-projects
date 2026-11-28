package app.lineagifier.statusbar

import android.content.Context
import android.content.SharedPreferences

/** everything the config app can switch for the status bar and control center. stored in prefs "overlay". */
data class BarConfig(
    val barEnabled: Boolean = false,
    val clockPos: String = "left",          // left | right   (no center: it'd sit under the notch)
    val clockSeconds: Boolean = false,
    val clock24h: Boolean = true,
    val showDate: Boolean = false,
    val batteryStyle: String = "both",      // icon | percent | both | hidden
    val traffic: String = "off",            // off | down | both
    val trafficSide: String = "right",      // left | right
    val textColor: String = "auto",         // auto (follows dark mode) | white | black
    val ccEnabled: Boolean = false,
    val ccTiles: Set<String> = setOf("wifi", "bluetooth", "airplane", "dark", "rotation", "dnd", "location", "torch", "screenshot"),
    val ccHandleHeightDp: Int = 140,
    val sizingEnabled: Boolean = false,
    val safeTopPt: Float = Sizing.SAFE_TOP_PT,
    val padPt: Float = Sizing.CORNER_PAD_PT,
) {
    companion object {
        const val PREFS = "overlay"
        fun prefs(ctx: Context): SharedPreferences = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun load(ctx: Context): BarConfig {
            val p = prefs(ctx); val d = BarConfig()
            return BarConfig(
                barEnabled = p.getBoolean("barEnabled", d.barEnabled),
                clockPos = p.getString("clockPos", d.clockPos)!!,
                clockSeconds = p.getBoolean("clockSeconds", d.clockSeconds),
                clock24h = p.getBoolean("clock24h", d.clock24h),
                showDate = p.getBoolean("showDate", d.showDate),
                batteryStyle = p.getString("batteryStyle", d.batteryStyle)!!,
                traffic = p.getString("traffic", d.traffic)!!,
                trafficSide = p.getString("trafficSide", d.trafficSide)!!,
                textColor = p.getString("textColor", d.textColor)!!,
                ccEnabled = p.getBoolean("ccEnabled", d.ccEnabled),
                ccTiles = p.getStringSet("ccTiles", d.ccTiles)!!.toSet(),
                ccHandleHeightDp = p.getInt("ccHandleHeightDp", d.ccHandleHeightDp),
                sizingEnabled = p.getBoolean("sizingEnabled", d.sizingEnabled),
                safeTopPt = p.getFloat("safeTopPt", d.safeTopPt),
                padPt = p.getFloat("padPt", d.padPt),
            )
        }

        fun save(ctx: Context, c: BarConfig) {
            prefs(ctx).edit()
                .putBoolean("barEnabled", c.barEnabled).putString("clockPos", c.clockPos)
                .putBoolean("clockSeconds", c.clockSeconds).putBoolean("clock24h", c.clock24h)
                .putBoolean("showDate", c.showDate).putString("batteryStyle", c.batteryStyle)
                .putString("traffic", c.traffic).putString("trafficSide", c.trafficSide)
                .putString("textColor", c.textColor).putBoolean("ccEnabled", c.ccEnabled)
                .putStringSet("ccTiles", c.ccTiles).putInt("ccHandleHeightDp", c.ccHandleHeightDp)
                .putBoolean("sizingEnabled", c.sizingEnabled).putFloat("safeTopPt", c.safeTopPt).putFloat("padPt", c.padPt)
                .apply()
        }
    }

    val needsService get() = barEnabled || ccEnabled
}
