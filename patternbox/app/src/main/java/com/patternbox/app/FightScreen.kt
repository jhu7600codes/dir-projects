package com.patternbox.app

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patternbox.core.Arena
import com.patternbox.core.Debris
import com.patternbox.core.FloatText
import com.patternbox.core.Gravity
import com.patternbox.core.Tool
import kotlin.math.max
import kotlin.random.Random

/** world is at least this many units wide so fighters aren't tiny on a portrait phone */
private const val MIN_WORLD_W = 560f

@Composable
fun FightScreen(state: GameState, padding: PaddingValues, round: Int, onRematch: () -> Unit) {
    val arena = remember(round) {
        Arena().also { it.start(state.loadouts[0], state.nameFor(0), state.loadouts[1], state.nameFor(1)) }
    }
    var tool by remember { mutableStateOf(Tool.GRAB) }
    var slow by remember { mutableStateOf(false) }
    var gravity by remember { mutableStateOf(Gravity.EARTH) }
    var bots by remember(round) { mutableStateOf(listOf(true, true)) }
    var frame by remember { mutableLongStateOf(0L) }
    val view = LocalView.current
    // touching a player's buttons turns that player's bot off
    fun takeOver(i: Int) {
        if (arena.bot[i]) { arena.bot[i] = false; bots = listOf(arena.bot[0], arena.bot[1]) }
    }

    LaunchedEffect(arena) {
        var last = 0L
        var acc = 0f
        var seenImpacts = 0
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1e9f).coerceAtMost(.1f)
                    acc += dt * (if (slow) .3f else 1f)
                    while (acc >= 1f / 60f) { arena.step(); acc -= 1f / 60f }
                }
                last = now
                frame = now
                if (arena.impacts > seenImpacts) {
                    val big = arena.impacts - seenImpacts >= 2
                    view.performHapticFeedback(if (big) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.CONTEXT_CLICK)
                    seenImpacts = arena.impacts
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ArenaCanvas(arena, tool, { frame }, Modifier.weight(1f).fillMaxWidth())

        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (t in Tool.values()) {
                FilterChip(selected = tool == t, onClick = { tool = t }, label = { Text(t.name.lowercase()) })
            }
            VerticalDivider(Modifier.padding(vertical = 8.dp).size(1.dp, 24.dp))
            for (i in 0..1) {
                FilterChip(selected = bots[i], onClick = {
                    arena.bot[i] = !arena.bot[i]; bots = listOf(arena.bot[0], arena.bot[1])
                }, label = { Text("p${i + 1} bot") })
            }
            VerticalDivider(Modifier.padding(vertical = 8.dp).size(1.dp, 24.dp))
            AssistChip(onClick = {
                gravity = Gravity.values()[(gravity.ordinal + 1) % Gravity.values().size]; arena.gravity = gravity
            }, label = { Text("gravity: ${gravity.label}") })
            FilterChip(selected = slow, onClick = { slow = !slow }, label = { Text("slowmo") })
            AssistChip(onClick = { arena.chaos() }, label = { Text("chaos") })
            AssistChip(onClick = { arena.heal() }, label = { Text("heal") })
            AssistChip(onClick = onRematch, label = { Text("rematch") })
        }

        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in 0..1) {
                ControlPad(
                    i,
                    onMove = { left, right, jump ->
                        takeOver(i)
                        arena.input[i].left = left; arena.input[i].right = right; arena.input[i].jump = jump
                    },
                    onSwing = { takeOver(i); arena.swing(arena.fighters[i]) },
                )
            }
        }
    }
}

@Composable
private fun ControlPad(i: Int, onMove: (left: Boolean, right: Boolean, jump: Boolean) -> Unit, onSwing: () -> Unit) {
    var l by remember { mutableStateOf(false) }
    var r by remember { mutableStateOf(false) }
    var j by remember { mutableStateOf(false) }
    fun push() = onMove(l, r, j)
    Surface(shape = RoundedCornerShape(28.dp), color = playerContainer(i)) {
        Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            HoldButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, i) { l = it; push() }
            HoldButton(Icons.Filled.KeyboardArrowUp, i) { j = it; push() }
            HoldButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, i) { r = it; push() }
            HoldButton(Icons.Filled.Star, i, strong = true) {
                if (it) onSwing()
            }
        }
    }
}

@Composable
private fun HoldButton(icon: ImageVector, i: Int, strong: Boolean = false, onHold: (Boolean) -> Unit) {
    var down by remember { mutableStateOf(false) }
    val bg = when {
        down -> playerColor(i)
        strong -> playerColor(i).copy(alpha = .35f)
        else -> Color.Transparent
    }
    Box(
        Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val d = awaitFirstDown()
                    d.consume(); down = true; onHold(true)
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == d.id } ?: break
                        if (!c.pressed) break
                    }
                    down = false; onHold(false)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (down) MaterialTheme.colorScheme.surface else onPlayerContainer(i))
    }
}

@Composable
private fun ArenaCanvas(arena: Arena, tool: Tool, frame: () -> Long, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val pals = listOf(fighterPalette(0), fighterPalette(1))
    val bg = cs.surfaceContainerLow
    val grid = cs.outlineVariant
    val floor = cs.surfaceContainerHigh
    val onSurface = cs.onSurface
    val dim = cs.onSurfaceVariant
    val crit = Color(0xFFFFC94A)
    val good = Color(0xFF7DDE9A)
    val measurer = rememberTextMeasurer()
    val toolNow by androidx.compose.runtime.rememberUpdatedState(tool)
    var scaleK by remember { mutableStateOf(1f) }

    Canvas(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(bg)
            .pointerInput(arena) {
                awaitEachGesture {
                    val d = awaitFirstDown()
                    d.consume()
                    val k = scaleK
                    arena.pointerDown(toolNow, d.position.x / k, d.position.y / k)
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
        val k = size.width / max(MIN_WORLD_W, size.width / density)
        scaleK = k
        val ww = size.width / k
        val wh = size.height / k
        if (arena.width != ww || arena.height != wh) arena.resize(ww, wh)

        val shake = if (arena.shake > .2f) Offset((Random.nextFloat() - .5f) * arena.shake * k, (Random.nextFloat() - .5f) * arena.shake * k) else Offset.Zero
        translate(shake.x, shake.y) {
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

                for (d in arena.debris) {
                    val pal = pals[d.owner]
                    val c = when (d.kind) {
                        Debris.Kind.BODY -> pal.body
                        Debris.Kind.WEAPON -> pal.blade
                        Debris.Kind.ARMOUR -> pal.metal
                    }
                    withTransform({ translate(d.x, d.y); rotateRad(d.ang, Offset.Zero) }) {
                        drawLine(c.copy(alpha = d.life.coerceIn(0f, 1f) * c.alpha), Offset(d.a.x, d.a.y), Offset(d.b.x, d.b.y),
                            if (d.kind == Debris.Kind.WEAPON) 4.5f else if (d.kind == Debris.Kind.BODY) 9f else 7f,
                            androidx.compose.ui.graphics.StrokeCap.Round)
                    }
                }

                for (f in arena.fighters) {
                    if (f.dead) continue
                    val sh = max(.3f, 1 - (arena.floorY - f.y) / 400f)
                    drawOval(Color.Black.copy(alpha = .25f), Offset(f.x - 34 * sh, arena.floorY - 4 * sh), Size(68 * sh, 12 * sh))
                    withTransform({
                        translate(f.x, f.y)
                        rotateRad(f.ang, Offset.Zero)
                        scale(f.face, 1f, Offset.Zero)
                    }) {
                        drawFighter(
                            f.loadout, pals[f.index], arena.swingAngle(f), f.plates, f.look,
                            dead = false, hot = f.hot, flash = f.stun > .25f,
                        )
                    }
                }

                for (b in arena.booms) {
                    val t = b.t / .5f
                    drawCircle(Color(0xFFFFBE5A).copy(alpha = .5f * (1 - t)), 30 + 140 * t, Offset(b.x, b.y))
                    drawCircle(Color.White.copy(alpha = 1 - t), 20 + 170 * t, Offset(b.x, b.y), style = Stroke(3f))
                }

                for (t in arena.texts) {
                    val c = when (t.tone) {
                        FloatText.Tone.CRIT -> crit
                        FloatText.Tone.METAL -> dim
                        FloatText.Tone.GOOD -> good
                        FloatText.Tone.NORMAL -> onSurface
                    }
                    val lay = measurer.measure(t.text, TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = c))
                    drawText(lay, topLeft = Offset(t.x - lay.size.width / 2f, t.y - lay.size.height), alpha = (t.life * 2).coerceIn(0f, 1f))
                }
            }
        }

        // hud, drawn unscaled in screen pixels
        val barW = size.width * .4f
        for (f in arena.fighters) {
            val x = if (f.index == 0) 16.dp.toPx() else size.width - 16.dp.toPx() - barW
            val y = 16.dp.toPx()
            val name = measurer.measure("${f.name}  ${max(0, kotlin.math.ceil(f.hp).toInt())}",
                TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = onSurface))
            drawText(name, topLeft = Offset(if (f.index == 0) x else x + barW - name.size.width, y))
            val by = y + name.size.height + 4.dp.toPx()
            val bh = 8.dp.toPx()
            drawRoundRect(floor, Offset(x, by), Size(barW, bh), androidx.compose.ui.geometry.CornerRadius(bh / 2))
            val fr = (f.hp / f.stats.hp).coerceIn(0f, 1f)
            drawRoundRect(pals[f.index].body, Offset(if (f.index == 0) x else x + barW * (1 - fr), by), Size(barW * fr, bh),
                androidx.compose.ui.geometry.CornerRadius(bh / 2))
        }
        val w = arena.winner
        if (w != null && arena.winT > .6f) {
            val big = measurer.measure(if (w.isEmpty()) "double ko" else "$w wins",
                TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Black, color = onSurface, textAlign = TextAlign.Center))
            val small = measurer.measure("tap rematch, or go build something meaner", TextStyle(fontSize = 13.sp, color = dim))
            val cy = size.height / 2
            drawRect(bg.copy(alpha = .75f), Offset(0f, cy - big.size.height.toFloat()), Size(size.width, (big.size.height * 2 + small.size.height).toFloat()))
            drawText(big, topLeft = Offset((size.width - big.size.width) / 2, cy - big.size.height / 2))
            drawText(small, topLeft = Offset((size.width - small.size.width) / 2, cy + big.size.height / 2 + 4))
        }
    }
}

