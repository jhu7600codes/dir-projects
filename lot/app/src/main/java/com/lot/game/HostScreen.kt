package com.lot.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private val hostTutorial = listOf(
    "you run the lot stand now. customers walk up with an order, you sell them tickets.",
    "if their ticket wins you have to pay the prize out of today's cash. or... pretend it lost. they might notice.",
    "the yell button is very funny. it also makes customers angry, and angry customers leave and wreck your reputation.",
    "don't make them wait too long either. at the end of the day you see your profit, and it goes into your wallet.",
)

private val yellLines = listOf(
    "you're not winning anyway",
    "hurry up, i have a life",
    "my grandma scratches faster",
    "this ticket smells like loss",
    "no refunds. no feelings.",
    "buy more or leave forever",
)

private val laughLines = listOf("haha fair", "lmao ok true", "you're funny, i like you", "*snorts*")
private val hurtLines = listOf("wow. ok.", "rude.", "excuse me??", "i'm telling my mom")
private val leaveLines = listOf("that's it, i'm leaving!", "one star review!!", "i'm going to the other stand", "worst. stand. ever.")
private val waitLines = listOf("hellooo?", "i'm getting old here", "*taps foot*", "any day now")

private enum class HPhase { ARRIVING, ORDERING, RESULTS, LEAVING, DAY_OVER }

private class Order(val tier: Tier?, val qty: Int) {
    val unit get() = tier?.price ?: TV_PRICE
    val price get() = unit * qty
    val label get() = "$qty ${(tier?.name ?: "tv ticket")}${if (qty > 1) "s" else ""}"
}

private class Customer(val dude: Dude, mood: Mood, val order: Order) {
    var mood by mutableStateOf(mood)
    var patience by mutableFloatStateOf(1f)
    var line by mutableStateOf("")
    var prizes: List<Int> = emptyList()
    val won get() = prizes.sum()
    var waitNagged = false
}

private class Floater(val text: String, val color: Color, val born: Float)

@Composable
fun HostScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    val scope = rememberCoroutineScope()
    var help by remember { mutableStateOf(false) }
    val paused by rememberUpdatedState(help || "host" !in state.tutorialsSeen)
    ModeTutorial(vm, state, "host", "host", hostTutorial, help) { help = false }

    var day by remember { mutableIntStateOf(0) }
    val total = 8
    var index by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(HPhase.ARRIVING) }
    var cust by remember { mutableStateOf<Customer?>(null) }
    val walk = remember { Animatable(1.3f) }
    var clock by remember { mutableFloatStateOf(0f) }
    var sales by remember { mutableIntStateOf(0) }
    var payouts by remember { mutableIntStateOf(0) }
    var dayYells by remember { mutableIntStateOf(0) }
    var dayServed by remember { mutableIntStateOf(0) }
    var walkouts by remember { mutableIntStateOf(0) }
    var scams by remember { mutableIntStateOf(0) }
    var repStart by remember { mutableIntStateOf(state.reputation) }
    var myYell by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var settled by remember { mutableStateOf(false) }
    val floaters = remember { mutableStateListOf<Floater>() }

    fun rep(delta: Int) {
        if (delta == 0) return
        vm.update { it.copy(reputation = (it.reputation + delta).coerceIn(0, 100)) }
        floaters.add(Floater("${if (delta > 0) "+" else ""}$delta rep", if (delta > 0) Mint else Pink, clock))
    }

    fun newCustomer(): Customer {
        val r = state.reputation / 100f
        val roll = Random.nextFloat()
        val mood = when {
            roll < 0.15f + 0.45f * r -> Mood.HAPPY
            roll < 0.75f + 0.1f * r -> Mood.MEH
            else -> Mood.GRUMPY
        }
        val tier = when (Random.nextInt(10)) {
            in 0..3 -> Tiers[0]
            in 4..6 -> Tiers[1]
            7 -> Tiers[2]
            else -> null
        }
        val qty = 1 + Random.nextInt(if (r > 0.6f) 3 else 2)
        return Customer(randomDude(), mood, Order(tier, qty))
    }

    suspend fun leave() {
        phase = HPhase.LEAVING
        fx.play(Sfx.WHOOSH)
        walk.animateTo(-1.4f, tween(800, easing = LinearEasing))
        if (index + 1 >= total) {
            phase = HPhase.DAY_OVER
        } else {
            index++
        }
    }

    // spawn customer whenever index/day changes
    LaunchedEffect(index, day) {
        val c = newCustomer()
        cust = c
        phase = HPhase.ARRIVING
        walk.snapTo(1.4f)
        fx.play(Sfx.WHOOSH)
        walk.animateTo(0f, tween(900, easing = LinearEasing))
        c.line = when (c.mood) {
            Mood.HAPPY -> "hiii! ${c.order.label} please!"
            Mood.MEH -> "uh. ${c.order.label}."
            else -> "${c.order.label}. make it quick."
        }
        phase = HPhase.ORDERING
    }

    // patience + clock
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                clock += dt
                if (!paused) {
                    val c = cust
                    if (c != null && phase == HPhase.ORDERING && !busy) {
                        val rate = when (c.mood) { Mood.HAPPY -> 0.05f; Mood.MEH -> 0.07f; Mood.GRUMPY -> 0.1f; Mood.ANGRY -> 0.16f }
                        c.patience -= dt * rate
                        if (c.patience < 0.35f && !c.waitNagged) {
                            c.waitNagged = true; c.line = waitLines.random()
                        }
                    }
                }
                floaters.removeAll { clock - it.born > 1.6f }
            }
        }
    }
    // walking out when patience is gone
    LaunchedEffect(cust, phase) {
        val c = cust ?: return@LaunchedEffect
        if (phase != HPhase.ORDERING) return@LaunchedEffect
        while (c.patience > 0f) delay(100)
        if (phase == HPhase.ORDERING && !busy) {
            busy = true
            c.mood = Mood.ANGRY
            c.line = "forget it, i'm out!"
            fx.play(Sfx.ANGRY); fx.buzz(Buzz.MEDIUM)
            walkouts++
            rep(-3)
            delay(900)
            leave()
            busy = false
        }
    }

    fun sell() {
        val c = cust ?: return
        if (phase != HPhase.ORDERING || busy) return
        busy = true
        scope.launch {
            fx.play(Sfx.BUY); fx.buzz(Buzz.LIGHT)
            sales += c.order.price
            dayServed++
            vm.update { it.copy(served = it.served + 1) }
            c.prizes = List(c.order.qty) {
                if (c.order.tier != null) genScratch(c.order.tier).prize
                else genTv((1..TV_POOL).shuffled().take(TV_PICKS)).prize
            }
            c.line = "ooh let's see..."
            phase = HPhase.RESULTS
            fx.play(Sfx.SCRATCH)
            delay(500); fx.play(Sfx.SCRATCH); delay(500)
            if (c.won > 0) {
                c.mood = Mood.HAPPY
                c.line = "I WON ${fmt(c.won)}! pay up!!"
                fx.play(Sfx.WIN); fx.buzz(Buzz.MEDIUM)
                busy = false
            } else {
                c.line = listOf("lost. obviously.", "nothing again...", "this game hates me", "why do i keep doing this").random()
                fx.play(Sfx.LOSE)
                delay(1300)
                leave()
                busy = false
            }
        }
    }

    fun pay() {
        val c = cust ?: return
        if (busy) return
        busy = true
        scope.launch {
            payouts += c.won
            fx.play(Sfx.COIN); fx.buzz(Buzz.SUCCESS)
            c.line = "pleasure doing business!"
            rep(2)
            delay(900)
            leave()
            busy = false
        }
    }

    fun scam() {
        val c = cust ?: return
        if (busy) return
        busy = true
        scope.launch {
            fx.play(Sfx.SNEAK)
            c.line = "wait, it lost? are you sure?"
            delay(1000)
            if (Random.nextFloat() < 0.55f) {
                scams++
                c.mood = Mood.MEH
                c.line = "aw shucks. i guess i lost."
                vm.update { it.copy(flags = it.flags + "scam") }
                rep(-1)
                fx.play(Sfx.LAUGH)
            } else {
                c.mood = Mood.ANGRY
                c.line = "I CAN READ, YOU KNOW! pay me!!"
                payouts += c.won
                fx.play(Sfx.ANGRY); fx.buzz(Buzz.FAIL)
                rep(-12)
            }
            delay(1200)
            leave()
            busy = false
        }
    }

    fun yell(line: String) {
        val c = cust ?: return
        if (phase != HPhase.ORDERING || busy) return
        busy = true
        dayYells++
        vm.update { it.copy(yells = it.yells + 1) }
        scope.launch {
            myYell = line
            fx.play(Sfx.YELL); fx.buzz(Buzz.HEAVY)
            delay(800)
            myYell = null
            val roll = Random.nextFloat()
            val (laugh, leaveP) = when (c.mood) {
                Mood.HAPPY -> 0.55f to 0f
                Mood.MEH -> 0.35f to 0.2f
                Mood.GRUMPY -> 0.15f to 0.4f
                Mood.ANGRY -> 0f to 0.85f
            }
            when {
                roll < laugh -> {
                    c.line = laughLines.random(); fx.play(Sfx.LAUGH); rep(1)
                    busy = false
                }
                roll < laugh + leaveP -> {
                    c.mood = Mood.ANGRY; c.line = leaveLines.random()
                    fx.play(Sfx.ANGRY); fx.buzz(Buzz.FAIL)
                    walkouts++
                    rep(if (c.line.contains("star")) -9 else -5)
                    delay(1000)
                    leave()
                    busy = false
                }
                else -> {
                    c.mood = c.mood.worse(); c.line = hurtLines.random()
                    c.patience = (c.patience - 0.15f).coerceAtLeast(0.05f)
                    fx.play(Sfx.ANGRY); fx.buzz(Buzz.LIGHT)
                    rep(-1)
                    busy = false
                }
            }
        }
    }

    // end of day bookkeeping
    LaunchedEffect(phase) {
        if (phase == HPhase.DAY_OVER && !settled) {
            settled = true
            val profit = sales - payouts
            vm.update {
                it.copy(
                    coins = (it.coins + profit).coerceAtLeast(0),
                    hostDays = it.hostDays + 1,
                    hostTotalProfit = it.hostTotalProfit + profit,
                    bestDayProfit = maxOf(it.bestDayProfit, profit),
                )
            }
            if (profit > 0) { fx.play(Sfx.WIN); fx.buzz(Buzz.SUCCESS) } else { fx.play(Sfx.LOSE); fx.buzz(Buzz.FAIL) }
        }
    }

    fun nextDay() {
        sales = 0; payouts = 0; dayYells = 0; dayServed = 0; walkouts = 0; scams = 0
        repStart = state.reputation
        settled = false
        index = 0
        day++
    }

    Column(Modifier.fillMaxSize()) {
        LotTopBar("host · day ${state.hostDays + if (phase == HPhase.DAY_OVER) 0 else 1}", state.coins, { nav.back() }, { help = true })
        if (phase == HPhase.DAY_OVER) {
            DaySummary(
                sales, payouts, dayServed, walkouts, dayYells, scams, repStart, state.reputation,
                onNext = { nextDay() }, onHome = { nav.back() },
            )
            return@Column
        }
        // hud
        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("customer", "${index + 1}/$total", Modifier.weight(1f))
            StatBox("profit", fmt(sales - payouts), Modifier.weight(1f), if (sales - payouts >= 0) Mint else Pink)
            StatBox("rep", "${state.reputation}", Modifier.weight(1f), repColor(state.reputation))
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF3B2B6E), Color(0xFFFF8C6B))), cornerRadius = CornerRadius(40f))
                // buildings
                val bs = listOf(0.0f to 0.45f, 0.18f to 0.3f, 0.36f to 0.5f, 0.6f to 0.35f, 0.78f to 0.55f)
                bs.forEach { (x, top) ->
                    drawRect(Color(0xFF2A1F4E), Offset(w * x, h * top), Size(w * 0.2f, h * 0.7f - h * top))
                    for (wy in 0 until 4) for (wx in 0 until 2) {
                        val lit = ((x * 10).toInt() + wy + wx) % 3 != 0
                        drawRect(if (lit) Gold.copy(alpha = 0.7f) else Color(0xFF1B1530), Offset(w * (x + 0.04f + wx * 0.08f), h * (top + 0.05f + wy * 0.07f)), Size(w * 0.04f, h * 0.035f))
                    }
                }
                drawRect(Color(0xFF555070), Offset(0f, h * 0.68f), Size(w, h * 0.32f))
                // customer
                val c = cust
                if (c != null) {
                    val walking = phase == HPhase.ARRIVING || phase == HPhase.LEAVING
                    val cx = w * (0.5f + walk.value * 0.75f)
                    val bob = if (walking) abs(sin(clock * 12f)) * h * 0.02f else 0f
                    val ch = h * 0.78f
                    val base = h * 1.02f - bob
                    drawDude(
                        c.dude, cx, base, ch, c.mood,
                        talk = !walking && clock % 3f < 0.6f, wobble = if (walking) sin(clock * 12f) * 0.6f else 0f,
                        look = if (phase == HPhase.LEAVING) -1f else 0f, t = clock,
                    )
                    // patience bar
                    if (phase == HPhase.ORDERING) {
                        val hc = headCenter(cx, base, ch)
                        val bw = w * 0.32f
                        val by = hc.y - ch * 0.38f
                        drawRoundRect(Ink, Offset(cx - bw / 2 - 4f, by - 4f), Size(bw + 8f, 26f), CornerRadius(13f))
                        val pc = when {
                            c.patience > 0.6f -> Mint
                            c.patience > 0.3f -> Gold
                            else -> Pink
                        }
                        drawRoundRect(pc, Offset(cx - bw / 2, by), Size(bw * c.patience.coerceIn(0f, 1f), 18f), CornerRadius(9f))
                    }
                }
                // awning from inside
                val stripes = 9
                for (i in 0 until stripes) {
                    drawRect(if (i % 2 == 0) Pink else Color.White, Offset(i * w / stripes, 0f), Size(w / stripes, h * 0.07f))
                }
                // counter (your side)
                drawRect(Color(0xFF8B5A2B), Offset(0f, h * 0.8f), Size(w, h * 0.2f))
                drawRect(Color(0xFFB07A45), Offset(0f, h * 0.78f), Size(w, h * 0.04f))
                drawRect(Ink, Offset(0f, h * 0.78f), Size(w, h * 0.04f), style = androidx.compose.ui.graphics.drawscope.Stroke(5f))
                // ticket stacks
                for (i in 0..2) {
                    val tx = w * (0.06f + i * 0.09f)
                    for (k in 0 until 4) {
                        drawRoundRect(Tiers[i].paper, Offset(tx, h * (0.83f - k * 0.012f)), Size(w * 0.07f, h * 0.05f), CornerRadius(6f))
                        drawRoundRect(Ink, Offset(tx, h * (0.83f - k * 0.012f)), Size(w * 0.07f, h * 0.05f), CornerRadius(6f), style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
                    }
                }
                // cash register
                val rx = w * 0.72f
                outlined(androidx.compose.ui.graphics.Path().apply {
                    moveTo(rx, h * 0.8f); lineTo(rx + w * 0.22f, h * 0.8f); lineTo(rx + w * 0.2f, h * 0.68f); lineTo(rx + w * 0.02f, h * 0.68f); close()
                }, Color(0xFF6E6A85), 5f)
                drawRoundRect(Mint, Offset(rx + w * 0.04f, h * 0.7f), Size(w * 0.14f, h * 0.04f), CornerRadius(6f))
                label(fmt(sales - payouts), rx + w * 0.11f, h * 0.72f, h * 0.03f, Ink)
                // floaters
                floaters.forEach { f ->
                    val age = clock - f.born
                    label(f.text, w * 0.75f, h * (0.35f - age * 0.08f), h * 0.05f, f.color.copy(alpha = (1f - age / 1.6f).coerceIn(0f, 1f)), Ink)
                }
            }
            val c = cust
            if (c != null && c.line.isNotEmpty() && phase != HPhase.ARRIVING) {
                Text(
                    c.line, color = Ink, fontWeight = FontWeight.Black, fontSize = 17.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 30.dp, start = 24.dp, end = 24.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(2.5.dp, Ink, RoundedCornerShape(16.dp))
                        .padding(10.dp),
                    textAlign = TextAlign.Center,
                )
            }
            if (c != null) {
                Text(
                    "${c.dude.name} · ${c.mood.label}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .background(Ink.copy(alpha = 0.7f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            myYell?.let {
                Text(
                    "\"${it.uppercase()}!\"", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(20.dp)
                        .background(Pink, RoundedCornerShape(16.dp))
                        .border(3.dp, Ink, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                )
            }
        }
        // actions
        Column(Modifier.padding(12.dp)) {
            val c = cust
            when {
                c == null -> Spacer(Modifier.height(52.dp))
                phase == HPhase.ORDERING -> {
                    ChunkyButton("sell ${c.order.label} (${fmt(c.order.price)}c)", Modifier.fillMaxWidth(), color = Mint, enabled = !busy, sfx = null) { sell() }
                    Spacer(Modifier.height(8.dp))
                    Text("yell something:", color = Dim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        yellLines.forEach { l ->
                            ChunkyButton(l, color = Pink, enabled = !busy, height = 46.dp, sfx = null) { yell(l) }
                        }
                    }
                }
                phase == HPhase.RESULTS && c.won > 0 && !busy -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ChunkyButton("pay ${fmt(c.won)}c", Modifier.weight(1f), color = Mint, sfx = null) { pay() }
                        ChunkyButton("pretend it lost", Modifier.weight(1f), color = Pink, sfx = null) { scam() }
                    }
                }
                else -> {
                    Text(
                        when (phase) {
                            HPhase.ARRIVING -> "someone's coming..."
                            HPhase.LEAVING -> "bye!"
                            else -> "..."
                        },
                        color = Dim, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(top = 14.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun repColor(r: Int) = when {
    r >= 70 -> Mint
    r >= 35 -> Gold
    else -> Pink
}

@Composable
private fun DaySummary(
    sales: Int, payouts: Int, served: Int, walkouts: Int, yells: Int, scams: Int,
    repStart: Int, repNow: Int, onNext: () -> Unit, onHome: () -> Unit,
) {
    val profit = sales - payouts
    val repDelta = repNow - repStart
    val title = when {
        profit >= 300 && repDelta >= 0 -> "stand of the year!"
        repDelta <= -15 -> "the health inspector is on the way"
        profit < 0 -> "you paid people to show up"
        yells >= 5 -> "loud, proud, and kind of broke"
        else -> "a perfectly mid day"
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("end of day", color = Dim, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(title, color = Gold, fontWeight = FontWeight.Black, fontSize = 26.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SumRow("ticket sales", "+${fmt(sales)}", Mint)
                SumRow("prizes paid", "-${fmt(payouts)}", Pink)
                SumRow("profit", (if (profit >= 0) "+" else "") + fmt(profit), if (profit >= 0) Gold else Pink)
                SumRow("customers served", "$served", Color.White)
                SumRow("walked out", "$walkouts", if (walkouts > 0) Pink else Color.White)
                SumRow("times you yelled", "$yells", Color.White)
                SumRow("sneaky \"losses\"", "$scams", Color.White)
                SumRow("reputation", "$repStart → $repNow", if (repDelta >= 0) Mint else Pink)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (profit >= 0) "profit went into your wallet" else "the loss came out of your wallet",
            color = Dim, fontSize = 13.sp,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChunkyButton("next day", Modifier.weight(1f), color = Mint, onClick = onNext)
            ChunkyButton("home", Modifier.weight(1f), color = CardHi, textColor = Color.White, onClick = onHome)
        }
    }
}

@Composable
private fun SumRow(k: String, v: String, color: Color) {
    Row(Modifier.fillMaxWidth()) {
        Text(k, color = Dim, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(v, color = color, fontSize = 17.sp, fontWeight = FontWeight.Black)
    }
}
