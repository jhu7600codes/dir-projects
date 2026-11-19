package com.lot.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** builds your character from the wardrobe */
fun youDude(s: GameState): Dude {
    val hat = cosmetic(s.outfitHat)?.value?.toInt() ?: 1
    val shirt = cosmetic(s.outfitShirt)?.value ?: 0xFF4BE3AC
    return Dude(
        Color(0xFFFFD7B5), Color(shirt), Color(0xFF6B3E26), hat,
        if (hat == 7) Gold else Pink, "you", s.outfitGlasses, 1f,
    )
}

// ================================ claw machine ================================

private const val CLAW_PRICE = 15

private class ClawPrize(val name: String, val coins: Int, val cosmetic: String?, val color: Color, val grip: Float)

private val clawPool = listOf(
    ClawPrize("rubber duck", 10, null, Gold, 0.85f),
    ClawPrize("one sock", 5, null, Color(0xFFEDEDED), 0.9f),
    ClawPrize("plush pigeon", 40, null, Color(0xFF9EA7B8), 0.75f),
    ClawPrize("plush raccoon", 60, null, Color(0xFF8A8592), 0.7f),
    ClawPrize("bag of coins", 100, null, Color(0xFFE0A800), 0.6f),
    ClawPrize("crown", 0, "hat_crown", Gold, 0.45f),
    ClawPrize("propeller hat", 0, "hat_prop", Sky, 0.5f),
    ClawPrize("tuxedo", 0, "shirt_tux", Color(0xFF2A2140), 0.5f),
)

private class PitItem(val x: Float, val prize: ClawPrize, val tilt: Float)

private fun DrawScope.drawClawPrize(p: ClawPrize, c: Offset, s: Float, tilt: Float) {
    rotate(tilt, c) {
        when (p.name) {
            "plush pigeon" -> drawPigeon(c, s, 0.3f, 1f)
            "plush raccoon" -> drawRaccoon(c, s * 0.9f, 0f, false)
            "bag of coins" -> {
                drawTrashBag(c, s * 0.9f, Color(0xFFB07D00), 0f)
                label("$", c.x, c.y + s * 0.1f, s * 0.5f, Gold, Ink)
            }
            "one sock" -> drawSymbol(Sym.SOCK, c, s)
            "crown" -> {
                drawCircle(Color.White.copy(alpha = 0.25f), s * 0.5f, c)
                val p2 = androidx.compose.ui.graphics.Path().apply {
                    moveTo(c.x - s * 0.4f, c.y + s * 0.25f); lineTo(c.x - s * 0.45f, c.y - s * 0.25f)
                    lineTo(c.x - s * 0.2f, c.y); lineTo(c.x, c.y - s * 0.35f)
                    lineTo(c.x + s * 0.2f, c.y); lineTo(c.x + s * 0.45f, c.y - s * 0.25f)
                    lineTo(c.x + s * 0.4f, c.y + s * 0.25f); close()
                }
                outlined(p2, Gold, s * 0.05f)
                circleO(Offset(c.x, c.y + s * 0.08f), s * 0.07f, Pink, s * 0.03f)
            }
            "propeller hat" -> {
                drawCircle(Color.White.copy(alpha = 0.25f), s * 0.5f, c)
                drawArc(Sky, 180f, 180f, true, c - Offset(s * 0.4f, s * 0.2f), Size(s * 0.8f, s * 0.6f))
                drawArc(Ink, 180f, 180f, true, c - Offset(s * 0.4f, s * 0.2f), Size(s * 0.8f, s * 0.6f), style = Stroke(s * 0.05f))
                drawLine(Ink, c - Offset(0f, s * 0.2f), c - Offset(0f, s * 0.35f), s * 0.05f)
                drawOval(Pink, c - Offset(s * 0.35f, s * 0.42f), Size(s * 0.7f, s * 0.1f))
            }
            "tuxedo" -> {
                drawRoundRect(p.color, c - Offset(s * 0.4f, s * 0.35f), Size(s * 0.8f, s * 0.7f), CornerRadius(s * 0.2f))
                drawRoundRect(Ink, c - Offset(s * 0.4f, s * 0.35f), Size(s * 0.8f, s * 0.7f), CornerRadius(s * 0.2f), style = Stroke(s * 0.05f))
                drawCircle(Pink, s * 0.08f, c - Offset(0f, s * 0.2f))
            }
            else -> { // rubber duck
                circleO(c + Offset(0f, s * 0.1f), s * 0.35f, Gold, s * 0.05f)
                circleO(c + Offset(s * 0.2f, -s * 0.25f), s * 0.2f, Gold, s * 0.05f)
                drawCircle(Ink, s * 0.04f, c + Offset(s * 0.25f, -s * 0.3f))
                drawOval(Color(0xFFFF8C42), c + Offset(s * 0.35f, -s * 0.25f), Size(s * 0.2f, s * 0.1f))
            }
        }
    }
}

private val clawTutorial = listOf(
    "the commissioner's claw machine. ${CLAW_PRICE}c a try. the claw slides back and forth by itself.",
    "tap drop when it's right above a prize. the closer you are, the better the grip.",
    "some prizes are clothes you can't buy anywhere else, like the crown. they're heavy. the claw is weak. good luck.",
)

@Composable
fun ClawScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    val scope = rememberCoroutineScope()
    var help by remember { mutableStateOf(false) }
    ModeTutorial(vm, state, "claw", "claw machine", clawTutorial, help) { help = false }
    val paused by rememberUpdatedState(help || "claw" !in state.tutorialsSeen)

    val pit = remember { mutableStateListOf<PitItem>().apply { repeat(7) { add(PitItem(0.18f + it * 0.12f, clawPool.random(), Random.nextFloat() * 40f - 20f)) } } }
    var clawX by remember { mutableFloatStateOf(0.5f) }
    val clawY = remember { Animatable(0.12f) }
    var dir by remember { mutableFloatStateOf(1f) }
    var phase by remember { mutableIntStateOf(0) } // 0 idle, 1 sliding, 2 busy
    var held by remember { mutableStateOf<ClawPrize?>(null) }
    var open by remember { mutableStateOf(true) }
    var msg by remember { mutableStateOf("insert coin") }
    var frame by remember { mutableLongStateOf(0L) }
    var confetti by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                frame++
                if (paused || phase != 1) return@withFrameNanos
                clawX += dir * 0.32f * dt
                if (clawX > 0.92f) { clawX = 0.92f; dir = -1f }
                if (clawX < 0.15f) { clawX = 0.15f; dir = 1f }
            }
        }
    }

    fun drop() {
        if (phase != 1) return
        phase = 2
        fx.play(Sfx.WHOOSH)
        scope.launch {
            clawY.animateTo(0.7f, tween(900, easing = LinearEasing))
            open = false
            fx.play(Sfx.CLICK); fx.buzz(Buzz.LIGHT)
            delay(250)
            val target = pit.minByOrNull { abs(it.x - clawX) }
            val dist = target?.let { abs(it.x - clawX) } ?: 1f
            if (target != null && dist < 0.065f) {
                held = target.prize
                pit.remove(target)
            }
            clawY.animateTo(0.12f, tween(900, easing = LinearEasing))
            val h = held
            if (h != null && Random.nextFloat() > h.grip - dist * 5f) {
                // the claw is weak
                held = null
                pit.add(PitItem(clawX.coerceIn(0.18f, 0.9f), h, Random.nextFloat() * 40f - 20f))
                msg = "the claw dropped the ${h.name}. classic."
                fx.play(Sfx.LOSE); fx.buzz(Buzz.FAIL)
            } else if (h == null) {
                msg = "grabbed air. very nice air."
                fx.play(Sfx.LOSE)
            } else {
                // carry to the chute
                val start = clawX
                val anim = Animatable(0f)
                anim.animateTo(1f, tween(700)) { clawX = start + (0.06f - start) * value }
                open = true
                delay(200)
                held = null
                val cos = h.cosmetic
                if (cos != null && cos !in state.wardrobe) {
                    vm.giveCosmetic(cos)
                    msg = "you won the ${h.name}! check your wardrobe."
                    fx.play(Sfx.BIG_WIN)
                } else {
                    val coins = if (cos != null) 100 else h.coins
                    vm.update { it.copy(coins = it.coins + coins) }
                    msg = if (cos != null) "another ${h.name}? sold it for 100c" else "you won a ${h.name}! +${coins}c"
                    fx.play(Sfx.WIN)
                }
                vm.update { it.copy(clawWins = it.clawWins + 1) }
                fx.buzz(Buzz.SUCCESS)
                confetti++
                pit.add(PitItem(0.18f + Random.nextFloat() * 0.72f, clawPool.random(), Random.nextFloat() * 40f - 20f))
            }
            open = true
            clawX = 0.5f
            phase = 0
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            LotTopBar("claw machine", state.coins, { nav.back() }, { help = true })
            Box(Modifier.fillMaxWidth().weight(1f).padding(12.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    @Suppress("UNUSED_EXPRESSION") frame
                    val w = size.width
                    val h = size.height
                    drawRoundRect(Ink, Offset(6f, 10f), size, CornerRadius(40f))
                    drawRoundRect(Brush.verticalGradient(listOf(Pink, Grape)), cornerRadius = CornerRadius(40f))
                    val gl = Offset(w * 0.06f, h * 0.1f)
                    val gs = Size(w * 0.88f, h * 0.72f)
                    drawRoundRect(Color(0xFF1B1530), gl, gs, CornerRadius(20f))
                    label("CLAW-O-MATIC", w / 2, h * 0.05f, h * 0.04f, Gold, Ink)
                    // chute
                    drawRect(Color(0xFF3A2E5C), Offset(gl.x, gl.y + gs.height * 0.55f), Size(gs.width * 0.14f, gs.height * 0.45f))
                    drawRect(Ink, Offset(gl.x, gl.y + gs.height * 0.55f), Size(gs.width * 0.14f, gs.height * 0.45f), style = Stroke(4f))
                    // pit
                    val ps = gs.width * 0.1f
                    pit.forEach { p ->
                        drawClawPrize(p.prize, Offset(gl.x + gs.width * p.x, gl.y + gs.height * 0.86f), ps, p.tilt)
                    }
                    // claw
                    val cx = gl.x + gs.width * clawX
                    val cy = gl.y + gs.height * clawY.value
                    drawLine(Color(0xFF9E9E9E), Offset(gl.x, gl.y + 8f), Offset(gl.x + gs.width, gl.y + 8f), 10f)
                    drawLine(Color(0xFFBDBDBD), Offset(cx, gl.y + 8f), Offset(cx, cy), 6f)
                    circleO(Offset(cx, cy), ps * 0.22f, Color(0xFFBDBDBD), 4f)
                    val spread = if (open) ps * 0.5f else ps * 0.18f
                    for (sx in listOf(-1f, 1f)) {
                        drawLine(Ink, Offset(cx, cy), Offset(cx + sx * spread, cy + ps * 0.45f), 10f, StrokeCap.Round)
                        drawLine(Ink, Offset(cx + sx * spread, cy + ps * 0.45f), Offset(cx + sx * spread * 0.4f, cy + ps * 0.75f), 10f, StrokeCap.Round)
                    }
                    held?.let { drawClawPrize(it, Offset(cx, cy + ps * 0.75f), ps, 0f) }
                    // glass shine
                    drawLine(Color.White.copy(alpha = 0.15f), gl + Offset(gs.width * 0.7f, 0f), gl + Offset(gs.width * 0.5f, gs.height), 30f)
                    drawRoundRect(Ink, gl, gs, CornerRadius(20f), style = Stroke(6f))
                }
            }
            Text(msg, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp))
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChunkyButton("insert ${CLAW_PRICE}c", Modifier.weight(1f), color = Gold, enabled = phase == 0 && state.coins >= CLAW_PRICE, sfx = Sfx.BUY, height = 64.dp) {
                    vm.update { it.copy(coins = it.coins - CLAW_PRICE, clawPlays = it.clawPlays + 1) }
                    msg = "tap drop when it's above something good"
                    phase = 1
                }
                ChunkyButton("DROP", Modifier.weight(1f), color = Pink, enabled = phase == 1, sfx = null, height = 64.dp) { drop() }
            }
        }
        Confetti(confetti)
    }
}

// ================================ pigeon flight ================================

private val flightTutorial = listOf(
    "a pigeon carries your bet up into the sky. the higher it goes, the bigger the multiplier.",
    "at some random point the pigeon sees a fry and drops everything. you lose the bet.",
    "cash out before that happens. greed is the real final boss.",
)

@Composable
fun FlightScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    var help by remember { mutableStateOf(false) }
    ModeTutorial(vm, state, "flight", "pigeon flight", flightTutorial, help) { help = false }
    val paused by rememberUpdatedState(help || "flight" !in state.tutorialsSeen)

    var bet by remember { mutableIntStateOf(10) }
    var flight by remember { mutableIntStateOf(0) }
    var flying by remember { mutableStateOf(false) }
    var crashed by remember { mutableStateOf(false) }
    var mult by remember { mutableFloatStateOf(1f) }
    var t by remember { mutableFloatStateOf(0f) }
    var fall by remember { mutableFloatStateOf(0f) }
    var msg by remember { mutableStateOf("pick a bet and launch") }
    var cashed by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    val history = remember { mutableStateListOf<Float>() }

    LaunchedEffect(flight) {
        if (flight == 0) return@LaunchedEffect
        val u = Random.nextFloat()
        val crashAt = (0.96f / (1f - u)).coerceIn(1f, 100f)
        var last = 0L
        t = 0f; mult = 1f; crashed = false; fall = 0f
        var ticks = 0f
        while (flying) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                if (paused) return@withFrameNanos
                t += dt
                mult = exp(0.18f * t)
                ticks += dt
                if (ticks > 0.5f) { ticks = 0f; fx.play(Sfx.CLICK) }
                if (mult >= crashAt) {
                    mult = crashAt
                    flying = false
                    crashed = true
                    history.add(0, crashAt); if (history.size > 8) history.removeAt(history.lastIndex)
                    msg = if (cashed) "the pigeon saw a fry at ${"%.2f".format(crashAt)}x. you got out in time." else "the pigeon saw a fry at ${"%.2f".format(crashAt)}x. your ticket is gone."
                    if (!cashed) { fx.play(Sfx.LOSE); fx.buzz(Buzz.FAIL) }
                }
            }
        }
        // ticket falling animation
        val a = Animatable(0f)
        a.animateTo(1f, tween(900)) { fall = value }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            LotTopBar("pigeon flight", state.coins, { nav.back() }, { help = true })
            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                history.forEach {
                    Text("${"%.1f".format(it)}x", color = if (it >= 2f) Mint else Pink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f).padding(12.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val alt = (kotlin.math.ln(mult) / kotlin.math.ln(30f)).coerceIn(0f, 1f)
                    drawRoundRect(
                        Brush.verticalGradient(listOf(lerp(Sky, Color(0xFF0B0820), alt), lerp(Color(0xFFFFD6A5), Color(0xFF2A1F4E), alt))),
                        cornerRadius = CornerRadius(30f),
                    )
                    if (alt > 0.4f) for (i in 0 until 25) drawCircle(Color.White.copy(alpha = (alt - 0.4f) * 1.5f), 2.5f, Offset(w * ((i * 37) % 100) / 100f, h * ((i * 61) % 100) / 100f))
                    // clouds scroll down as you climb
                    for (i in 0 until 6) {
                        val cy = ((i * 0.22f + t * 0.12f) % 1.3f) * h - h * 0.15f
                        val cx = w * ((i * 0.37f) % 1f)
                        drawCircle(Color.White.copy(alpha = 0.7f * (1f - alt)), w * 0.08f, Offset(cx, cy))
                        drawCircle(Color.White.copy(alpha = 0.7f * (1f - alt)), w * 0.06f, Offset(cx + w * 0.08f, cy + 6f))
                    }
                    // ground at the start
                    val groundY = h * 0.9f + t * h * 0.25f
                    if (groundY < h * 1.2f) drawRect(Color(0xFF4CAF50), Offset(0f, groundY), Size(w, h))
                    val pc = Offset(w / 2 + sin(t * 2f) * w * 0.05f, h * 0.48f)
                    drawPigeon(pc, w * 0.18f, abs(sin(t * 14f)), 1f)
                    if (crashed) {
                        // the fry
                        rotate(20f, pc + Offset(w * 0.2f, -h * 0.05f)) {
                            drawRoundRect(Gold, pc + Offset(w * 0.18f, -h * 0.09f), Size(w * 0.03f, w * 0.12f), CornerRadius(4f))
                        }
                        label("!", pc.x + w * 0.08f, pc.y - w * 0.15f, w * 0.1f, Pink, Ink)
                    }
                    val tp = pc + Offset(0f, w * 0.12f + if (crashed) fall * h * 0.6f else 0f)
                    rotate(if (crashed) fall * 200f else 0f, tp) {
                        drawRoundRect(Color(0xFFFFF3D6), tp - Offset(w * 0.05f, w * 0.03f), Size(w * 0.1f, w * 0.06f), CornerRadius(4f))
                        drawRect(Pink, tp - Offset(w * 0.05f, w * 0.03f), Size(w * 0.1f, w * 0.015f))
                    }
                    label("${"%.2f".format(mult)}x", w / 2, h * 0.16f, h * 0.1f, if (crashed && !cashed) Pink else Gold, Ink)
                }
            }
            Text(msg, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp))
            if (flying) {
                ChunkyButton(
                    if (cashed) "cashed out!" else "CASH OUT ${fmt((bet * mult).toInt())}c", Modifier.fillMaxWidth().padding(12.dp),
                    color = Mint, height = 70.dp, enabled = !cashed, sfx = Sfx.BIG_WIN,
                ) {
                    val win = (bet * mult).toInt()
                    val m = (mult * 100).toInt()
                    cashed = true
                    vm.update { it.copy(coins = it.coins + win, flightBest = maxOf(it.flightBest, m)) }
                    msg = "cashed out at ${"%.2f".format(mult)}x: +${fmt(win)}c"
                    fx.buzz(Buzz.SUCCESS)
                    confetti++
                }
            } else {
                Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 50, 100, 500).forEach { b ->
                        ChunkyButton(
                            "${b}c", Modifier.weight(1f), color = if (b == bet) Sky else CardHi,
                            textColor = if (b == bet) Ink else Color.White, height = 50.dp, enabled = state.coins >= b,
                        ) { bet = b }
                    }
                }
                ChunkyButton("launch (${bet}c)", Modifier.fillMaxWidth().padding(12.dp), color = Gold, enabled = state.coins >= bet, sfx = Sfx.WHOOSH, height = 60.dp) {
                    vm.update { it.copy(coins = it.coins - bet, flightPlays = it.flightPlays + 1) }
                    cashed = false
                    flying = true
                    msg = "it's climbing..."
                    flight++
                }
            }
        }
        Confetti(confetti)
    }
}

// ================================ ticket tower ================================

private val towerTutorial = listOf(
    "stack tickets as high as you can. a ticket slides back and forth on top of the tower.",
    "tap to drop it. whatever hangs over the edge gets cut off, so the tickets get smaller.",
    "perfect drops keep the full size. miss completely and the tower is done. free to play, you earn coins per floor.",
)

private class Block(val left: Float, val width: Float, val color: Color)

@Composable
fun TowerScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    var help by remember { mutableStateOf(false) }
    ModeTutorial(vm, state, "tower", "ticket tower", towerTutorial, help) { help = false }
    val paused by rememberUpdatedState(help || "tower" !in state.tutorialsSeen)

    var game by remember { mutableIntStateOf(0) }
    val blocks = remember(game) { mutableStateListOf(Block(0.3f, 0.4f, Pink)) }
    var x by remember(game) { mutableFloatStateOf(0f) }
    var dir by remember(game) { mutableFloatStateOf(1f) }
    var over by remember(game) { mutableStateOf(false) }
    var perfects by remember(game) { mutableIntStateOf(0) }
    var cut by remember(game) { mutableStateOf<Pair<Block, Int>?>(null) }
    var cutFall by remember { mutableFloatStateOf(0f) }
    var cam by remember(game) { mutableFloatStateOf(0f) }
    var frame by remember { mutableLongStateOf(0L) }
    var msg by remember(game) { mutableStateOf("") }
    val height = blocks.size - 1

    fun finish() {
        over = true
        val coins = height * 4 + perfects * 5
        vm.update { it.copy(coins = it.coins + coins, towerPlays = it.towerPlays + 1, towerBest = maxOf(it.towerBest, height)) }
        msg = "tower of $height · +${coins}c"
        fx.play(if (height > 0) Sfx.WIN else Sfx.LOSE); fx.buzz(Buzz.FAIL)
    }

    LaunchedEffect(game) {
        if (game == 0) return@LaunchedEffect
        var last = 0L
        while (!over) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                frame++
                if (paused) return@withFrameNanos
                val top = blocks.last()
                val speed = 0.45f + blocks.size * 0.03f
                x += dir * speed * dt
                if (x + top.width > 1f) { x = 1f - top.width; dir = -1f }
                if (x < 0f) { x = 0f; dir = 1f }
                cutFall += dt * 1.5f
                cam += ((blocks.size - 6).coerceAtLeast(0) - cam) * dt * 4f
            }
        }
    }

    fun dropBlock() {
        if (over || game == 0) return
        val top = blocks.last()
        val l = maxOf(x, top.left)
        val r = minOf(x + top.width, top.left + top.width)
        val color = ballColors[blocks.size % ballColors.size]
        if (r - l <= 0.005f) {
            cut = Block(x, top.width, color) to blocks.size
            cutFall = 0f
            finish()
            return
        }
        if (abs(x - top.left) < 0.015f) {
            blocks.add(Block(top.left, top.width, color))
            perfects++
            msg = "PERFECT!"
            fx.play(Sfx.COIN); fx.buzz(Buzz.MEDIUM)
        } else {
            val offL = if (x < top.left) Block(x, top.left - x, color) else Block(top.left + top.width, x + top.width - (top.left + top.width), color)
            cut = offL to blocks.size
            cutFall = 0f
            blocks.add(Block(l, r - l, color))
            msg = ""
            fx.play(Sfx.BALL); fx.buzz(Buzz.LIGHT)
        }
        x = if (dir > 0) 0f else 1f - blocks.last().width
    }

    Column(Modifier.fillMaxSize()) {
        LotTopBar("ticket tower", state.coins, { nav.back() }, { help = true })
        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("floors", "$height", Modifier.weight(1f), Gold)
            StatBox("perfect", "$perfects", Modifier.weight(1f), Mint)
            StatBox("best", "${state.towerBest}", Modifier.weight(1f))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp)
                .clickable(remember { MutableInteractionSource() }, null) { dropBlock() }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                @Suppress("UNUSED_EXPRESSION") frame
                val w = size.width
                val h = size.height
                val bh = h * 0.07f
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF241B4A), Color(0xFF6E5A9E))), cornerRadius = CornerRadius(30f))
                fun yOf(i: Int) = h * 0.95f - (i + 1 - cam) * bh
                fun drawB(b: Block, y: Float) {
                    drawRoundRect(b.color, Offset(b.left * w, y), Size(b.width * w, bh * 0.92f), CornerRadius(6f))
                    drawRoundRect(Ink, Offset(b.left * w, y), Size(b.width * w, bh * 0.92f), CornerRadius(6f), style = Stroke(4f))
                    if (b.width > 0.12f) label("lot", b.left * w + b.width * w / 2, y + bh * 0.46f, bh * 0.5f, Ink.copy(alpha = 0.6f))
                }
                blocks.forEachIndexed { i, b -> drawB(b, yOf(i)) }
                cut?.let { (b, i) -> if (cutFall < 1.5f) drawB(b, yOf(i) + cutFall * cutFall * h * 0.5f) }
                if (!over && game > 0) drawB(Block(x, blocks.last().width, ballColors[blocks.size % ballColors.size]), yOf(blocks.size))
                if (msg.isNotEmpty()) label(msg, w / 2, h * 0.1f, h * 0.05f, Gold, Ink)
            }
            if (game == 0 || over) {
                Column(Modifier.align(Alignment.Center).padding(20.dp)) {
                    Panel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (over) "the tower is done" else "ticket tower", color = Gold, fontWeight = FontWeight.Black, fontSize = 24.sp)
                            Text(if (over) msg else "free to play. tap anywhere to drop.", color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(12.dp))
                            ChunkyButton(if (over) "build another" else "start", Modifier.fillMaxWidth(), color = Mint) { game++ }
                        }
                    }
                }
            }
        }
    }
}

// ================================ profile + wardrobe ================================

@Composable
fun ProfileScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    var clock by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) withFrameNanos { now -> if (last != 0L) clock += (now - last) / 1e9f; last = now }
    }
    val me = youDude(state)
    Column(Modifier.fillMaxSize()) {
        LotTopBar("profile", state.coins, { nav.back() })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Panel(Modifier.fillMaxWidth(), color = CardHi) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Canvas(Modifier.size(120.dp)) {
                            drawCircle(Gold.copy(alpha = 0.15f), size.minDimension / 2)
                            drawDude(me, size.width / 2, size.height * 0.98f, size.height * 0.85f, Mood.HAPPY, wobble = sin(clock * 2f) * 0.3f, t = clock)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("you", color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp)
                            Text("rank: ${rankOf(state.badges.size)}", color = modeColor("campaign"), fontWeight = FontWeight.Bold)
                            Text("${state.badges.size}/${Badges.size} badges · ${state.items.size}/${Items.size} items", color = Dim, fontSize = 13.sp)
                            Text("campaign ${state.campaignLevel.coerceAtMost(Campaign.size)}/${Campaign.size}", color = Dim, fontSize = 13.sp)
                        }
                    }
                }
            }
            item {
                ShareButton(
                    vm, state, "outfit", "${state.nickname.ifBlank { "my" }}'s fit",
                    org.json.JSONObject().put("hat", state.outfitHat).put("shirt", state.outfitShirt).put("glasses", state.outfitGlasses),
                    wide = true,
                )
            }
            listOf("hat" to "hats", "shirt" to "shirts", "glasses" to "extras").forEach { (kind, title) ->
                item { Text(title, color = Gold, fontWeight = FontWeight.Black, fontSize = 18.sp) }
                Cosmetics.filter { it.kind == kind }.chunked(2).forEach { row ->
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { c ->
                                val owned = c.id in state.wardrobe
                                val wearing = when (kind) {
                                    "hat" -> state.outfitHat == c.id
                                    "shirt" -> state.outfitShirt == c.id
                                    else -> state.outfitGlasses
                                }
                                Panel(Modifier.weight(1f), color = if (wearing) CardHi else Card) {
                                    Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Canvas(Modifier.size(64.dp)) {
                                            val d = when (kind) {
                                                "hat" -> me.copy(hat = c.value.toInt(), hatColor = if (c.value.toInt() == 7) Gold else Pink)
                                                "shirt" -> me.copy(shirt = Color(c.value))
                                                else -> me.copy(glasses = true)
                                            }
                                            drawDude(d, size.width / 2, size.height * 1.05f, size.height * 0.75f, Mood.HAPPY, t = clock)
                                        }
                                        Text(c.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
                                        Spacer(Modifier.height(6.dp))
                                        when {
                                            owned -> ChunkyButton(
                                                if (kind == "glasses") (if (wearing) "take off" else "wear") else if (wearing) "wearing" else "wear",
                                                Modifier.fillMaxWidth(), color = if (wearing) Mint else Sky, height = 40.dp,
                                                enabled = kind == "glasses" || !wearing,
                                            ) { vm.wear(c) }
                                            c.price > 0 -> ChunkyButton("${c.price}c", Modifier.fillMaxWidth(), height = 40.dp, enabled = state.coins >= c.price, sfx = null) {
                                                if (vm.buyCosmetic(c)) { fx.play(Sfx.BUY); fx.buzz(Buzz.SUCCESS) }
                                            }
                                            else -> Text("claw machine only", color = Pink, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.height(40.dp).padding(top = 10.dp))
                                        }
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            item { Text("stats", color = Gold, fontWeight = FontWeight.Black, fontSize = 18.sp) }
            val stats = listOf(
                "tickets bought" to state.ticketsBought, "tickets won" to state.wins, "scratched" to state.scratched,
                "tv draws" to state.tvPlayed, "biggest win" to state.biggestWin, "prizes claimed the hard way" to state.claims,
                "host days" to state.hostDays, "customers served" to state.served, "times yelled" to state.yells,
                "tickets stolen" to state.steals, "times caught" to state.timesCaught, "best stealer round" to state.bestRound,
                "bingo wins" to state.bingoWins, "trash bags searched" to state.bagsSearched, "pigeon race wins" to state.derbyWins,
                "slot spins" to state.slotSpins, "tickets inspected" to state.inspected, "best ticket rain" to state.rainBest,
                "best hi-lo streak" to state.hiloBestStreak, "claw prizes" to state.clawWins, "best flight (x100)" to state.flightBest,
                "tallest tower" to state.towerBest, "daily quests done" to state.questsCompleted,
            )
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        stats.forEach { (k, v) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text(k, color = Dim, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text(fmt(v), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
