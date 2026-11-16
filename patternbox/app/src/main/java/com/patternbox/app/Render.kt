package com.patternbox.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.patternbox.core.Geo
import com.patternbox.core.Loadout
import com.patternbox.core.Vec
import kotlin.math.cos
import kotlin.math.sin

data class FighterPalette(
    val body: Color,
    val metal: Color,
    val blade: Color,
    val bladeOutline: Color,
)

fun DrawScope.polyline(pts: List<Vec>, color: Color, width: Float) {
    if (pts.size < 2) return
    val path = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
    }
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** draws a fighter in its local space: caller already translated / rotated / mirrored */
fun DrawScope.drawFighter(
    l: Loadout,
    pal: FighterPalette,
    swing: Float = Geo.REST_SWING,
    plates: BooleanArray? = null,
    look: Float = 0f,
    dead: Boolean = false,
    hot: Boolean = false,
    flash: Boolean = false,
) {
    val bodyColor = if (flash) Color.White else pal.body
    val body = Geo.body(l.body)
    polyline(body, bodyColor, 9f)
    for (p in body) drawCircle(bodyColor, 6f, Offset(p.x, p.y))

    // armour plates with a rivet in the middle
    val ap = Geo.armour(l.armour)
    for (i in 1 until ap.size) {
        if (plates != null && !plates[i - 1]) continue
        drawLine(pal.metal.copy(alpha = .8f), Offset(ap[i - 1].x, ap[i - 1].y), Offset(ap[i].x, ap[i].y), 7f, StrokeCap.Round)
        drawCircle(Color(0xFFE9EEF4), 2.2f, Offset((ap[i - 1].x + ap[i].x) / 2, (ap[i - 1].y + ap[i].y) / 2))
    }

    // googly eyes
    val top = Geo.head(l.body)
    for (ex in floatArrayOf(-6f, 6f)) {
        val c = Offset(top.x + ex, top.y - 4)
        drawCircle(Color.White, 6f, c)
        drawCircle(Color(0xFF111111), 6f, c, style = Stroke(1.5f))
        if (dead) {
            drawLine(Color(0xFF111111), c + Offset(-3f, -3f), c + Offset(3f, 3f), 2f)
            drawLine(Color(0xFF111111), c + Offset(3f, -3f), c + Offset(-3f, 3f), 2f)
        } else {
            drawCircle(Color(0xFF111111), 2.6f, c + Offset(2.5f + cos(look), sin(look) * 2.5f))
        }
    }

    // weapon held at the last body dot
    val w = Geo.weaponLocal(l.body, l.weapon, swing)
    polyline(w, pal.bladeOutline, 8f)
    polyline(w, if (hot) Color(0xFFFFF6C2) else pal.blade, 4.5f)
    drawCircle(Color(0xFF6B4A2B), 5f, Offset(body.last().x, body.last().y))
}
