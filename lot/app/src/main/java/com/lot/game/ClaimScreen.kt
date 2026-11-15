package com.lot.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private enum class Stage { SIGN, WALK, THIEF, FIGHT, RESULT }

private val claimTutorial = listOf(
    "you won! but nobody hands out money for free. first, sign the back of your ticket.",
    "then walk to the stand. puddles, cones and pigeons all damage your ticket. falling in a hole too.",
    "watch out for ticket thieves. if one grabs it, mash pull to get it back. don't pull when he fakes you out.",
    "the cashier notices every single defect. each one makes the fight harder. tap argue in the green, block his stamps with the right arrow.",
    "too much effort? the lazy cash-out pays 70% right away. coward.",
)

@Composable
fun ClaimScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val claim = vm.currentClaim
    if (claim == null) {
        LaunchedEffect(Unit) { nav.back() }
        return
    }
    var help by remember { mutableStateOf(false) }
    ModeTutorial(vm, state, "claim", "claiming", claimTutorial, help, countsAsMode = false) { help = false }
    val paused = help || "claim" !in state.tutorialsSeen

    var stage by remember(claim.id) { mutableStateOf(Stage.SIGN) }
    val defects = remember(claim.id) { mutableStateListOf<String>() }
    var walkCoins by remember(claim.id) { mutableIntStateOf(0) }
    var resultText by remember(claim.id) { mutableStateOf("") }
    var resultAmount by remember(claim.id) { mutableIntStateOf(0) }
    val level = remember(claim.id) { Level(claim.id) }

    Column(Modifier.fillMaxSize()) {
        LotTopBar(
            when (stage) {
                Stage.SIGN -> "sign it"
                Stage.WALK -> "walk to the stand"
                Stage.THIEF -> "THIEF!"
                Stage.FIGHT -> "the cashier"
                Stage.RESULT -> "done"
            },
            state.coins, { nav.back() }, { help = true },
        )
        Text(
            "${claim.name} · prize ${fmt(claim.prize)}c · ${defects.size} defect${if (defects.size == 1) "" else "s"}",
            color = Dim, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Box(Modifier.weight(1f)) {
            when (stage) {
                Stage.SIGN -> SignStage { nitpicks ->
                    defects.addAll(nitpicks)
                    vm.flag("signed")
                    stage = Stage.WALK
                }
                Stage.WALK -> WalkStage(
                    level, paused, "sleeve" in state.items, "shoes" in state.items,
                    onDefect = { defects.add(it) },
                    onCoin = { walkCoins++ },
                    onThief = { stage = Stage.THIEF },
                    onArrive = {
                        if (level.walkDefects == 0) vm.flag("cleanwalk")
                        stage = Stage.FIGHT
                    },
                )
                Stage.THIEF -> ThiefStage(paused, "whistle" in state.items) { won ->
                    if (won) {
                        vm.flag("thiefwin")
                        defects.add("thief fingerprints all over it")
                        stage = Stage.WALK
                    } else {
                        vm.loseClaim(claim.id, setOf("thieflose"))
                        resultText = "the thief ran off with your ticket.\nthat's ${fmt(claim.prize)} coins gone. classic."
                        resultAmount = 0
                        stage = Stage.RESULT
                    }
                }
                Stage.FIGHT -> FightStage(defects.toList(), paused, "lawyer" in state.items) { won, flawless ->
                    val bonus = walkCoins
                    if (won) {
                        val amount = claim.prize + bonus
                        vm.payClaim(claim.id, amount, true, if (flawless) setOf("flawless") else emptySet())
                        resultText = "the cashier grumbles and pays up.\n+${fmt(claim.prize)} prize" +
                            if (bonus > 0) "\n+$bonus coins found on the way" else ""
                        resultAmount = amount
                    } else {
                        val amount = claim.prize / 2 + bonus
                        vm.payClaim(claim.id, amount, true, setOf("denied"))
                        resultText = "you lost the argument.\nhe pays half for \"ticket damage\": +${fmt(claim.prize / 2)}" +
                            if (bonus > 0) "\n+$bonus coins found on the way" else ""
                        resultAmount = amount
                    }
                    stage = Stage.RESULT
                }
                Stage.RESULT -> ResultStage(resultText, resultAmount, defects.toList()) { nav.back() }
            }
        }
    }
}

// ============================ sign ============================

@Composable
private fun SignStage(onDone: (List<String>) -> Unit) {
    val fx = LocalFx.current
    val path = remember { Path() }
    var ver by remember { mutableIntStateOf(0) }
    var length by remember { mutableFloatStateOf(0f) }
    var strokes by remember { mutableIntStateOf(0) }
    var last by remember { mutableStateOf(Offset.Zero) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("sign the back of your ticket", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
        Text("use your finger. the cashier is very picky.", color = Dim, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Panel(Modifier.fillMaxWidth(), color = Color(0xFFFFF3D6)) {
            Column(Modifier.padding(14.dp)) {
                Text("this ticket belongs to:", color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { p -> path.moveTo(p.x, p.y); last = p; strokes++; ver++ },
                            ) { ch, _ ->
                                ch.consume()
                                val p = ch.position
                                length += hypot(p.x - last.x, p.y - last.y)
                                last = p
                                path.lineTo(p.x, p.y)
                                ver++
                                if (ver % 6 == 0) fx.buzz(Buzz.TICK)
                            }
                        }
                ) {
                    @Suppress("UNUSED_EXPRESSION") ver
                    val ly = size.height * 0.8f
                    drawLine(Ink.copy(alpha = 0.4f), Offset(0f, ly), Offset(size.width, ly), 3f)
                    label("x", 20f, ly - 30f, 40f, Ink.copy(alpha = 0.4f))
                    drawPath(path, Color(0xFF1E3A70), style = Stroke(7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                Text("by signing you agree to absolutely everything", color = Ink.copy(alpha = 0.5f), fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChunkyButton("clear", Modifier.weight(1f), color = CardHi, textColor = Color.White) {
                path.reset(); length = 0f; strokes = 0; ver++
            }
            ChunkyButton("done", Modifier.weight(1f), color = Mint, enabled = length > 250f) {
                fx.play(Sfx.COIN)
                val picks = mutableListOf<String>()
                when {
                    length < 700f -> picks.add("signature is suspiciously tiny")
                    length > 5000f -> picks.add("signature looks like a seismograph")
                }
                if (strokes > 6) picks.add("way too many pen lifts")
                if (strokes == 1 && length < 1200f) picks.add("signature is just one squiggle")
                onDone(picks)
            }
        }
    }
}

// ============================ platformer ============================

private class Plat(val x: Float, val top: Float, val w: Float)
private class Puddle(val x: Float, val w: Float) { var hit = false }
private class Cone(val x: Float) { var hit = false }
private class Pigeon(val x0: Float, val range: Float, val y: Float, val speed: Float, val phase: Float) { var hit = false }
private class Coin(val x: Float, val y: Float) { var taken = false }

private class Level(seed: Long) {
    val length = 120f
    val gaps = mutableListOf<Pair<Float, Float>>()
    val plats = mutableListOf<Plat>()
    val puddles = mutableListOf<Puddle>()
    val cones = mutableListOf<Cone>()
    val pigeons = mutableListOf<Pigeon>()
    val coins = mutableListOf<Coin>()
    val thiefX: Float?
    var thiefDone = false
    var walkDefects = 0
    var sleeveUsed = false

    // player state survives the thief interruption
    var px = 2f
    var py = 0f
    var vx = 0f
    var vy = 0f
    var onGround = true
    var invuln = 0f
    var knock = 0f
    var checkpoint = 2f
    var t = 0f
    var facing = 1f

    init {
        val rng = Random(seed)
        var x = 10f
        while (x < length - 12f) {
            when (rng.nextInt(6)) {
                0 -> { // gap, sometimes with a helper platform
                    val w = 2f + rng.nextFloat() * 1.4f
                    gaps.add(x to x + w)
                    if (rng.nextBoolean()) plats.add(Plat(x + w / 2 - 1.5f, 2.6f, 3f))
                    x += w
                }
                1 -> puddles.add(Puddle(x, 1.4f + rng.nextFloat()))
                2 -> cones.add(Cone(x))
                3 -> pigeons.add(Pigeon(x, 1.5f + rng.nextFloat() * 1.5f, 1.4f + rng.nextFloat() * 1.6f, 1.2f + rng.nextFloat(), rng.nextFloat() * 6f))
                4 -> {
                    val top = 2.4f + rng.nextFloat() * 1.4f
                    plats.add(Plat(x, top, 3.5f))
                    repeat(3) { coins.add(Coin(x + 0.6f + it * 1.1f, top + 1f)) }
                }
                else -> repeat(4) { coins.add(Coin(x + it * 1f, 0.9f + sin(it.toFloat()) * 0.4f)) }
            }
            x += 6f + rng.nextFloat() * 5f
        }
        thiefX = if (rng.nextFloat() < 0.3f) 30f + rng.nextFloat() * 60f else null
    }

    fun groundAt(x: Float) = gaps.none { x > it.first + 0.15f && x < it.second - 0.15f }
}

private val walkDefectNames = mapOf(
    "puddle" to listOf("puddle stain on the corner", "suspiciously wet", "smells like street water"),
    "cone" to listOf("creased (tripped over a cone)", "folded in half like a coward"),
    "pigeon" to listOf("pigeon took a bite", "pigeon poop. just. why."),
    "fall" to listOf("soggy from the sewer", "smells like a sewer now"),
)

@Composable
private fun WalkStage(
    lv: Level, paused: Boolean, sleeve: Boolean, shoes: Boolean,
    onDefect: (String) -> Unit, onCoin: () -> Unit, onThief: () -> Unit, onArrive: () -> Unit,
) {
    val fx = LocalFx.current
    var left by remember { mutableStateOf(false) }
    var right by remember { mutableStateOf(false) }
    var jump by remember { mutableStateOf(false) }
    var frame by remember { mutableLongStateOf(0L) }
    var done by remember { mutableStateOf(false) }
    val isPaused by rememberUpdatedState(paused)
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toast) { if (toast != null) { delay(1300); toast = null } }

    fun hurt(kind: String) {
        if (sleeve && !lv.sleeveUsed) {
            lv.sleeveUsed = true
            lv.invuln = 1.2f
            toast = "the sleeve blocked it! (once)"
            fx.play(Sfx.SNEAK); fx.buzz(Buzz.LIGHT)
            return
        }
        val d = walkDefectNames.getValue(kind).random()
        lv.walkDefects++
        onDefect(d)
        toast = d
        lv.invuln = 1.2f
        fx.play(Sfx.ANGRY); fx.buzz(Buzz.MEDIUM)
    }

    LaunchedEffect(Unit) {
        var last = 0L
        var jumpWas = false
        while (!done) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.033f)
                last = now
                frame++
                if (isPaused) return@withFrameNanos
                lv.t += dt
                val dir = (if (right) 1f else 0f) - (if (left) 1f else 0f)
                if (dir != 0f) lv.facing = dir
                lv.vx = if (lv.knock > 0f) -lv.facing * 5f else dir * (if (shoes) 8.5f else 6.5f)
                lv.knock -= dt
                lv.invuln -= dt
                if (jump && !jumpWas && lv.onGround) {
                    lv.vy = if (shoes) 15f else 13.5f; lv.onGround = false; fx.play(Sfx.BALL)
                }
                if (!jump && lv.vy > 4f) lv.vy *= 0.6f
                jumpWas = jump
                lv.vy -= 34f * dt
                val prevY = lv.py
                lv.px = (lv.px + lv.vx * dt).coerceAtLeast(0.5f)
                lv.py += lv.vy * dt
                lv.onGround = false
                if (lv.vy <= 0f) {
                    if (prevY >= -0.01f && lv.py <= 0f && lv.groundAt(lv.px)) {
                        lv.py = 0f; lv.vy = 0f; lv.onGround = true
                        lv.checkpoint = lv.px
                    }
                    for (p in lv.plats) {
                        if (lv.px > p.x - 0.3f && lv.px < p.x + p.w + 0.3f && prevY >= p.top - 0.01f && lv.py <= p.top) {
                            lv.py = p.top; lv.vy = 0f; lv.onGround = true
                        }
                    }
                }
                if (lv.py < -7f) {
                    hurt("fall")
                    lv.px = (lv.checkpoint - 1.5f).coerceAtLeast(0.5f)
                    while (!lv.groundAt(lv.px) && lv.px > 0.5f) lv.px -= 0.5f
                    lv.py = 0f; lv.vy = 0f
                }
                if (lv.invuln <= 0f) {
                    for (pd in lv.puddles) if (!pd.hit && lv.onGround && lv.py < 0.1f && lv.px > pd.x && lv.px < pd.x + pd.w) { pd.hit = true; hurt("puddle") }
                    for (c in lv.cones) if (!c.hit && abs(lv.px - c.x) < 0.55f && lv.py < 0.9f) { c.hit = true; hurt("cone"); lv.knock = 0.25f; lv.vy = 7f }
                    for (pg in lv.pigeons) {
                        val gx = pg.x0 + sin(lv.t * pg.speed + pg.phase) * pg.range
                        if (!pg.hit && abs(gx - lv.px) < 0.7f && abs(pg.y - (lv.py + 0.9f)) < 0.8f) { pg.hit = true; hurt("pigeon") }
                    }
                }
                for (c in lv.coins) if (!c.taken && abs(c.x - lv.px) < 0.6f && abs(c.y - (lv.py + 0.8f)) < 0.9f) {
                    c.taken = true; onCoin(); fx.play(Sfx.COIN)
                }
                val tx = lv.thiefX
                if (tx != null && !lv.thiefDone && lv.px > tx) {
                    lv.thiefDone = true
                    done = true
                    fx.play(Sfx.CAUGHT); fx.buzz(Buzz.HEAVY)
                    onThief()
                }
                if (!done && lv.px >= lv.length) {
                    done = true
                    fx.play(Sfx.WIN)
                    onArrive()
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                @Suppress("UNUSED_EXPRESSION") frame
                val w = size.width
                val h = size.height
                val unit = h / 9f
                val viewW = w / unit
                val cam = (lv.px - viewW * 0.35f).coerceIn(0f, lv.length + 6f - viewW)
                val gy = h * 0.8f
                fun sx(x: Float) = (x - cam) * unit
                fun sy(y: Float) = gy - y * unit

                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF6EC6FF), Color(0xFFFFD6A5))), cornerRadius = CornerRadius(30f))
                // clouds + far buildings (parallax)
                for (i in 0 until 8) {
                    val cx = ((i * 9f - cam * 0.15f) % (viewW + 10f) + viewW + 10f) % (viewW + 10f) - 5f
                    drawCircle(Color.White.copy(alpha = 0.8f), unit * 0.7f, Offset(cx * unit, h * (0.12f + (i % 3) * 0.07f)))
                    drawCircle(Color.White.copy(alpha = 0.8f), unit * 0.5f, Offset(cx * unit + unit * 0.7f, h * (0.13f + (i % 3) * 0.07f)))
                }
                for (i in 0 until 30) {
                    val bx = i * 6f - cam * 0.4f
                    val bh = 2.5f + (i * 7 % 5)
                    drawRect(Color(0xFF8E7CC3).copy(alpha = 0.6f), Offset(bx * unit, sy(bh)), Size(unit * 4.5f, bh * unit))
                }
                // ground with gaps
                var x0 = -2f
                val ends = lv.gaps.sortedBy { it.first } + (lv.length + 20f to lv.length + 20f)
                for ((a, b) in ends) {
                    drawRect(Color(0xFF6B4F3A), Offset(sx(x0), gy), Size((a - x0) * unit, h - gy))
                    drawRect(Color(0xFF4CAF50), Offset(sx(x0), gy - unit * 0.15f), Size((a - x0) * unit, unit * 0.3f))
                    x0 = b
                }
                // platforms
                lv.plats.forEach { p ->
                    drawRoundRect(Color(0xFFB5651D), Offset(sx(p.x), sy(p.top)), Size(p.w * unit, unit * 0.45f), CornerRadius(8f))
                    drawRoundRect(Ink, Offset(sx(p.x), sy(p.top)), Size(p.w * unit, unit * 0.45f), CornerRadius(8f), style = Stroke(4f))
                }
                lv.puddles.forEach { pd ->
                    drawOval(Color(0xFF3F7CC4).copy(alpha = 0.8f), Offset(sx(pd.x), gy - unit * 0.12f), Size(pd.w * unit, unit * 0.3f))
                }
                lv.cones.forEach { c -> drawCone(sx(c.x), gy, unit * 0.9f) }
                lv.coins.forEach { c -> if (!c.taken) drawCoin(Offset(sx(c.x), sy(c.y)), unit * 0.25f) }
                lv.pigeons.forEach { pg ->
                    val gx = pg.x0 + sin(lv.t * pg.speed + pg.phase) * pg.range
                    val dir = if (kotlin.math.cos(lv.t * pg.speed + pg.phase) > 0) 1f else -1f
                    drawPigeon(Offset(sx(gx), sy(pg.y)), unit * 0.7f, abs(sin(lv.t * 14f)), dir)
                }
                // the stand at the end
                drawStand(sx(lv.length), sx(lv.length + 5f), sy(5.2f), sy(2.2f), gy)
                drawDude(CashierDude, sx(lv.length + 2.5f), sy(2.2f), unit * 1.6f, Mood.GRUMPY, look = -1f, t = lv.t)
                // you
                val blink = lv.invuln > 0f && (lv.t * 12f).toInt() % 2 == 0
                if (!blink) {
                    val moving = abs(lv.vx) > 0.1f && lv.onGround
                    drawDude(
                        YouDude, sx(lv.px), sy(lv.py), unit * 1.9f,
                        if (lv.invuln > 0f) Mood.GRUMPY else Mood.HAPPY,
                        wobble = if (moving) sin(lv.t * 14f) * 0.6f else 0f, look = lv.facing,
                        holdTicket = true, t = lv.t,
                    )
                    val hp = handPoint(sx(lv.px), sy(lv.py), unit * 1.9f)
                    drawRoundRect(Gold, hp + Offset(-unit * 0.25f, -unit * 0.3f), Size(unit * 0.5f, unit * 0.3f), CornerRadius(4f))
                    drawRoundRect(Ink, hp + Offset(-unit * 0.25f, -unit * 0.3f), Size(unit * 0.5f, unit * 0.3f), CornerRadius(4f), style = Stroke(3f))
                }
                // progress
                drawRoundRect(Ink.copy(alpha = 0.5f), Offset(w * 0.1f, 20f), Size(w * 0.8f, 16f), CornerRadius(8f))
                drawRoundRect(Mint, Offset(w * 0.1f, 20f), Size(w * 0.8f * (lv.px / lv.length).coerceIn(0f, 1f), 16f), CornerRadius(8f))
            }
            toast?.let {
                Text(
                    if (it.startsWith("the sleeve")) it else "ticket damage: $it", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 44.dp)
                        .background(Pink, RoundedCornerShape(12.dp))
                        .border(2.dp, Ink, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        Row(
            Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HoldButton("◀", Modifier.weight(1f), CardHi) { left = it }
            HoldButton("▶", Modifier.weight(1f), CardHi) { right = it }
            HoldButton("jump", Modifier.weight(1.3f), Gold) { jump = it }
        }
    }
}

@Composable
fun HoldButton(text: String, modifier: Modifier, color: Color, height: Dp = 70.dp, onChange: (Boolean) -> Unit) {
    val fx = LocalFx.current
    var down by remember { mutableStateOf(false) }
    Box(
        modifier
            .height(height)
            .offset(y = if (down) 2.dp else 0.dp)
            .background(Ink, RoundedCornerShape(16.dp))
            .padding(bottom = if (down) 1.dp else 4.dp)
            .background(if (down) color.copy(alpha = 0.7f) else color, RoundedCornerShape(16.dp))
            .border(2.5.dp, Ink, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    down = true; onChange(true); fx.buzz(Buzz.TICK)
                    tryAwaitRelease()
                    down = false; onChange(false)
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (color == Gold || color == Mint) Ink else Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
    }
}

// ============================ thief ============================

@Composable
private fun ThiefStage(paused: Boolean, whistle: Boolean, onEnd: (Boolean) -> Unit) {
    val fx = LocalFx.current
    var rope by remember { mutableFloatStateOf(0f) } // -1 = you win, 1 = thief wins
    var fake by remember { mutableStateOf(false) }
    var warn by remember { mutableStateOf(false) }
    var fakeStart by remember { mutableFloatStateOf(0f) }
    var fakePenalty by remember { mutableFloatStateOf(0f) }
    var clock by remember { mutableFloatStateOf(0f) }
    var over by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }
    val isPaused by rememberUpdatedState(paused)

    LaunchedEffect(Unit) {
        delay(1200)
        started = true
        var last = 0L
        var nextFake = 2.5f
        var fakeEnd = 0f
        while (!over) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                if (isPaused) return@withFrameNanos
                clock += dt
                // he telegraphs every fake with a warning first, so you can stop in time
                if (!fake && !warn && clock > nextFake - 0.7f) { warn = true; fx.play(Sfx.SNEAK) }
                if (!fake && clock > nextFake) { warn = false; fake = true; fakeStart = clock; fakePenalty = 0f; fakeEnd = clock + 0.8f; fx.play(Sfx.LAUGH) }
                if (fake && clock > fakeEnd) { fake = false; nextFake = clock + 2.2f + Random.nextFloat() * 2f }
                val pull = (0.2f + clock * 0.006f) * (if (whistle) 0.7f else 1f)
                if (!fake) rope += pull * dt
                if (rope >= 1f && !over) { over = true; fx.play(Sfx.LOSE); fx.buzz(Buzz.FAIL); onEnd(false) }
                if (rope <= -1f && !over) { over = true; fx.play(Sfx.WIN); fx.buzz(Buzz.SUCCESS); onEnd(true) }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRoundRect(Color(0xFF3A3550), cornerRadius = CornerRadius(30f))
                drawRect(Color(0xFF555070), Offset(0f, h * 0.72f), Size(w, h * 0.28f))
                val mid = w / 2 + rope * w * 0.3f
                val yRope = h * 0.52f
                val shake = sin(clock * 40f) * 3f
                drawDude(YouDude, w * 0.18f + rope * w * 0.15f + shake, h * 0.9f, h * 0.55f, if (rope > 0.4f) Mood.ANGRY else Mood.GRUMPY, wobble = -0.6f, look = 1f, t = clock)
                drawDude(ThiefDude, w * 0.82f + rope * w * 0.15f - shake, h * 0.9f, h * 0.55f, if (fake) Mood.HAPPY else Mood.GRUMPY, wobble = 0.6f, look = -1f, talk = fake, t = clock)
                drawMask(w * 0.82f + rope * w * 0.15f - shake, h * 0.9f, h * 0.55f)
                drawLine(Color(0xFFB5651D), Offset(w * 0.25f + rope * w * 0.15f, yRope), Offset(w * 0.75f + rope * w * 0.15f, yRope), 10f, StrokeCap.Round)
                rotate(rope * 20f, Offset(mid, yRope)) {
                    drawRoundRect(Gold, Offset(mid - w * 0.08f, yRope - w * 0.05f), Size(w * 0.16f, w * 0.1f), CornerRadius(8f))
                    drawRoundRect(Ink, Offset(mid - w * 0.08f, yRope - w * 0.05f), Size(w * 0.16f, w * 0.1f), CornerRadius(8f), style = Stroke(5f))
                    label("lot", mid, yRope, w * 0.05f, Ink)
                }
                // meter
                drawRoundRect(Ink, Offset(w * 0.1f, h * 0.08f), Size(w * 0.8f, 30f), CornerRadius(15f))
                drawRoundRect(Mint, Offset(w * 0.1f, h * 0.08f), Size(w * 0.4f * (1f - rope), 30f), CornerRadius(15f))
                drawLine(Color.White, Offset(w * 0.5f, h * 0.08f - 6f), Offset(w * 0.5f, h * 0.08f + 36f), 4f)
            }
            Text(
                when {
                    !started -> "a thief grabbed your ticket!!"
                    fake -> "he's faking! DON'T pull!"
                    warn -> "careful... he's up to something"
                    else -> "mash pull!"
                },
                color = if (fake) Pink else Gold, fontWeight = FontWeight.Black, fontSize = 22.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 50.dp),
            )
        }
        ChunkyButton(
            "PULL!", Modifier.fillMaxWidth(), color = if (fake) Pink else if (warn) Gold else Mint, height = 90.dp,
            enabled = started && !over, sfx = null,
        ) {
            if (fake) {
                // short grace window for taps already on their way, and a small capped penalty
                if (clock - fakeStart > 0.25f && fakePenalty < 0.12f) {
                    rope += 0.04f; fakePenalty += 0.04f; fx.play(Sfx.ANGRY); fx.buzz(Buzz.MEDIUM)
                }
            } else {
                rope -= 0.09f; fx.play(Sfx.CLICK)
            }
        }
    }
}

// ============================ cashier fight ============================

private val extraNitpicks = listOf(
    "the ink is too inky", "printed on a tuesday. suspicious.", "you breathed on it",
    "the corner is bent 0.1mm", "smells like optimism", "the 7 looks smug",
    "you're holding it wrong", "this ticket has bad vibes", "your shoes are untied",
)
private val yourArgs = listOf(
    "it's still a valid ticket!", "read the rules!", "i know my rights!", "the 7 is NOT smug!",
    "i want your manager!", "pay me, you goblin!",
)
private val stampNames = listOf("stamp of rejection", "fine print slap", "calls the manager", "rule book smack", "paperwork avalanche")
private enum class FPhase { INTRO, ATTACK, WINDUP, DEFEND, DONE }

@Composable
internal fun FightStage(
    defects: List<String>, paused: Boolean, lawyer: Boolean,
    boss: Dude = CashierDude,
    extraLines: Int = 2,
    fixedHp: Int? = null,
    opener: String = "let me take a look at this...",
    challenge: String? = null,
    onEnd: (won: Boolean, flawless: Boolean) -> Unit,
) {
    val fx = LocalFx.current
    val lines = remember { (defects + extraNitpicks.shuffled().take(extraLines)) }
    val maxHp = remember { (fixedHp ?: (3 + lines.size - if (lawyer) 2 else 0)).coerceIn(2, 30) }
    var hp by remember { mutableIntStateOf(maxHp) }
    var myHp by remember { mutableIntStateOf(5) }
    var phase by remember { mutableStateOf(FPhase.INTRO) }
    var speech by remember { mutableStateOf("") }
    var myLine by remember { mutableStateOf<String?>(null) }
    var introIdx by remember { mutableIntStateOf(-1) }
    var marker by remember { mutableFloatStateOf(0f) }
    var zone by remember { mutableFloatStateOf(0.5f) }
    var arrow by remember { mutableIntStateOf(0) } // -1 left, 0 up, 1 right
    var window by remember { mutableFloatStateOf(1f) }
    var hitFlash by remember { mutableFloatStateOf(0f) }
    var hurtFlash by remember { mutableFloatStateOf(0f) }
    var clock by remember { mutableFloatStateOf(0f) }
    val taps = remember { Channel<Int>(Channel.CONFLATED) }
    val isPaused by rememberUpdatedState(paused)
    val zoneW = (0.3f - lines.size * 0.012f).coerceAtLeast(0.14f)
    val speed = 1.1f + lines.size * 0.06f

    LaunchedEffect(Unit) {
        var last = 0L
        var dir = 1f
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                clock += dt
                hitFlash = (hitFlash - dt * 3f).coerceAtLeast(0f)
                hurtFlash = (hurtFlash - dt * 3f).coerceAtLeast(0f)
                if (isPaused) return@withFrameNanos
                if (phase == FPhase.ATTACK) {
                    marker += dir * speed * dt
                    if (marker > 1f) { marker = 1f; dir = -1f }
                    if (marker < 0f) { marker = 0f; dir = 1f }
                }
                if (phase == FPhase.DEFEND) window = (window - dt / 0.95f).coerceAtLeast(0f)
            }
        }
    }

    LaunchedEffect(Unit) {
        while (isPaused) delay(100)
        // the cashier lists every single defect
        speech = opener
        fx.play(Sfx.DRUM)
        delay(1300)
        for (i in lines.indices) {
            introIdx = i
            speech = "${lines[i]}. ${if (i == lines.lastIndex) "REJECTED." else "and..."}"
            fx.play(Sfx.ANGRY)
            fx.buzz(Buzz.LIGHT)
            withTimeoutOrNull(1500) { taps.receive() }
        }
        speech = challenge ?: "${lines.size} problems. i'm not paying. fight me."
        delay(1200)
        var damaged = false
        while (hp > 0 && myHp > 0) {
            // you argue
            phase = FPhase.ATTACK
            zone = zoneW / 2 + Random.nextFloat() * (1f - zoneW)
            speech = "go on, argue. i dare you."
            taps.tryReceive()
            taps.receive()
            val d = abs(marker - zone)
            when {
                d <= zoneW * 0.15f -> { hp -= 2; myLine = "PERFECT: " + yourArgs.random(); hitFlash = 1f; fx.play(Sfx.BIG_WIN); fx.buzz(Buzz.HEAVY) }
                d <= zoneW / 2 -> { hp -= 1; myLine = yourArgs.random(); hitFlash = 1f; fx.play(Sfx.COIN); fx.buzz(Buzz.MEDIUM) }
                else -> { myLine = "uh... um... (you stammered)"; fx.play(Sfx.LOSE) }
            }
            hp = hp.coerceAtLeast(0)
            phase = FPhase.WINDUP
            delay(900)
            myLine = null
            if (hp <= 0) break
            // he attacks
            speech = "${stampNames.random()}!!"
            delay(400L + Random.nextLong(700))
            arrow = Random.nextInt(3) - 1
            window = 1f
            phase = FPhase.DEFEND
            fx.play(Sfx.WHOOSH)
            taps.tryReceive()
            val got = withTimeoutOrNull(950) { taps.receive() }
            if (got == arrow + 10) {
                speech = "blocked?! how dare you"
                fx.play(Sfx.SNEAK); fx.buzz(Buzz.LIGHT)
            } else {
                myHp--
                damaged = true
                hurtFlash = 1f
                speech = if (got == null) "too slow! REJECTED!" else "wrong way! haha!"
                fx.play(Sfx.YELL); fx.buzz(Buzz.HEAVY)
            }
            phase = FPhase.WINDUP
            delay(800)
        }
        phase = FPhase.DONE
        val won = hp <= 0
        speech = if (won) "FINE. take your money. and get out." else "come back when your ticket is perfect. here's half."
        if (won) { fx.play(Sfx.BIG_WIN); fx.buzz(Buzz.SUCCESS) } else { fx.play(Sfx.LOSE); fx.buzz(Buzz.FAIL) }
        delay(1800)
        onEnd(won, won && !damaged)
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable(remember { MutableInteractionSource() }, null) { if (phase == FPhase.INTRO || phase == FPhase.ATTACK) taps.trySend(0) }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val shakeX = sin(clock * 60f) * 14f * hurtFlash
                drawRoundRect(Color(0xFF3A2E5C), cornerRadius = CornerRadius(30f))
                // back wall + awning
                for (i in 0 until 9) drawRect(if (i % 2 == 0) Pink else Color.White, Offset(i * w / 9, 0f), Size(w / 9, h * 0.07f))
                // cashier
                val cx = w * 0.5f + shakeX + sin(clock * 30f) * 10f * hitFlash
                val mood = when {
                    phase == FPhase.DONE && hp <= 0 -> Mood.MEH
                    hp < maxHp / 2 -> Mood.ANGRY
                    else -> Mood.GRUMPY
                }
                drawDude(boss, cx, h * 0.78f, h * 0.62f, mood, talk = phase == FPhase.INTRO && clock % 1f < 0.6f, wobble = hitFlash, t = clock)
                // magnifying glass
                val hc = headCenter(cx, h * 0.78f, h * 0.62f)
                val hr = h * 0.62f * 0.2f
                val mg = hc + Offset(hr * 0.38f, -hr * 0.08f)
                drawCircle(Color(0xFFBFE6FF).copy(alpha = 0.5f), hr * 0.5f, mg)
                drawCircle(Ink, hr * 0.5f, mg, style = Stroke(hr * 0.12f))
                drawLine(Ink, mg + Offset(hr * 0.35f, hr * 0.35f), mg + Offset(hr * 1.1f, hr * 1.3f), hr * 0.16f, StrokeCap.Round)
                // counter
                drawRect(Color(0xFF8E6CFF), Offset(0f, h * 0.72f), Size(w, h * 0.28f))
                drawRect(Ink, Offset(0f, h * 0.72f), Size(w, h * 0.04f))
                // clipboard of defects
                val shown = lines.take(introIdx + 1)
                if (phase == FPhase.INTRO && shown.isNotEmpty()) {
                    drawRoundRect(Color(0xFFFFF3D6), Offset(w * 0.03f, h * 0.2f), Size(w * 0.3f, h * 0.48f), CornerRadius(10f))
                    drawRoundRect(Ink, Offset(w * 0.03f, h * 0.2f), Size(w * 0.3f, h * 0.48f), CornerRadius(10f), style = Stroke(4f))
                    shown.takeLast(9).forEachIndexed { i, _ ->
                        drawLine(Pink, Offset(w * 0.06f, h * (0.25f + i * 0.048f)), Offset(w * 0.08f, h * (0.27f + i * 0.048f)), 4f)
                        drawLine(Pink, Offset(w * 0.08f, h * (0.27f + i * 0.048f)), Offset(w * 0.12f, h * (0.23f + i * 0.048f)), 4f)
                        drawLine(Ink.copy(alpha = 0.4f), Offset(w * 0.14f, h * (0.255f + i * 0.048f)), Offset(w * 0.3f, h * (0.255f + i * 0.048f)), 4f)
                    }
                }
                // hp bars
                fun bar(y: Float, frac: Float, color: Color, name: String) {
                    drawRoundRect(Ink, Offset(w * 0.05f, y), Size(w * 0.9f, 26f), CornerRadius(13f))
                    drawRoundRect(color, Offset(w * 0.05f + 3f, y + 3f), Size((w * 0.9f - 6f) * frac.coerceIn(0f, 1f), 20f), CornerRadius(10f))
                    label(name, w * 0.5f, y + 13f, 18f, Color.White, Ink)
                }
                bar(h * 0.08f + 6f, hp / maxHp.toFloat(), Pink, "cashier patience $hp/$maxHp")
                bar(h * 0.94f - 30f, myHp / 5f, Mint, "your nerve $myHp/5")
                // incoming stamp arrow
                if (phase == FPhase.DEFEND) {
                    val c = Offset(w * 0.5f, h * 0.55f)
                    val r = w * 0.12f
                    rotate(arrow * 90f, c) {
                        outlined(Path().apply {
                            moveTo(c.x, c.y - r); lineTo(c.x + r * 0.8f, c.y); lineTo(c.x + r * 0.3f, c.y); lineTo(c.x + r * 0.3f, c.y + r)
                            lineTo(c.x - r * 0.3f, c.y + r); lineTo(c.x - r * 0.3f, c.y); lineTo(c.x - r * 0.8f, c.y); close()
                        }, Gold, 6f)
                    }
                    drawArc(Pink, -90f, 360f * window, false, c - Offset(r * 1.3f, r * 1.3f), Size(r * 2.6f, r * 2.6f), style = Stroke(10f, cap = StrokeCap.Round))
                }
            }
            if (speech.isNotEmpty()) {
                Text(
                    speech, color = Ink, fontWeight = FontWeight.Black, fontSize = 16.sp, textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 44.dp, start = 30.dp, end = 30.dp)
                        .background(Color.White, RoundedCornerShape(14.dp))
                        .border(2.5.dp, Ink, RoundedCornerShape(14.dp))
                        .padding(10.dp),
                )
            }
            myLine?.let {
                Text(
                    "\"$it\"", color = Ink, fontWeight = FontWeight.Black, fontSize = 18.sp, textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(20.dp)
                        .background(Mint, RoundedCornerShape(14.dp))
                        .border(2.5.dp, Ink, RoundedCornerShape(14.dp))
                        .padding(10.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        when (phase) {
            FPhase.ATTACK -> {
                Canvas(Modifier.fillMaxWidth().height(40.dp)) {
                    val w = size.width
                    val h = size.height
                    drawRoundRect(Color(0xFF4A2030), cornerRadius = CornerRadius(h / 2))
                    drawRoundRect(Mint, Offset((zone - zoneW / 2) * w, 0f), Size(zoneW * w, h), CornerRadius(10f))
                    drawRoundRect(Gold, Offset((zone - zoneW * 0.15f) * w, 0f), Size(zoneW * 0.3f * w, h), CornerRadius(6f))
                    drawRoundRect(Ink, Offset.Zero, size, CornerRadius(h / 2), style = Stroke(4f))
                    drawLine(Ink, Offset(marker * w, -4f), Offset(marker * w, h + 4f), 12f, StrokeCap.Round)
                    drawLine(Color.White, Offset(marker * w, 0f), Offset(marker * w, h), 5f, StrokeCap.Round)
                }
                Spacer(Modifier.height(8.dp))
                ChunkyButton("ARGUE!", Modifier.fillMaxWidth(), color = Mint, height = 70.dp, sfx = null) { taps.trySend(0) }
            }
            FPhase.DEFEND, FPhase.WINDUP -> {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(-1 to "◀", 0 to "▲", 1 to "▶").forEach { (d, t) ->
                        ChunkyButton(t, Modifier.weight(1f), color = Sky, height = 70.dp, enabled = phase == FPhase.DEFEND, sfx = Sfx.CLICK) { taps.trySend(d + 10) }
                    }
                }
                Spacer(Modifier.height(48.dp))
            }
            else -> {
                Text(
                    if (phase == FPhase.INTRO) "tap to skip his complaints (he'll keep going)" else "",
                    color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().height(126.dp).padding(top = 20.dp),
                )
            }
        }
    }
}

// ============================ result ============================

@Composable
private fun ResultStage(text: String, amount: Int, defects: List<String>, onBack: () -> Unit) {
    var confetti by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { if (amount > 0) confetti++ }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(if (amount > 0) "+${fmt(amount)}" else "0", color = if (amount > 0) Gold else Pink, fontWeight = FontWeight.Black, fontSize = 56.sp)
            Text(text, color = Color.White, fontSize = 17.sp, textAlign = TextAlign.Center, lineHeight = 24.sp)
            Spacer(Modifier.height(16.dp))
            if (defects.isNotEmpty()) {
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("defects found:", color = Pink, fontWeight = FontWeight.Black)
                        defects.forEach { Text("· $it", color = Dim, fontSize = 14.sp) }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            ChunkyButton("back", Modifier.fillMaxWidth(), onClick = onBack)
        }
        Confetti(confetti)
    }
}
