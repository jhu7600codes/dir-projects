package app.lineagifier.qs

import android.app.AlarmManager
import android.content.ContentResolver
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.text.format.DateFormat
import app.lineagifier.shell.ShellRunner
import java.util.Date

/** like aosp's Tile.STATE_*: 0 unavailable, 1 inactive, 2 active */
data class TileState(val state: Int, val secondary: String? = null) {
    companion object { const val UNAVAILABLE = 0; const val OFF = 1; const val ON = 2 }
}

/**
 * reads and flips tile state as root, with the commands aosp 13 really has (checked on the aosp 13 emulator:
 * cmd wifi set-wifi-enabled, svc bluetooth, cmd connectivity airplane-mode, cmd notification set_dnd,
 * cmd location set-location-enabled, cmd uimode night, cmd power set-mode).
 */
class Backends(private val ctx: Context, private val shell: ShellRunner) {
    private var services: Set<String> = emptySet()
    private var nightAvailable = false
    private var torchId: String? = null

    suspend fun probe() {
        services = shell.run("cmd -l").out.lines().map { it.trim() }.toSet()
        nightAvailable = !shell.run("dumpsys color_display | head -3").out.contains("Not available")
        torchId = try {
            val cm = ctx.getSystemService(CameraManager::class.java)
            cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
        } catch (t: Throwable) { null }
    }

    private suspend fun get(ns: String, key: String) = shell.run("settings get $ns $key").out.trim()
    private fun on(b: Boolean) = if (b) TileState.ON else TileState.OFF

    suspend fun read(b: Backend, torchOn: Boolean, caffeineLeft: Int?): TileState = when (b) {
        Backend.WIFI -> if ("wifi" !in services) TileState(TileState.UNAVAILABLE) else TileState(on(get("global", "wifi_on") != "0"))
        Backend.BT -> if ("bluetooth_manager" !in services) TileState(TileState.UNAVAILABLE) else TileState(on(get("global", "bluetooth_on") == "1"))
        Backend.TORCH -> if (torchId == null) TileState(TileState.UNAVAILABLE) else TileState(on(torchOn))
        Backend.DND -> TileState(on(get("global", "zen_mode").let { it != "0" && it != "null" }))
        Backend.ALARM -> {
            val next = ctx.getSystemService(AlarmManager::class.java).nextAlarmClock
            if (next == null) TileState(TileState.OFF)
            else TileState(TileState.ON, DateFormat.format(if (DateFormat.is24HourFormat(ctx)) "EEE HH:mm" else "EEE h:mm a", Date(next.triggerTime)).toString())
        }
        Backend.AIRPLANE -> TileState(on(get("global", "airplane_mode_on") == "1"))
        Backend.ROTATION -> TileState(on(get("system", "accelerometer_rotation") == "1"))
        Backend.SAVER -> {
            // aosp makes battery saver unavailable while plugged in
            val plugged = shell.run("dumpsys battery | grep -E 'powered: true'").out.isNotBlank()
            if (plugged) TileState(TileState.UNAVAILABLE) else TileState(on(get("global", "low_power") == "1"))
        }
        Backend.LOCATION -> TileState(on(get("secure", "location_mode").let { it != "0" && it != "null" }))
        Backend.DARK -> TileState(on(shell.run("cmd uimode night").out.contains("yes")))
        Backend.NIGHT -> if (!nightAvailable) TileState(TileState.UNAVAILABLE) else TileState(on(get("secure", "night_display_activated") == "1"))
        Backend.CAFFEINE -> TileState(on(caffeineLeft != null), caffeineLeft?.let { Tiles.caffeineLabel(it) })
        Backend.HEADS_UP -> TileState(on(get("global", "heads_up_notifications_enabled") != "0"))
        Backend.SYNC -> TileState(on(ContentResolver.getMasterSyncAutomatically()))
        Backend.AMBIENT -> TileState(on(get("secure", "doze_enabled").let { it == "1" || it == "null" }))
        Backend.AOD -> TileState(on(get("secure", "doze_always_on") == "1"))
        Backend.VPN -> TileState(TileState.OFF)
    }

    /** the command that flips [b] away from [isOn]; null = handled in-app or opens settings instead */
    fun toggle(b: Backend, isOn: Boolean): String? = toggleCommand(b, isOn)

    companion object {
    fun toggleCommand(b: Backend, isOn: Boolean): String? = when (b) {
        Backend.WIFI -> "cmd wifi set-wifi-enabled ${if (isOn) "disabled" else "enabled"}"
        Backend.BT -> "svc bluetooth ${if (isOn) "disable" else "enable"}"
        Backend.DND -> "cmd notification set_dnd ${if (isOn) "off" else "on"}"
        Backend.AIRPLANE -> "cmd connectivity airplane-mode ${if (isOn) "disable" else "enable"}"
        Backend.ROTATION -> "settings put system accelerometer_rotation ${if (isOn) 0 else 1}"
        Backend.SAVER -> "cmd power set-mode ${if (isOn) 0 else 1}"
        Backend.LOCATION -> "cmd location set-location-enabled ${!isOn}"
        Backend.DARK -> "cmd uimode night ${if (isOn) "no" else "yes"}"
        Backend.NIGHT -> "settings put secure night_display_activated ${if (isOn) 0 else 1}"
        Backend.HEADS_UP -> "settings put global heads_up_notifications_enabled ${if (isOn) 0 else 1}"
        Backend.AMBIENT -> "settings put secure doze_enabled ${if (isOn) 0 else 1}"
        Backend.AOD -> "settings put secure doze_always_on ${if (isOn) 0 else 1}"
        Backend.TORCH, Backend.ALARM, Backend.CAFFEINE, Backend.SYNC, Backend.VPN -> null
    }
    }

    fun setTorch(on: Boolean) { torchId?.let { ctx.getSystemService(CameraManager::class.java).setTorchMode(it, on) } }
    fun setSync(on: Boolean) = ContentResolver.setMasterSyncAutomatically(on)

    suspend fun brightness(): Pair<Int, Boolean> =
        (get("system", "screen_brightness").toIntOrNull() ?: 128) to (get("system", "screen_brightness_mode") == "1")

    suspend fun setBrightness(v: Int) {
        shell.run("settings put system screen_brightness_mode 0")
        shell.run("settings put system screen_brightness ${v.coerceIn(1, 255)}")
    }

    suspend fun setAutoBrightness(on: Boolean) = shell.run("settings put system screen_brightness_mode ${if (on) 1 else 0}")
}
