package app.lineagifier.install

import app.lineagifier.catalog.Entry
import app.lineagifier.system.PackageState

/** packages lineagifier will never disable or remove, no matter what the catalog says */
val PROTECTED = setOf(
    "android", "com.android.systemui", "com.android.settings", "com.android.shell",
    "com.android.phone", "com.android.documentsui", "com.android.webview", "com.google.android.webview",
    "com.android.permissioncontroller", "com.google.android.permissioncontroller",
    "com.android.packageinstaller", "com.google.android.packageinstaller",
    "com.android.externalstorage", "com.android.networkstack", "com.android.inputmethod.latin",
    "moe.shizuku.privileged.api", "app.lineagifier",
)

/** what we know about who's the default for a role, and which apps can be home */
data class DefaultsState(
    /** null = `cmd role` isn't available on this image */
    val roleHolders: Map<String, List<String>>?,
    /** every enabled package with a MAIN/HOME activity */
    val homePackages: Set<String>,
    /** package that currently resolves as home, null if it's the chooser or unknown */
    val currentHome: String?,
)

data class Decision(val allowed: Boolean, val reason: String, val replacement: String? = null)

object Safety {
    const val HOME = "android.app.role.HOME"

    fun installedReplacement(entry: Entry, pkgs: PackageState): String? =
        entry.replacementIds().firstOrNull { pkgs.isEnabled(it) }

    /**
     * may we disable/remove [stock] for [entry]? the rules, in order:
     *  - never a protected package, never something that isn't a system package on this image
     *  - the replacement has to be installed and enabled first
     *  - if the slot has a role, the replacement has to hold it already
     *  - anything that can be home is only removable when another enabled launcher is the current home
     */
    fun canRemove(stock: String, entry: Entry, pkgs: PackageState, defaults: DefaultsState): Decision {
        if (stock in PROTECTED) return Decision(false, "$stock is protected, lineagifier never removes it")
        if (stock !in entry.replaces) return Decision(false, "$stock isn't listed as replaceable by ${entry.name}")
        if (!pkgs.isOnImage(stock)) return Decision(false, "$stock isn't a system app on this image")
        if (!pkgs.isEnabled(stock)) return Decision(false, "$stock is already disabled or removed")

        val repl = installedReplacement(entry, pkgs)
            ?: return Decision(false, "install a replacement for ${entry.name} first")

        val role = entry.role
        if (role != null && defaults.roleHolders != null) {
            val holders = defaults.roleHolders[role].orEmpty()
            if (repl !in holders) return Decision(false, "set $repl as the default first (holds $role: ${holders.ifEmpty { listOf("nobody") }.joinToString()})", repl)
        }

        val isHomeApp = stock in defaults.homePackages || role == HOME
        if (isHomeApp) {
            val others = defaults.homePackages - stock
            if (others.isEmpty()) return Decision(false, "no other launcher is installed, removing $stock would leave you without one", repl)
            val cur = defaults.currentHome
            if (cur == null || cur == stock || cur !in others)
                return Decision(false, "the current launcher isn't a replacement yet (${cur ?: "unknown"})", repl)
        }
        return Decision(true, "ok, $repl replaces it", repl)
    }
}
