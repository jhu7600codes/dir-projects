package com.patternbox.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patternbox.core.Arena
import com.patternbox.core.Debris
import com.patternbox.core.FloatText
import com.patternbox.core.GameMap
import com.patternbox.core.Particle
import com.patternbox.core.Prop
import com.patternbox.core.Tool
import com.patternbox.core.WeaponType
import kotlin.math.ceil
import kotlin.math.max
import kotlin.random.Random

/** world is at least this many units wide so fighters aren't tiny on a portrait phone */
private const val MIN_WORLD_W = 560f

@Composable
fun ArenaView(
    arena: Arena,
    tool: Tool?,
    frame: () -> Long,
    shakeOn: Boolean,
    hudExtra: String?,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val bg = cs.surfaceContainerLow
    val grid = cs.outlineVariant
    val floor = if (arena.map == GameMap.ICE) Color(0xFF9ADCFF).copy(alpha = .45f) else cs.surfaceContainerHigh
    val onSurface = cs.onSurface
    val dim = cs.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val toolNow by rememberUpdatedState(tool)
    var scaleK by remember { mutableFloatStateOf(1f) }
    val pals = remember(cs) { (0..5).map { paletteFor(cs, it) } }

    Canvas(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(bg)
            .pointerInput(arena) {
                awaitEachGesture {
                    val d = awaitFirstDown()
                    val t = toolNow ?: return@awaitEachGesture
                    d.consume()
                    val k = scaleK
                    arena.pointerDown(t, d.position.x / k, d.position.y / k)
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == d.id } ?: break
                        c.consume()
                        if (!c.pressed) break
                        arena.pointerMove(c.position.x / k, c.position.y / k)
                    }
                    arena.pointerUp()
                }
            }
    ) {
        frame() // read so the canvas redraws every frame
        // a mirrored online game keeps the host's world size and gets letterboxed instead
        val k = if (arena.remote) minOf(size.width / arena.width, size.height / arena.height)
        else size.width / max(MIN_WORLD_W, size.width / density)
        scaleK = k
        val ww = if (arena.remote) arena.width else size.width / k
        val wh = if (arena.remote) arena.height else size.height / k
        if (!arena.remote) arena.resize(ww, wh)
        val ox = if (arena.remote) (size.width - ww * k) / 2 else 0f
        val oy = if (arena.remote) (size.height - wh * k) / 2 else 0f

        val sh = if (shakeOn && arena.shake > .2f) Offset((Random.nextFloat() - .5f) * arena.shake * k, (Random.nextFloat() - .5f) * arena.shake * k) else Offset.Zero
        translate(sh.x + ox, sh.y + oy) {
            scale(k, pivot = Offset.Zero) {
                // lock screen dot grid
                var gx = 30f
                while (gx < ww) {
                    var gy = 30f
                    while (gy < arena.floorY) { drawCircle(grid, 2.5f, Offset(gx, gy)); gy += 60f }
                    gx += 60f
                }
                drawRect(floor, Offset(-20f, arena.floorY), Size(ww + 40, 80f))
                drawLine(grid, Offset(0f, arena.floorY), Offset(ww, arena.floorY), 2f)

                // king of the hill zone
                arena.zoneX?.let { zx ->
                    val zw = arena.zoneW
                    val owner = arena.fighters.filter { !it.dead && !it.isMinion && kotlin.math.abs(it.x - zx) < zw / 2 && it.y > arena.floorY - 220 }
                        .minByOrNull { kotlin.math.abs(it.x - zx) }
                    val c = owner?.let { pals[it.team.mod(6)].body } ?: onSurface
                    drawRect(c.copy(alpha = .12f), Offset(zx - zw / 2, arena.floorY - 220), Size(zw, 220f))
                    drawRect(c.copy(alpha = .7f), Offset(zx - zw / 2, arena.floorY - 4), Size(zw, 4f))
                    val lay = measurer.measure("zone", TextStyle(fontSize = (11f / k).sp, color = c, fontFamily = FontFamily.Monospace))
                    drawText(lay, topLeft = Offset(zx - lay.size.width / 2f, arena.floorY - 236))
                }

                for (p in arena.props) drawProp(p, grid, onSurface)

                for (d in arena.debris) {
                    val pal = arena.fighters.firstOrNull { it.id == d.owner }?.let { pals[it.team.mod(6)] } ?: pals[0]
                    val owner = arena.fighters.firstOrNull { it.id == d.owner }
                    val c = when (d.kind) {
                        Debris.Kind.BODY -> pal.body
                        Debris.Kind.WEAPON -> owner?.let { weaponColor(it.loadout.weaponMat, pal) } ?: pal.blade
                        Debris.Kind.ARMOUR -> owner?.let { armourColor(it.loadout.armourMat, pal) } ?: pal.metal
                        Debris.Kind.HAT -> pal.hat
                        Debris.Kind.WOOD -> Color(0xFFA0703C)
                    }
                    withTransform({ translate(d.x, d.y); rotateRad(d.ang, Offset.Zero) }) {
                        drawLine(c.copy(alpha = d.life.coerceIn(0f, 1f) * c.alpha), Offset(d.a.x, d.a.y), Offset(d.b.x, d.b.y),
                            when (d.kind) { Debris.Kind.WEAPON -> 4.5f; Debris.Kind.BODY -> 9f; Debris.Kind.HAT -> 4f; else -> 7f },
                            StrokeCap.Round)
                    }
                }

                for (f in arena.fighters) {
                    if (f.dead) continue
                    val s = max(.3f, 1 - (arena.floorY - f.y) / 400f) * f.scale
                    val invis = f.invisT > 0
                    if (!invis) drawOval(Color.Black.copy(alpha = .22f), Offset(f.x - 34 * s, arena.floorY - 4 * s), Size(68 * s, 12 * s))
                    withTransform({
                        translate(f.x, f.y)
                        rotateRad(f.ang, Offset.Zero)
                        scale(f.face * f.scale, f.scale, Offset.Zero)
                    }) {
                        // invisible fighters are a faint shimmer
                        if (invis) drawContext.canvas.saveLayer(androidx.compose.ui.geometry.Rect(-120f, -120f, 120f, 120f), androidx.compose.ui.graphics.Paint().apply { alpha = .16f })
                        drawFighter(
                            f.loadout, pals[f.team.mod(6)],
                            FighterLook(
                                weapon = arena.weaponLocal(f) ?: emptyList(), plates = f.plates, look = f.look,
                                hot = f.hot, flash = f.stun > .25f && f.stun < .4f, frozen = f.freezeT > 0,
                                burning = f.burnT > 0, rage = f.rageT > 0, bubble = f.bubbleT > 0, time = arena.time,
                                poisoned = f.poisonT > 0,
                            ),
                        )
                        if (invis) drawContext.canvas.restore()
                    }
                    if (f.stun > .4f) {
                        // little stars over the head when stunned
                        for (i in 0..2) {
                            val a = arena.time * 6 + i * 2.1f
                            drawCircle(Color(0xFFFFE14D), 3f, Offset(f.x + kotlin.math.cos(a) * 18, f.y - 52 + kotlin.math.sin(a) * 5))
                        }
                    }
                }

                for (b in arena.beams) {
                    val c = pals[b.team.mod(6)].body
                    val a = (b.life / .25f).coerceIn(0f, 1f)
                    drawLine(c.copy(alpha = .35f * a), Offset(b.x1, b.y1), Offset(b.x2, b.y2), 14f, StrokeCap.Round)
                    drawLine(Color.White.copy(alpha = a), Offset(b.x1, b.y1), Offset(b.x2, b.y2), 4f, StrokeCap.Round)
                }

                for (p in arena.projectiles) {
                    val pal = pals[p.owner.team.mod(6)]
                    val c = weaponColor(p.owner.loadout.weaponMat, pal)
                    withTransform({ translate(p.x, p.y); rotateRad(p.ang, Offset.Zero) }) {
                        if (p.kind == WeaponType.BOMB) {
                            drawCircle(Color(0xFF2A2A30), 12f, Offset.Zero)
                            drawCircle(if ((arena.time * 8).toInt() % 2 == 0) Color(0xFFFF5040) else c, 4f, Offset(0f, -12f))
                        }
                        polyline(p.shape, pal.bladeOutline, 6f)
                        polyline(p.shape, c, 3.5f)
                    }
                }

                for (pt in arena.particles) {
                    val a = (pt.life / pt.maxLife).coerceIn(0f, 1f)
                    val (c, r) = when (pt.kind) {
                        Particle.Kind.SPARK -> Color(0xFFFFF0A0) to 2.5f
                        Particle.Kind.FIRE -> Color(0xFFFF8A3D) to 4f + (1 - a) * 4
                        Particle.Kind.ICE -> Color(0xFFBFEAFF) to 3f
                        Particle.Kind.ZAP -> Color(0xFFFFE14D) to 2.5f
                        Particle.Kind.SMOKE -> dim.copy(alpha = .6f) to 6f + (1 - a) * 10
                        Particle.Kind.HEAL -> Color(0xFF6FCF8E) to 3.5f
                        Particle.Kind.MAGIC -> Color(0xFFC77DFF) to 3f
                        Particle.Kind.POISON -> Color(0xFF8BD450) to 4f
                    }
                    drawCircle(c.copy(alpha = c.alpha * a), r, Offset(pt.x, pt.y))
                }

                for (b in arena.booms) {
                    val t = b.t / .5f
                    drawCircle(Color(0xFFFFBE5A).copy(alpha = .45f * (1 - t)), b.radius * (.2f + .8f * t), Offset(b.x, b.y))
                    drawCircle(Color.White.copy(alpha = 1 - t), b.radius * (.15f + t), Offset(b.x, b.y), style = Stroke(3f))
                }

                for (t in arena.texts) {
                    val c = when (t.tone) {
                        FloatText.Tone.CRIT -> Color(0xFFFFC94A)
                        FloatText.Tone.METAL -> dim
                        FloatText.Tone.GOOD -> Color(0xFF6FCF8E)
                        FloatText.Tone.FIRE -> Color(0xFFFF8A3D)
                        FloatText.Tone.ICE -> Color(0xFF9ADCFF)
                        FloatText.Tone.ZAP -> Color(0xFFFFE14D)
                        FloatText.Tone.NORMAL, FloatText.Tone.SPEECH -> onSurface
                    }
                    val speech = t.tone == FloatText.Tone.SPEECH
                    val lay = measurer.measure(t.text, TextStyle(fontSize = ((if (speech) 13f else 15f) / k).sp, fontWeight = FontWeight.Bold, fontFamily = if (speech) FontFamily.Default else FontFamily.Monospace, color = c))
                    val tl = Offset(t.x - lay.size.width / 2f, t.y - lay.size.height)
                    val alpha = (t.life * 2).coerceIn(0f, 1f)
                    if (speech) {
                        drawRoundRect(bg.copy(alpha = .9f * alpha), tl - Offset(8f, 4f), Size(lay.size.width + 16f, lay.size.height + 8f), CornerRadius(12f))
                    }
                    drawText(lay, topLeft = tl, alpha = alpha)
                }
            }
        }
        drawHud(arena, pals, measurer, onSurface, floor, hudExtra, dim)
    }
}

private fun DrawScope.drawProp(p: Prop, grid: Color, ink: Color) {
    val tl = Offset(p.x - p.w / 2, p.y - p.h / 2)
    when (p.kind) {
        Prop.Kind.PLATFORM -> {
            drawRoundRect(grid, tl, Size(p.w, p.h), CornerRadius(6f))
            var x = p.x - p.w / 2 + 12
            while (x < p.x + p.w / 2 - 6) { drawCircle(ink.copy(alpha = .35f), 2.5f, Offset(x, p.y)); x += 20 }
        }
        Prop.Kind.PAD -> {
            drawRoundRect(Color(0xFF6FCF8E), tl, Size(p.w, p.h), CornerRadius(5f))
            val path = Path().apply {
                moveTo(p.x - 24, p.y + 6)
                for (i in 0..6) lineTo(p.x - 24 + i * 8f, p.y + 6 + (if (i % 2 == 0) 0f else 8f))
            }
            drawPath(path, ink.copy(alpha = .6f), style = Stroke(2f))
        }
        Prop.Kind.LAVA -> {
            drawRoundRect(Color(0xFFFF5A1F), tl, Size(p.w, p.h + 6), CornerRadius(6f))
            drawRoundRect(Color(0xFFFFB13D).copy(alpha = .8f), tl + Offset(6f, 2f), Size(p.w - 12, 4f), CornerRadius(2f))
        }
        Prop.Kind.SPIKES -> {
            val n = (p.w / 14).toInt()
            val path = Path()
            for (i in 0 until n) {
                val x0 = p.x - p.w / 2 + i * 14f
                path.moveTo(x0, p.y + p.h / 2); path.lineTo(x0 + 7, p.y - p.h / 2); path.lineTo(x0 + 14, p.y + p.h / 2); path.close()
            }
            drawPath(path, Color(0xFFB0B8C4))
        }
        Prop.Kind.CRATE, Prop.Kind.TNT -> withTransform({ translate(p.x, p.y); rotateRad(p.ang, Offset.Zero) }) {
            val s = p.w / 2
            val tnt = p.kind == Prop.Kind.TNT
            val c = if (p.flash > 0) Color.White else if (tnt) Color(0xFFD9443A) else Color(0xFFB98348)
            drawRect(c, Offset(-s, -s), Size(p.w, p.h))
            drawRect(Color.Black.copy(alpha = .35f), Offset(-s, -s), Size(p.w, p.h), style = Stroke(2.5f))
            if (tnt) {
                drawRect(Color(0xFFF2EBDD), Offset(-s, -5f), Size(p.w, 10f))
                drawLine(Color(0xFF2A2A30), Offset(0f, -s), Offset(5f, -s - 8), 2f)
            } else {
                drawLine(Color.Black.copy(alpha = .3f), Offset(-s, -s), Offset(s, s), 2.5f)
                drawLine(Color.Black.copy(alpha = .3f), Offset(s, -s), Offset(-s, s), 2.5f)
            }
        }
    }
}

private fun DrawScope.drawHud(
    arena: Arena, pals: List<FighterPalette>, measurer: androidx.compose.ui.text.TextMeasurer,
    ink: Color, track: Color, extra: String?, dim: Color,
) {
    val fs = arena.fighters.filter { !it.isMinion }.take(8)
    val pad = 14.dp.toPx()
    val colW = (size.width - pad * 3) / 2
    val rowH = 30.dp.toPx()
    val many = fs.size > 2
    fs.forEachIndexed { i, f ->
        val col = if (many) i % 2 else f.team.coerceIn(0, 1).let { if (fs.size == 2) i else it }
        val row = if (many) i / 2 else 0
        val x = pad + col * (colW + pad)
        val y = pad + row * rowH
        val right = col == 1
        val name = measurer.measure(
            "${f.name}  ${max(0, ceil(f.hp).toInt())}" + (arena.scores[f.team]?.let { "  ·  ${it.toInt()}/30" } ?: if (arena.zoneX != null) "  ·  0/30" else ""),
            TextStyle(fontSize = if (many) 11.sp else 13.sp, fontWeight = FontWeight.Bold, color = if (f.dead) dim else ink),
            overflow = TextOverflow.Ellipsis, maxLines = 1, constraints = Constraints(maxWidth = colW.toInt()),
        )
        drawText(name, topLeft = Offset(if (right) x + colW - name.size.width else x, y))
        val by = y + name.size.height + 2.dp.toPx()
        val bh = (if (many) 5 else 7).dp.toPx()
        drawRoundRect(track, Offset(x, by), Size(colW, bh), CornerRadius(bh / 2))
        val fr = (f.hp / f.maxHp).coerceIn(0f, 1f)
        drawRoundRect(pals[f.team.mod(6)].body, Offset(if (right) x + colW * (1 - fr) else x, by), Size(colW * fr, bh), CornerRadius(bh / 2))
        // ability cooldown as a thin line under the hp bar
        val cdMax = f.loadout.ability.cooldown
        if (cdMax > 0 && !f.dead) {
            val ready = 1 - (f.abilityCd / cdMax).coerceIn(0f, 1f)
            drawRect(if (ready >= 1f) Color(0xFFC77DFF) else dim.copy(alpha = .5f), Offset(if (right) x + colW * (1 - ready) else x, by + bh + 2), Size(colW * ready, 2.dp.toPx()))
        }
    }
    if (extra != null) {
        val lay = measurer.measure(extra, TextStyle(fontSize = 12.sp, color = dim, fontFamily = FontFamily.Monospace))
        drawText(lay, topLeft = Offset((size.width - lay.size.width) / 2, size.height - lay.size.height - 6.dp.toPx()))
    }
}

