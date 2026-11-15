package com.lot.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt
import kotlin.random.Random

private val playerTutorial = listOf(
    "welcome, sucker! you start with 500 fake coins. spend them in the shop on tickets.",
    "scratch tickets: rub the grey layer with your finger. find 3 matching symbols and you win that symbol's prize.",
    "tv tickets: pick 5 numbers from 1 to 30, then watch the live draw. match 2 or more to win.",
    "broke? there's always mom. check the wallet tab for your stats and ticket history.",
)

@Composable
fun PlayerScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    var tab by remember { mutableIntStateOf(0) }
    var help by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(msg) { if (msg != null) { delay(1800); msg = null } }

    ModeTutorial(vm, state, "player", "player", playerTutorial, help) { help = false }

    Column(Modifier.fillMaxSize()) {
        LotTopBar("player", state.coins, { nav.back() }, { help = true })
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChunkyButton("shop", Modifier.weight(1f), color = if (tab == 0) Gold else CardHi, textColor = if (tab == 0) Ink else Color.White, height = 46.dp) { tab = 0 }
            ChunkyButton("wallet", Modifier.weight(1f), color = if (tab == 1) Gold else CardHi, textColor = if (tab == 1) Ink else Color.White, height = 46.dp) { tab = 1 }
        }
        Box(Modifier.weight(1f)) {
            if (tab == 0) {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.coins < Tiers[0].price) {
                        item {
                            Panel(Modifier.fillMaxWidth(), color = CardHi) {
                                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("you're broke lol", color = Pink, fontWeight = FontWeight.Black, fontSize = 20.sp)
                                    Text("mom sighs and reaches for her purse...", color = Dim, fontSize = 14.sp)
                                    Spacer(Modifier.height(8.dp))
                                    ChunkyButton("beg mom (+100)", Modifier.fillMaxWidth(), color = Pink, sfx = Sfx.COIN) { vm.begMom() }
                                }
                            }
                        }
                    }
                    items(Tiers) { tier ->
                        ShopCard(
                            title = tier.name, price = tier.price, sub = "top prize ${fmt(tier.topPrize)}",
                            canBuy = state.coins >= tier.price,
                            art = { MiniTicket(tier) },
                        ) {
                            if (vm.buyScratch(tier)) { fx.play(Sfx.BUY); nav.go(Screen.Scratch) } else msg = "not enough coins"
                        }
                    }
                    item {
                        ShopCard(
                            title = "tv ticket", price = TV_PRICE, sub = "pick 5, watch the live draw. jackpot ${fmt(tvPrize(5))}",
                            canBuy = state.coins >= TV_PRICE,
                            art = {
                                Canvas(Modifier.size(72.dp)) {
                                    drawRoundRect(Color(0xFF333344), Offset(size.width * 0.05f, size.height * 0.15f), Size(size.width * 0.9f, size.height * 0.65f), CornerRadius(14f))
                                    drawRoundRect(Sky, Offset(size.width * 0.12f, size.height * 0.22f), Size(size.width * 0.76f, size.height * 0.5f), CornerRadius(8f))
                                    drawBall(Offset(size.width * 0.35f, size.height * 0.47f), size.width * 0.13f, 7)
                                    drawBall(Offset(size.width * 0.65f, size.height * 0.47f), size.width * 0.13f, 21)
                                    drawLine(Ink, Offset(size.width * 0.35f, size.height * 0.15f), Offset(size.width * 0.25f, 0f), 5f)
                                    drawLine(Ink, Offset(size.width * 0.65f, size.height * 0.15f), Offset(size.width * 0.75f, 0f), 5f)
                                }
                            },
                        ) {
                            if (state.coins >= TV_PRICE) nav.go(Screen.TvPick) else msg = "not enough coins"
                        }
                    }
                }
            } else {
                WalletTab(state)
            }
            msg?.let {
                Text(
                    it, color = Ink, fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(20.dp)
                        .background(Pink, RoundedCornerShape(50))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun MiniTicket(tier: Tier) {
    Canvas(Modifier.size(72.dp)) {
        val tl = Offset(size.width * 0.06f, size.height * 0.14f)
        val sz = Size(size.width * 0.88f, size.height * 0.72f)
        drawRoundRect(tier.paper, tl, sz, CornerRadius(12f))
        drawRoundRect(Ink, tl, sz, CornerRadius(12f), style = Stroke(5f))
        drawRoundRect(Color(0xFFA9A9B8), tl + Offset(sz.width * 0.12f, sz.height * 0.35f), Size(sz.width * 0.76f, sz.height * 0.5f), CornerRadius(6f))
        drawRect(tier.accent, tl + Offset(0f, sz.height * 0.08f), Size(sz.width, sz.height * 0.18f))
        drawSymbol(tier.prizes.last().first, Offset(size.width * 0.72f, size.height * 0.62f), size.width * 0.32f)
    }
}

@Composable
private fun ShopCard(title: String, price: Int, sub: String, canBuy: Boolean, art: @Composable () -> Unit, onBuy: () -> Unit) {
    Panel(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            art()
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(sub, color = Dim, fontSize = 13.sp)
            }
            Spacer(Modifier.width(8.dp))
            ChunkyButton("${fmt(price)}c", Modifier.width(88.dp), enabled = canBuy, height = 46.dp, onClick = onBuy)
        }
    }
}

@Composable
private fun WalletTab(state: GameState) {
    val df = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Panel(Modifier.fillMaxWidth(), color = CardHi) {
                Column(Modifier.padding(18.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.size(56.dp)) { drawCoin(center, size.minDimension / 2) }
                    Text(fmt(state.coins), color = Gold, fontWeight = FontWeight.Black, fontSize = 40.sp)
                    Text("fake coins", color = Dim)
                }
            }
        }
        item {
            val net = state.totalWon - state.totalSpent
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox("tickets", fmt(state.ticketsBought), Modifier.weight(1f))
                    StatBox("wins", fmt(state.wins), Modifier.weight(1f), Mint)
                    StatBox("win rate", if (state.ticketsBought == 0) "-" else "${state.wins * 100 / state.ticketsBought}%", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox("spent", fmt(state.totalSpent), Modifier.weight(1f), Pink)
                    StatBox("won", fmt(state.totalWon), Modifier.weight(1f), Mint)
                    StatBox("net", (if (net >= 0) "+" else "") + fmt(net), Modifier.weight(1f), if (net >= 0) Mint else Pink)
                }
                StatBox("biggest single win", fmt(state.biggestWin), Modifier.fillMaxWidth(), Gold)
            }
        }
        item { Text("ticket history", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp) }
        if (state.history.isEmpty()) item { Text("nothing yet. go waste some coins.", color = Dim) }
        items(state.history) { r ->
            Panel(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.type, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("${r.detail} · ${df.format(Date(r.time))}", color = Dim, fontSize = 12.sp)
                    }
                    val net = r.prize - r.cost
                    Text(
                        if (r.prize > 0) "+${fmt(r.prize)}" else "-${fmt(r.cost)}",
                        color = if (net >= 0) Mint else Pink, fontWeight = FontWeight.Black, fontSize = 17.sp,
                    )
                }
            }
        }
    }
}

// ======================= scratch =======================

private val loseLines = listOf(
    "nothing. classic.", "the house says thanks", "so close! (it wasn't)", "maybe try being luckier",
    "that ticket was a scam and so are you", "your fake money is in a better place now",
)

@Composable
fun ScratchScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    val t = vm.currentScratch
    if (t == null) {
        LaunchedEffect(Unit) { nav.back() }
        return
    }
    val density = LocalDensity.current
    val brush = with(density) { 26.dp.toPx() }
    val grid = 18
    var revealed by remember(t.uid) { mutableStateOf(false) }
    val path = remember(t.uid) { Path() }
    var ver by remember(t.uid) { mutableIntStateOf(0) }
    val cleared = remember(t.uid) { BooleanArray(grid * grid) }
    var clearedCount by remember(t.uid) { mutableIntStateOf(0) }
    var confetti by remember { mutableIntStateOf(0) }
    val loseLine = remember(t.uid) { loseLines.random() }
    val coverAlpha by animateFloatAsState(if (revealed) 0f else 1f, tween(450), label = "cover")
    val inf = rememberInfiniteTransition(label = "glow")
    val glow by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "g")

    fun reveal() {
        if (revealed) return
        revealed = true
        vm.settleScratch(t)
        if (t.prize > 0) {
            fx.play(if (t.prize >= t.tier.price * 5) Sfx.BIG_WIN else Sfx.WIN)
            fx.buzz(Buzz.SUCCESS)
            confetti++
        } else {
            fx.play(Sfx.LOSE)
            fx.buzz(Buzz.FAIL)
        }
    }
    LaunchedEffect(clearedCount) { if (clearedCount > grid * grid * 0.62f) reveal() }
    DisposableEffect(t.uid) { onDispose { vm.settleScratch(t) } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            LotTopBar("scratch!", state.coins, { nav.back() })
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Panel(Modifier.fillMaxWidth(), color = t.tier.paper) {
                    Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(t.tier.accent, RoundedCornerShape(10.dp))
                                .padding(6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(t.tier.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        }
                        Text("match 3 symbols to win!", color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(6.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .border(3.dp, Ink, RoundedCornerShape(12.dp))
                        ) {
                            Canvas(Modifier.fillMaxSize()) {
                                val cw = size.width / 3
                                drawRoundRect(Color.White, cornerRadius = CornerRadius(30f))
                                t.cells.forEachIndexed { i, s ->
                                    val c = Offset(cw * (i % 3) + cw / 2, cw * (i / 3) + cw / 2)
                                    if (revealed && s == t.winSym) {
                                        drawCircle(Gold.copy(alpha = glow * 0.7f), cw * 0.46f, c)
                                    }
                                    drawSymbol(s, c, cw * 0.62f)
                                }
                                for (i in 1..2) {
                                    drawLine(Ink.copy(alpha = 0.15f), Offset(cw * i, 0f), Offset(cw * i, size.height), 3f)
                                    drawLine(Ink.copy(alpha = 0.15f), Offset(0f, cw * i), Offset(size.width, cw * i), 3f)
                                }
                            }
                            if (coverAlpha > 0f) {
                                Canvas(
                                    Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            compositingStrategy = CompositingStrategy.Offscreen
                                            alpha = coverAlpha
                                        }
                                        .pointerInput(t.uid) {
                                            fun mark(p: Offset) {
                                                val cs = size.width.toFloat() / grid
                                                val rad = brush / 2 + cs / 2
                                                val gx0 = ((p.x - rad) / cs).toInt().coerceIn(0, grid - 1)
                                                val gx1 = ((p.x + rad) / cs).toInt().coerceIn(0, grid - 1)
                                                val gy0 = ((p.y - rad) / cs).toInt().coerceIn(0, grid - 1)
                                                val gy1 = ((p.y + rad) / cs).toInt().coerceIn(0, grid - 1)
                                                var added = 0
                                                for (gy in gy0..gy1) for (gx in gx0..gx1) {
                                                    val cx = gx * cs + cs / 2
                                                    val cy = gy * cs + cs / 2
                                                    val dx = cx - p.x
                                                    val dy = cy - p.y
                                                    if (sqrt(dx * dx + dy * dy) <= brush / 2 + cs * 0.3f && !cleared[gy * grid + gx]) {
                                                        cleared[gy * grid + gx] = true; added++
                                                    }
                                                }
                                                if (added > 0) clearedCount += added
                                            }
                                            detectDragGestures(
                                                onDragStart = { p ->
                                                    path.moveTo(p.x, p.y); path.lineTo(p.x + 0.5f, p.y)
                                                    mark(p); ver++
                                                },
                                            ) { ch, _ ->
                                                ch.consume()
                                                val p = ch.position
                                                path.lineTo(p.x, p.y)
                                                mark(p); ver++
                                                fx.play(Sfx.SCRATCH)
                                                if (ver % 4 == 0) fx.buzz(Buzz.TICK)
                                            }
                                        }
                                ) {
                                    @Suppress("UNUSED_EXPRESSION") ver
                                    drawRoundRect(
                                        Brush.linearGradient(listOf(Color(0xFFB8B8C8), Color(0xFF8C8CA0), Color(0xFFC8C8D6))),
                                        cornerRadius = CornerRadius(30f),
                                    )
                                    val step = size.width / 6
                                    for (gy in 0 until 6) for (gx in 0 until 6) {
                                        if ((gx + gy) % 2 == 0) drawCoin(Offset(step * gx + step / 2, step * gy + step / 2), step * 0.18f)
                                    }
                                    label("scratch me", size.width / 2, size.height / 2, size.width * 0.11f, Color.White, Ink)
                                    drawPath(path, Color.Black, style = Stroke(brush, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = BlendMode.Clear)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            t.tier.prizes.forEach { (s, p) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Canvas(Modifier.size(26.dp)) { drawSymbol(s, center, size.width * 0.9f) }
                                    Text(fmt(p), color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    when {
                        !revealed -> "rub the grey stuff with your finger"
                        t.prize > 0 -> "you won ${fmt(t.prize)} fake coins!"
                        else -> loseLine
                    },
                    color = if (revealed && t.prize > 0) Gold else Color.White,
                    fontWeight = FontWeight.Black, fontSize = if (revealed) 22.sp else 16.sp, textAlign = TextAlign.Center,
                )
            }
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!revealed) {
                    ChunkyButton("just reveal it", Modifier.weight(1f), color = CardHi, textColor = Color.White) { reveal() }
                } else {
                    ChunkyButton("again (${t.tier.price}c)", Modifier.weight(1f), enabled = state.coins >= t.tier.price, sfx = Sfx.BUY) {
                        vm.buyScratch(t.tier)
                    }
                    ChunkyButton("shop", Modifier.weight(1f), color = CardHi, textColor = Color.White) { nav.back() }
                }
            }
        }
        Confetti(confetti)
    }
}

// ======================= tv =======================

@Composable
fun TvPickScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    val picks = remember { mutableStateListOf<Int>() }
    Column(Modifier.fillMaxSize()) {
        LotTopBar("pick 5 numbers", state.coins, { nav.back() })
        Text(
            "${picks.size}/5 picked", color = Dim, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Column(
            Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (row in 0 until 5) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (col in 0 until 6) {
                        val n = row * 6 + col + 1
                        val on = n in picks
                        val sc by animateFloatAsState(if (on) 1.12f else 1f, label = "b")
                        Canvas(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable {
                                    if (on) picks.remove(n) else if (picks.size < TV_PICKS) picks.add(n)
                                    fx.play(Sfx.BALL); fx.buzz(Buzz.LIGHT)
                                }
                        ) {
                            val r = size.minDimension * 0.4f * sc
                            if (on) drawBall(center, r, n, true)
                            else {
                                drawCircle(CardHi, r, center)
                                drawCircle(Ink, r, center, style = Stroke(r * 0.1f))
                                label(n.toString(), center.x, center.y, r * 0.8f, Dim)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "match 2 = ${tvPrize(2)} · 3 = ${tvPrize(3)} · 4 = ${fmt(tvPrize(4))} · 5 = ${fmt(tvPrize(5))}",
                color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChunkyButton("quick pick", Modifier.weight(1f), color = Sky) {
                picks.clear(); picks.addAll((1..TV_POOL).shuffled().take(TV_PICKS))
            }
            ChunkyButton("clear", Modifier.weight(1f), color = CardHi, textColor = Color.White) { picks.clear() }
        }
        ChunkyButton(
            "go on air (${TV_PRICE}c)",
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            enabled = picks.size == TV_PICKS && state.coins >= TV_PRICE,
            color = Pink, sfx = Sfx.BUY,
        ) {
            if (vm.buyTv(picks.toList())) nav.swap(Screen.TvShow)
        }
    }
}

private val introLines = listOf(
    "good evening and welcome to lot live!",
    "the only show where your money goes to die!",
    "let's spin that globe, baby!",
)
private val preLines = listOf("here it comes...", "drumroll please...", "the air machine decides...", "come on, come on...", "ball number %d...")
private val hitLines = listOf("it's %d! you have that one, lucky you", "%d! somebody scream!", "%d!! is this rigged? (yes)")
private val missLines = listOf("%d! nobody cares about %d", "%d. my ex's favorite number. ugh", "a %d. not yours, haha", "%d. the crowd goes mild")

private fun endLine(m: Int) = when (m) {
    0 -> "zero matches! honestly impressive"
    1 -> "one match. participation trophy, no money"
    2 -> "two matches! enough for a sad sandwich"
    3 -> "three matches!! you absolute legend"
    4 -> "FOUR?! security, check this person"
    else -> "JACKPOT! i'm calling my lawyer"
}

private class Globe(n: Int) {
    val nums = (1..n).toMutableList()
    val px = FloatArray(n) { (Random.nextFloat() - 0.5f) * 1.2f }
    val py = FloatArray(n) { (Random.nextFloat() - 0.5f) * 1.2f }
    val vx = FloatArray(n) { (Random.nextFloat() - 0.5f) * 2f }
    val vy = FloatArray(n) { (Random.nextFloat() - 0.5f) * 2f }
    val alive = BooleanArray(n) { true }
    val br = 0.12f

    fun step(dt: Float, mixing: Boolean) {
        for (i in nums.indices) {
            if (!alive[i]) continue
            vy[i] += 2.2f * dt
            if (mixing) {
                vx[i] += (Random.nextFloat() - 0.5f) * 14f * dt
                if (py[i] > 0.3f) vy[i] -= Random.nextFloat() * 22f * dt
            }
            vx[i] = vx[i].coerceIn(-3f, 3f); vy[i] = vy[i].coerceIn(-3f, 3f)
            px[i] += vx[i] * dt; py[i] += vy[i] * dt
            val d = sqrt(px[i] * px[i] + py[i] * py[i])
            val lim = 1f - br
            if (d > lim) {
                val nx = px[i] / d
                val ny = py[i] / d
                val dot = vx[i] * nx + vy[i] * ny
                if (dot > 0) { vx[i] -= 1.85f * dot * nx; vy[i] -= 1.85f * dot * ny }
                px[i] = nx * lim; py[i] = ny * lim
            }
        }
    }
}

@Composable
fun TvShowScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    val t = vm.currentTv
    if (t == null) {
        LaunchedEffect(Unit) { nav.back() }
        return
    }
    val globe = remember(t.uid) { Globe(TV_POOL) }
    var frame by remember { mutableIntStateOf(0) }
    var clock by remember { mutableFloatStateOf(0f) }
    var line by remember(t.uid) { mutableStateOf("") }
    var talking by remember { mutableStateOf(false) }
    val drawn = remember(t.uid) { mutableStateListOf<Int>() }
    var flying by remember(t.uid) { mutableStateOf<Int?>(null) }
    val flight = remember(t.uid) { Animatable(0f) }
    var done by remember(t.uid) { mutableStateOf(false) }
    var fast by remember(t.uid) { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }

    DisposableEffect(t.uid) { onDispose { vm.settleTv(t) } }

    LaunchedEffect(t.uid) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                clock += dt
                globe.step(dt, !done)
                frame++
            }
        }
    }

    LaunchedEffect(t.uid) {
        suspend fun say(s: String, ms: Long) {
            line = s; talking = true
            delay(if (fast) ms / 6 else ms)
            talking = false
        }
        for (l in introLines) say(l, 1500)
        for (i in 0 until TV_PICKS) {
            val n = t.draws[i]
            fx.play(Sfx.DRUM)
            say(preLines.random().format(i + 1), 1300)
            val idx = globe.nums.indexOf(n)
            if (idx >= 0) globe.alive[idx] = false
            flying = n
            fx.play(Sfx.WHOOSH)
            flight.snapTo(0f)
            flight.animateTo(1f, tween(if (fast) 150 else 650, easing = FastOutSlowInEasing))
            flying = null
            drawn.add(n)
            fx.play(Sfx.BALL)
            if (n in t.picks) {
                fx.play(Sfx.COIN); fx.buzz(Buzz.MEDIUM)
                say(hitLines.random().format(n, n), 1300)
            } else {
                fx.buzz(Buzz.LIGHT)
                say(missLines.random().format(n, n), 1300)
            }
        }
        vm.settleTv(t)
        done = true
        line = endLine(t.matches)
        talking = true
        if (t.prize > 0) {
            fx.play(if (t.matches >= 3) Sfx.BIG_WIN else Sfx.WIN); fx.buzz(Buzz.SUCCESS); confetti++
        } else {
            fx.play(Sfx.LOSE); fx.buzz(Buzz.FAIL)
        }
        delay(1500)
        talking = false
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            LotTopBar("lot live", state.coins, { nav.back() })
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    @Suppress("UNUSED_EXPRESSION") frame
                    val w = size.width
                    val h = size.height
                    drawRoundRect(Color(0xFF241A45), cornerRadius = CornerRadius(40f))
                    // spotlights
                    for (sx in listOf(0.28f, 0.7f)) {
                        val sway = kotlin.math.sin(clock * 0.8f + sx * 5) * w * 0.06f
                        val p = Path().apply {
                            moveTo(w * sx - w * 0.03f, 0f); lineTo(w * sx + w * 0.03f, 0f)
                            lineTo(w * sx + w * 0.2f + sway, h * 0.95f); lineTo(w * sx - w * 0.2f + sway, h * 0.95f); close()
                        }
                        drawPath(p, Color(0xFFFFF3B0).copy(alpha = 0.08f))
                    }
                    // floor
                    drawRect(Color(0xFF3A2E5C), Offset(0f, h * 0.82f), Size(w, h * 0.18f))
                    for (i in 0 until 10) drawRect(if (i % 2 == 0) Pink else Gold, Offset(i * w / 10, h * 0.82f), Size(w / 10, h * 0.025f))
                    // curtains
                    for (side in listOf(0f, 1f)) {
                        for (f in 0 until 3) {
                            val x0 = if (side == 0f) f * w * 0.04f else w - (f + 1) * w * 0.04f
                            drawRect(if (f % 2 == 0) Color(0xFFB0203A) else Color(0xFF8C1A2E), Offset(x0, 0f), Size(w * 0.04f, h * 0.82f))
                        }
                    }
                    drawRect(Color(0xFFB0203A), Offset(0f, 0f), Size(w, h * 0.06f))
                    // globe stand
                    val gc = Offset(w * 0.68f, h * 0.4f)
                    val gr = minOf(w * 0.27f, h * 0.3f)
                    val stand = Path().apply {
                        moveTo(gc.x - gr * 0.3f, gc.y + gr * 0.9f); lineTo(gc.x + gr * 0.3f, gc.y + gr * 0.9f)
                        lineTo(gc.x + gr * 0.6f, h * 0.84f); lineTo(gc.x - gr * 0.6f, h * 0.84f); close()
                    }
                    outlined(stand, Grape, 6f)
                    drawCircle(Sky.copy(alpha = 0.15f), gr, gc)
                    for (i in globe.nums.indices) {
                        if (!globe.alive[i]) continue
                        drawBall(Offset(gc.x + globe.px[i] * gr, gc.y + globe.py[i] * gr), gr * globe.br, globe.nums[i])
                    }
                    drawCircle(Ink, gr, gc, style = Stroke(8f))
                    drawCircle(Color.White.copy(alpha = 0.35f), gr * 0.2f, gc + Offset(-gr * 0.5f, -gr * 0.5f))
                    // host
                    drawDude(
                        HostDude, w * 0.25f, h * 0.86f, h * 0.55f, if (done && t.prize == 0) Mood.MEH else Mood.HAPPY,
                        talk = talking, wobble = kotlin.math.sin(clock * 3f) * 0.4f, look = 1f, t = clock,
                    )
                    // sunglasses
                    val hc = headCenter(w * 0.25f, h * 0.86f, h * 0.55f)
                    val hr = h * 0.55f * 0.2f
                    drawRoundRect(Ink, Offset(hc.x - hr * 0.75f, hc.y - hr * 0.3f), Size(hr * 0.65f, hr * 0.42f), CornerRadius(hr * 0.15f))
                    drawRoundRect(Ink, Offset(hc.x + hr * 0.1f, hc.y - hr * 0.3f), Size(hr * 0.65f, hr * 0.42f), CornerRadius(hr * 0.15f))
                    drawLine(Ink, Offset(hc.x - hr * 0.1f, hc.y - hr * 0.15f), Offset(hc.x + hr * 0.1f, hc.y - hr * 0.15f), hr * 0.1f)
                    // mic
                    val hp = handPoint(w * 0.25f, h * 0.86f, h * 0.55f) + Offset(hr * 1.4f, -hr * 0.2f)
                    drawLine(Ink, hp, hp + Offset(hr * 0.3f, -hr * 0.9f), hr * 0.22f, StrokeCap.Round)
                    circleO(hp + Offset(hr * 0.35f, -hr * 1.05f), hr * 0.25f, Color(0xFF9E9E9E), 5f)
                    circleO(hp, hr * 0.2f, HostDude.skin, 5f)
                    // flying ball
                    flying?.let { n ->
                        val p = flight.value
                        val from = gc
                        val to = Offset(w * 0.5f, h * 1.05f)
                        val x = from.x + (to.x - from.x) * p
                        val y = from.y + (to.y - from.y) * p - kotlin.math.sin(p * Math.PI.toFloat()) * h * 0.25f
                        drawBall(Offset(x, y), gr * 0.2f * (1f + p * 0.5f), n, n in t.picks)
                    }
                }
                if (line.isNotEmpty()) {
                    Text(
                        line,
                        color = Ink, fontWeight = FontWeight.Black, fontSize = 16.sp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 28.dp, start = 20.dp, end = 120.dp)
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .border(2.5.dp, Ink, RoundedCornerShape(16.dp))
                            .padding(10.dp),
                    )
                }
            }
            // drawn balls + your ticket
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("drawn", color = Dim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                BallRow(List(TV_PICKS) { drawn.getOrNull(it) }) { it in t.picks }
                Spacer(Modifier.height(6.dp))
                Text("your ticket", color = Dim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                BallRow(t.picks) { it in drawn }
            }
            if (done) {
                Text(
                    if (t.prize > 0) "${t.matches} matches · you won ${fmt(t.prize)}!" else "${t.matches} matches · no prize",
                    color = if (t.prize > 0) Gold else Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChunkyButton("play again", Modifier.weight(1f), enabled = state.coins >= TV_PRICE) { nav.swap(Screen.TvPick) }
                    ChunkyButton("shop", Modifier.weight(1f), color = CardHi, textColor = Color.White) { nav.back() }
                }
            } else {
                ChunkyButton(
                    if (fast) "speeding..." else "skip ahead", Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = CardHi, textColor = Color.White, enabled = !fast,
                ) { fast = true }
            }
        }
        Confetti(confetti)
    }
}

@Composable
private fun BallRow(nums: List<Int?>, hl: (Int) -> Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        nums.forEach { n ->
            Canvas(
                Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                val r = size.height * 0.42f
                if (n == null) {
                    drawCircle(CardHi, r, center)
                    label("?", center.x, center.y, r, Dim)
                } else drawBall(center, r, n, hl(n))
            }
        }
    }
}
