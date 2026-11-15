package com.lot.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

val Ink = Color(0xFF1A1226)
val Gold = Color(0xFFFFC93C)
val Pink = Color(0xFFFF5C8A)
val Mint = Color(0xFF4BE3AC)
val Sky = Color(0xFF6EC6FF)
val Grape = Color(0xFF8E6CFF)

// ---------- text ----------

private val fillPaint = Paint().apply {
    isAntiAlias = true
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
}
private val strokePaint = Paint().apply {
    isAntiAlias = true
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    style = Paint.Style.STROKE
    strokeJoin = Paint.Join.ROUND
}

/** draws text centered on (x, y) */
fun DrawScope.label(text: String, x: Float, y: Float, size: Float, color: Color, outline: Color? = null) {
    fillPaint.textSize = size
    fillPaint.color = color.toArgb()
    val fm = fillPaint.fontMetrics
    val base = y - (fm.ascent + fm.descent) / 2f
    if (outline != null) {
        strokePaint.textSize = size
        strokePaint.strokeWidth = size * 0.18f
        strokePaint.color = outline.toArgb()
        drawContext.canvas.nativeCanvas.drawText(text, x, base, strokePaint)
    }
    drawContext.canvas.nativeCanvas.drawText(text, x, base, fillPaint)
}

fun starPath(c: Offset, outer: Float, inner: Float, points: Int = 5, rot: Float = -PI.toFloat() / 2): Path {
    val p = Path()
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outer else inner
        val a = rot + i * PI.toFloat() / points
        val x = c.x + r * cos(a)
        val y = c.y + r * sin(a)
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    return p
}

fun DrawScope.outlined(path: Path, fill: Color, w: Float) {
    drawPath(path, fill)
    drawPath(path, Ink, style = Stroke(w, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
}

fun DrawScope.circleO(c: Offset, r: Float, fill: Color, w: Float) {
    drawCircle(fill, r, c)
    drawCircle(Ink, r, c, style = Stroke(w))
}

// ---------- characters ----------

data class Dude(
    val skin: Color,
    val shirt: Color,
    val hair: Color,
    val hat: Int,
    val hatColor: Color,
    val name: String,
    val glasses: Boolean,
    val wide: Float,
)

private val skins = listOf(0xFFFFD7B5, 0xFFF1C27D, 0xFFE0AC69, 0xFFC68642, 0xFF8D5524, 0xFFB5E48C, 0xFFA0C4FF).map { Color(it) }
private val shirts = listOf(0xFFFF5C8A, 0xFF4BE3AC, 0xFF6EC6FF, 0xFFFFC93C, 0xFF8E6CFF, 0xFFFF8C42, 0xFFE9E9E9).map { Color(it) }
private val hairs = listOf(0xFF2B1B10, 0xFF6B3E26, 0xFFE8C468, 0xFFB23A48, 0xFF222222, 0xFF9E9E9E).map { Color(it) }
private val names = listOf(
    "gary", "linda", "big tony", "grandma rose", "kevin", "chad", "doris", "mr. beans", "sasha",
    "boris", "tiffany", "uncle bob", "pavel", "jessica", "lil steve", "karen", "nikolai", "greg",
)

fun randomDude(rng: Random = Random): Dude = Dude(
    skin = skins.random(rng),
    shirt = shirts.random(rng),
    hair = hairs.random(rng),
    hat = listOf(0, 0, 1, 2, 3, 4, 6).random(rng),
    hatColor = shirts.random(rng),
    name = names.random(rng),
    glasses = rng.nextFloat() < 0.3f,
    wide = 0.85f + rng.nextFloat() * 0.35f,
)

val HostDude = Dude(Color(0xFFFFD7B5), Color(0xFF2A2140), Color(0xFF222222), 6, Color(0xFF222222), "the host", false, 1f)
fun guardDude(rng: Random) = Dude(skins.random(rng), Color(0xFF2E5AAC), Color(0xFF222222), 5, Color(0xFF1E3A70), "guard", false, 1.1f)

fun headCenter(cx: Float, baseY: Float, h: Float) = Offset(cx, baseY - 0.44f * h - 0.17f * h)
fun handPoint(cx: Float, baseY: Float, h: Float) = Offset(cx, baseY - 0.44f * h + 0.13f * h)

/**
 * a cartoon person standing on baseY, h tall.
 * look: -1..1 horizontal pupil direction. wobble: little rotation.
 */
fun DrawScope.drawDude(
    d: Dude, cx: Float, baseY: Float, h: Float, mood: Mood,
    talk: Boolean = false, wobble: Float = 0f, look: Float = 0f, holdTicket: Boolean = false,
    t: Float = 0f,
) {
    val w = h * 0.022f
    rotate(wobble * 5f, Offset(cx, baseY)) {
        // body
        val bodyW = h * 0.52f * d.wide
        val bodyTop = baseY - 0.44f * h
        drawRoundRect(d.shirt, Offset(cx - bodyW / 2, bodyTop), Size(bodyW, 0.5f * h), CornerRadius(h * 0.18f))
        drawRoundRect(Ink, Offset(cx - bodyW / 2, bodyTop), Size(bodyW, 0.5f * h), CornerRadius(h * 0.18f), style = Stroke(w))
        if (d.hat == 6 && d.name == "the host") {
            // bow tie
            val bt = Offset(cx, bodyTop + h * 0.04f)
            val p = Path().apply {
                moveTo(bt.x, bt.y); lineTo(bt.x - h * 0.09f, bt.y - h * 0.045f); lineTo(bt.x - h * 0.09f, bt.y + h * 0.045f); close()
                moveTo(bt.x, bt.y); lineTo(bt.x + h * 0.09f, bt.y - h * 0.045f); lineTo(bt.x + h * 0.09f, bt.y + h * 0.045f); close()
            }
            outlined(p, Pink, w * 0.8f)
            circleO(bt, h * 0.022f, Pink, w * 0.8f)
        }
        if (d.hat == 5) {
            // guard badge on chest
            outlined(starPath(Offset(cx - bodyW * 0.22f, bodyTop + h * 0.12f), h * 0.04f, h * 0.02f), Gold, w * 0.6f)
        }
        // head
        val hc = headCenter(cx, baseY, h)
        val hr = h * 0.2f
        val face = if (mood == Mood.ANGRY) lerp(d.skin, Color(0xFFFF3B3B), 0.45f) else d.skin
        // hair back
        if (d.hat == 0 || d.hat == 6) {
            drawCircle(d.hair, hr * 1.02f, hc + Offset(0f, -hr * 0.12f))
        }
        circleO(hc, hr, face, w)
        if (d.hat == 0) {
            for (i in -1..1) drawCircle(d.hair, hr * 0.28f, hc + Offset(i * hr * 0.35f, -hr * 0.92f))
        }
        if (d.hat == 6) {
            // slick hair dome
            drawArc(d.hair, 180f, 180f, true, hc - Offset(hr * 1.02f, hr * 1.05f), Size(hr * 2.04f, hr * 1.3f))
        }
        // eyes
        val ex = hr * 0.38f
        val ey = hc.y - hr * 0.08f
        for (sx in listOf(-1f, 1f)) {
            val e = Offset(cx + sx * ex, ey)
            if (mood == Mood.HAPPY && !talk && sin(t * 1.3f + cx) > 0.97f) {
                drawLine(Ink, e - Offset(hr * 0.18f, 0f), e + Offset(hr * 0.18f, 0f), w, StrokeCap.Round)
            } else {
                circleO(e, hr * 0.24f, Color.White, w * 0.7f)
                drawCircle(Ink, hr * 0.11f, e + Offset(look * hr * 0.1f, hr * 0.03f))
                drawCircle(Color.White, hr * 0.035f, e + Offset(look * hr * 0.1f - hr * 0.04f, -hr * 0.02f))
            }
            if (d.glasses) drawCircle(Ink, hr * 0.3f, e, style = Stroke(w * 0.8f))
            // brows
            val by = ey - hr * 0.36f
            val (outY, inY) = when (mood) {
                Mood.HAPPY -> -hr * 0.06f to -hr * 0.1f
                Mood.MEH -> 0f to 0f
                Mood.GRUMPY -> -hr * 0.02f to hr * 0.08f
                Mood.ANGRY -> -hr * 0.1f to hr * 0.16f
            }
            drawLine(
                Ink, Offset(cx + sx * (ex + hr * 0.22f), by + outY), Offset(cx + sx * (ex - hr * 0.2f), by + inY),
                w * 1.2f, StrokeCap.Round,
            )
        }
        if (d.glasses) drawLine(Ink, Offset(cx - ex + hr * 0.3f, ey), Offset(cx + ex - hr * 0.3f, ey), w * 0.8f)
        // cheeks
        if (mood == Mood.HAPPY) {
            drawCircle(Pink.copy(alpha = 0.45f), hr * 0.14f, Offset(cx - hr * 0.62f, hc.y + hr * 0.25f))
            drawCircle(Pink.copy(alpha = 0.45f), hr * 0.14f, Offset(cx + hr * 0.62f, hc.y + hr * 0.25f))
        }
        // mouth
        val my = hc.y + hr * 0.45f
        val mw = hr * 0.42f
        if (talk) {
            val open = 0.5f + 0.5f * kotlin.math.abs(sin(t * 18f))
            drawOval(Ink, Offset(cx - mw * 0.6f, my - hr * 0.12f * open), Size(mw * 1.2f, hr * 0.32f * open + hr * 0.05f))
        } else {
            val p = Path()
            when (mood) {
                Mood.HAPPY -> { p.moveTo(cx - mw, my - hr * 0.05f); p.quadraticBezierTo(cx, my + hr * 0.35f, cx + mw, my - hr * 0.05f) }
                Mood.MEH -> { p.moveTo(cx - mw * 0.7f, my + hr * 0.05f); p.lineTo(cx + mw * 0.7f, my + hr * 0.02f) }
                Mood.GRUMPY -> { p.moveTo(cx - mw * 0.8f, my + hr * 0.15f); p.quadraticBezierTo(cx, my - hr * 0.12f, cx + mw * 0.8f, my + hr * 0.15f) }
                Mood.ANGRY -> { p.moveTo(cx - mw, my + hr * 0.2f); p.quadraticBezierTo(cx, my - hr * 0.25f, cx + mw, my + hr * 0.2f) }
            }
            drawPath(p, Ink, style = Stroke(w * 1.1f, cap = StrokeCap.Round))
        }
        // hats
        when (d.hat) {
            1 -> { // cap
                drawArc(d.hatColor, 180f, 180f, true, hc - Offset(hr * 1.02f, hr * 1.1f), Size(hr * 2.04f, hr * 1.4f))
                drawArc(Ink, 180f, 180f, true, hc - Offset(hr * 1.02f, hr * 1.1f), Size(hr * 2.04f, hr * 1.4f), style = Stroke(w))
                drawRoundRect(d.hatColor, Offset(hc.x, hc.y - hr * 0.48f), Size(hr * 1.3f, hr * 0.2f), CornerRadius(hr * 0.1f))
                drawRoundRect(Ink, Offset(hc.x, hc.y - hr * 0.48f), Size(hr * 1.3f, hr * 0.2f), CornerRadius(hr * 0.1f), style = Stroke(w))
            }
            2 -> { // top hat
                val top = Rect(hc.x - hr * 0.6f, hc.y - hr * 2.0f, hc.x + hr * 0.6f, hc.y - hr * 0.75f)
                drawRect(Ink, top.topLeft, top.size)
                drawRect(d.hatColor, Offset(top.left, top.bottom - hr * 0.3f), Size(top.width, hr * 0.18f))
                drawRoundRect(Ink, Offset(hc.x - hr * 1.0f, hc.y - hr * 0.85f), Size(hr * 2f, hr * 0.2f), CornerRadius(hr * 0.1f))
            }
            3 -> { // beanie
                drawArc(d.hatColor, 180f, 180f, true, hc - Offset(hr * 1.02f, hr * 1.25f), Size(hr * 2.04f, hr * 1.6f))
                drawArc(Ink, 180f, 180f, true, hc - Offset(hr * 1.02f, hr * 1.25f), Size(hr * 2.04f, hr * 1.6f), style = Stroke(w))
                circleO(hc - Offset(0f, hr * 1.3f), hr * 0.22f, Color.White, w * 0.8f)
            }
            4 -> { // party cone
                val p = Path().apply {
                    moveTo(hc.x - hr * 0.55f, hc.y - hr * 0.78f); lineTo(hc.x + hr * 0.1f, hc.y - hr * 2.0f)
                    lineTo(hc.x + hr * 0.6f, hc.y - hr * 0.72f); close()
                }
                outlined(p, d.hatColor, w)
                circleO(Offset(hc.x + hr * 0.1f, hc.y - hr * 2.0f), hr * 0.14f, Gold, w * 0.7f)
            }
            5 -> { // police cap
                val p = Path().apply {
                    moveTo(hc.x - hr * 1.1f, hc.y - hr * 1.25f); lineTo(hc.x + hr * 1.1f, hc.y - hr * 1.25f)
                    lineTo(hc.x + hr * 0.85f, hc.y - hr * 0.6f); lineTo(hc.x - hr * 0.85f, hc.y - hr * 0.6f); close()
                }
                outlined(p, d.hatColor, w)
                drawRoundRect(Ink, Offset(hc.x - hr * 0.9f, hc.y - hr * 0.68f), Size(hr * 1.8f, hr * 0.2f), CornerRadius(hr * 0.1f))
                outlined(starPath(Offset(hc.x, hc.y - hr * 0.95f), hr * 0.18f, hr * 0.08f), Gold, w * 0.5f)
                // mustache
                drawLine(Ink, Offset(hc.x - hr * 0.3f, hc.y + hr * 0.32f), Offset(hc.x + hr * 0.3f, hc.y + hr * 0.32f), w * 2.2f, StrokeCap.Round)
            }
        }
        if (mood == Mood.ANGRY) {
            // steam
            for (sx in listOf(-1f, 1f)) {
                val puff = (t * 2f + sx) % 1f
                drawCircle(Color.White.copy(alpha = 1f - puff), hr * (0.15f + puff * 0.2f), Offset(hc.x + sx * hr * 1.2f, hc.y - hr * (0.6f + puff)))
            }
        }
        if (holdTicket) {
            val hp = handPoint(cx, baseY, h)
            circleO(hp + Offset(-h * 0.11f, 0f), h * 0.045f, d.skin, w * 0.7f)
            circleO(hp + Offset(h * 0.11f, 0f), h * 0.045f, d.skin, w * 0.7f)
        }
    }
}

// ---------- props ----------

fun DrawScope.drawCoin(c: Offset, r: Float) {
    circleO(c, r, Gold, r * 0.14f)
    drawCircle(Color(0xFFE0A800), r * 0.68f, c, style = Stroke(r * 0.12f))
    label("c", c.x, c.y - r * 0.05f, r * 1.1f, Color(0xFFB07D00))
}

val ballColors = listOf(Pink, Mint, Sky, Gold, Grape, Color(0xFFFF8C42))

fun DrawScope.drawBall(c: Offset, r: Float, n: Int, highlight: Boolean = false) {
    val col = ballColors[n % ballColors.size]
    if (highlight) drawCircle(Gold.copy(alpha = 0.5f), r * 1.35f, c)
    circleO(c, r, col, r * 0.1f)
    drawCircle(Color.White, r * 0.58f, c)
    drawCircle(Color.White.copy(alpha = 0.6f), r * 0.18f, c + Offset(-r * 0.5f, -r * 0.5f))
    label(n.toString(), c.x, c.y, r * 0.72f, Ink)
}

fun DrawScope.drawSymbol(s: Sym, c: Offset, size: Float) {
    val w = size * 0.05f
    val r = size / 2
    when (s) {
        Sym.SOCK -> {
            val p = Path().apply {
                moveTo(c.x - r * 0.3f, c.y - r * 0.8f); lineTo(c.x + r * 0.25f, c.y - r * 0.8f)
                lineTo(c.x + r * 0.25f, c.y + r * 0.2f); quadraticBezierTo(c.x + r * 0.9f, c.y + r * 0.25f, c.x + r * 0.8f, c.y + r * 0.65f)
                quadraticBezierTo(c.x + r * 0.6f, c.y + r * 0.9f, c.x - r * 0.1f, c.y + r * 0.7f)
                quadraticBezierTo(c.x - r * 0.35f, c.y + r * 0.6f, c.x - r * 0.3f, c.y + r * 0.2f); close()
            }
            outlined(p, Color(0xFFEDEDED), w)
            drawLine(Pink, Offset(c.x - r * 0.3f, c.y - r * 0.55f), Offset(c.x + r * 0.25f, c.y - r * 0.55f), w * 1.6f)
            drawLine(Sky, Offset(c.x - r * 0.3f, c.y - r * 0.35f), Offset(c.x + r * 0.25f, c.y - r * 0.35f), w * 1.6f)
            // stink lines
            for (i in 0..1) drawLine(Mint, Offset(c.x - r * 0.6f - i * r * 0.15f, c.y - r * 0.3f + i * r * 0.3f), Offset(c.x - r * 0.75f - i * r * 0.15f, c.y - r * 0.5f + i * r * 0.3f), w, StrokeCap.Round)
        }
        Sym.CHERRY -> {
            val stem = Path().apply {
                moveTo(c.x - r * 0.35f, c.y + r * 0.2f); quadraticBezierTo(c.x - r * 0.1f, c.y - r * 0.5f, c.x + r * 0.25f, c.y - r * 0.75f)
                moveTo(c.x + r * 0.4f, c.y + r * 0.25f); quadraticBezierTo(c.x + r * 0.3f, c.y - r * 0.3f, c.x + r * 0.25f, c.y - r * 0.75f)
            }
            drawPath(stem, Color(0xFF2E7D32), style = Stroke(w * 1.3f, cap = StrokeCap.Round))
            circleO(Offset(c.x - r * 0.35f, c.y + r * 0.4f), r * 0.36f, Color(0xFFE53935), w)
            circleO(Offset(c.x + r * 0.4f, c.y + r * 0.45f), r * 0.36f, Color(0xFFE53935), w)
            drawCircle(Color.White.copy(alpha = 0.7f), r * 0.08f, Offset(c.x - r * 0.48f, c.y + r * 0.28f))
        }
        Sym.BELL -> {
            val p = Path().apply {
                moveTo(c.x - r * 0.7f, c.y + r * 0.45f)
                quadraticBezierTo(c.x - r * 0.5f, c.y + r * 0.3f, c.x - r * 0.5f, c.y - r * 0.1f)
                quadraticBezierTo(c.x - r * 0.5f, c.y - r * 0.75f, c.x, c.y - r * 0.75f)
                quadraticBezierTo(c.x + r * 0.5f, c.y - r * 0.75f, c.x + r * 0.5f, c.y - r * 0.1f)
                quadraticBezierTo(c.x + r * 0.5f, c.y + r * 0.3f, c.x + r * 0.7f, c.y + r * 0.45f); close()
            }
            circleO(Offset(c.x, c.y + r * 0.6f), r * 0.16f, Color(0xFFB07D00), w)
            outlined(p, Gold, w)
            circleO(Offset(c.x, c.y - r * 0.82f), r * 0.1f, Gold, w * 0.8f)
        }
        Sym.STAR -> outlined(starPath(c, r * 0.85f, r * 0.4f), Gold, w)
        Sym.CLOVER -> {
            drawLine(Color(0xFF1B5E20), c, c + Offset(r * 0.3f, r * 0.85f), w * 1.6f, StrokeCap.Round)
            for (i in 0 until 4) {
                val a = i * PI.toFloat() / 2 + PI.toFloat() / 4
                circleO(c + Offset(cos(a) * r * 0.36f, sin(a) * r * 0.36f), r * 0.32f, Color(0xFF43A047), w)
            }
            drawCircle(Color(0xFF43A047), r * 0.2f, c)
        }
        Sym.SEVEN -> label("7", c.x, c.y, size * 0.95f, Color(0xFFE53935), Ink)
        Sym.SKULL -> {
            drawRoundRect(Ink, Offset(c.x - r * 0.42f - w, c.y + r * 0.05f - w), Size(r * 0.84f + w * 2, r * 0.6f + w * 2), CornerRadius(r * 0.15f))
            drawRoundRect(Color(0xFFF2F2F2), Offset(c.x - r * 0.42f, c.y + r * 0.05f), Size(r * 0.84f, r * 0.6f), CornerRadius(r * 0.15f))
            circleO(Offset(c.x, c.y - r * 0.15f), r * 0.68f, Color(0xFFF2F2F2), w)
            drawRect(Color(0xFFF2F2F2), Offset(c.x - r * 0.38f, c.y + r * 0.1f), Size(r * 0.76f, r * 0.3f))
            drawCircle(Ink, r * 0.2f, Offset(c.x - r * 0.27f, c.y - r * 0.12f))
            drawCircle(Ink, r * 0.2f, Offset(c.x + r * 0.27f, c.y - r * 0.12f))
            drawPath(Path().apply { moveTo(c.x, c.y + r * 0.08f); lineTo(c.x - r * 0.1f, c.y + r * 0.25f); lineTo(c.x + r * 0.1f, c.y + r * 0.25f); close() }, Ink)
            for (i in -1..1) drawLine(Ink, Offset(c.x + i * r * 0.18f, c.y + r * 0.42f), Offset(c.x + i * r * 0.18f, c.y + r * 0.62f), w * 0.8f)
        }
        Sym.DIAMOND -> {
            val p = Path().apply {
                moveTo(c.x - r * 0.75f, c.y - r * 0.25f); lineTo(c.x - r * 0.4f, c.y - r * 0.65f); lineTo(c.x + r * 0.4f, c.y - r * 0.65f)
                lineTo(c.x + r * 0.75f, c.y - r * 0.25f); lineTo(c.x, c.y + r * 0.75f); close()
            }
            outlined(p, Color(0xFF7FE7FF), w)
            drawLine(Ink, Offset(c.x - r * 0.75f, c.y - r * 0.25f), Offset(c.x + r * 0.75f, c.y - r * 0.25f), w * 0.7f)
            drawLine(Ink, Offset(c.x - r * 0.2f, c.y - r * 0.25f), Offset(c.x, c.y + r * 0.75f), w * 0.7f)
            drawLine(Ink, Offset(c.x + r * 0.2f, c.y - r * 0.25f), Offset(c.x, c.y + r * 0.75f), w * 0.7f)
        }
    }
}

fun DrawScope.drawMedal(c: Offset, r: Float, color: Color, locked: Boolean) {
    val w = r * 0.1f
    val ribbon = if (locked) Color(0xFF4A4363) else Pink
    val ribbon2 = if (locked) Color(0xFF3B3552) else Sky
    outlined(Path().apply {
        moveTo(c.x - r * 0.6f, c.y - r * 1.5f); lineTo(c.x - r * 0.05f, c.y - r * 1.5f); lineTo(c.x + r * 0.2f, c.y - r * 0.4f); lineTo(c.x - r * 0.3f, c.y - r * 0.4f); close()
    }, ribbon, w)
    outlined(Path().apply {
        moveTo(c.x + r * 0.6f, c.y - r * 1.5f); lineTo(c.x + r * 0.05f, c.y - r * 1.5f); lineTo(c.x - r * 0.2f, c.y - r * 0.4f); lineTo(c.x + r * 0.3f, c.y - r * 0.4f); close()
    }, ribbon2, w)
    val body = if (locked) Color(0xFF5B5478) else color
    circleO(c, r, body, w)
    drawCircle(Ink.copy(alpha = 0.25f), r * 0.75f, c, style = Stroke(w * 0.8f))
    if (locked) label("?", c.x, c.y, r * 1.1f, Color(0xFF8C84AD))
    else outlined(starPath(c, r * 0.55f, r * 0.25f), Color.White, w * 0.6f)
}

/** stand with a striped awning, drawn with its counter top at counterY */
fun DrawScope.drawStand(left: Float, right: Float, top: Float, counterY: Float, bottom: Float, sign: String = "lot") {
    val w = (right - left) * 0.012f
    // back wall
    drawRect(Color(0xFF3A2E5C), Offset(left, top), Size(right - left, bottom - top))
    // awning stripes
    val awH = (counterY - top) * 0.32f
    val stripes = 8
    val sw = (right - left) / stripes
    for (i in 0 until stripes) {
        val col = if (i % 2 == 0) Pink else Color.White
        val p = Path().apply {
            moveTo(left + i * sw, top); lineTo(left + (i + 1) * sw, top)
            lineTo(left + (i + 1) * sw, top + awH); quadraticBezierTo(left + (i + 0.5f) * sw, top + awH * 1.45f, left + i * sw, top + awH); close()
        }
        drawPath(p, col)
        drawPath(p, Ink, style = Stroke(w))
    }
    // sign
    val sx = (left + right) / 2
    val sy = top - awH * 0.55f
    val sr = Rect(sx - (right - left) * 0.18f, sy - awH * 0.45f, sx + (right - left) * 0.18f, sy + awH * 0.45f)
    drawRoundRect(Gold, sr.topLeft, sr.size, CornerRadius(awH * 0.2f))
    drawRoundRect(Ink, sr.topLeft, sr.size, CornerRadius(awH * 0.2f), style = Stroke(w))
    label(sign, sx, sy, awH * 0.7f, Ink)
    // counter
    drawRect(Color(0xFF8E6CFF), Offset(left, counterY), Size(right - left, bottom - counterY))
    drawRect(Ink, Offset(left, counterY), Size(right - left, bottom - counterY), style = Stroke(w))
    drawRect(Color(0xFFB9A3FF), Offset(left - w * 2, counterY - w * 3), Size(right - left + w * 4, w * 4))
    drawRect(Ink, Offset(left - w * 2, counterY - w * 3), Size(right - left + w * 4, w * 4), style = Stroke(w))
    // posters on the counter front
    val ph = (bottom - counterY) * 0.5f
    for (i in 0..2) {
        val cx = left + (right - left) * (0.2f + i * 0.3f)
        val cy = counterY + (bottom - counterY) * 0.5f
        withTransform({ rotate(-6f + i * 6f, Offset(cx, cy)) }) {
            drawRoundRect(Color(0xFFFFF3D6), Offset(cx - ph * 0.6f, cy - ph / 2), Size(ph * 1.2f, ph), CornerRadius(ph * 0.08f))
            drawRoundRect(Ink, Offset(cx - ph * 0.6f, cy - ph / 2), Size(ph * 1.2f, ph), CornerRadius(ph * 0.08f), style = Stroke(w * 0.7f))
            label(listOf("win!", "$$$", "lol")[i], cx, cy, ph * 0.38f, Pink)
        }
    }
}

fun DrawScope.drawBush(c: Offset, r: Float, eyes: Boolean, look: Float, t: Float) {
    val w = r * 0.06f
    val bumps = listOf(Offset(-0.6f, 0.1f), Offset(0.6f, 0.1f), Offset(-0.3f, -0.3f), Offset(0.3f, -0.3f), Offset(0f, -0.5f), Offset(0f, 0.15f))
    bumps.forEach { drawCircle(Ink, r * 0.5f + w, c + it * r) }
    bumps.forEach { drawCircle(Color(0xFF2E9E5B), r * 0.5f, c + it * r) }
    drawCircle(Color(0xFF41C27A), r * 0.2f, c + Offset(-0.35f, -0.45f) * r)
    if (eyes) {
        val blink = sin(t * 0.9f) > 0.96f
        for (sx in listOf(-1f, 1f)) {
            val e = c + Offset(sx * r * 0.2f, -r * 0.1f)
            if (blink) drawLine(Ink, e - Offset(r * 0.1f, 0f), e + Offset(r * 0.1f, 0f), w, StrokeCap.Round)
            else {
                circleO(e, r * 0.13f, Color.White, w * 0.6f)
                drawCircle(Ink, r * 0.06f, e + Offset(look * r * 0.05f, 0f))
            }
        }
    }
}

// ---------- claim / bingo / dumpster props ----------

val YouDude = Dude(Color(0xFFFFD7B5), Color(0xFF4BE3AC), Color(0xFF6B3E26), 1, Color(0xFFFF5C8A), "you", false, 1f)
val ThiefDude = Dude(Color(0xFFE0AC69), Color(0xFF222233), Color(0xFF222233), 3, Color(0xFF222233), "thief", false, 0.9f)
val CashierDude = Dude(Color(0xFFF1C27D), Color(0xFFE9E9E9), Color(0xFF9E9E9E), 1, Color(0xFF2E9E5B), "cashier", true, 1.15f)
fun grannyDude(i: Int) = Dude(
    listOf(Color(0xFFFFD7B5), Color(0xFFC68642), Color(0xFFF1C27D))[i % 3],
    listOf(Color(0xFFB39DDB), Color(0xFF80CBC4), Color(0xFFF48FB1))[i % 3],
    Color(0xFFE0E0E0), 0, Color.White, listOf("granny rose", "nana pearl", "mrs. fitz")[i % 3], true, 1.05f,
)

/** black bandit mask over a dude's eyes */
fun DrawScope.drawMask(cx: Float, baseY: Float, h: Float) {
    val hc = headCenter(cx, baseY, h)
    val hr = h * 0.2f
    drawRoundRect(Ink, Offset(hc.x - hr * 0.95f, hc.y - hr * 0.32f), Size(hr * 1.9f, hr * 0.42f), CornerRadius(hr * 0.2f))
    for (sx in listOf(-1f, 1f)) drawCircle(Color.White, hr * 0.12f, Offset(hc.x + sx * hr * 0.38f, hc.y - hr * 0.1f))
}

fun DrawScope.drawPigeon(c: Offset, s: Float, flap: Float, dir: Float) {
    val w = s * 0.06f
    drawOval(Ink, Offset(c.x - s * 0.5f - w, c.y - s * 0.3f - w), Size(s + w * 2, s * 0.6f + w * 2))
    drawOval(Color(0xFF9EA7B8), Offset(c.x - s * 0.5f, c.y - s * 0.3f), Size(s, s * 0.6f))
    val head = Offset(c.x + dir * s * 0.45f, c.y - s * 0.3f)
    circleO(head, s * 0.22f, Color(0xFF7D8699), w)
    drawCircle(Color(0xFF4BE3AC).copy(alpha = 0.7f), s * 0.1f, head + Offset(-dir * s * 0.05f, s * 0.15f))
    drawCircle(Ink, s * 0.05f, head + Offset(dir * s * 0.06f, -s * 0.03f))
    drawPath(Path().apply {
        moveTo(head.x + dir * s * 0.18f, head.y); lineTo(head.x + dir * s * 0.36f, head.y + s * 0.06f); lineTo(head.x + dir * s * 0.18f, head.y + s * 0.1f); close()
    }, Gold)
    val wingY = c.y - s * 0.1f - flap * s * 0.45f
    outlined(Path().apply {
        moveTo(c.x - dir * s * 0.2f, c.y - s * 0.05f); lineTo(c.x - dir * s * 0.1f, wingY); lineTo(c.x + dir * s * 0.25f, c.y - s * 0.1f); close()
    }, Color(0xFF7D8699), w)
}

fun DrawScope.drawCone(x: Float, groundY: Float, s: Float) {
    val p = Path().apply {
        moveTo(x - s * 0.5f, groundY); lineTo(x - s * 0.15f, groundY - s); lineTo(x + s * 0.15f, groundY - s); lineTo(x + s * 0.5f, groundY); close()
    }
    outlined(p, Color(0xFFFF8C42), s * 0.06f)
    drawRect(Color.White, Offset(x - s * 0.3f, groundY - s * 0.55f), Size(s * 0.6f, s * 0.14f))
}

fun DrawScope.drawTrashBag(c: Offset, s: Float, color: Color, shake: Float) {
    rotate(shake * 8f, Offset(c.x, c.y + s * 0.5f)) {
        val p = Path().apply {
            moveTo(c.x - s * 0.15f, c.y - s * 0.5f)
            quadraticBezierTo(c.x - s * 0.7f, c.y - s * 0.2f, c.x - s * 0.55f, c.y + s * 0.5f)
            lineTo(c.x + s * 0.55f, c.y + s * 0.5f)
            quadraticBezierTo(c.x + s * 0.7f, c.y - s * 0.2f, c.x + s * 0.15f, c.y - s * 0.5f)
            close()
        }
        outlined(p, color, s * 0.05f)
        // knot
        circleO(Offset(c.x, c.y - s * 0.55f), s * 0.1f, color, s * 0.04f)
        drawLine(Color.White.copy(alpha = 0.25f), Offset(c.x - s * 0.3f, c.y - s * 0.1f), Offset(c.x - s * 0.35f, c.y + s * 0.3f), s * 0.05f, StrokeCap.Round)
    }
}

fun DrawScope.drawRaccoon(c: Offset, s: Float, t: Float, angry: Boolean) {
    val w = s * 0.05f
    // tail
    for (i in 0 until 4) circleO(Offset(c.x + s * (0.55f + i * 0.12f), c.y + s * 0.2f - i * s * 0.08f + sin(t * 8f + i) * s * 0.04f), s * 0.13f, if (i % 2 == 0) Color(0xFF6E6A75) else Color(0xFF2A2830), w)
    drawOval(Ink, Offset(c.x - s * 0.5f - w, c.y - s * 0.1f - w), Size(s + w * 2, s * 0.55f + w * 2))
    drawOval(Color(0xFF8A8592), Offset(c.x - s * 0.5f, c.y - s * 0.1f), Size(s, s * 0.55f))
    val hc = Offset(c.x - s * 0.35f, c.y - s * 0.15f)
    circleO(hc + Offset(-s * 0.2f, -s * 0.28f), s * 0.1f, Color(0xFF8A8592), w)
    circleO(hc + Offset(s * 0.2f, -s * 0.28f), s * 0.1f, Color(0xFF8A8592), w)
    circleO(hc, s * 0.32f, Color(0xFFB5B0BC), w)
    drawRoundRect(Color(0xFF2A2830), Offset(hc.x - s * 0.3f, hc.y - s * 0.12f), Size(s * 0.6f, s * 0.18f), CornerRadius(s * 0.09f))
    for (sx in listOf(-1f, 1f)) {
        drawCircle(if (angry) Color(0xFFFF3B3B) else Color.White, s * 0.06f, hc + Offset(sx * s * 0.13f, -s * 0.03f))
        drawCircle(Ink, s * 0.03f, hc + Offset(sx * s * 0.13f, -s * 0.03f))
    }
    drawCircle(Ink, s * 0.05f, hc + Offset(0f, s * 0.12f))
}
