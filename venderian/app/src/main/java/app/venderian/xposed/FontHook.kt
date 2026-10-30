package app.venderian.xposed

import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.graphics.fonts.FontStyle
import android.os.Build
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.nio.ByteBuffer
import java.util.zip.ZipFile

/**
 * the lsposed module (experimental, root only). in every app you tick in lsposed, it puts
 * the letters-only venderian font in front of the system fonts as a fallback, so venderian
 * text (private use codepoints) shows as the script, while the app itself still just has
 * plain text. the font has only the 28 letters, so nothing else in the app changes.
 *
 * no reboot: only the ticked apps need a restart. if anything goes wrong it logs to lsposed
 * and leaves the app alone.
 */
class FontHook : IXposedHookZygoteInit, IXposedHookLoadPackage {

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!lpparam.isFirstApplication || lpparam.packageName == OWN_PACKAGE) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        try {
            install()
        } catch (t: Throwable) {
            XposedBridge.log("venderian: couldn't add the font to ${lpparam.packageName}: $t")
        }
    }

    private fun install() {
        val path = modulePath ?: return
        val bytes = ZipFile(path).use { zip -> zip.getInputStream(zip.getEntry(FONT_ASSET)).use { it.readBytes() } }
        // Font.Builder wants a direct buffer
        val buffer = ByteBuffer.allocateDirect(bytes.size).put(bytes).also { it.flip() }
        val family = FontFamily.Builder(Font.Builder(buffer).build()).build()

        fun withVenderian(systemFamily: String, like: Typeface?): Typeface {
            val builder = Typeface.CustomFallbackBuilder(family).setSystemFallback(systemFamily)
            if (like != null) {
                val slant = if (like.isItalic) FontStyle.FONT_SLANT_ITALIC else FontStyle.FONT_SLANT_UPRIGHT
                builder.setStyle(FontStyle(like.weight, slant))
            }
            return builder.build()
        }

        // 1. named families: "sans-serif", "sans-serif-medium"... what apps ask for by name
        step("system font map") {
            @Suppress("UNCHECKED_CAST")
            val map = field("sSystemFontMap").get(null) as MutableMap<String, Typeface>
            val patched = map.mapValues { (name, typeface) -> withVenderian(name, typeface) }
            synchronized(map) { map.putAll(patched) }
        }

        // 2. the default: what all text without its own font uses
        val default = withVenderian("sans-serif", Typeface.DEFAULT)
        step("default typeface") {
            Typeface::class.java.getDeclaredMethod("setDefault", Typeface::class.java)
                .apply { isAccessible = true }
                .invoke(null, default)
        }

        // 3. Typeface.DEFAULT and friends, and the per-style defaults behind defaultFromStyle()
        step("default fields") {
            setStatic("DEFAULT", default)
            setStatic("SANS_SERIF", default)
            setStatic("DEFAULT_BOLD", withVenderian("sans-serif", Typeface.DEFAULT_BOLD))
        }
        step("style defaults") {
            @Suppress("UNCHECKED_CAST")
            val defaults = field("sDefaults").get(null) as Array<Typeface?>
            for (i in defaults.indices) defaults[i] = defaults[i]?.let { withVenderian("sans-serif", it) }
        }
    }

    private inline fun step(name: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            XposedBridge.log("venderian: $name: $t")
        }
    }

    private fun field(name: String): Field = Typeface::class.java.getDeclaredField(name).apply { isAccessible = true }

    /** sets a static final field (art lets reflection change it once the final flag is off) */
    private fun setStatic(name: String, value: Any) {
        val f = field(name)
        if (Modifier.isFinal(f.modifiers)) {
            Field::class.java.getDeclaredField("accessFlags").apply { isAccessible = true }
                .setInt(f, f.modifiers and Modifier.FINAL.inv())
        }
        f.set(null, value)
    }

    companion object {
        private const val OWN_PACKAGE = "app.venderian"
        private const val FONT_ASSET = "assets/venderian-pua.ttf"
        private var modulePath: String? = null
    }
}
