package com.lot.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

// ================================ bingo ================================

private const val BINGO_FEE = 20
private const val BINGO_PRIZE = 150

private val bingoTutorial = listOf(
    "welcome to the bingo hall. it's you against three grannies who have been doing this since 1962.",
    "the caller shouts numbers. tap them on your card to daub them. the middle is free.",
    "get 5 in a row (across, down or diagonal) and smash BINGO before any granny does.",
    "yelling bingo without a line costs you 10 coins and the grannies will judge you. entry is $BINGO_FEE, a win pays $BINGO_PRIZE.",
)

private fun newCard(rng: Random): List<Int> {
    val cols = (0 until 5).map { c -> (c * 15 + 1..c * 15 + 15).shuffled(rng).take(5) }
    return List(25) { i -> if (i == 12) 0 else cols[i % 5][i / 5] }
}

private val lines5: List<List<Int>> = buildList {
    for (r in 0 until 5) add((0 until 5).map { r * 5 + it })
    for (c in 0 until 5) add((0 until 5).map { it * 5 + c })
    add((0 until 5).map { it * 6 })
    add((0 until 5).map { it * 4 + 4 })
}

private fun hasLine(d: BooleanArray) = lines5.any { l -> l.all { d[it] } }
private fun bestLine(d: BooleanArray) = lines5.maxOf { l -> l.count { d[it] } }

private class Granny(val idx: Int) {
    val card = newCard(Random)
    val daubed = BooleanArray(25).also { it[12] = true }
    val pending = mutableListOf<Pair<Float, Int>>()
    var shoutAt: Float? = null
}

private fun letterOf(n: Int) = "BINGO"[(n - 1) / 15]

@Composable
fun BingoScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    var help by remember { mutableStateOf(false) }
    ModeTutorial(vm, state, "bingo", "bingo", bingoTutorial, help) { help = false }
    val paused by rememberUpdatedState(help || "bingo" !in state.tutorialsSeen)

    var round by remember { mutableIntStateOf(0) } // 0 = lobby
    var result by remember { mutableStateOf<String?>(null) }
    var won by remember { mutableStateOf(false) }
    val card = remember(round) { newCard(Random) }
    val daubed = remember(round) { mutableStateListOf<Boolean>().apply { addAll(List(25) { it == 12 }) } }
    val called = remember(round) { mutableStateListOf<Int>() }
    val callTimes = remember(round) { mutableMapOf<Int, Float>() }
    val dauber by rememberUpdatedState("dauber" in state.items)
    val grannies = remember(round) { List(3) { Granny(it) } }
    var clock by remember(round) { mutableFloatStateOf(0f) }
    var lockout by remember(round) { mutableFloatStateOf(0f) }
    var msg by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(msg) { if (msg != null) { delay(1400); msg = null } }

    fun finish(win: Boolean, text: String) {
        if (result != null) return
        won = win
        result = text
        if (win) {
            fx.play(Sfx.BIG_WIN); fx.buzz(Buzz.SUCCESS)
            vm.update { it.copy(coins = it.coins + BINGO_PRIZE, bingoWins = it.bingoWins + 1) }
        } else {
            fx.play(Sfx.LOSE); fx.buzz(Buzz.FAIL)
        }
    }

    // caller + grannies
    LaunchedEffect(round) {
        if (round == 0) return@LaunchedEffect
        val bag = (1..75).shuffled().toMutableList()
        var nextCall = 1.5f
        var last = 0L
        while (result == null) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                if (paused || result != null) return@withFrameNanos
                clock += dt
                lockout = (lockout - dt).coerceAtLeast(0f)
                if (clock >= nextCall && bag.isNotEmpty()) {
                    val n = bag.removeAt(0)
                    called.add(n)
                    callTimes[n] = clock
                    fx.play(Sfx.BALL)
                    nextCall = clock + (2.6f - called.size * 0.025f).coerceAtLeast(1.5f)
                    grannies.forEach { g ->
                        val i = g.card.indexOf(n)
                        if (i >= 0 && Random.nextFloat() > 0.1f) g.pending.add(clock + 0.7f + Random.nextFloat() * 2.2f to i)
                    }
                }
                if (dauber) {
                    callTimes.forEach { (n, at) ->
                        val i = card.indexOf(n)
                        if (i >= 0 && !daubed[i] && clock - at >= 2f) daubed[i] = true
                    }
                }
                grannies.forEach { g ->
                    val due = g.pending.filter { it.first <= clock }
                    if (due.isNotEmpty()) {
                        due.forEach { g.daubed[it.second] = true }
                        g.pending.removeAll(due)
                        if (g.shoutAt == null && hasLine(g.daubed)) g.shoutAt = clock + 0.5f + Random.nextFloat() * 1.3f
                    }
                    val s = g.shoutAt
                    if (s != null && clock >= s) finish(false, "${grannyDude(g.idx).name} yells BINGO first. she's been training for this.")
                }
                if (bag.isEmpty() && clock > nextCall + 4f) finish(false, "the bag ran out. everyone loses. the hall is sad.")
                tick++
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        LotTopBar("bingo hall", state.coins, { nav.back() }, { help = true })
        if (round == 0) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Canvas(Modifier.fillMaxWidth().height(160.dp)) {
                    for (i in 0 until 3) drawDude(grannyDude(i), size.width * (0.2f + i * 0.3f), size.height * 0.9f, size.height * 0.8f, Mood.GRUMPY, look = 0f)
                }
                Spacer(Modifier.height(12.dp))
                Text("three grannies. one card each. no mercy.", color = Dim, fontSize = 15.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text("wins: ${state.bingoWins} · played: ${state.bingoPlayed}", color = Dim, fontSize = 13.sp)
                Spacer(Modifier.height(20.dp))
                ChunkyButton("play (${BINGO_FEE}c, win $BINGO_PRIZE)", Modifier.fillMaxWidth(), color = Sky, enabled = state.coins >= BINGO_FEE, sfx = Sfx.BUY) {
                    vm.update { it.copy(coins = it.coins - BINGO_FEE, bingoPlayed = it.bingoPlayed + 1) }
                    result = null
                    round++
                }
            }
            return@Column
        }
        @Suppress("UNUSED_EXPRESSION") tick
        // grannies row
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            grannies.forEach { g ->
                Panel(Modifier.weight(1f), color = CardHi) {
                    Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Canvas(Modifier.fillMaxWidth().height(60.dp)) {
                            val close = bestLine(g.daubed)
                            drawDude(
                                grannyDude(g.idx), size.width / 2, size.height * 1.05f, size.height * 0.95f,
                                if (close >= 4) Mood.HAPPY else Mood.GRUMPY, talk = g.shoutAt != null, t = clock,
                                wobble = if (close >= 4) sin(clock * 10f) * 0.4f else 0f,
                            )
                        }
                        Text(grannyDude(g.idx).name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        val best = bestLine(g.daubed)
                        Text("$best/5 in a line", color = if (best >= 4) Pink else Dim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        // the caller
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val n = called.lastOrNull()
            Canvas(Modifier.size(70.dp)) {
                if (n == null) {
                    drawCircle(CardHi, size.minDimension / 2)
                    label("...", center.x, center.y, 30f, Dim)
                } else drawBall(center, size.minDimension * 0.46f, n)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (n == null) "eyes down..." else "${letterOf(n)} $n!", color = Gold, fontWeight = FontWeight.Black, fontSize = 26.sp)
                Text(called.dropLast(1).takeLast(6).reversed().joinToString("  ") { "${letterOf(it)}$it" }, color = Dim, fontSize = 13.sp)
            }
        }
        // your card
        Panel(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            color = Color(0xFFFFF3D6),
        ) {
            Column(Modifier.padding(8.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    "BINGO".forEach { ch ->
                        Text(ch.toString(), color = Pink, fontWeight = FontWeight.Black, fontSize = 22.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }
                for (r in 0 until 5) {
                    Row(Modifier.fillMaxWidth()) {
                        for (c in 0 until 5) {
                            val i = r * 5 + c
                            val num = card[i]
                            val on = daubed[i]
                            val sc by animateFloatAsState(if (on) 1f else 0f, label = "daub")
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1.15f)
                                    .padding(2.dp)
                                    .background(Color.White, RoundedCornerShape(8.dp))
                                    .border(1.5.dp, Ink.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clickable(enabled = result == null && !on) {
                                        if (num in called) {
                                            daubed[i] = true; fx.play(Sfx.BALL); fx.buzz(Buzz.LIGHT)
                                        } else {
                                            msg = "$num wasn't called yet!"; fx.play(Sfx.ANGRY)
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxSize(0.8f)
                                        .scale(sc)
                                        .background(Pink.copy(alpha = 0.75f), CircleShape)
                                )
                                Text(if (num == 0) "free" else "$num", color = Ink, fontWeight = FontWeight.Black, fontSize = if (num == 0) 13.sp else 18.sp)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        msg?.let { Text(it, color = Pink, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally)) }
        if (result == null) {
            ChunkyButton(
                if (lockout > 0f) "the grannies are judging you..." else "BINGO!",
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                color = Pink, height = 66.dp, enabled = lockout <= 0f, sfx = Sfx.YELL,
            ) {
                if (hasLine(daubed.toBooleanArray())) {
                    finish(true, "BINGO! the grannies are furious. +$BINGO_PRIZE coins")
                } else {
                    lockout = 3f
                    msg = "that's not a bingo. -10c"
                    fx.play(Sfx.LOSE); fx.buzz(Buzz.FAIL)
                    vm.update { it.copy(coins = (it.coins - 10).coerceAtLeast(0), flags = it.flags + "falsebingo") }
                }
            }
        } else {
            Text(
                result ?: "", color = if (won) Gold else Pink, fontWeight = FontWeight.Black, fontSize = 18.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChunkyButton("again (${BINGO_FEE}c)", Modifier.weight(1f), color = Sky, enabled = state.coins >= BINGO_FEE, sfx = Sfx.BUY) {
                    vm.update { it.copy(coins = it.coins - BINGO_FEE, bingoPlayed = it.bingoPlayed + 1) }
                    result = null
                    round++
                }
                ChunkyButton("leave", Modifier.weight(1f), color = CardHi, textColor = Color.White) { nav.back() }
            }
        }
    }
}

// ================================ dumpster ================================

private val dumpTutorial = listOf(
    "it's night. behind the lot stand are 10 trash bags. people throw away tickets without checking them properly.",
    "tap a bag a few times to rip it open. most of it is garbage. sometimes it's a ticket.",
    "compare its numbers with tonight's winning numbers. all 3 match = winner. keep it or toss it. you only have 3 pockets.",
    "raccoons live here. if one shows up, tap it like crazy or it steals something from your pockets.",
    "winners you keep go to your wallet as unclaimed tickets. you still have to claim them at the stand.",
)

private val junk = listOf(
    "a banana peel", "half a pizza (still warm??)", "one sock", "a cracked phone", "a single shoe", "a love letter. yikes.",
    "47 receipts", "a rubber duck", "someone's homework", "an empty energy drink", "a broken umbrella", "mystery goo",
)

private class TrashTicket(val nums: List<Int>, val winner: Boolean, val prize: Int)
private sealed interface Found {
    data class Junk(val name: String) : Found
    data class Ticket(val t: TrashTicket) : Found
    data object Raccoon : Found
}

@Composable
fun DumpsterScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    var help by remember { mutableStateOf(false) }
    ModeTutorial(vm, state, "dumpster", "dumpster diving", dumpTutorial, help) { help = false }

    var night by remember { mutableIntStateOf(1) }
    val board = remember(night) { (1..20).shuffled().take(3).sorted() }
    val bagColors = remember(night) { List(10) { listOf(Color(0xFF2A2830), Color(0xFF2E5AAC), Color(0xFF2E9E5B), Color(0xFF444455)).random() } }
    val rips = remember(night) { mutableStateListOf<Int>().apply { addAll(List(10) { 0 }) } }
    val opened = remember(night) { mutableStateListOf<Boolean>().apply { addAll(List(10) { false }) } }
    val pockets = remember(night) { mutableStateListOf<TrashTicket>() }
    var found by remember(night) { mutableStateOf<Found?>(null) }
    var raccoonTaps by remember { mutableIntStateOf(0) }
    var raccoonTime by remember { mutableFloatStateOf(0f) }
    var over by remember(night) { mutableStateOf(false) }
    var summary by remember(night) { mutableStateOf("") }
    var clock by remember { mutableFloatStateOf(0f) }
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(msg) { if (msg != null) { delay(1600); msg = null } }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                clock += dt
                if (found == Found.Raccoon) {
                    raccoonTime -= dt
                    if (raccoonTime <= 0f) {
                        found = null
                        if (pockets.isNotEmpty()) {
                            pockets.removeAt(Random.nextInt(pockets.size))
                            msg = "the raccoon stole a ticket from your pocket!"
                        } else msg = "the raccoon bit you and left. rude."
                        fx.play(Sfx.ANGRY); fx.buzz(Buzz.FAIL)
                    }
                }
            }
        }
    }

    fun endNight() {
        if (over) return
        over = true
        val winners = pockets.filter { it.winner }
        val searched = opened.count { it }
        winners.forEach { vm.addUnclaimed("trash ticket", it.prize) }
        vm.update { it.copy(bagsSearched = it.bagsSearched + searched, trashWinners = it.trashWinners + winners.size) }
        summary = when {
            winners.isNotEmpty() -> "you found ${winners.size} winning ticket${if (winners.size > 1) "s" else ""} worth ${fmt(winners.sumOf { it.prize })}c!\nthey're in your wallet. go claim them."
            pockets.isNotEmpty() -> "you went home with ${pockets.size} losing ticket${if (pockets.size > 1) "s" else ""}. and the smell."
            else -> "you went home with nothing but the smell."
        }
        if (winners.isNotEmpty()) { fx.play(Sfx.WIN); fx.buzz(Buzz.SUCCESS) } else fx.play(Sfx.LOSE)
    }

    fun openBag(i: Int) {
        if (opened[i] || found != null || over) return
        rips[i] = rips[i] + 1
        fx.play(Sfx.SCRATCH); fx.buzz(Buzz.LIGHT)
        if (rips[i] >= 4) {
            opened[i] = true
            val roll = Random.nextFloat()
            found = when {
                roll < 0.13f -> {
                    raccoonTaps = 0; raccoonTime = 3f; fx.play(Sfx.CAUGHT); fx.buzz(Buzz.HEAVY); Found.Raccoon
                }
                roll < 0.48f -> {
                    val winner = Random.nextFloat() < 0.32f
                    val nums = if (winner) board.shuffled() else {
                        val others = (1..20).filter { it !in board }.shuffled()
                        if (Random.nextFloat() < 0.6f) (board.shuffled().take(2) + others.first()).shuffled() else others.take(3)
                    }
                    fx.play(Sfx.COIN)
                    Found.Ticket(TrashTicket(nums, winner, 40 + Random.nextInt(9) * 45))
                }
                else -> Found.Junk(junk.random())
            }
            if (opened.all { it } && found !is Found.Ticket && found != Found.Raccoon) endNight()
        }
    }

    Column(Modifier.fillMaxSize()) {
        LotTopBar("dumpster diving", state.coins, { nav.back() }, { help = true })
        Text(
            "tonight's winners: ${board.joinToString(" · ")}",
            color = Gold, fontWeight = FontWeight.Black, fontSize = 20.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (p in 0 until 3) {
                val t = pockets.getOrNull(p)
                Panel(Modifier.weight(1f), color = if (t != null) CardHi else Card) {
                    Text(
                        t?.nums?.joinToString(" ") ?: "empty pocket", color = if (t != null) Color.White else Dim,
                        fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp)
        ) {
            Canvas(Modifier.fillMaxSize().sceneClip()) {
                drawRoundRect(Color(0xFF1E1838), cornerRadius = CornerRadius(30f))
                drawRect(Color(0xFF2A2240), Offset(0f, size.height * 0.62f), Size(size.width, size.height * 0.38f))
                // street lamp
                drawLine(Color(0xFF555566), Offset(size.width * 0.9f, size.height * 0.1f), Offset(size.width * 0.9f, size.height * 0.62f), 10f)
                drawCircle(Gold.copy(alpha = 0.15f + 0.05f * sin(clock * 3f)), size.width * 0.3f, Offset(size.width * 0.9f, size.height * 0.12f))
                drawCircle(Gold, 14f, Offset(size.width * 0.9f, size.height * 0.1f))
                // dumpster
                drawRoundRect(Color(0xFF2E7D32), Offset(size.width * 0.05f, size.height * 0.18f), Size(size.width * 0.5f, size.height * 0.3f), CornerRadius(12f))
                drawRoundRect(Ink, Offset(size.width * 0.05f, size.height * 0.18f), Size(size.width * 0.5f, size.height * 0.3f), CornerRadius(12f), style = androidx.compose.ui.graphics.drawscope.Stroke(5f))
                label("lot staff only", size.width * 0.3f, size.height * 0.33f, size.width * 0.04f, Color.White.copy(alpha = 0.7f))
            }
            Column(Modifier.fillMaxSize().padding(top = 8.dp), verticalArrangement = Arrangement.Bottom) {
                for (row in 0 until 2) {
                    Row(Modifier.fillMaxWidth()) {
                        for (col in 0 until 5) {
                            val i = row * 5 + col
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(0.8f)
                                    .clickable(remember { MutableInteractionSource() }, null) { openBag(i) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Canvas(Modifier.fillMaxSize()) {
                                    if (opened[i]) {
                                        drawOval(bagColors[i].copy(alpha = 0.6f), Offset(size.width * 0.1f, size.height * 0.7f), Size(size.width * 0.8f, size.height * 0.2f))
                                        drawCircle(Gold.copy(alpha = 0.3f), 6f, Offset(size.width * 0.3f, size.height * 0.75f))
                                    } else {
                                        val shake = if (rips[i] > 0) sin(clock * 30f) * rips[i] * 0.15f else 0f
                                        drawTrashBag(Offset(size.width / 2, size.height * 0.55f), size.width * 0.75f, bagColors[i], shake)
                                        repeat(rips[i]) { r ->
                                            drawLine(Color.White.copy(alpha = 0.6f), Offset(size.width * (0.35f + r * 0.08f), size.height * 0.5f), Offset(size.width * (0.4f + r * 0.08f), size.height * 0.62f), 3f)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            msg?.let {
                Text(
                    it, color = Ink, fontWeight = FontWeight.Black, fontSize = 15.sp, textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp)
                        .background(Pink, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                )
            }
            // what you found
            when (val f = found) {
                is Found.Junk -> FoundCard("you found...", f.name) {
                    ChunkyButton("toss it", Modifier.fillMaxWidth(), color = CardHi, textColor = Color.White) {
                        found = null
                        if (opened.all { it }) endNight()
                    }
                }
                is Found.Ticket -> FoundCard("a discarded ticket!", "") {
                    Panel(Modifier.fillMaxWidth(), color = Color(0xFFFFF3D6)) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("crumpled lot ticket", color = Ink.copy(alpha = 0.6f), fontSize = 12.sp)
                            Text(f.t.nums.joinToString("   "), color = Ink, fontWeight = FontWeight.Black, fontSize = 30.sp)
                            Text("prize if winner: ${f.t.prize}c", color = Ink.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ChunkyButton("keep", Modifier.weight(1f), color = Mint, enabled = pockets.size < 3) {
                            pockets.add(f.t); found = null
                            if (opened.all { it }) endNight()
                        }
                        ChunkyButton("toss", Modifier.weight(1f), color = CardHi, textColor = Color.White) {
                            if (f.t.winner) msg = "uh oh. that one was a winner."
                            found = null
                            if (opened.all { it }) endNight()
                        }
                    }
                    if (pockets.size >= 3) Text("pockets full", color = Pink, fontSize = 12.sp)
                }
                Found.Raccoon -> Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(remember { MutableInteractionSource() }, null) {
                            raccoonTaps++
                            fx.play(Sfx.CLICK); fx.buzz(Buzz.TICK)
                            if (raccoonTaps >= 8) {
                                found = null
                                vm.flag("raccoon")
                                msg = "the raccoon ran off. you win this time."
                                fx.play(Sfx.WIN)
                                if (opened.all { it }) endNight()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxWidth().height(220.dp)) {
                        val j = sin(clock * 25f) * 8f
                        drawRaccoon(Offset(size.width * 0.45f + j, size.height * 0.55f), size.height * 0.75f, clock, true)
                    }
                    Text(
                        "RACCOON! tap it! ${8 - raccoonTaps} · ${"%.1f".format(raccoonTime.coerceAtLeast(0f))}s",
                        color = Gold, fontWeight = FontWeight.Black, fontSize = 22.sp,
                        modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                    )
                }
                null -> {}
            }
            if (over) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Panel(Modifier.fillMaxWidth().padding(12.dp)) {
                        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("night over", color = Gold, fontWeight = FontWeight.Black, fontSize = 26.sp)
                            Text(summary, color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ChunkyButton("next night", Modifier.weight(1f), color = Mint) { night++ }
                                ChunkyButton("home", Modifier.weight(1f), color = CardHi, textColor = Color.White) { nav.back() }
                            }
                        }
                    }
                }
            }
        }
        if (!over) {
            ChunkyButton(
                "go home (${opened.count { it }}/10 searched)", Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                color = CardHi, textColor = Color.White, enabled = found == null,
            ) { endNight() }
        }
    }
}

@Composable
private fun FoundCard(title: String, item: String, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Panel(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Gold, fontWeight = FontWeight.Black, fontSize = 20.sp)
                if (item.isNotEmpty()) Text(item, color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                content()
            }
        }
    }
}
