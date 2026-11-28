package app.lineagifier.qs

import android.provider.AlarmClock
import android.provider.Settings
import app.lineagifier.R

/**
 * the tiles, by their lineage-20.0 systemui spec names (config.xml quick_settings_tiles_stock).
 *
 * icons and labels for aosp tiles are loaded at runtime from the device's own systemui (same drawables,
 * same animations, same language). lineage-only tiles use lineage's vector icons, copied into res/drawable
 * as los_* with their apache-2.0 headers, and lineage's english labels from cm_strings.xml.
 */
enum class Backend { WIFI, BT, TORCH, DND, ALARM, AIRPLANE, ROTATION, SAVER, LOCATION, DARK, NIGHT, CAFFEINE, HEADS_UP, SYNC, AMBIENT, AOD, VPN }

data class TileDef(
    val spec: String,
    val backend: Backend,
    /** systemui string for the label, and the english label if systemui doesn't have it */
    val labelRes: String?,
    val fallbackLabel: String,
    /** systemui drawables (animated on 13) for on/off, or a bundled icon */
    val iconOn: String? = null,
    val iconOff: String? = null,
    val bundled: Int? = null,
    /** systemui string-array: [unavailable, off, on] */
    val statesArray: String = "tile_states_default",
    /** a chevron on the tile, like aosp's tiles that open a detail panel */
    val chevron: Boolean = false,
    /** what a long press opens */
    val settings: String? = null,
    val lineage: Boolean = false,
)

object Tiles {
    val ALL = listOf(
        TileDef("internet", Backend.WIFI, "quick_settings_internet_label", "Internet", "ic_wifi_signal_4", "ic_qs_no_internet_available",
            statesArray = "tile_states_internet", chevron = true, settings = Settings.ACTION_WIFI_SETTINGS),
        TileDef("bt", Backend.BT, "quick_settings_bluetooth_label", "Bluetooth", "qs_bluetooth_icon_on", "qs_bluetooth_icon_off",
            statesArray = "tile_states_bt", chevron = true, settings = Settings.ACTION_BLUETOOTH_SETTINGS),
        TileDef("flashlight", Backend.TORCH, "quick_settings_flashlight_label", "Flashlight", "qs_flashlight_icon_on", "qs_flashlight_icon_off",
            statesArray = "tile_states_flashlight"),
        TileDef("dnd", Backend.DND, "quick_settings_dnd_label", "Do Not Disturb", "qs_dnd_icon_on", "qs_dnd_icon_off",
            statesArray = "tile_states_dnd", settings = "android.settings.ZEN_MODE_SETTINGS"),
        TileDef("alarm", Backend.ALARM, "status_bar_alarm", "Alarm", "ic_alarm", "ic_alarm",
            statesArray = "tile_states_alarm", settings = AlarmClock.ACTION_SHOW_ALARMS),
        TileDef("airplane", Backend.AIRPLANE, "airplane_mode", "Airplane mode", "qs_airplane_icon_on", "qs_airplane_icon_off",
            statesArray = "tile_states_airplane", settings = Settings.ACTION_AIRPLANE_MODE_SETTINGS),
        TileDef("rotation", Backend.ROTATION, "quick_settings_rotation_unlocked_label", "Auto-rotate", "qs_auto_rotate_icon_on", "qs_auto_rotate_icon_off",
            statesArray = "tile_states_rotation", settings = "android.settings.AUTO_ROTATE_SETTINGS"),
        TileDef("battery", Backend.SAVER, "battery_detail_switch_title", "Battery Saver", "qs_battery_saver_icon_on", "qs_battery_saver_icon_off",
            statesArray = "tile_states_battery", settings = Settings.ACTION_BATTERY_SAVER_SETTINGS),
        TileDef("location", Backend.LOCATION, "quick_settings_location_label", "Location", "qs_location_icon_on", "qs_location_icon_off",
            statesArray = "tile_states_location", settings = Settings.ACTION_LOCATION_SOURCE_SETTINGS),
        TileDef("dark", Backend.DARK, "quick_settings_ui_mode_night_label", "Dark theme", "qs_light_dark_theme_icon_on", "qs_light_dark_theme_icon_off",
            statesArray = "tile_states_dark", settings = "android.settings.DARK_THEME_SETTINGS"),
        TileDef("night", Backend.NIGHT, "quick_settings_night_display_label", "Night Light", "qs_nightlight_icon_on", "qs_nightlight_icon_off",
            statesArray = "tile_states_night", settings = Settings.ACTION_NIGHT_DISPLAY_SETTINGS),
        // lineage-only tiles (lineage-20.0 packages/SystemUI/src/.../qs/tiles)
        TileDef("caffeine", Backend.CAFFEINE, "quick_settings_caffeine_label", "Caffeine", bundled = R.drawable.los_ic_qs_caffeine, lineage = true),
        TileDef("heads_up", Backend.HEADS_UP, "quick_settings_heads_up_label", "Heads up", bundled = R.drawable.los_ic_qs_heads_up,
            settings = Settings.ACTION_NOTIFICATION_ASSISTANT_SETTINGS, lineage = true),
        TileDef("sync", Backend.SYNC, "quick_settings_sync_label", "Sync", bundled = R.drawable.los_ic_qs_sync, settings = Settings.ACTION_SYNC_SETTINGS, lineage = true),
        TileDef("ambient_display", Backend.AMBIENT, "quick_settings_ambient_display_label", "Ambient display", bundled = R.drawable.los_ic_qs_ambient_display,
            settings = Settings.ACTION_DISPLAY_SETTINGS, lineage = true),
        TileDef("aod", Backend.AOD, "quick_settings_aod_label", "AOD", bundled = R.drawable.los_ic_qs_aod, settings = Settings.ACTION_DISPLAY_SETTINGS, lineage = true),
        TileDef("vpn", Backend.VPN, "quick_settings_vpn_label", "VPN", bundled = R.drawable.los_ic_qs_vpn, settings = Settings.ACTION_VPN_SETTINGS, lineage = true),
    )

    fun bySpec(s: String) = ALL.firstOrNull { it.spec == s }

    /** lineage 20's quick_settings_tiles_default order, minus the tiles twoyi can't have (controls, wallet, cast, ...) */
    val DEFAULT = listOf("internet", "bt", "flashlight", "dnd", "alarm", "airplane", "rotation", "battery", "caffeine", "dark", "location", "heads_up")

    /** lineage-20.0 CaffeineTile.DURATIONS: 5, 10, 30 minutes, then -1 (infinity) */
    val CAFFEINE_DURATIONS = listOf(5 * 60, 10 * 60, 30 * 60, -1)

    /**
     * CaffeineTile.handleClick: a tap within 5s of the last one while caffeine is on steps to the next duration
     * (past infinity it turns off); any other tap toggles, starting at 5 minutes. returns the new duration index,
     * or -1 for off.
     */
    fun caffeineClick(on: Boolean, index: Int, lastClickMs: Long, nowMs: Long): Int =
        if (on && lastClickMs != -1L && nowMs - lastClickMs < 5000) { if (index + 1 >= CAFFEINE_DURATIONS.size) -1 else index + 1 }
        else if (on) -1 else 0

    /** CaffeineTile.formatValueWithRemainingTime */
    fun caffeineLabel(secondsLeft: Int): String =
        if (secondsLeft == -1) "\u221E" else "%02d:%02d".format(secondsLeft / 60 % 60, secondsLeft % 60)

    /** settingslib BrightnessUtils (android13-release): hybrid log gamma between slider and setting */
    const val GAMMA_SPACE_MAX = 65535
    private const val R_ = 0.5f
    private const val A = 0.17883277f
    private const val B = 0.28466892f
    private const val C = 0.55991073f

    fun gammaToLinear(v: Int, min: Int, max: Int): Int {
        val n = v.toFloat() / GAMMA_SPACE_MAX
        val ret = if (n <= R_) (n / R_) * (n / R_) else kotlin.math.exp((n - C) / A) + B
        return Math.round(min + (max - min) * (ret / 12f))
    }

    fun linearToGamma(v: Int, min: Int, max: Int): Int {
        val n = (v - min).toFloat() / (max - min) * 12f
        val ret = if (n <= 1f) kotlin.math.sqrt(n) * R_ else A * kotlin.math.ln(n - B) + C
        return Math.round(GAMMA_SPACE_MAX * ret)
    }
}
