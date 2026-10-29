package app.venderian.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import app.venderian.data.Letter
import app.venderian.ui.theme.VenderianFont
import kotlin.math.hypot

/** venderian text in the venderian font */
@Composable
fun VText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 32.sp,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    Text(
        text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        style = TextStyle(fontFamily = VenderianFont, fontSize = size, lineHeight = size * 1.6f),
    )
}

// grid area a glyph is drawn in: x -1..6, y -2..9 (see docs/alphabet.json)
private const val GRID_LEFT = -1f
private const val GRID_TOP = 9f
private const val GRID_W = 7f
private const val GRID_H = 11f

/**
 * draws a letter stroke by stroke, the way it's written: headline first, then the strokes
 * in alphabet.json order. a faint copy of the whole letter sits underneath as a guide.
 * change [replay] to draw it again.
 */
@Composable
fun StrokeOrderGlyph(
    letter: Letter,
    strokeWidth: Float,
    modifier: Modifier = Modifier,
    replay: Int = 0,
    msPerStroke: Int = 550,
    color: Color = MaterialTheme.colorScheme.primary,
    guide: Color = LocalContentColor.current.copy(alpha = 0.12f),
) {
    val strokes = remember(letter) { strokesOf(letter) }
    val progress = remember(letter) { Animatable(0f) }
    LaunchedEffect(letter, replay) {
        progress.snapTo(0f)
        for (i in strokes.indices) {
            progress.animateTo(i + 1f, tween(msPerStroke, easing = LinearEasing))
        }
    }
    Canvas(modifier.aspectRatio(GRID_W / GRID_H)) {
        val unit = size.height / GRID_H
        strokes.forEach { drawPolyline(it, 1f, unit, strokeWidth, guide) }
        strokes.forEachIndexed { i, s ->
            val f = (progress.value - i).coerceIn(0f, 1f)
            if (f > 0f) drawPolyline(s, f, unit, strokeWidth, color)
            // a dot where the stroke being drawn starts
            if (f > 0f && f < 1f) {
                drawCircle(color, radius = unit * strokeWidth * 0.9f, center = point(s.first(), unit))
            }
        }
    }
}

/** the letter's strokes as drawn by hand, headline first */
fun strokesOf(letter: Letter): List<List<Offset>> {
    val headline = if (letter.headline) listOf(listOf(Offset(-0.5f, 6f), Offset(4.5f, 6f))) else emptyList()
    return headline + letter.strokes.map { s -> s.map { Offset(it[0], it[1]) } }
}

private fun DrawScope.point(p: Offset, unit: Float) = Offset((p.x - GRID_LEFT) * unit, (GRID_TOP - p.y) * unit)

private fun DrawScope.drawPolyline(points: List<Offset>, fraction: Float, unit: Float, width: Float, color: Color) {
    val lengths = points.zipWithNext { a, b -> hypot(b.x - a.x, b.y - a.y) }
    var remaining = lengths.sum() * fraction
    val path = Path()
    val start = point(points.first(), unit)
    path.moveTo(start.x, start.y)
    for ((i, len) in lengths.withIndex()) {
        if (remaining <= 0f) break
        val a = points[i]
        val b = points[i + 1]
        val t = (remaining / len).coerceAtMost(1f)
        val p = point(Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t), unit)
        path.lineTo(p.x, p.y)
        remaining -= len
    }
    val closed = fraction >= 1f && points.first() == points.last()
    if (closed) path.close()
    drawPath(path, color, style = Stroke(width * unit, cap = if (closed) StrokeCap.Butt else StrokeCap.Square, join = StrokeJoin.Miter))
}
