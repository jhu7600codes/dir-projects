package app.lineagifier.statusbar

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import app.lineagifier.shell.ShellRunner

/**
 * control center tiles. state is read and toggled through root shell commands that exist on aosp 13;
 * a tile only shows up if its command/service exists on this image (twoyi may have no bluetooth or radio).
 */
data class Tile(val id: String, val label: String, val service: String?)

object ControlCenter {
    val TILES = listOf(
        Tile("wifi", "wi-fi", "wifi"),
        Tile("bluetooth", "bluetooth", "bluetooth_manager"),
        Tile("airplane", "airplane", "connectivity"),
        Tile("dark", "dark mode", "uimode"),
        Tile("rotation", "auto-rotate", null),
        Tile("dnd", "do not disturb", "notification"),
        Tile("location", "location", "location"),
        Tile("torch", "flashlight", null),
        Tile("screenshot", "screenshot", "input"),
    )

    /** tiles whose backing service exists. torch also needs a camera with a flash. */
    suspend fun available(ctx: Context, shell: ShellRunner): Set<String> {
        val services = shell.run("cmd -l").out.lines().map { it.trim() }.toSet()
        return TILES.filter { t ->
            when (t.id) {
                "torch" -> torchCamera(ctx) != null
                "rotation" -> true
                else -> t.service in services
            }
        }.map { it.id }.toSet()
    }

    fun torchCamera(ctx: Context): String? = try {
        val cm = ctx.getSystemService(CameraManager::class.java)
        cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
    } catch (t: Throwable) { null }

    private suspend fun setting(shell: ShellRunner, ns: String, key: String) = shell.run("settings get $ns $key").out.trim()

    /** one round of state reads for every tile */
    suspend fun states(shell: ShellRunner, ids: Set<String>, torchOn: Boolean): Map<String, Boolean> = ids.associateWith { id ->
        when (id) {
            "wifi" -> setting(shell, "global", "wifi_on") != "0"
            "bluetooth" -> setting(shell, "global", "bluetooth_on") == "1"
            "airplane" -> setting(shell, "global", "airplane_mode_on") == "1"
            "dark" -> shell.run("cmd uimode night").out.contains("yes")
            "rotation" -> setting(shell, "system", "accelerometer_rotation") == "1"
            "dnd" -> setting(shell, "global", "zen_mode").let { it != "0" && it != "null" }
            "location" -> setting(shell, "secure", "location_mode").let { it != "0" && it != "null" }
            "torch" -> torchOn
            else -> false
        }
    }

    /** the command that flips a tile from [on] */
    fun toggleCommand(id: String, on: Boolean): String? = when (id) {
        "wifi" -> "cmd wifi set-wifi-enabled ${if (on) "disabled" else "enabled"}"
        "bluetooth" -> "svc bluetooth ${if (on) "disable" else "enable"}"
        "airplane" -> "cmd connectivity airplane-mode ${if (on) "disable" else "enable"}"
        "dark" -> "cmd uimode night ${if (on) "no" else "yes"}"
        "rotation" -> "settings put system accelerometer_rotation ${if (on) 0 else 1}"
        "dnd" -> "cmd notification set_dnd ${if (on) "off" else "on"}"
        "location" -> "cmd location set-location-enabled ${!on}"
        // screenshot after the panel closes (the service delays it)
        "screenshot" -> "input keyevent 120"
        else -> null
    }

    suspend fun brightness(shell: ShellRunner): Int = setting(shell, "system", "screen_brightness").toIntOrNull() ?: 128

    suspend fun setBrightness(shell: ShellRunner, v: Int) {
        shell.run("settings put system screen_brightness_mode 0")
        shell.run("settings put system screen_brightness ${v.coerceIn(1, 255)}")
    }

    fun volume(ctx: Context): Float {
        val am = ctx.getSystemService(AudioManager::class.java)
        return am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }

    fun setVolume(ctx: Context, f: Float) {
        val am = ctx.getSystemService(AudioManager::class.java)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, (f * am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toInt(), 0)
    }
}
