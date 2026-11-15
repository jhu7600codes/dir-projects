package com.lot.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

// ================================ ticket rain ================================

private val rainTutorial = listOf(
    "a lottery truck crashed and it's raining tickets. this is the best day of your life.",
    "drag anywhere to move your basket. catch tickets and coins. golden tickets are worth a lot.",
    "bricks cost you a life (3 lives). skulls eat 25 points. catching stuff in a row builds a combo.",
    "you get your score in coins when the round ends. 45 seconds. go.",
)

private class Drop(var x: Float, var y: Float, val kind: Int, val value: Int, val speed: Float, val spin: Float)

private const val RAIN_TIME = 45f

@Composable
fun RainScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    var help by remember { mutableStateOf(false) }
    ModeTutorial(vm, state, "rain", "ticket rain", rainTutorial, help) { help = false }
    val paused by rememberUpdatedState(help || "rain" !in state.tutorialsSeen)

    var round by remember { mutableIntStateOf(0) }
    val drops = remember(round) { mutableListOf<Drop>() }
    var basket by remember { mutableFloatStateOf(0.5f) }
    var score by remember(round) { mutableIntStateOf(0) }
    var lives by remember(round) { mutableIntStateOf(3) }
    var streak by remember(round) { mutableIntStateOf(0) }
    var timeLeft by remember(round) { mutableFloatStateOf(RAIN_TIME) }
    var over by remember(round) { mutableStateOf(false) }
    var frame by remember { mutableLongStateOf(0L) }
    var shake by remember { mutableFloatStateOf(0f) }
    var popText by remember { mutableStateOf("") }
    var popAge by remember { mutableFloatStateOf(9f) }
    val combo = (1 + streak / 5).coerceAtMost(4)

    fun finish() {
        if (over) return
        over = true
        val s = score
        vm.update { it.copy(coins = it.coins + s, rainPlays = it.rainPlays + 1, rainBest = maxOf(it.rainBest, s)) }
        fx.play(if (s > 0) Sfx.WIN else Sfx.LOSE); fx.buzz(Buzz.SUCCESS)
    }

    LaunchedEffect(round) {
        if (round == 0) return@LaunchedEffect
        var last = 0L
        var spawn = 0.5f
        while (!over) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                frame++
                shake = (shake - dt * 3f).coerceAtLeast(0f)
                popAge += dt
                if (paused) return@withFrameNanos
                val elapsed = RAIN_TIME - timeLeft
                timeLeft -= dt
                spawn -= dt
                if (spawn <= 0f) {
                    spawn = (0.75f - elapsed * 0.011f).coerceAtLeast(0.26f)
                    val roll = Random.nextFloat()
                    val kind = when {
                        roll < 0.44f -> 0
                        roll < 0.64f -> 2
                        roll < 0.68f -> 1
                        roll < 0.86f -> 3
                        else -> 4
                    }
                    val value = when (kind) { 0 -> 5 + Random.nextInt(3) * 5; 1 -> 100; 2 -> 3; else -> 0 }
                    drops.add(Drop(0.08f + Random.nextFloat() * 0.84f, -0.05f, kind, value, 0.32f + Random.nextFloat() * 0.18f + elapsed * 0.006f, Random.nextFloat() * 2f - 1f))
                }
                val it = drops.iterator()
                while (it.hasNext()) {
                    val d = it.next()
                    d.y += d.speed * dt
                    if (d.y > 0.83f && d.y < 0.92f && abs(d.x - basket) < 0.11f) {
                        when (d.kind) {
                            0, 1, 2 -> {
                                val gain = d.value * (1 + streak / 5).coerceAtMost(4)
                                score += gain; streak++
                                popText = "+$gain"; popAge = 0f
                                fx.play(if (d.kind == 1) Sfx.BIG_WIN else Sfx.COIN); fx.buzz(Buzz.TICK)
                            }
                            3 -> {
                                lives--; streak = 0; shake = 1f
                                popText = "OUCH"; popAge = 0f
                                fx.play(Sfx.ANGRY); fx.buzz(Buzz.HEAVY)
                            }
                            else -> {
                                score = (score - 25).coerceAtLeast(0); streak = 0
                                popText = "-25"; popAge = 0f
                                fx.play(Sfx.LOSE); fx.buzz(Buzz.MEDIUM)
                            }
                        }
                        it.remove()
                    } else if (d.y > 1.05f) {
                        if (d.kind == 0 || d.kind == 1) streak = 0
                        it.remove()
                    }
                }
                if (timeLeft <= 0f || lives <= 0) { timeLeft = timeLeft.coerceAtLeast(0f); finish() }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        LotTopBar("ticket rain", state.coins, { nav.back() }, { help = true })
        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("score", fmt(score), Modifier.weight(1f), Gold)
            StatBox("combo", "x$combo", Modifier.weight(1f), Mint)
            StatBox("lives", "♥".repeat(lives.coerceAtLeast(0)).ifEmpty { "-" }, Modifier.weight(1f), Pink)
            StatBox("time", "${timeLeft.toInt()}", Modifier.weight(1f))
        }
        Box(Modifier.fillMaxWidth().weight(1f).padding(12.dp)) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val e = awaitPointerEvent()
                                e.changes.firstOrNull()?.let { c ->
                                    basket = (c.position.x / size.width).coerceIn(0.08f, 0.92f)
                                    c.consume()
                                }
                            }
                        }
                    }
            ) {
                @Suppress("UNUSED_EXPRESSION") frame
                val w = size.width
                val h = size.height
                val sx = sin(frame * 1.7f) * 12f * shake
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF4A5A8A), Color(0xFF9AA9D6))), cornerRadius = CornerRadius(30f))
                // rain streaks
                for (i in 0 until 30) {
                    val rx = ((i * 97 + frame * 3) % 1000) / 1000f * w
                    val ry = ((i * 53 + frame * 9) % 1000) / 1000f * h
                    drawLine(Color.White.copy(alpha = 0.15f), Offset(rx, ry), Offset(rx - 6f, ry + 22f), 2f)
                }
                drawRect(Color(0xFF555070), Offset(0f, h * 0.93f), Size(w, h * 0.07f))
                val u = w * 0.07f
                drops.forEach { d ->
                    val c = Offset(d.x * w + sx, d.y * h)
                    rotate(d.spin * (frame % 360).toFloat(), c) {
                        when (d.kind) {
                            0 -> {
                                drawRoundRect(Color(0xFFFFF3D6), c - Offset(u * 0.7f, u * 0.4f), Size(u * 1.4f, u * 0.8f), CornerRadius(6f))
                                drawRect(Pink, c - Offset(u * 0.7f, u * 0.4f), Size(u * 1.4f, u * 0.18f))
                                drawRoundRect(Ink, c - Offset(u * 0.7f, u * 0.4f), Size(u * 1.4f, u * 0.8f), CornerRadius(6f), style = Stroke(3f))
                                label("${d.value}", c.x, c.y + u * 0.08f, u * 0.4f, Ink)
                            }
                            1 -> {
                                drawCircle(Gold.copy(alpha = 0.4f), u * 1.1f, c)
                                drawRoundRect(Gold, c - Offset(u * 0.8f, u * 0.45f), Size(u * 1.6f, u * 0.9f), CornerRadius(6f))
                                drawRoundRect(Ink, c - Offset(u * 0.8f, u * 0.45f), Size(u * 1.6f, u * 0.9f), CornerRadius(6f), style = Stroke(3f))
                                outlined(starPath(c, u * 0.35f, u * 0.15f), Color.White, 2f)
                            }
                            2 -> drawCoin(c, u * 0.4f)
                            3 -> {
                                drawRect(Color(0xFFB23A48), c - Offset(u * 0.6f, u * 0.32f), Size(u * 1.2f, u * 0.64f))
                                drawRect(Ink, c - Offset(u * 0.6f, u * 0.32f), Size(u * 1.2f, u * 0.64f), style = Stroke(3f))
                                drawLine(Ink.copy(alpha = 0.5f), c - Offset(u * 0.6f, 0f), c + Offset(u * 0.6f, 0f), 2f)
                            }
                            else -> drawSymbol(Sym.SKULL, c, u * 1.1f)
                        }
                    }
                }
                // you + basket
                val bx = basket * w + sx
                drawDude(YouDude, bx, h * 0.98f, h * 0.2f, if (shake > 0f) Mood.ANGRY else Mood.HAPPY, look = 0f, t = frame / 60f)
                val bt = h * 0.86f
                outlined(Path().apply {
                    moveTo(bx - w * 0.12f, bt); lineTo(bx + w * 0.12f, bt); lineTo(bx + w * 0.09f, bt + h * 0.07f); lineTo(bx - w * 0.09f, bt + h * 0.07f); close()
                }, Color(0xFFB5651D), 5f)
                for (i in 1..3) drawLine(Ink.copy(alpha = 0.4f), Offset(bx - w * 0.12f + i * w * 0.06f, bt), Offset(bx - w * 0.1f + i * w * 0.05f, bt + h * 0.07f), 3f)
                if (popAge < 0.8f) label(popText, bx, bt - h * 0.06f - popAge * h * 0.08f, h * 0.04f, if (popText.startsWith("+")) Gold else Pink, Ink)
            }
            if (round == 0 || over) {
                Column(Modifier.align(Alignment.Center).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Panel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (over) "round over" else "ticket rain", color = Gold, fontWeight = FontWeight.Black, fontSize = 26.sp)
                            Text(
                                if (over) "you caught ${fmt(score)} coins worth of stuff" else "free to play. best: ${fmt(state.rainBest)}",
                                color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(12.dp))
                            ChunkyButton(if (over) "again" else "start", Modifier.fillMaxWidth(), color = Mint) {
                                drops.clear(); round++
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================================ hi-lo ================================

private val hiloTutorial = listOf(
    "hi-lo: a ball between 1 and 30 is showing. guess if the next one is higher or lower.",
    "every right guess multiplies your pot. riskier guesses multiply more.",
    "one wrong guess and the pot is gone. cash out whenever you get scared. you will get scared.",
)

@Composable
fun HiloScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    val scope = rememberCoroutineScope()
    var help by remember { mutableStateOf(false) }
    ModeTutorial(vm, state, "hilo", "hi-lo", hiloTutorial, help) { help = false }

    var bet by remember { mutableIntStateOf(10) }
    var pot by remember { mutableIntStateOf(0) }
    var ball by remember { mutableIntStateOf(15) }
    var streak by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("place a bet to start") }
    val history = remember { mutableStateListOf<Int>() }
    val pop = remember { Animatable(1f) }

    fun mult(higher: Boolean): Float {
        val p = if (higher) (30 - ball) / 30f else (ball - 1) / 30f
        return if (p <= 0f) 0f else (0.95f / p).coerceAtMost(15f)
    }

    fun guess(higher: Boolean) {
        val m = mult(higher)
        if (!playing || m <= 0f) return
        val next = Random.nextInt(1, 31)
        history.add(0, ball)
        if (history.size > 8) history.removeAt(history.lastIndex)
        val old = ball
        ball = next
        scope.launch { pop.snapTo(0.6f); pop.animateTo(1f, spring(0.4f, 600f)) }
        when {
            next == old -> { msg = "same number. nothing happens."; fx.play(Sfx.CLICK) }
            (next > old) == higher -> {
                pot = (pot * m).toInt().coerceAtLeast(pot + 1)
                streak++
                msg = "yes! pot is ${fmt(pot)}"
                fx.play(Sfx.COIN); fx.buzz(Buzz.LIGHT)
                vm.update { it.copy(hiloBestStreak = maxOf(it.hiloBestStreak, streak)) }
            }
            else -> {
                msg = "nope. ${fmt(pot)} coins, gone."
                pot = 0; playing = false; streak = 0
                fx.play(Sfx.LOSE); fx.buzz(Buzz.FAIL)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        LotTopBar("hi-lo", state.coins, { nav.back() }, { help = true })
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(if (playing) "pot: ${fmt(pot)}c" else " ", color = Gold, fontWeight = FontWeight.Black, fontSize = 30.sp)
            Text("streak $streak", color = Dim, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            Canvas(Modifier.size(180.dp).scale(pop.value)) { drawBall(center, size.minDimension * 0.45f, ball) }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                history.forEach { n -> Canvas(Modifier.size(30.dp)) { drawBall(center, size.minDimension * 0.45f, n) } }
            }
            Spacer(Modifier.height(10.dp))
            Text(msg, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center)
        }
        if (playing) {
            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val lo = mult(false)
                val hi = mult(true)
                ChunkyButton(if (lo > 0f) "lower x${"%.2f".format(lo)}" else "lower (impossible)", Modifier.weight(1f), color = Sky, enabled = lo > 0f, height = 64.dp) { guess(false) }
                ChunkyButton(if (hi > 0f) "higher x${"%.2f".format(hi)}" else "higher (impossible)", Modifier.weight(1f), color = Pink, enabled = hi > 0f, height = 64.dp) { guess(true) }
            }
            ChunkyButton("cash out ${fmt(pot)}c", Modifier.fillMaxWidth().padding(12.dp), color = Mint, enabled = streak > 0, sfx = Sfx.BIG_WIN) {
                val p = pot
                vm.update { it.copy(coins = it.coins + p, hiloBestCashout = maxOf(it.hiloBestCashout, p)) }
                msg = "cashed out ${fmt(p)}c. coward. (smart)"
                pot = 0; playing = false; streak = 0
                fx.buzz(Buzz.SUCCESS)
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
            ChunkyButton("play (${bet}c)", Modifier.fillMaxWidth().padding(12.dp), color = Gold, enabled = state.coins >= bet, sfx = Sfx.BUY) {
                vm.update { it.copy(coins = it.coins - bet) }
                pot = bet; streak = 0; playing = true
                ball = Random.nextInt(1, 31)
                history.clear()
                msg = "higher or lower?"
            }
        }
    }
}
