package app.lineagifier.statusbar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** simple line icons for the control center, drawn on a 24x24 grid so there's no icon dependency */
@Composable
fun TileIcon(id: String, color: Color = Color.White) {
    Canvas(Modifier.size(28.dp)) {
        val u = size.width / 24f
        val st = Stroke(2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(vararg pts: Float) = Path().apply { moveTo(pts[0] * u, pts[1] * u); var i = 2; while (i < pts.size) { lineTo(pts[i] * u, pts[i + 1] * u); i += 2 } }
        fun arc(cx: Float, cy: Float, r: Float, start: Float, sweep: Float) =
            drawArc(color, start, sweep, false, Offset((cx - r) * u, (cy - r) * u), Size(2 * r * u, 2 * r * u), style = st)
        when (id) {
            "wifi" -> { arc(12f, 20f, 15f, 225f, 90f); arc(12f, 20f, 10f, 225f, 90f); arc(12f, 20f, 5f, 225f, 90f); drawCircle(color, 1.6f * u, Offset(12 * u, 19.5f * u)) }
            "bluetooth" -> drawPath(p(6f, 7f, 18f, 17f, 12f, 22f, 12f, 2f, 18f, 7f, 6f, 17f), color, style = st)
            "airplane" -> {
                drawPath(p(12f, 2f, 12f, 22f), color, style = st)
                drawPath(p(2f, 14f, 12f, 9f, 22f, 14f), color, style = st)
                drawPath(p(8f, 21f, 12f, 19f, 16f, 21f), color, style = st)
            }
            "dark" -> moon(color, u, st)
            "rotation" -> { arc(12f, 12f, 8f, 300f, 300f); drawPath(p(18f, 3f, 19f, 7.5f, 14.5f, 8f), color, style = st) }
            "dnd" -> { drawCircle(color, 9f * u, Offset(12 * u, 12 * u), style = st); drawPath(p(7.5f, 12f, 16.5f, 12f), color, style = st) }
            "location" -> {
                drawPath(Path().apply {
                    moveTo(12 * u, 22 * u); cubicTo(5 * u, 14 * u, 5 * u, 10 * u, 5 * u, 9 * u)
                    arcTo(Rect(5 * u, 2 * u, 19 * u, 16 * u), 180f, 180f, false); cubicTo(19 * u, 10 * u, 19 * u, 14 * u, 12 * u, 22 * u)
                }, color, style = st)
                drawCircle(color, 2.5f * u, Offset(12 * u, 9 * u), style = st)
            }
            "torch" -> { drawPath(p(7f, 2f, 17f, 2f, 17f, 7f, 14f, 11f, 14f, 22f, 10f, 22f, 10f, 11f, 7f, 7f, 7f, 2f), color, style = st); drawPath(p(12f, 14f, 12f, 16f), color, style = st) }
            "screenshot" -> {
                drawPath(p(3f, 8f, 3f, 3f, 8f, 3f), color, style = st); drawPath(p(16f, 3f, 21f, 3f, 21f, 8f), color, style = st)
                drawPath(p(21f, 16f, 21f, 21f, 16f, 21f), color, style = st); drawPath(p(8f, 21f, 3f, 21f, 3f, 16f), color, style = st)
            }
        }
    }
}

private fun DrawScope.moon(color: Color, u: Float, st: Stroke) {
    drawPath(Path().apply {
        moveTo(20 * u, 14.5f * u)
        cubicTo(16 * u, 17 * u, 8 * u, 14 * u, 9.5f * u, 4 * u)
        cubicTo(4 * u, 6 * u, 3 * u, 14 * u, 8 * u, 18.5f * u)
        cubicTo(12 * u, 22 * u, 18 * u, 20 * u, 20 * u, 14.5f * u)
    }, color, style = st)
}
