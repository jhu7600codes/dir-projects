package com.patternbox.app

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.patternbox.core.ArmourMat
import com.patternbox.core.Geo
import com.patternbox.core.Loadout
import com.patternbox.core.Vec
import com.patternbox.core.WeaponMat
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class FighterPalette(
    val body: Color,
    val metal: Color,
    val blade: Color,
    val bladeOutline: Color,
    val hat: Color,
)

fun paletteFor(scheme: ColorScheme, team: Int) = FighterPalette(
    body = teamColor(scheme, team),
    metal = scheme.outline,
    blade = scheme.inverseSurface,
    bladeOutline = scheme.surfaceContainerLowest,
    hat = scheme.onSurface,
)

fun weaponColor(mat: WeaponMat, pal: FighterPalette): Color = when (mat) {
    WeaponMat.STEEL -> pal.blade
    WeaponMat.FIRE -> Color(0xFFFF7A3D)
    WeaponMat.ICE -> Color(0xFF9ADCFF)
    WeaponMat.SHOCK -> Color(0xFFFFE14D)
    WeaponMat.BOUNCY -> Color(0xFFFF7AC8)
    WeaponMat.GLASS -> Color(0xFFBFF4FF).copy(alpha = .75f)
    WeaponMat.POISON -> Color(0xFF8BD450)
    WeaponMat.VAMPIRE -> Color(0xFFB0203A)
}

fun armourColor(mat: ArmourMat, pal: FighterPalette): Color = when (mat) {
    ArmourMat.STEEL -> pal.metal
    ArmourMat.GOLD -> Color(0xFFE8B931)
    ArmourMat.PAPER -> Color(0xFFE9DFC8)
    ArmourMat.RUBBER -> Color(0xFF3A3A44)
    ArmourMat.SPIKY -> pal.metal
    ArmourMat.ICE -> Color(0xFFA8E2FF)
    ArmourMat.MIRROR -> Color(0xFFE8F4FF)
    ArmourMat.BALLOON -> Color(0xFFFF8FB8)
}

fun DrawScope.polyline(pts: List<Vec>, color: Color, width: Float) {
    if (pts.size < 2) return
    val path = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
    }
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

class FighterLook(
    val swing: Float = Geo.REST_SWING,
    val weapon: List<Vec>? = null,      // null = use rest pose, emptyList = no weapon in hand
    val plates: BooleanArray? = null,
    val look: Float = 0f,
    val dead: Boolean = false,
    val hot: Boolean = false,
    val flash: Boolean = false,
    val frozen: Boolean = false,
    val burning: Boolean = false,
    val rage: Boolean = false,
    val bubble: Boolean = false,
    val time: Float = 0f,
    val poisoned: Boolean = false,
)

/** draws a fighter in its local space: caller already translated / rotated / mirrored */
fun DrawScope.drawFighter(l: Loadout, pal: FighterPalette, look: FighterLook = FighterLook()) {
    var bodyColor = when {
        look.flash -> Color.White
        look.frozen -> Color(0xFFB8E6FF)
        look.rage -> Color(0xFFFF4D3D)
        else -> pal.body
    }
    if (look.burning && ((look.time * 10).toInt() % 2 == 0)) bodyColor = Color(0xFFFF9A3D)
    if (look.poisoned && ((look.time * 6).toInt() % 2 == 0)) bodyColor = Color(0xFF8BD450)
    val body = Geo.body(l.body)
    polyline(body, bodyColor, 9f)
    for (p in body) drawCircle(bodyColor, 6f, Offset(p.x, p.y))

    // armour plates with a rivet in the middle (spikes for spiky armour)
    val ap = Geo.armour(l.armour)
    val ac = armourColor(l.armourMat, pal)
    for (i in 1 until ap.size) {
        if (look.plates != null && !look.plates[i - 1]) continue
        val a = Offset(ap[i - 1].x, ap[i - 1].y)
        val b = Offset(ap[i].x, ap[i].y)
        drawLine(ac.copy(alpha = .85f), a, b, 7f, StrokeCap.Round)
        val mid = (a + b) / 2f
        if (l.armourMat == ArmourMat.SPIKY) {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val len = hypot(dx, dy).coerceAtLeast(1f)
            val n = Offset(-dy / len, dx / len) * 9f
            for (k in listOf(.25f, .5f, .75f)) {
                val p = a + (b - a) * k
                drawLine(ac, p, p + n, 2.5f, StrokeCap.Round)
                drawLine(ac, p, p - n, 2.5f, StrokeCap.Round)
            }
        } else drawCircle(Color(0xFFE9EEF4), 2.2f, mid)
    }

    // hat
    val hat = Geo.hat(l.body, l.hat)
    if (hat.size == 1) drawCircle(pal.hat, 4f, Offset(hat[0].x, hat[0].y))
    polyline(hat, pal.hat, 4f)

    // googly eyes
    val top = Geo.head(l.body)
    for (ex in floatArrayOf(-6f, 6f)) {
        val c = Offset(top.x + ex, top.y - 4)
        drawCircle(Color.White, 6f, c)
        drawCircle(Color(0xFF111111), 6f, c, style = Stroke(1.5f))
        if (look.dead) {
            drawLine(Color(0xFF111111), c + Offset(-3f, -3f), c + Offset(3f, 3f), 2f)
            drawLine(Color(0xFF111111), c + Offset(3f, -3f), c + Offset(-3f, 3f), 2f)
        } else {
            drawCircle(Color(0xFF111111), 2.6f, c + Offset(2.5f + cos(look.look), sin(look.look) * 2.5f))
        }
    }
    if (look.rage) {
        // angry brows
        drawLine(Color(0xFF111111), Offset(top.x - 11, top.y - 13), Offset(top.x - 2, top.y - 9), 2.5f)
        drawLine(Color(0xFF111111), Offset(top.x + 11, top.y - 13), Offset(top.x + 2, top.y - 9), 2.5f)
    }

    // weapon held at the last body dot
    val w = look.weapon ?: Geo.weaponLocal(l.body, l.weapon, look.swing)
    if (w.isNotEmpty()) {
        polyline(w, pal.bladeOutline, 8f)
        polyline(w, if (look.hot) Color(0xFFFFF6C2) else weaponColor(l.weaponMat, pal), 4.5f)
    }
    drawCircle(Color(0xFF6B4A2B), 5f, Offset(body.last().x, body.last().y))

    if (look.bubble) {
        drawCircle(Color(0xFF9ADCFF).copy(alpha = .18f), Geo.R + 14, Offset.Zero)
        drawCircle(Color(0xFF9ADCFF).copy(alpha = .8f), Geo.R + 14, Offset.Zero, style = Stroke(2.5f))
    }
}
