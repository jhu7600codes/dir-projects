package com.androdrop.xposed

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

private const val TAG = "androdrop-xposed"
private const val ANDRODROP_PACKAGE = "com.androdrop.app"
private const val ANDRODROP_SHARE_ACTIVITY = "com.androdrop.app.ui.ShareReceiverActivity"

/**
 * EXPERIMENTAL — pins androdrop to the top of the system share sheet. Read
 * xposed/README.md before enabling this; it has not been verified against a
 * real device (no rooted hardware/emulator was available while building
 * this). Written for LineageOS + microG, i.e. no Google Play Services —
 * so this hooks plain AOSP's ResolverActivity/ChooserActivity machinery,
 * not Google's Nearby Share.
 *
 * Rather than hooking ResolverActivity/ChooserActivity/ChooserListAdapter
 * directly — private classes AOSP substantially rewrites nearly every
 * release, and LineageOS may patch further — this hooks the one call that
 * has stayed stable across all of them: the client-facing
 * `PackageManager.queryIntentActivities()` that ResolverActivity/
 * ChooserActivity uses to build its candidate list in the first place. If
 * this method's signature has moved on your build, `handleLoadPackage`
 * catches and logs rather than crashing system_server — check the LSPosed
 * Manager log for "androdrop-xposed" and see the README for how to find
 * the right signature on your exact build.
 */
class ShareSheetHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Module scope is "android" (System Framework, see AndroidManifest's
        // xposed_scope) — that fires handleLoadPackage once, with this
        // packageName, when the framework classes load into the Zygote.
        if (lpparam.packageName != "android") return

        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "queryIntentActivities",
                Intent::class.java,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        runCatching { injectIfShareIntent(param) }
                            .onFailure { XposedBridge.log("$TAG: inject failed: $it") }
                    }
                },
            )
            XposedBridge.log("$TAG: hooked ApplicationPackageManager.queryIntentActivities")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: failed to hook queryIntentActivities on this build: $t")
        }
    }

    private fun injectIfShareIntent(param: XC_MethodHook.MethodHookParam) {
        val intent = param.args.getOrNull(0) as? Intent ?: return
        if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_SEND_MULTIPLE) return

        @Suppress("UNCHECKED_CAST")
        val results = param.result as? MutableList<ResolveInfo> ?: return
        if (results.any { it.activityInfo?.packageName == ANDRODROP_PACKAGE }) return

        val pm = param.thisObject as? PackageManager ?: return
        val probe = Intent(intent.action).apply {
            type = intent.type
            component = ComponentName(ANDRODROP_PACKAGE, ANDRODROP_SHARE_ACTIVITY)
        }
        // Ask Android to resolve androdrop's own activity for real, rather
        // than hand-building a ResolveInfo — this way the icon, label, and
        // permission checks all come from data the OS already trusts.
        val androdropInfo = runCatching { pm.queryIntentActivities(probe, 0) }
            .getOrNull()
            ?.firstOrNull()
            ?: return

        results.add(0, androdropInfo)
        XposedBridge.log("$TAG: pinned androdrop into the share sheet for ${intent.action}")
    }
}
