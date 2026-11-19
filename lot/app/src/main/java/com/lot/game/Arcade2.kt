package com.lot.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// ================================ plinko ================================

private val plinkoMults = listOf(13f, 3f, 1.3f, 0.7f, 0.3f, 0.7f, 1.3f, 3f, 13f)
private const val PLINKO_ROWS = 8

private class PBall(val moves: List<Int>, val bet: Int) {
    var t = 0f
    var landed = false
    val slot get() = moves.sum()
}

private val plinkoTutorial = listOf(
    "plinko: drop a ball, it bounces left or right off every peg and lands in a slot.",
    "the slot's number multiplies your bet. the edges pay 13x, the middle pays almost nothing. physics is rigged like that.",
    "you can drop a bunch of balls at once. go wild.",
)

@Composable
fun PlinkoScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    var help by remember { mutableStateOf(false) }
    ModeTutorial(vm, state, "plinko", "plinko", plinkoTutorial, help) { help = false }
    val paused by rememberUpdatedState(help || "plinko" !in state.tutorialsSeen)
    var bet by remember { mutableIntStateOf(10) }
    val balls = remember { mutableStateListOf<PBall>() }
    var frame by remember { mutableLongStateOf(0L) }
    var flash by remember { mutableIntStateOf(-1) }
    var flashT by remember { mutableFloatStateOf(0f) }
    var last by remember { mutableStateOf("drop a ball!") }
    var session by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        var prev = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (prev == 0L) 0f else ((now - prev) / 1e9f).coerceAtMost(0.05f)
                prev = now
                frame++
                flashT = (flashT - dt).coerceAtLeast(0f)
                if (paused) return@withFrameNanos
                val done = mutableListOf<PBall>()
                balls.forEach { b ->
                    val before = b.t.toInt()
                    b.t += dt * 3.2f
                    if (b.t.toInt() != before && b.t < PLINKO_ROWS) fx.play(Sfx.CLICK)
                    if (b.t >= PLINKO_ROWS + 0.6f && !b.landed) {
                        b.landed = true
                        val m = plinkoMults[b.slot]
                        val win = (b.bet * m).toInt()
                        session += win - b.bet
                        flash = b.slot; flashT = 0.6f
                        last = "${"%.1f".format(m)}x · ${if (win > 0) "+${fmt(win)}c" else "nothing"}"
                        vm.update {
                            it.copy(
                                coins = it.coins + win, plinkoDrops = it.plinkoDrops + 1,
                                flags = if (m >= 13f) it.flags + "plinkoedge" else it.flags,
                            )
                        }
                        if (m >= 3f) { fx.play(Sfx.BIG_WIN); fx.buzz(Buzz.SUCCESS) } else if (m >= 1f) fx.play(Sfx.COIN) else fx.play(Sfx.BALL)
                        done.add(b)
                    }
                }
                balls.removeAll(done)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        LotTopBar("plinko", state.coins, { nav.back() }, { help = true })
        Text(
            last, color = Gold, fontWeight = FontWeight.Black, fontSize = 18.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "this session: ${if (session >= 0) "+" else ""}${fmt(session)}c", color = if (session >= 0) Mint else Pink,
            fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
            Canvas(Modifier.fillMaxSize().sceneClip()) {
                @Suppress("UNUSED_EXPRESSION") frame
                val w = size.width
                val h = size.height
                drawRect(Brush.verticalGradient(listOf(Color(0xFF2A2148), Color(0xFF3B2B6E))))
                val sp = w / 10f
                val top = h * 0.07f
                val rowH = (h * 0.82f) / (PLINKO_ROWS + 1)
                val cx = w / 2
                fun pos(row: Int, k: Int) = Offset(cx + (k - row / 2f) * sp, top + row * rowH)
                // pegs
                for (r in 0 until PLINKO_ROWS) for (j in 0..r) {
                    val p = pos(r, j) + Offset(0f, rowH * 0.45f)
                    drawCircle(Color.White.copy(alpha = 0.85f), sp * 0.09f, p)
                }
                // slots
                val sy = top + PLINKO_ROWS * rowH + rowH * 0.3f
                plinkoMults.forEachIndexed { k, m ->
                    val x = cx + (k - PLINKO_ROWS / 2f) * sp
                    val col = when {
                        m >= 13f -> Pink
                        m >= 3f -> Color(0xFFFF8C42)
                        m >= 1f -> Gold
                        else -> Sky
                    }
                    val lit = flash == k && flashT > 0f
                    drawRoundRect(if (lit) Color.White else col, Offset(x - sp * 0.46f, sy), Size(sp * 0.92f, rowH * 1.1f), CornerRadius(10f))
                    drawRoundRect(Ink, Offset(x - sp * 0.46f, sy), Size(sp * 0.92f, rowH * 1.1f), CornerRadius(10f), style = Stroke(3f))
                    label(if (m % 1f == 0f) "${m.toInt()}x" else "${m}x", x, sy + rowH * 0.55f, sp * 0.3f, Ink)
                }
                // balls
                balls.forEach { b ->
                    val r = b.t.toInt().coerceAtMost(PLINKO_ROWS)
                    val frac = (b.t - r).coerceIn(0f, 1f)
                    val k = b.moves.take(r).sum()
                    val a = pos(r, k)
                    val next = if (r < PLINKO_ROWS) pos(r + 1, k + b.moves[r]) else a + Offset(0f, rowH * 0.6f)
                    val x = a.x + (next.x - a.x) * frac
                    val y = a.y + (next.y - a.y) * frac - sin(frac * PI.toFloat()) * rowH * 0.35f
                    circleO(Offset(x, y), sp * 0.17f, Gold, 3f)
                }
            }
        }
        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 20, 50, 100).forEach { b ->
                ChunkyButton("${b}c", Modifier.weight(1f), color = if (b == bet) Sky else CardHi, textColor = if (b == bet) Ink else Color.White, height = 46.dp) { bet = b }
            }
        }
        ChunkyButton(
            "DROP (${bet}c)", Modifier.fillMaxWidth().padding(12.dp), color = Gold, height = 60.dp,
            enabled = state.coins >= bet && balls.size < 12, sfx = Sfx.WHOOSH,
        ) {
            vm.update { it.copy(coins = it.coins - bet) }
            balls.add(PBall(List(PLINKO_ROWS) { Random.nextInt(2) }, bet))
        }
    }
}

// ================================ memory match ================================

private val memoryTutorial = listOf(
    "memory match: 16 cards, 8 pairs of lottery symbols. flip two at a time.",
    "match them all before the 60 seconds run out. faster = more coins.",
    "matching the skulls costs you 10 seconds. they're still a pair though.",
)

@Composable
fun MemoryScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    val scope = rememberCoroutineScope()
    var help by remember { mutableStateOf(false) }
    ModeTutorial(vm, state, "memory", "memory match", memoryTutorial, help) { help = false }
    val paused by rememberUpdatedState(help || "memory" !in state.tutorialsSeen)

    var round by remember { mutableIntStateOf(0) }
    val cards = remember(round) { (Sym.entries + Sym.entries).shuffled() }
    val up = remember(round) { mutableStateListOf<Int>() }
    val matched = remember(round) { mutableStateListOf<Int>() }
    var timeLeft by remember(round) { mutableFloatStateOf(60f) }
    var mistakes by remember(round) { mutableIntStateOf(0) }
    var lock by remember(round) { mutableStateOf(false) }
    var over by remember(round) { mutableStateOf(false) }
    var result by remember(round) { mutableStateOf("") }

    fun finish(cleared: Boolean) {
        if (over) return
        over = true
        val pairs = matched.size / 2
        val score = pairs * 20 + if (cleared) (timeLeft * 3).toInt() else 0
        vm.update {
            it.copy(
                coins = it.coins + score, memoryPlays = it.memoryPlays + 1, memoryBest = maxOf(it.memoryBest, score),
                flags = if (cleared && mistakes == 0) it.flags + "memoryperfect" else it.flags,
            )
        }
        result = if (cleared) "cleared with ${timeLeft.toInt()}s left! +${score}c" else "time's up. $pairs pairs. +${score}c"
        fx.play(if (cleared) Sfx.BIG_WIN else Sfx.LOSE)
    }

    LaunchedEffect(round) {
        if (round == 0) return@LaunchedEffect
        var prev = 0L
        while (!over) {
            withFrameNanos { now ->
                val dt = if (prev == 0L) 0f else ((now - prev) / 1e9f).coerceAtMost(0.05f)
                prev = now
                if (paused || over) return@withFrameNanos
                timeLeft -= dt
                if (timeLeft <= 0f) { timeLeft = 0f; finish(false) }
            }
        }
    }

    fun tap(i: Int) {
        if (lock || over || round == 0 || i in up || i in matched) return
        up.add(i)
        fx.play(Sfx.CLICK); fx.buzz(Buzz.TICK)
        if (up.size == 2) {
            val (a, b) = up[0] to up[1]
            if (cards[a] == cards[b]) {
                matched.addAll(listOf(a, b)); up.clear()
                if (cards[a] == Sym.SKULL) { timeLeft = (timeLeft - 10f).coerceAtLeast(0.1f); fx.play(Sfx.ANGRY) } else fx.play(Sfx.COIN)
                if (matched.size == cards.size) finish(true)
            } else {
                mistakes++
                lock = true
                scope.launch { delay(700); up.clear(); lock = false }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        LotTopBar("memory match", state.coins, { nav.back() }, { help = true })
        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("time", "${timeLeft.toInt()}", Modifier.weight(1f), if (timeLeft < 10f) Pink else Color.White)
            StatBox("pairs", "${matched.size / 2}/8", Modifier.weight(1f), Mint)
            StatBox("mistakes", "$mistakes", Modifier.weight(1f), Pink)
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (r in 0 until 4) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (c in 0 until 4) {
                            val i = r * 4 + c
                            val faceUp = round > 0 && (i in up || i in matched)
                            val flip by animateFloatAsState(if (faceUp) 180f else 0f, tween(260), label = "flip")
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(0.8f)
                                    .graphicsLayer { rotationY = flip; cameraDistance = 12f * density }
                                    .background(
                                        if (flip > 90f) (if (i in matched) Color(0xFFDDF7E9) else Color.White) else Grape,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .border(2.5.dp, Ink, RoundedCornerShape(12.dp))
                                    .clickable { tap(i) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Canvas(Modifier.fillMaxSize().graphicsLayer { rotationY = if (flip > 90f) 180f else 0f }) {
                                    if (flip > 90f) drawSymbol(cards[i], center, size.minDimension * 0.6f)
                                    else label("?", center.x, center.y, size.minDimension * 0.4f, Color.White.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }
        if (round == 0 || over) {
            if (result.isNotEmpty()) Text(result, color = Gold, fontWeight = FontWeight.Black, fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            ChunkyButton(if (round == 0) "start (free)" else "again", Modifier.fillMaxWidth().padding(12.dp), color = Mint, height = 58.dp) { round++ }
        } else {
            Spacer(Modifier.height(82.dp))
        }
    }
}

// ================================ whack-a-raccoon ================================

private val whackTutorial = listOf(
    "raccoons are invading the lot stand trash cans. whack them.",
    "golden raccoons are worth 5. grannies also pop out sometimes. do NOT whack the grannies.",
    "40 seconds. it gets faster. your score turns into coins.",
)

private class Hole { var who = 0; var until = 0f; var since = 0f; var hit = false } // who: 0 none, 1 raccoon, 2 golden, 3 granny

private fun DrawScope.drawCan(c: Offset, w: Float, h: Float) {
    drawRoundRect(Color(0xFF6E6A85), Offset(c.x - w / 2, c.y), Size(w, h), CornerRadius(10f))
    for (i in 1..3) drawLine(Ink.copy(alpha = 0.3f), Offset(c.x - w / 2 + i * w / 4, c.y + 6f), Offset(c.x - w / 2 + i * w / 4, c.y + h - 6f), 4f)
    drawRoundRect(Ink, Offset(c.x - w / 2, c.y), Size(w, h), CornerRadius(10f), style = Stroke(4f))
    drawOval(Color(0xFF1B1530), Offset(c.x - w * 0.55f, c.y - h * 0.12f), Size(w * 1.1f, h * 0.24f))
    drawOval(Ink, Offset(c.x - w * 0.55f, c.y - h * 0.12f), Size(w * 1.1f, h * 0.24f), style = Stroke(4f))
}

@Composable
fun WhackScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    var help by remember { mutableStateOf(false) }
    ModeTutorial(vm, state, "whack", "whack-a-raccoon", whackTutorial, help) { help = false }
    val paused by rememberUpdatedState(help || "whack" !in state.tutorialsSeen)

    var round by remember { mutableIntStateOf(0) }
    val holes = remember(round) { List(9) { Hole() } }
    var score by remember(round) { mutableIntStateOf(0) }
    var timeLeft by remember(round) { mutableFloatStateOf(40f) }
    var clock by remember(round) { mutableFloatStateOf(0f) }
    var over by remember(round) { mutableStateOf(false) }
    var pop by remember { mutableStateOf("") }
    var popAt by remember { mutableFloatStateOf(-9f) }

    fun finish() {
        if (over) return
        over = true
        val s = score.coerceAtLeast(0)
        vm.update { it.copy(coins = it.coins + s * 3, whackPlays = it.whackPlays + 1, whackBest = maxOf(it.whackBest, s)) }
        fx.play(if (s > 0) Sfx.WIN else Sfx.LOSE)
    }

    LaunchedEffect(round) {
        if (round == 0) return@LaunchedEffect
        var prev = 0L
        var spawn = 0.6f
        while (!over) {
            withFrameNanos { now ->
                val dt = if (prev == 0L) 0f else ((now - prev) / 1e9f).coerceAtMost(0.05f)
                prev = now
                if (paused || over) return@withFrameNanos
                clock += dt
                timeLeft -= dt
                spawn -= dt
                val elapsed = 40f - timeLeft
                if (spawn <= 0f) {
                    spawn = (0.85f - elapsed * 0.012f).coerceAtLeast(0.35f)
                    val free = holes.filter { it.who == 0 }
                    if (free.isNotEmpty()) {
                        val hl = free.random()
                        val roll = Random.nextFloat()
                        hl.who = when { roll < 0.08f -> 2; roll < 0.28f -> 3; else -> 1 }
                        hl.since = clock
                        hl.until = clock + (1.15f - elapsed * 0.012f).coerceAtLeast(0.55f)
                        hl.hit = false
                    }
                }
                holes.forEach { if (it.who != 0 && clock > it.until) it.who = 0 }
                if (timeLeft <= 0f) { timeLeft = 0f; finish() }
            }
        }
    }

    fun whack(i: Int) {
        val hl = holes[i]
        if (over || round == 0 || hl.who == 0 || hl.hit) return
        hl.hit = true
        hl.until = clock + 0.25f
        when (hl.who) {
            1 -> { score += 1; pop = "+1"; fx.play(Sfx.CLICK); fx.buzz(Buzz.LIGHT) }
            2 -> { score += 5; pop = "+5!"; fx.play(Sfx.COIN); fx.buzz(Buzz.MEDIUM) }
            else -> {
                score -= 3; pop = "OW! -3"
                fx.play(Sfx.ANGRY); fx.buzz(Buzz.HEAVY)
                vm.flag("whackgranny")
            }
        }
        popAt = clock
    }

    Column(Modifier.fillMaxSize()) {
        LotTopBar("whack-a-raccoon", state.coins, { nav.back() }, { help = true })
        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("score", "$score", Modifier.weight(1f), Gold)
            StatBox("time", "${timeLeft.toInt()}", Modifier.weight(1f), if (timeLeft < 10f) Pink else Color.White)
            StatBox("best", "${state.whackBest}", Modifier.weight(1f))
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .sceneClip()
                    .pointerInput(round) {
                        detectTapGestures { p ->
                            val col = (p.x / (size.width / 3f)).toInt().coerceIn(0, 2)
                            val row = (p.y / (size.height / 3f)).toInt().coerceIn(0, 2)
                            whack(row * 3 + col)
                        }
                    }
            ) {
                @Suppress("UNUSED_EXPRESSION") clock
                val w = size.width
                val h = size.height
                drawRect(Brush.verticalGradient(listOf(Color(0xFF3B2B6E), Color(0xFF2A2240))))
                val cw = w / 3f
                val ch = h / 3f
                for (i in 0 until 9) {
                    val cx = cw * (i % 3) + cw / 2
                    val cy = ch * (i / 3) + ch * 0.58f
                    val hl = holes[i]
                    val canW = cw * 0.62f
                    val canH = ch * 0.34f
                    if (hl.who != 0) {
                        val rise = ((clock - hl.since) / 0.15f).coerceIn(0f, 1f) * ((hl.until - clock) / 0.15f).coerceIn(0f, 1f)
                        clipRect(cx - cw / 2, cy - ch, cx + cw / 2, cy + 4f) {
                            val baseY = cy + ch * 0.5f - rise * ch * 0.55f
                            when (hl.who) {
                                3 -> drawDude(grannyDude(i), cx, baseY + ch * 0.25f, ch * 0.75f, if (hl.hit) Mood.ANGRY else Mood.GRUMPY, t = clock)
                                else -> {
                                    if (hl.who == 2) drawCircle(Gold.copy(alpha = 0.5f), ch * 0.3f, Offset(cx, baseY - ch * 0.15f))
                                    drawRaccoon(Offset(cx + ch * 0.1f, baseY - ch * 0.1f), ch * 0.42f, clock, hl.hit)
                                }
                            }
                        }
                        if (hl.hit) label(if (hl.who == 3) "OW" else "BONK", cx, cy - ch * 0.42f, ch * 0.13f, if (hl.who == 3) Pink else Gold, Ink)
                    }
                    drawCan(Offset(cx, cy), canW, canH)
                }
                if (clock - popAt < 0.7f) label(pop, w / 2, h * 0.08f, h * 0.06f, if (pop.startsWith("OW")) Pink else Gold, Ink)
            }
            if (round == 0 || over) {
                Column(Modifier.align(Alignment.Center).padding(20.dp)) {
                    Panel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (over) "time's up!" else "whack-a-raccoon", color = Gold, fontWeight = FontWeight.Black, fontSize = 24.sp)
                            Text(
                                if (over) "score $score · +${score.coerceAtLeast(0) * 3}c" else "free to play. don't hit the grannies.",
                                color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(12.dp))
                            ChunkyButton(if (over) "again" else "start", Modifier.fillMaxWidth(), color = Mint) { round++ }
                        }
                    }
                }
            }
        }
    }
}
