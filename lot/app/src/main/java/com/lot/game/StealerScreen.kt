package com.lot.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private val stealTutorial = listOf(
    "you're hiding in a bush next to the lot stand. very professional.",
    "today's winning numbers are on the board. customers hold up their tickets at the counter, but the numbers are tiny.",
    "drag the binoculars around to zoom in and read the tickets. if all 3 numbers match the board, it's a winner.",
    "hit grab! while they're at the counter, then tap sneak when the marker is in the green zone. miss and the guards catch you.",
    "you have 3 lives. every round brings more guards and faster customers. good luck, criminal.",
)

private const val COUNTER_X = 0.64f
private const val ZOOM = 3.2f

internal class StealTicket(val nums: List<Int>, val winner: Boolean)
internal class Walker(val dude: Dude, val ticket: StealTicket) {
    var x = 1.15f
    var phase = 0 // 0 walking in, 1 at counter, 2 walking out
    var timer = 0f
    var robbed = false
}

internal class Guard(val dude: Dude, var x: Float, var dir: Float, val speed: Float)

private class MiniGame(val walker: Walker, val steps: Int, val zoneW: Float, val speed: Float) {
    var done by mutableIntStateOf(0)
    var marker by mutableFloatStateOf(0f)
    var dir = 1f
    var zone by mutableFloatStateOf(0.5f)
    var perfect = true
    var flash by mutableStateOf<String?>(null)
}

internal class StealGame {
    var round by mutableIntStateOf(1)
    var lives by mutableIntStateOf(3)
    var loot by mutableIntStateOf(0)
    var board by mutableStateOf(listOf<Int>())
    val queue = ArrayDeque<Walker>()
    var current by mutableStateOf<Walker?>(null)
    var atCounter by mutableStateOf(false)
    val guards = mutableListOf<Guard>()
    var roundDone by mutableStateOf(false)
    var gameOver by mutableStateOf(false)
    var frame by mutableLongStateOf(0L)
    var clock = 0f
    var winnersLeft by mutableIntStateOf(0)

    val walkSpeed get() = 0.22f + 0.035f * round
    val counterTime get() = (5.2f - 0.4f * round).coerceAtLeast(1.7f)
    val guardCount get() = (1 + round / 2).coerceAtMost(5)
    val prize get() = 100 + 50 * round

    fun startRound(r: Int) {
        round = r
        roundDone = false
        board = (1..20).shuffled().take(3).sorted()
        val count = 4 + r
        val winners = (1 + (r - 1) / 3).coerceAtMost(count)
        val winSlots = (0 until count).shuffled().take(winners).toSet()
        queue.clear()
        for (i in 0 until count) {
            val t = if (i in winSlots) StealTicket(board.shuffled(), true) else decoy()
            queue.add(Walker(randomDude(), t))
        }
        winnersLeft = winners
        current = null
        atCounter = false
        guards.clear()
        repeat(guardCount) {
            guards.add(Guard(guardDude(Random), Random.nextFloat(), if (Random.nextBoolean()) 1f else -1f, 0.08f + Random.nextFloat() * 0.06f + r * 0.01f))
        }
    }

    private fun decoy(): StealTicket {
        val others = (1..20).filter { it !in board }.shuffled()
        val nums = if (Random.nextFloat() < 0.6f) {
            (board.shuffled().take(2) + others.first()).shuffled()
        } else {
            others.take(3)
        }
        return StealTicket(nums, false)
    }

    fun step(dt: Float) {
        guards.forEach { g ->
            g.x += g.dir * g.speed * dt
            if (g.x > 0.95f) { g.x = 0.95f; g.dir = -1f }
            if (g.x < 0.3f) { g.x = 0.3f; g.dir = 1f }
        }
        val c = current
        if (c == null) {
            if (queue.isNotEmpty()) current = queue.removeFirst()
            else if (!roundDone) roundDone = true
            return
        }
        when (c.phase) {
            0 -> {
                c.x -= walkSpeed * dt
                if (c.x <= COUNTER_X) { c.x = COUNTER_X; c.phase = 1; c.timer = counterTime; atCounter = true }
            }
            1 -> {
                c.timer -= dt
                if (c.timer <= 0f) leaveCounter(c)
            }
            2 -> {
                c.x -= walkSpeed * 1.3f * dt
                if (c.x < -0.25f) current = null
            }
        }
    }

    fun leaveCounter(c: Walker) {
        c.phase = 2
        atCounter = false
        if (c.ticket.winner && !c.robbed) winnersLeft--
    }
}

@Composable
fun StealerScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    var help by remember { mutableStateOf(false) }
    ModeTutorial(vm, state, "stealer", "stealer", stealTutorial, help) { help = false }
    val g = remember { StealGame().also { it.startRound(1) } }
    var mini by remember { mutableStateOf<MiniGame?>(null) }
    var banner by remember { mutableStateOf<String?>("round 1") }
    var bannerColor by remember { mutableStateOf(Gold) }
    var lens by remember { mutableStateOf(Offset.Unspecified) }
    val paused by rememberUpdatedState(help || "stealer" !in state.tutorialsSeen)

    fun showBanner(text: String, color: Color) { banner = text; bannerColor = color }

    LaunchedEffect(banner) { if (banner != null) { delay(1500); banner = null } }

    LaunchedEffect(g.round) { vm.update { it.copy(bestRound = maxOf(it.bestRound, g.round)) } }

    LaunchedEffect(g.roundDone) {
        if (g.roundDone && !g.gameOver) {
            fx.play(Sfx.WIN)
            showBanner("round ${g.round} cleared!", Mint)
            delay(1600)
            g.startRound(g.round + 1)
            showBanner("round ${g.round} · ${g.guardCount} guard${if (g.guardCount > 1) "s" else ""}", Gold)
        }
    }

    // main loop
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                g.clock += dt
                val m = mini
                if (m != null) {
                    m.marker += m.dir * m.speed * dt
                    if (m.marker > 1f) { m.marker = 1f; m.dir = -1f }
                    if (m.marker < 0f) { m.marker = 0f; m.dir = 1f }
                } else if (!paused && banner == null && !g.gameOver) {
                    g.step(dt)
                }
                g.frame++
            }
        }
    }

    fun grab() {
        val c = g.current ?: return
        if (c.phase != 1 || mini != null) return
        fx.play(Sfx.SNEAK); fx.buzz(Buzz.MEDIUM)
        val steps = (1 + g.guardCount).coerceAtMost(5)
        val zoneW = (0.34f - 0.03f * g.guardCount - 0.008f * g.round).coerceAtLeast(0.12f)
        mini = MiniGame(c, steps, zoneW, 0.9f + 0.12f * g.round).also { it.zone = 0.25f + Random.nextFloat() * 0.5f }
    }

    fun finishMini(success: Boolean, m: MiniGame) {
        val c = m.walker
        mini = null
        if (success) {
            c.robbed = true
            if (c.ticket.winner) {
                val p = g.prize
                g.loot += p
                g.winnersLeft--
                fx.play(if (m.perfect) Sfx.BIG_WIN else Sfx.COIN); fx.buzz(Buzz.SUCCESS)
                showBanner(if (m.perfect) "perfect steal! +${fmt(p)}" else "got it! +${fmt(p)}", Mint)
                vm.update {
                    it.copy(
                        coins = it.coins + p, steals = it.steals + 1, stealLoot = it.stealLoot + p,
                        perfectSteals = it.perfectSteals + if (m.perfect) 1 else 0,
                    )
                }
            } else {
                fx.play(Sfx.LOSE); fx.buzz(Buzz.LIGHT)
                showBanner("it was a dud. you stole trash.", Sky)
                vm.update { it.copy(flags = it.flags + "dud") }
            }
        } else {
            g.lives--
            fx.play(Sfx.CAUGHT); fx.buzz(Buzz.FAIL)
            vm.update { it.copy(coins = (it.coins - 50).coerceAtLeast(0), timesCaught = it.timesCaught + 1) }
            if (g.lives <= 0) {
                g.gameOver = true
            } else {
                showBanner("busted! -50c, -1 life", Pink)
            }
        }
        if (c.phase == 1) g.leaveCounter(c)
    }

    fun sneakTap() {
        val m = mini ?: return
        val d = abs(m.marker - m.zone)
        if (d <= m.zoneW / 2) {
            if (d > m.zoneW * 0.15f) m.perfect = false
            m.done++
            m.flash = if (d <= m.zoneW * 0.15f) "perfect!" else "nice"
            fx.play(Sfx.SNEAK); fx.buzz(Buzz.LIGHT)
            if (m.done >= m.steps) finishMini(true, m)
            else m.zone = (m.zoneW / 2 + Random.nextFloat() * (1f - m.zoneW)).coerceIn(0f, 1f)
        } else {
            finishMini(false, m)
        }
    }

    Column(Modifier.fillMaxSize()) {
        LotTopBar("stealer", state.coins, { nav.back() }, { help = true })
        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("round", "${g.round}", Modifier.weight(1f))
            StatBox("lives", "♥".repeat(g.lives.coerceAtLeast(0)).ifEmpty { "-" }, Modifier.weight(1f), Pink)
            StatBox("loot", fmt(g.loot), Modifier.weight(1f), Mint)
        }
        Text(
            "winning numbers: ${g.board.joinToString(" · ")}",
            color = Gold, fontWeight = FontWeight.Black, fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 6.dp),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp)
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(onDragStart = { p -> lens = p }) { ch, drag ->
                            ch.consume()
                            val l = if (lens == Offset.Unspecified) ch.position else lens
                            lens = Offset(
                                (l.x + drag.x).coerceIn(0f, size.width.toFloat()),
                                (l.y + drag.y).coerceIn(0f, size.height.toFloat()),
                            )
                        }
                    }
            ) {
                @Suppress("UNUSED_EXPRESSION") g.frame
                val w = size.width
                val h = size.height
                if (lens == Offset.Unspecified) lens = Offset(w * 0.62f, h * 0.55f)
                stealScene(g, w, h)
                // binoculars lens
                val pro = "lens" in state.items
                val r = w * (if (pro) 0.26f else 0.2f)
                val zoom = if (pro) 4f else ZOOM
                val lc = lens
                val clip = Path().apply { addOval(Rect(lc, r)) }
                clipPath(clip) {
                    drawRect(Color.Black)
                    withTransform({ scale(zoom, zoom, lc) }) { stealScene(g, w, h) }
                    drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)), lc, r), r, lc)
                }
                drawCircle(Ink, r + 7f, lc, style = Stroke(16f))
                drawCircle(Color(0xFF555566), r + 7f, lc, style = Stroke(6f))
                drawArc(Color.White.copy(alpha = 0.35f), 200f, 60f, false, lc - Offset(r * 0.8f, r * 0.8f), Size(r * 1.6f, r * 1.6f), style = Stroke(8f, cap = StrokeCap.Round))
            }
            Text(
                "drag to move binoculars",
                color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(6.dp),
            )
            banner?.let {
                Text(
                    it, color = Ink, fontWeight = FontWeight.Black, fontSize = 22.sp, textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(20.dp)
                        .background(bannerColor, RoundedCornerShape(16.dp))
                        .border(3.dp, Ink, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                )
            }
            mini?.let { m -> SneakGame(m, g) { sneakTap() } }
            if (g.gameOver) {
                GameOverPanel(g, state.bestRound, onRetry = {
                    g.lives = 3; g.loot = 0; g.gameOver = false
                    g.startRound(1)
                    showBanner("round 1", Gold)
                }, onHome = { nav.back() })
            }
        }
        val c = g.current
        ChunkyButton(
            if (g.atCounter && c != null) "grab it! (${"%.1f".format(c.timer.coerceAtLeast(0f))}s)" else "wait for a customer...",
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            color = Mint,
            enabled = g.atCounter && mini == null && !g.gameOver && banner == null,
            height = 60.dp,
            sfx = null,
        ) { grab() }
    }
}

internal fun DrawScope.stealScene(g: StealGame, w: Float, h: Float) {
    val t = g.clock
    // sky + wall
    drawRect(Brush.verticalGradient(listOf(Color(0xFF241B4A), Color(0xFF5B3D7A))), size = Size(w, h * 0.72f))
    drawCircle(Color(0xFFFFF3B0), w * 0.06f, Offset(w * 0.15f, h * 0.12f))
    drawCircle(Color(0xFF241B4A), w * 0.05f, Offset(w * 0.18f, h * 0.1f))
    for (i in 0 until 12) drawCircle(Color.White.copy(alpha = 0.5f), 2.5f, Offset(w * ((i * 37) % 100) / 100f, h * ((i * 23) % 30) / 100f))
    // brick wall
    drawRect(Color(0xFF4A3466), Offset(0f, h * 0.38f), Size(w, h * 0.34f))
    for (row in 0 until 6) for (col in 0 until 9) {
        val bx = w * (col / 8f) + if (row % 2 == 0) 0f else w / 16f
        drawRect(Color(0xFF3D2A57), Offset(bx, h * (0.38f + row * 0.057f)), Size(w / 9f, h * 0.05f), style = Stroke(3f))
    }
    // guards walk behind, on the far sidewalk
    g.guards.forEach { gd ->
        drawDude(gd.dude, w * gd.x, h * 0.74f, h * 0.24f, Mood.GRUMPY, wobble = sin(t * 9f + gd.x * 10) * 0.5f, look = gd.dir, t = t)
        // flashlight cone
        val hc = headCenter(w * gd.x, h * 0.74f, h * 0.24f)
        val p = Path().apply {
            moveTo(hc.x, hc.y + h * 0.04f)
            lineTo(hc.x + gd.dir * w * 0.25f, hc.y + h * 0.12f)
            lineTo(hc.x + gd.dir * w * 0.25f, hc.y + h * 0.0f)
            close()
        }
        drawPath(p, Color(0xFFFFF3B0).copy(alpha = 0.12f))
    }
    // the stand
    drawStand(w * 0.46f, w * 0.98f, h * 0.3f, h * 0.55f, h * 0.74f)
    // board with winning numbers
    val br = Rect(w * 0.55f, h * 0.4f, w * 0.89f, h * 0.52f)
    drawRoundRect(Color(0xFF1E3A2B), br.topLeft, br.size, CornerRadius(12f))
    drawRoundRect(Ink, br.topLeft, br.size, CornerRadius(12f), style = Stroke(5f))
    label("winners: ${g.board.joinToString(" ")}", br.center.x, br.center.y, h * 0.035f, Color.White)
    // the street
    drawRect(Color(0xFF3A3550), Offset(0f, h * 0.72f), Size(w, h * 0.28f))
    drawRect(Color(0xFF5A5575), Offset(0f, h * 0.72f), Size(w, h * 0.03f))
    // customer
    val c = g.current
    if (c != null) {
        val cx = w * c.x
        val walking = c.phase != 1
        val ch = h * 0.42f
        val base = h * 0.95f - if (walking) abs(sin(t * 11f)) * h * 0.012f else 0f
        drawDude(
            c.dude, cx, base, ch, if (c.robbed) Mood.ANGRY else Mood.MEH,
            wobble = if (walking) sin(t * 11f) * 0.5f else 0f, look = if (c.phase == 1) 1f else -1f,
            holdTicket = c.phase == 1 && !c.robbed, t = t,
        )
        if (!c.robbed) {
            val hp = handPoint(cx, base, ch)
            val tw = w * 0.085f
            val th = w * 0.045f
            val tl = Offset(hp.x - tw / 2, hp.y - th * 0.9f)
            drawRoundRect(Color(0xFFFFF3D6), tl, Size(tw, th), CornerRadius(3f))
            drawRoundRect(Ink, tl, Size(tw, th), CornerRadius(3f), style = Stroke(1.5f))
            drawRect(Pink, tl, Size(tw, th * 0.22f))
            // tiny numbers, unreadable without the binoculars
            label(c.ticket.nums.joinToString(" "), tl.x + tw / 2, tl.y + th * 0.62f, w * 0.0105f, Color(0xFF333333))
        }
    }
    // your hiding bush, in the foreground
    drawBush(Offset(w * 0.15f, h * 0.88f), w * 0.15f, true, if (c != null && c.phase == 1) 1f else sin(t), t)
}

@Composable
private fun SneakGame(m: MiniGame, g: StealGame, onTap: () -> Unit) {
    LaunchedEffect(m.done) { delay(450); m.flash = null }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(remember { MutableInteractionSource() }, null) { onTap() },
        contentAlignment = Alignment.Center,
    ) {
        Panel(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("sneak! ${m.done}/${m.steps}", color = Mint, fontWeight = FontWeight.Black, fontSize = 24.sp)
                Text("tap when the marker is in the green", color = Dim, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                // tiny path view: you sneaking from the bush to the counter
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                ) {
                    @Suppress("UNUSED_EXPRESSION") g.frame
                    val w = size.width
                    val h = size.height
                    drawRoundRect(Color(0xFF3A3550), cornerRadius = CornerRadius(20f))
                    drawBush(Offset(w * 0.1f, h * 0.65f), h * 0.35f, false, 0f, 0f)
                    drawRect(Pink, Offset(w * 0.82f, h * 0.2f), Size(w * 0.16f, h * 0.7f))
                    label("lot", w * 0.9f, h * 0.35f, h * 0.2f, Ink)
                    for (i in 0 until m.steps) {
                        val x = w * (0.2f + 0.6f * (i + 1) / m.steps)
                        drawCircle(if (i < m.done) Mint else Color.White.copy(alpha = 0.3f), h * 0.05f, Offset(x, h * 0.85f))
                    }
                    // you, a guy in a black hoodie and a mask
                    val px = w * (0.15f + 0.65f * m.done / m.steps)
                    val you = Dude(Color(0xFFFFD7B5), Color(0xFF222233), Color(0xFF222233), 3, Color(0xFF222233), "you", false, 0.9f)
                    drawDude(you, px, h * 0.98f, h * 0.85f, Mood.MEH, wobble = sin(g.clock * 8f) * 0.3f, look = 1f, t = g.clock)
                    val hc = headCenter(px, h * 0.98f, h * 0.85f)
                    drawRect(Ink, Offset(hc.x - h * 0.17f, hc.y - h * 0.07f), Size(h * 0.34f, h * 0.06f))
                    // guards watching
                    for (i in 0 until g.guardCount.coerceAtMost(3)) {
                        drawCircle(Gold.copy(alpha = 0.6f + 0.4f * sin(g.clock * 5f + i)), h * 0.06f, Offset(w * (0.45f + i * 0.12f), h * 0.15f))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    drawRoundRect(Color(0xFF4A2030), cornerRadius = CornerRadius(h / 2))
                    val zx = (m.zone - m.zoneW / 2) * w
                    drawRoundRect(Mint, Offset(zx, 0f), Size(m.zoneW * w, h), CornerRadius(12f))
                    drawRoundRect(Gold, Offset((m.zone - m.zoneW * 0.15f) * w, 0f), Size(m.zoneW * 0.3f * w, h), CornerRadius(8f))
                    drawRoundRect(Ink, Offset.Zero, size, CornerRadius(h / 2), style = Stroke(5f))
                    val mx = m.marker * w
                    drawLine(Ink, Offset(mx, -4f), Offset(mx, h + 4f), 14f, StrokeCap.Round)
                    drawLine(Color.White, Offset(mx, 0f), Offset(mx, h), 6f, StrokeCap.Round)
                }
                Spacer(Modifier.height(8.dp))
                Text(m.flash ?: " ", color = Gold, fontWeight = FontWeight.Black, fontSize = 18.sp)
                ChunkyButton("sneak!", Modifier.fillMaxWidth(), color = Mint, height = 60.dp, sfx = null) { onTap() }
            }
        }
    }
}

@Composable
private fun GameOverPanel(g: StealGame, best: Int, onRetry: () -> Unit, onHome: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        Panel(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("busted for good", color = Pink, fontWeight = FontWeight.Black, fontSize = 28.sp)
                Text("the guards escort you out of the bush", color = Dim, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox("reached round", "${g.round}", Modifier.weight(1f))
                    StatBox("loot", fmt(g.loot), Modifier.weight(1f), Mint)
                    StatBox("best", "$best", Modifier.weight(1f), Gold)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChunkyButton("try again", Modifier.weight(1f), color = Mint, onClick = onRetry)
                    ChunkyButton("home", Modifier.weight(1f), color = CardHi, textColor = Color.White, onClick = onHome)
                }
            }
        }
    }
}
