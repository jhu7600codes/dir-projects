package app.lineagifier.statusbar

import android.content.Context
import android.content.SharedPreferences
import app.lineagifier.qs.Tiles

/** settings for the quick settings panel. stored in prefs "overlay". */
data class BarConfig(
    val ccEnabled: Boolean = false,
    /** tile specs in panel order */
    val ccTiles: List<String> = Tiles.DEFAULT,
    val ccHandleHeightDp: Int = 140,
) {
    companion object {
        const val PREFS = "overlay"
        fun prefs(ctx: Context): SharedPreferences = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun load(ctx: Context): BarConfig {
            val p = prefs(ctx); val d = BarConfig()
            return BarConfig(
                ccEnabled = p.getBoolean("ccEnabled", d.ccEnabled),
                ccTiles = p.getString("ccTileOrder", null)?.split(',')?.filter { Tiles.bySpec(it) != null } ?: d.ccTiles,
                ccHandleHeightDp = p.getInt("ccHandleHeightDp", d.ccHandleHeightDp),
            )
        }

        fun save(ctx: Context, c: BarConfig) {
            prefs(ctx).edit()
                .putBoolean("ccEnabled", c.ccEnabled).putString("ccTileOrder", c.ccTiles.joinToString(","))
                .putInt("ccHandleHeightDp", c.ccHandleHeightDp)
                .apply()
        }
    }

    val needsService get() = ccEnabled
}
