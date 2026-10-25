package app.stash

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * the launcher icon: light blue with dark bars, or black with white bars while the app is
 * dark (settings → appearance → dark icon in dark mode). android can't swap an icon's colors
 * by itself, so the manifest has two launcher entries (.IconLight / .IconDark) and this turns
 * on the right one. some launchers move the icon back to the app drawer when it switches.
 */
object AppIcon {
    fun apply(context: Context, dark: Boolean) {
        val pm = context.packageManager
        val light = ComponentName(context, "app.stash.IconLight")
        val darkIcon = ComponentName(context, "app.stash.IconDark")
        val (on, off) = if (dark) darkIcon to light else light to darkIcon
        // already right: don't touch it (each switch makes the launcher redraw / reshuffle)
        if (pm.getComponentEnabledSetting(on) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED &&
            pm.getComponentEnabledSetting(off) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        ) return
        pm.setComponentEnabledSetting(on, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(off, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
    }
}
