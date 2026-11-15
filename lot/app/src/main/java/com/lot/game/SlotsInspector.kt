package com.lot.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.random.Random

// ================================ slots ================================

private val slotsTutorial = listOf(
    "lucky lot slots. pull the lever (spin), then stop each reel yourself with its stop button.",
    "reels drift a little after you tap, so it's mostly luck. mostly.",
    "3 of a kind pays: 7s 50x, clovers 20x, stars 10x, bells 6x, cherries 4x, socks 2x. any 2 cherries gives your bet back.",
    "3 skulls takes your bet AGAIN. the machine is evil. that's the brand.",
)

private val slotPays = mapOf(
    Sym.SEVEN to 50, Sym.CLOVER to 20, Sym.STAR to 10, Sym.BELL to 6, Sym.CHERRY to 4, Sym.SOCK to 2,
)

private fun reelStrip(seed: Int): List<Sym> {
    val base = List(5) { Sym.SOCK } + List(5) { Sym.CHERRY } + List(4) { Sym.BELL } + List(3) { Sym.STAR } +
        List(2) { Sym.CLOVER } + List(1) { Sym.SEVEN } + List(2) { Sym.SKULL }
    return base.shuffled(Random(seed))
}

@Composable
fun SlotsScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    var help by remember { mutableStateOf(false) }
    ModeTutorial(vm, state, "slots", "slots", slotsTutorial, help) { help = false }
    val paused by rememberUpdatedState(help || "slots" !in state.tutorialsSeen)
    val magnet by rememberUpdatedState("magnet" in state.items)

    val strips = remember { List(3) { reelStrip(it * 31 + 7) } }
    val pos = remember { FloatArray(3) { Random.nextInt(22).toFloat() } }
    val spinning = remember { BooleanArray(3) }
    val target = remember { FloatArray(3) { -1f } }
    var spinTime by remember { mutableFloatStateOf(0f) }
    var active by remember { mutableStateOf(false) }
    var bet by remember { mutableIntStateOf(5) }
    var result by remember { mutableStateOf("pull the lever") }
    var resultGood by remember { mutableStateOf(false) }
    var frame by remember { mutableLongStateOf(0L) }
    var lever by remember { mutableFloatStateOf(0f) }
    var confetti by remember { mutableIntStateOf(0) }
    val n = strips[0].size

    fun symAt(r: Int) = strips[r][(((Math.round(pos[r]) % n) + n) % n).toInt()]

    fun settle() {
        val s = (0 until 3).map { symAt(it) }
        vm.update { it.copy(slotSpins = it.slotSpins + 1) }
        val pay = when {
            s.all { it == Sym.SKULL } -> -1
            s.distinct().size == 1 -> bet * (slotPays[s[0]] ?: 0)
            s.count { it == Sym.CHERRY } >= 2 -> bet
            else -> 0
        }
        when {
            pay < 0 -> {
                result = "3 SKULLS. the machine takes another ${bet}c"
                resultGood = false
                vm.update { it.copy(coins = (it.coins - bet).coerceAtLeast(0), flags = it.flags + "skullslot") }
                fx.play(Sfx.ANGRY); fx.buzz(Buzz.FAIL)
            }
            pay > 0 -> {
                resultGood = true
                result = if (pay == bet) "two cherries. here's your bet back." else "WIN! +${fmt(pay)}c"
                vm.update {
                    it.copy(coins = it.coins + pay, slotSevens = it.slotSevens + if (s.all { x -> x == Sym.SEVEN }) 1 else 0)
                }
                if (pay > bet) { fx.play(if (pay >= bet * 10) Sfx.BIG_WIN else Sfx.WIN); fx.buzz(Buzz.SUCCESS); confetti++ } else fx.play(Sfx.COIN)
            }
            else -> {
                resultGood = false
                result = listOf("nothing.", "the machine laughs at you", "so close (no)", "try again, it's \"due\"").random()
                fx.play(Sfx.LOSE)
            }
        }
    }

    fun stop(r: Int) {
        if (!spinning[r] || target[r] >= 0f) return
        val drift = if (magnet) 0f else Random.nextInt(2).toFloat()
        target[r] = ceil(pos[r]) + drift
        fx.play(Sfx.CLICK); fx.buzz(Buzz.LIGHT)
    }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                frame++
                lever = (lever - dt * 2f).coerceAtLeast(0f)
                if (paused || !active) return@withFrameNanos
                spinTime += dt
                for (r in 0 until 3) {
                    if (!spinning[r]) continue
                    if (target[r] < 0f && spinTime > 3.5f + r * 0.8f) stop(r)
                    if (target[r] >= 0f) {
                        pos[r] = (pos[r] + 6f * dt).coerceAtMost(target[r])
                        if (pos[r] >= target[r]) {
                            pos[r] = target[r]; spinning[r] = false
                            fx.play(Sfx.BALL); fx.buzz(Buzz.MEDIUM)
                        }
                    } else {
                        pos[r] += 10f * dt
                    }
                }
                if (spinning.none { it }) { active = false; settle() }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            LotTopBar("lucky lot slots", state.coins, { nav.back() }, { help = true })
            Box(Modifier.fillMaxWidth().weight(1f).padding(12.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    @Suppress("UNUSED_EXPRESSION") frame
                    val w = size.width
                    val h = size.height
                    // cabinet
                    val cab = Offset(w * 0.04f, h * 0.06f)
                    val cs = Size(w * 0.8f, h * 0.88f)
                    drawRoundRect(Ink, cab + Offset(6f, 10f), cs, CornerRadius(40f))
                    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFF5252), Color(0xFFB71C1C))), cab, cs, CornerRadius(40f))
                    drawRoundRect(Ink, cab, cs, CornerRadius(40f), style = Stroke(6f))
                    // marquee lights
                    for (i in 0 until 12) {
                        val on = ((frame / 6 + i) % 2 == 0L) || !active
                        drawCircle(if (on) Gold else Color(0xFF7A5A00), w * 0.014f, Offset(cab.x + cs.width * (0.06f + i * 0.08f), cab.y + h * 0.04f))
                    }
                    label("lucky lot", cab.x + cs.width / 2, cab.y + h * 0.12f, h * 0.06f, Gold, Ink)
                    // reels window
                    val rw = cs.width * 0.26f
                    val rh = h * 0.42f
                    val top = cab.y + h * 0.2f
                    for (r in 0 until 3) {
                        val left = cab.x + cs.width * 0.07f + r * (rw + cs.width * 0.04f)
                        drawRoundRect(Color.White, Offset(left, top), Size(rw, rh), CornerRadius(14f))
                        val cell = rh / 3f
                        clipRect(left, top, left + rw, top + rh) {
                            val base = floor(pos[r])
                            val frac = pos[r] - base
                            for (j in -2..2) {
                                val idx = (((base.toInt() + j) % n) + n) % n
                                val cy = top + rh / 2 + (j - frac) * cell
                                drawSymbol(strips[r][idx], Offset(left + rw / 2, cy), cell * 0.7f)
                            }
                            drawRect(Brush.verticalGradient(listOf(Ink.copy(alpha = 0.35f), Color.Transparent, Color.Transparent, Ink.copy(alpha = 0.35f)), top, top + rh), Offset(left, top), Size(rw, rh))
                        }
                        drawRoundRect(Ink, Offset(left, top), Size(rw, rh), CornerRadius(14f), style = Stroke(5f))
                    }
                    // payline
                    drawLine(Pink.copy(alpha = 0.8f), Offset(cab.x + cs.width * 0.04f, top + rh / 2), Offset(cab.x + cs.width * 0.96f, top + rh / 2), 5f)
                    // tray
                    drawRoundRect(Color(0xFF6E1414), Offset(cab.x + cs.width * 0.2f, cab.y + cs.height * 0.82f), Size(cs.width * 0.6f, cs.height * 0.1f), CornerRadius(12f))
                    if (resultGood) for (i in 0 until 5) drawCoin(Offset(cab.x + cs.width * (0.3f + i * 0.1f), cab.y + cs.height * 0.86f), h * 0.025f)
                    // lever
                    val lx = cab.x + cs.width + w * 0.05f
                    val ly = top + rh * 0.5f
                    val knobY = ly - rh * 0.55f * (1f - lever) + rh * 0.45f * lever
                    drawRoundRect(Color(0xFF555566), Offset(lx - 10f, ly - 20f), Size(20f, 60f), CornerRadius(8f))
                    drawLine(Color(0xFFBDBDBD), Offset(lx, ly), Offset(lx, knobY), 14f)
                    circleO(Offset(lx, knobY), w * 0.05f, Pink, 5f)
                }
            }
            Text(
                result, color = if (resultGood) Gold else Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            )
            Spacer(Modifier.height(8.dp))
            if (active) {
                @Suppress("UNUSED_VARIABLE") val tick = frame // stop buttons follow the reels
                Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (r in 0 until 3) {
                        ChunkyButton("stop", Modifier.weight(1f), color = Pink, height = 64.dp, enabled = spinning[r] && target[r] < 0f, sfx = null) { stop(r) }
                    }
                }
            } else {
                Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 20, 50).forEach { b ->
                        ChunkyButton(
                            "${b}c", Modifier.weight(1f), color = if (b == bet) Sky else CardHi,
                            textColor = if (b == bet) Ink else Color.White, height = 64.dp, enabled = state.coins >= b,
                        ) { bet = b }
                    }
                }
            }
            ChunkyButton(
                "SPIN (${bet}c)", Modifier.fillMaxWidth().padding(12.dp), color = Gold,
                enabled = !active && state.coins >= bet, height = 60.dp, sfx = Sfx.BUY,
            ) {
                vm.update { it.copy(coins = it.coins - bet) }
                for (r in 0 until 3) { spinning[r] = true; target[r] = -1f }
                spinTime = 0f
                lever = 1f
                result = "spinning..."
                resultGood = false
                active = true
                fx.play(Sfx.DRUM)
            }
        }
        Confetti(confetti)
    }
}

// ================================ inspector ================================

data class TicketSpec(
    val paper: Color,
    val logo: String,
    val stars: Int,
    val holo: Color?,
    val stripe: Color,
    val serial: String,
    val fake: String?,
)

private val RealPaper = Color(0xFFFFE08A)

fun realTicket(n: Int) = TicketSpec(RealPaper, "lot", 3, Mint, Pink, "LOT-%04d".format(n), null)

private fun genInspect(level: Int, rng: Random): TicketSpec {
    val base = realTicket(rng.nextInt(10000))
    if (rng.nextFloat() < 0.5f) return base
    val subtle = level >= 5
    return when (rng.nextInt(6)) {
        0 -> base.copy(paper = if (subtle) Color(0xFFFFD050) else listOf(Color(0xFFB8E0FF), Color(0xFFD7FFD0)).random(rng), fake = "wrong paper color")
        1 -> base.copy(logo = (if (subtle) listOf("l0t", "iot", "lot.") else listOf("1ot", "lol", "lott", "LoT")).random(rng), fake = "logo misspelled")
        2 -> base.copy(stars = if (rng.nextBoolean()) 2 else 4, fake = "wrong number of stars")
        3 -> if (subtle) base.copy(holo = Color(0xFF7FE7FF), fake = "hologram is the wrong color") else base.copy(holo = null, fake = "hologram missing")
        4 -> base.copy(stripe = if (subtle) Color(0xFFFF8AB0) else Sky, fake = "wrong stripe color")
        else -> base.copy(serial = (if (subtle) "L0T-" else "LTO-") + base.serial.drop(4), fake = "bad serial number")
    }
}

fun DrawScope.drawInspectTicket(t: TicketSpec, tl: Offset, sz: Size) {
    val w = sz.width
    val h = sz.height
    drawRoundRect(Ink, tl + Offset(4f, 6f), sz, CornerRadius(w * 0.05f))
    drawRoundRect(t.paper, tl, sz, CornerRadius(w * 0.05f))
    drawRect(t.stripe, tl + Offset(0f, h * 0.08f), Size(w, h * 0.14f))
    drawRoundRect(Ink, tl, sz, CornerRadius(w * 0.05f), style = Stroke(w * 0.012f))
    label(t.logo, tl.x + w * 0.25f, tl.y + h * 0.48f, h * 0.3f, Ink)
    for (i in 0 until t.stars) {
        outlined(starPath(Offset(tl.x + w * (0.52f + i * 0.11f), tl.y + h * 0.45f), h * 0.08f, h * 0.035f), Gold, w * 0.006f)
    }
    label(t.serial, tl.x + w * 0.24f, tl.y + h * 0.82f, h * 0.11f, Ink.copy(alpha = 0.7f))
    t.holo?.let {
        val c = Offset(tl.x + w * 0.82f, tl.y + h * 0.76f)
        drawCircle(Brush.sweepGradient(listOf(it, Color.White, it, Color.White, it), c), h * 0.13f, c)
        drawCircle(Ink, h * 0.13f, c, style = Stroke(w * 0.006f))
    }
}

private val inspectTutorial = listOf(
    "you work quality control at the lot ticket factory. counterfeiters are everywhere.",
    "the top of the screen shows what a real ticket looks like. compare every ticket that comes in.",
    "check the paper color, the logo spelling, the stars, the hologram, the stripe and the serial number.",
    "approve real ones, reject fakes. you have a timer, it gets shorter. 3 mistakes and your shift is over.",
)

@Composable
fun InspectorScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    val scope = rememberCoroutineScope()
    var help by remember { mutableStateOf(false) }
    ModeTutorial(vm, state, "inspector", "inspector", inspectTutorial, help) { help = false }
    val paused by rememberUpdatedState(help || "inspector" !in state.tutorialsSeen)
    val glasses by rememberUpdatedState("glasses" in state.items)

    var shift by remember { mutableIntStateOf(1) }
    var score by remember(shift) { mutableIntStateOf(0) }
    var strikes by remember(shift) { mutableIntStateOf(0) }
    var streak by remember(shift) { mutableIntStateOf(0) }
    var best by remember(shift) { mutableIntStateOf(0) }
    var ticket by remember(shift) { mutableStateOf(genInspect(0, Random)) }
    var timeLeft by remember(shift) { mutableFloatStateOf(1f) }
    var deciding by remember(shift) { mutableStateOf(false) }
    var stamp by remember(shift) { mutableStateOf<String?>(null) }
    var note by remember(shift) { mutableStateOf("") }
    var over by remember(shift) { mutableStateOf(false) }
    val slide = remember(shift) { Animatable(1.2f) }
    val level = score / 5
    val perTicket = (4.5f - level * 0.3f).coerceAtLeast(1.6f) * (if (glasses) 1.4f else 1f)

    LaunchedEffect(shift) { slide.animateTo(0f, tween(400, easing = FastOutSlowInEasing)) }

    fun endShift() {
        over = true
        val pay = score * 5
        vm.update { it.copy(coins = it.coins + pay, inspected = it.inspected + score + strikes, inspectBest = maxOf(it.inspectBest, best)) }
        fx.play(if (pay > 0) Sfx.WIN else Sfx.LOSE)
    }

    fun decide(approve: Boolean?) {
        if (deciding || over) return
        deciding = true
        val real = ticket.fake == null
        val correct = approve != null && approve == real
        scope.launch {
            stamp = when (approve) { true -> "APPROVED"; false -> "REJECTED"; null -> "TOO SLOW" }
            if (correct) {
                score++; streak++; best = maxOf(best, streak)
                note = if (real) "real ticket, good" else "nice catch: ${ticket.fake}"
                fx.play(Sfx.COIN); fx.buzz(Buzz.LIGHT)
            } else {
                strikes++; streak = 0
                note = when {
                    approve == null -> "the belt doesn't wait"
                    real -> "that was a real one! the boss is not happy"
                    else -> "that was fake: ${ticket.fake}"
                }
                fx.play(Sfx.ANGRY); fx.buzz(Buzz.FAIL)
            }
            delay(650)
            slide.animateTo(-1.2f, tween(250))
            stamp = null
            if (strikes >= 3) {
                endShift()
            } else {
                ticket = genInspect(score / 5, Random)
                timeLeft = 1f
                slide.snapTo(1.2f)
                slide.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
            }
            deciding = false
        }
    }

    LaunchedEffect(shift) {
        var last = 0L
        while (!over) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                if (paused || deciding || over || slide.isRunning) return@withFrameNanos
                timeLeft -= dt / perTicket
                if (timeLeft <= 0f) { timeLeft = 0f; decide(null) }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        LotTopBar("inspector", state.coins, { nav.back() }, { help = true })
        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("checked", "$score", Modifier.weight(1f), Mint)
            StatBox("streak", "$streak", Modifier.weight(1f), Gold)
            StatBox("mistakes", "$strikes/3", Modifier.weight(1f), Pink)
        }
        Box(Modifier.fillMaxWidth().weight(1f).padding(12.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRoundRect(Color(0xFF2E2A45), cornerRadius = CornerRadius(30f))
                // reference
                label("a REAL ticket looks like this:", w / 2, h * 0.05f, h * 0.035f, Dim)
                drawInspectTicket(realTicket(1234), Offset(w * 0.2f, h * 0.09f), Size(w * 0.6f, w * 0.3f))
                // conveyor
                val beltY = h * 0.5f
                drawRect(Color(0xFF444455), Offset(0f, beltY), Size(w, h * 0.42f))
                for (i in 0 until 14) {
                    val x = ((i * w / 10f - slide.value * w * 0.3f) % (w * 1.4f) + w * 1.4f) % (w * 1.4f) - w * 0.2f
                    drawLine(Color(0xFF5A5A6E), Offset(x, beltY), Offset(x, beltY + h * 0.42f), 6f)
                }
                val tw = w * 0.84f
                val th = tw * 0.5f
                val tx = (w - tw) / 2 + slide.value * w
                val ty = beltY + (h * 0.42f - th) / 2
                drawInspectTicket(ticket, Offset(tx, ty), Size(tw, th))
                stamp?.let {
                    val col = when (it) { "APPROVED" -> Color(0xFF2E9E5B); "REJECTED" -> Color(0xFFE53935); else -> Color(0xFF9E9E9E) }
                    rotate(-14f, Offset(w / 2, ty + th / 2)) {
                        drawRoundRect(col, Offset(w * 0.2f, ty + th * 0.3f), Size(w * 0.6f, th * 0.4f), CornerRadius(12f), style = Stroke(10f))
                        label(it, w / 2, ty + th / 2, th * 0.22f, col)
                    }
                }
                // timer
                drawRoundRect(Ink, Offset(w * 0.05f, h * 0.94f), Size(w * 0.9f, h * 0.035f), CornerRadius(20f))
                drawRoundRect(if (timeLeft > 0.3f) Mint else Pink, Offset(w * 0.05f, h * 0.94f), Size(w * 0.9f * timeLeft, h * 0.035f), CornerRadius(20f))
            }
            if (over) {
                Column(
                    Modifier.align(Alignment.Center).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Panel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("shift over", color = Gold, fontWeight = FontWeight.Black, fontSize = 26.sp)
                            Text("checked $score tickets · best streak $best", color = Color.White, fontSize = 16.sp)
                            Text("paycheck: +${score * 5}c", color = Mint, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ChunkyButton("next shift", Modifier.weight(1f), color = Mint) { shift++ }
                                ChunkyButton("home", Modifier.weight(1f), color = CardHi, textColor = Color.White) { nav.back() }
                            }
                        }
                    }
                }
            }
        }
        Text(
            note, color = Dim, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        )
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChunkyButton("REJECT", Modifier.weight(1f), color = Pink, height = 66.dp, enabled = !deciding && !over, sfx = null) { decide(false) }
            ChunkyButton("APPROVE", Modifier.weight(1f), color = Mint, height = 66.dp, enabled = !deciding && !over, sfx = null) { decide(true) }
        }
    }
}
