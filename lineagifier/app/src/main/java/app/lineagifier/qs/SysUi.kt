package app.lineagifier.qs

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue

/**
 * reads colors, icons and strings from the device's own systemui, so the panel matches it 1:1 and follows
 * the accent color, dark/light theme and language. anything missing falls back to lineage-20.0's values.
 */
@SuppressLint("DiscouragedApi")
class SysUi(private val ctx: Context) {
    private val pkg = "com.android.systemui"
    private val sysCtx: Context? = try { ctx.createPackageContext(pkg, Context.CONTEXT_IGNORE_SECURITY) } catch (t: Throwable) { null }
    private val res: Resources? = sysCtx?.resources

    /** lineage-20.0 SystemUI styles.xml: Theme.SystemUI.QuickSettings */
    private val theme: Resources.Theme? = sysCtx?.let { c ->
        val id = c.resources.getIdentifier("Theme.SystemUI.QuickSettings", "style", pkg)
        if (id == 0) null else c.resources.newTheme().also { it.applyStyle(id, true) }
    }

    private fun attrColor(attrId: Int): Int? {
        val t = theme ?: return null
        val tv = TypedValue()
        if (!t.resolveAttribute(attrId, tv, true)) return null
        return if (tv.resourceId != 0) try { res!!.getColor(tv.resourceId, t) } catch (e: Throwable) { tv.data } else tv.data
    }

    private fun sysAttr(name: String) = res?.getIdentifier(name, "attr", pkg) ?: 0

    private fun sys(color: Int, fallback: Long): Int =
        if (Build.VERSION.SDK_INT >= 31) ctx.getColor(color) else fallback.toInt()

    data class Colors(
        val active: Int, val inactive: Int,
        val labelActive: Int, val labelInactive: Int, val labelUnavailable: Int,
        val secondaryActive: Int, val secondaryInactive: Int,
        val background: Int, val onSurface: Int, val onSurfaceVariant: Int,
    )

    /** the same attrs QSTileViewImpl reads (lineage-20.0 QSTileViewImpl.kt lines 95-112) */
    fun colors(night: Boolean): Colors {
        val active = attrColor(android.R.attr.colorAccent) ?: sys(android.R.color.system_accent1_100, 0xFFA8C7FA)
        val inactive = sysAttr("offStateColor").takeIf { it != 0 }?.let { attrColor(it) }
            ?: if (night) sys(android.R.color.system_neutral1_800, 0xFF2D3134) else sys(android.R.color.system_neutral1_50, 0xFFF1F0F4)
        val primary = attrColor(android.R.attr.textColorPrimary) ?: if (night) 0xFFE3E2E6.toInt() else 0xFF1B1B1F.toInt()
        val primaryInv = attrColor(android.R.attr.textColorPrimaryInverse) ?: if (night) 0xFF1B1B1F.toInt() else 0xFFE3E2E6.toInt()
        val secondary = attrColor(android.R.attr.textColorSecondary) ?: if (night) 0xFFC5C6D0.toInt() else 0xFF44474F.toInt()
        val secondaryInv = attrColor(android.R.attr.textColorSecondaryInverse) ?: if (night) 0xFF44474F.toInt() else 0xFFC5C6D0.toInt()
        val tertiary = attrColor(android.R.attr.textColorTertiary) ?: (secondary and 0x00FFFFFF or 0x80000000.toInt())
        // the shade scrim behind quick settings
        val bg = if (night) sys(android.R.color.system_neutral1_900, 0xFF1B1B1F) else sys(android.R.color.system_neutral1_50, 0xFFF1F0F4)
        return Colors(active, inactive, primaryInv, primary, tertiary, secondaryInv, secondary, bg, primary, secondary)
    }

    /** a drawable from systemui, then from the framework (internet's wifi icons live there) */
    fun drawable(name: String?): Drawable? {
        if (name == null) return null
        res?.getIdentifier(name, "drawable", pkg)?.takeIf { it != 0 }?.let { id ->
            return try { res.getDrawable(id, theme) } catch (t: Throwable) { null }
        }
        val fw = Resources.getSystem()
        return fw.getIdentifier(name, "drawable", "android").takeIf { it != 0 }?.let { try { fw.getDrawable(it, null) } catch (t: Throwable) { null } }
    }

    fun string(name: String?): String? {
        if (name == null) return null
        res?.getIdentifier(name, "string", pkg)?.takeIf { it != 0 }?.let { return try { res.getString(it) } catch (t: Throwable) { null } }
        val fw = Resources.getSystem()
        return fw.getIdentifier(name, "string", "android").takeIf { it != 0 }?.let { try { fw.getString(it) } catch (t: Throwable) { null } }
    }

    /** [unavailable, off, on] from systemui's tile_states_* arrays (android 13), english fallback */
    fun states(array: String): List<String> {
        val id = res?.getIdentifier(array, "array", pkg) ?: 0
        val def = res?.getIdentifier("tile_states_default", "array", pkg) ?: 0
        val list = listOf(id, def).firstOrNull { it != 0 }?.let { try { res!!.getStringArray(it).toList() } catch (t: Throwable) { null } }
        return list?.takeIf { it.size >= 3 } ?: listOf("Unavailable", "Off", "On")
    }
}
