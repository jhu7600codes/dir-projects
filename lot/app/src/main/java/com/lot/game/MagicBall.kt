package com.lot.game

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private val yesLines = listOf(
    "yes, obviously", "the stars say yes", "100% (not legally binding)", "sure, why not", "yes, but don't tell mom",
    "the pigeons agree", "absolutely, go for it", "signs point to coins", "yes yes yes", "i'd bet on it. oh wait, you did",
)
private val maybeLines = listOf("ask me after lunch", "the vibes are unclear", "i'm on break", "shake me again later", "no comment")
private val noLines = listOf("lol no", "absolutely not", "the raccoon says no", "that's a no from me", "not in this economy")

private val ballTutorial = listOf(
    "the magic ball knows everything. probably. type a question if you want, it doesn't really read it.",
    "bet on what it answers: yes (half its answers, pays 1.9x), maybe or no (a quarter each, pays 3.8x).",
    "then SHAKE YOUR PHONE to place the bet and make it answer. shake hard and fast and it likes you a little more.",
    "keep shaking after it starts thinking and it gets annoyed. annoyed balls say no.",
)

private enum class BallPhase { SETUP, SHAKING, THINKING, ANSWER }

@Composable
fun MagicBallScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    val ctx = LocalContext.current
    var help by remember { mutableStateOf(false) }
    ModeTutorial(vm, state, "ball", "magic ball", ballTutorial, help) { help = false }
    val paused by rememberUpdatedState(help || "ball" !in state.tutorialsSeen)

    var question by remember { mutableStateOf("") }
    var bet by remember { mutableIntStateOf(10) }
    var pick by remember { mutableIntStateOf(0) } // 0 yes, 1 maybe, 2 no
    var phase by remember { mutableStateOf(BallPhase.SETUP) }
    var meter by remember { mutableFloatStateOf(0f) }
    var shake by remember { mutableFloatStateOf(0f) }
    var shakeTime by remember { mutableFloatStateOf(0f) }
    var thinkTime by remember { mutableFloatStateOf(0f) }
    var answer by remember { mutableStateOf("") }
    var answerKind by remember { mutableIntStateOf(-1) }
    var resultText by remember { mutableStateOf("") }
    var clock by remember { mutableFloatStateOf(0f) }
    var confetti by remember { mutableIntStateOf(0) }
    var power by remember { mutableStateOf(false) }
    var annoyed by remember { mutableStateOf(false) }
    val hasSensor = remember {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }
    val listening = phase == BallPhase.SHAKING || phase == BallPhase.THINKING

    DisposableEffect(listening) {
        if (!listening) return@DisposableEffect onDispose { }
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) ?: sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val linear = sensor?.type == Sensor.TYPE_LINEAR_ACCELERATION
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val (x, y, z) = Triple(e.values[0], e.values[1], e.values[2])
                val mag = sqrt(x * x + y * y + z * z) - if (linear) 0f else SensorManager.GRAVITY_EARTH
                shake = kotlin.math.abs(mag)
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm?.unregisterListener(listener) }
    }

    LaunchedEffect(Unit) {
        var last = 0L
        var tick = 0f
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                clock += dt
                if (paused) return@withFrameNanos
                if (phase == BallPhase.SHAKING) {
                    shakeTime += dt
                    if (shake > 4f) {
                        meter += (shake - 4f) * dt * 0.06f
                        tick += dt
                        if (tick > 0.12f) { tick = 0f; fx.buzz(Buzz.TICK); fx.play(Sfx.SCRATCH) }
                    }
                    shake *= 0.9f
                    if (meter >= 1f) {
                        meter = 1f
                        power = shakeTime < 1.6f
                        thinkTime = 0f
                        phase = BallPhase.THINKING
                    }
                } else if (phase == BallPhase.THINKING) {
                    thinkTime += dt
                    // short grace window so you can stop shaking once it starts thinking
                    if (thinkTime > 0.8f && shake > 18f) annoyed = true
                    shake *= 0.9f
                }
            }
        }
    }

    LaunchedEffect(phase) {
        if (phase != BallPhase.THINKING) return@LaunchedEffect
        fx.play(Sfx.DRUM)
        delay(1600)
        // pick the answer. a power shake nudges it towards your bet, annoying it forces a no
        val weights = floatArrayOf(0.5f, 0.25f, 0.25f)
        if (power) weights[pick] += 0.05f
        val kind = if (annoyed) 2 else {
            var r = Random.nextFloat() * weights.sum()
            var k = 0
            while (k < 2 && r >= weights[k]) { r -= weights[k]; k++ }
            k
        }
        answerKind = kind
        answer = when (kind) { 0 -> yesLines.random(); 1 -> maybeLines.random(); else -> noLines.random() }
        val win = kind == pick
        val pay = if (!win) 0 else (bet * if (pick == 0) 1.9f else 3.8f).toInt()
        vm.update {
            it.copy(
                coins = it.coins + pay, ballShakes = it.ballShakes + 1, ballWins = it.ballWins + if (win) 1 else 0,
                flags = it.flags + listOfNotNull(if (power) "powershake" else null, if (annoyed) "annoyed" else null),
            )
        }
        resultText = when {
            annoyed && win -> "you annoyed it into saying no. and you bet on no. genius. +${fmt(pay)}c"
            annoyed -> "you kept shaking. the ball got annoyed and said no."
            win -> "it said what you bet on! +${fmt(pay)}c"
            else -> "not what you bet on. the ball keeps your ${fmt(bet)}c"
        }
        if (win) { fx.play(Sfx.BIG_WIN); fx.buzz(Buzz.SUCCESS); confetti++ } else { fx.play(Sfx.LOSE); fx.buzz(Buzz.FAIL) }
        phase = BallPhase.ANSWER
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            LotTopBar("magic ball", state.coins, { nav.back() }, { help = true })
            Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxWidth(0.85f).aspectRatio(1f)) {
                    val wob = when (phase) {
                        BallPhase.SHAKING -> (shake.coerceAtMost(20f) / 20f) * 18f + meter * 6f
                        BallPhase.THINKING -> 4f
                        else -> 0f
                    }
                    val c = center + Offset(sin(clock * 47f) * wob, sin(clock * 39f) * wob)
                    val r = size.minDimension * 0.46f
                    drawCircle(Ink.copy(alpha = 0.5f), r, c + Offset(10f, 16f))
                    drawCircle(Brush.radialGradient(listOf(Color(0xFF4A4363), Color(0xFF111018)), c - Offset(r * 0.35f, r * 0.35f), r * 1.4f), r, c)
                    drawCircle(Color.White.copy(alpha = 0.25f), r * 0.18f, c - Offset(r * 0.45f, r * 0.5f))
                    if (phase == BallPhase.ANSWER) {
                        drawCircle(Color(0xFF0B1240), r * 0.5f, c)
                        val tri = Path().apply {
                            moveTo(c.x, c.y - r * 0.4f); lineTo(c.x + r * 0.42f, c.y + r * 0.3f); lineTo(c.x - r * 0.42f, c.y + r * 0.3f); close()
                        }
                        drawPath(tri, when (answerKind) { 0 -> Color(0xFF2E7DFF); 1 -> Grape; else -> Color(0xFF7A1F3D) })
                        val words = answer.split(" ")
                        val lines = mutableListOf<String>()
                        var cur = ""
                        words.forEach { wd -> if ((cur + " " + wd).trim().length > 11) { lines.add(cur.trim()); cur = wd } else cur = "$cur $wd" }
                        if (cur.isNotBlank()) lines.add(cur.trim())
                        lines.forEachIndexed { i, l -> label(l, c.x, c.y + (i - (lines.size - 1) / 2f) * r * 0.11f + r * 0.02f, r * 0.09f, Color.White) }
                    } else {
                        drawCircle(Color.White, r * 0.36f, c)
                        label(if (phase == BallPhase.THINKING) "..." else "8", c.x, c.y, r * 0.42f, Ink)
                    }
                }
                if (question.isNotBlank() && phase != BallPhase.SETUP) {
                    Text("\"${question.trim()}\"", color = Dim, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.TopCenter))
                }
            }
            when (phase) {
                BallPhase.SETUP, BallPhase.ANSWER -> Column(Modifier.padding(12.dp)) {
                    if (phase == BallPhase.ANSWER) {
                        Text(resultText, color = if (answerKind == pick) Gold else Pink, fontWeight = FontWeight.Black, fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        question, { question = it.take(60) }, Modifier.fillMaxWidth(), singleLine = true,
                        placeholder = { Text("ask a question (optional)", color = Dim) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Gold, unfocusedBorderColor = Dim, cursorColor = Gold),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("yes\n1.9x", "maybe\n3.8x", "no\n3.8x").forEachIndexed { i, l ->
                            ChunkyButton(l, Modifier.weight(1f), color = if (pick == i) listOf(Sky, Grape, Pink)[i] else CardHi, textColor = if (pick == i) Ink else Color.White, height = 58.dp) { pick = i }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 50, 100, 500).forEach { b ->
                            ChunkyButton("${b}c", Modifier.weight(1f), color = if (b == bet) Gold else CardHi, textColor = if (b == bet) Ink else Color.White, height = 44.dp, enabled = state.coins >= b) { bet = b }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    ChunkyButton("bet ${bet}c and shake!", Modifier.fillMaxWidth(), color = Mint, enabled = state.coins >= bet, height = 58.dp, sfx = Sfx.BUY) {
                        vm.update { it.copy(coins = it.coins - bet) }
                        meter = 0f; shakeTime = 0f; shake = 0f; power = false; annoyed = false; answerKind = -1
                        phase = BallPhase.SHAKING
                    }
                }
                BallPhase.SHAKING -> Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (hasSensor) "SHAKE YOUR PHONE!" else "no motion sensor, tap to shake", color = Gold, fontWeight = FontWeight.Black, fontSize = 24.sp)
                    Canvas(Modifier.fillMaxWidth().height(26.dp).padding(vertical = 4.dp)) {
                        drawRoundRect(CardHi, cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f))
                        drawRoundRect(Mint, size = androidx.compose.ui.geometry.Size(size.width * meter, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f))
                    }
                    ChunkyButton("can't shake? tap here", Modifier.fillMaxWidth(), color = CardHi, textColor = Color.White, height = 50.dp, sfx = Sfx.SCRATCH) {
                        meter = (meter + 0.07f).coerceAtMost(0.999f)
                        if (meter >= 0.99f) { power = false; phase = BallPhase.THINKING }
                    }
                }
                BallPhase.THINKING -> Text(
                    "the ball is thinking... stop shaking it", color = Dim, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp),
                )
            }
        }
        Confetti(confetti)
    }
}
