package app.lineagifier.statusbar

/** small pure helpers for the overlay, kept apart so they can be unit tested */
object BarLogic {
    /**
     * parses `dumpsys window displays | grep -A1 mLastStatusBarAppearanceRegions` (aosp 13). systemui draws dark
     * icons when the first region carries LIGHT_STATUS_BARS. null if the dump has no region (unknown).
     */
    fun lightStatusBar(dump: String): Boolean? {
        val region = dump.lines().firstOrNull { it.contains("AppearanceRegion{") } ?: return null
        return region.contains("LIGHT_STATUS_BARS")
    }
}
