package com.lot.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

// ================================ pigeon derby ================================

private class Racer(val name: String, val odds: Int, val prob: Float, val color: Color)

private val racers = listOf(
    Racer("sir flaps-a-lot", 2, 0.38f, Color(0xFFE9E9E9)),
    Racer("gerald", 3, 0.26f, Color(0xFF9EA7B8)),
    Racer("crumb thief", 4, 0.18f, Color(0xFFC8A27A)),
    Racer("big steve", 6, 0.11f, Color(0xFF7D6E99)),
    Racer("the void", 10, 0.07f, Color(0xFF3A3A44)),
)

private val derbyTutorial = listOf(
    "welcome to the pigeon derby. five pigeons, one finish line, zero training.",
    "pick a pigeon and a bet. lower odds win more often, higher odds pay more. the void pays 10x. nobody has ever seen the void win.",
    "then watch the race. pigeons get distracted. it's part of the sport.",
)

private val raceEvents = listOf(
    "%s stops to eat a fry!", "%s is just walking now", "%s flew backwards for a second??",
    "%s is showing off for a lady pigeon", "%s pooped mid-flight. not disqualified.", "%s has entered beast mode",
    "%s got distracted by a shiny bottle cap", "the crowd is chanting %s's name",
)

@Composable
fun DerbyScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    var help by remember { mutableStateOf(false) }
    ModeTutorial(vm, state, "derby", "pigeon derby", derbyTutorial, help) { help = false }
    val paused by rememberUpdatedState(help || "derby" !in state.tutorialsSeen)

    var pick by remember { mutableIntStateOf(0) }
    var bet by remember { mutableIntStateOf(10) }
    var race by remember { mutableIntStateOf(0) }
    var racing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var resultWin by remember { mutableStateOf(false) }
    var clock by remember { mutableFloatStateOf(0f) }
    var comment by remember { mutableStateOf("place your bets!") }
    var confetti by remember { mutableIntStateOf(0) }
    val finishTimes = remember { FloatArray(racers.size) { 7f } }
    val waves = remember { IntArray(racers.size) { 1 } }
    var winner by remember { mutableIntStateOf(-1) }

    LaunchedEffect(race) {
        if (race == 0) return@LaunchedEffect
        // the winner is decided up front by the odds, the race just acts it out
        val seed = "birdseed" in state.items
        val weights = racers.mapIndexed { i, r -> r.prob * if (seed && i == pick) 1.3f else 1f }
        var roll = Random.nextFloat() * weights.sum()
        var w = 0
        while (w < weights.lastIndex && roll >= weights[w]) { roll -= weights[w]; w++ }
        winner = w
        racers.indices.forEach {
            finishTimes[it] = if (it == w) 7f else 7.3f + Random.nextFloat() * 2.4f
            waves[it] = 1 + Random.nextInt(3)
        }
        clock = 0f
        racing = true
        fx.play(Sfx.DRUM)
        comment = "and they're off!"
        var last = 0L
        var nextEvent = 1.6f
        while (racing) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                if (paused) return@withFrameNanos
                clock += dt
                if (clock > nextEvent && clock < 6.5f) {
                    nextEvent = clock + 1.4f + Random.nextFloat()
                    comment = raceEvents.random().format(racers.random().name)
                    fx.play(Sfx.BALL)
                }
                if (clock > 6.2f && clock < 6.3f) comment = "photo finish incoming!"
                if (clock >= 7f) {
                    racing = false
                    val win = winner == pick
                    resultWin = win
                    if (win) {
                        val payout = bet * racers[pick].odds
                        result = "${racers[pick].name} WINS! you get ${fmt(payout)} coins"
                        vm.update {
                            it.copy(
                                coins = it.coins + payout, derbyWins = it.derbyWins + 1,
                                flags = if (racers[pick].odds >= 10) it.flags + "longshot" else it.flags,
                            )
                        }
                        fx.play(Sfx.BIG_WIN); fx.buzz(Buzz.SUCCESS); confetti++
                    } else {
                        result = "${racers[winner].name} wins. your pigeon is ashamed."
                        fx.play(Sfx.LOSE); fx.buzz(Buzz.FAIL)
                    }
                    comment = "${racers[winner].name} takes it!"
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            LotTopBar("pigeon derby", state.coins, { nav.back() }, { help = true })
            Text(
                comment, color = Gold, fontWeight = FontWeight.Black, fontSize = 18.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Box(Modifier.fillMaxWidth().weight(1f).padding(12.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawRoundRect(Color(0xFF3B7A3A), cornerRadius = CornerRadius(30f))
                    val laneH = h / racers.size
                    val startX = w * 0.1f
                    val endX = w * 0.88f
                    // finish line
                    for (i in 0 until 16) {
                        drawRect(if (i % 2 == 0) Color.White else Ink, Offset(endX, i * h / 16), Size(w * 0.025f, h / 16))
                    }
                    racers.forEachIndexed { i, r ->
                        val y = laneH * i + laneH / 2
                        if (i > 0) drawLine(Color.White.copy(alpha = 0.3f), Offset(0f, laneH * i), Offset(w, laneH * i), 3f)
                        val t = if (race == 0) 0f else (clock / finishTimes[i]).coerceIn(0f, 1f)
                        val p = (t + 0.04f * sin(2 * PI.toFloat() * waves[i] * t)).coerceIn(0f, 1f)
                        val x = startX + (endX - startX + w * 0.05f) * p
                        val flying = racing || (race > 0 && t < 1f)
                        drawPigeon(Offset(x, y), laneH * 0.55f, if (flying) abs(sin(clock * 16f + i)) else 0.2f, 1f, r.color)
                        label("${i + 1}", w * 0.04f, y, laneH * 0.3f, Color.White, Ink)
                        if (i == pick) drawCircle(Gold, laneH * 0.42f, Offset(x, y), style = androidx.compose.ui.graphics.drawscope.Stroke(5f))
                    }
                }
            }
            if (racing) {
                Spacer(Modifier.height(130.dp))
            } else {
                Column(Modifier.padding(horizontal = 12.dp)) {
                    result?.let {
                        Text(it, color = if (resultWin) Gold else Pink, fontWeight = FontWeight.Black, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        racers.forEachIndexed { i, r ->
                            ChunkyButton(
                                "${i + 1}\n${r.odds}x", Modifier.weight(1f), color = if (i == pick) Gold else CardHi,
                                textColor = if (i == pick) Ink else Color.White, height = 58.dp,
                            ) { pick = i }
                        }
                    }
                    Text(racers[pick].name, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(10, 50, 100, 500).forEach { b ->
                            ChunkyButton(
                                "${b}c", Modifier.weight(1f), color = if (b == bet) Sky else CardHi,
                                textColor = if (b == bet) Ink else Color.White, height = 44.dp, enabled = state.coins >= b,
                            ) { bet = b }
                        }
                    }
                }
                ChunkyButton(
                    "race! (bet ${fmt(bet)}c, win ${fmt(bet * racers[pick].odds)})",
                    Modifier.fillMaxWidth().padding(12.dp), color = Mint, enabled = state.coins >= bet, sfx = Sfx.BUY,
                ) {
                    vm.update { it.copy(coins = it.coins - bet, derbyBets = it.derbyBets + 1) }
                    result = null
                    race++
                }
            }
        }
        Confetti(confetti)
    }
}

// ================================ item shop ================================

@Composable
fun ItemsScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    Column(Modifier.fillMaxSize()) {
        LotTopBar("item shop", state.coins, { nav.back() })
        Text(
            "permanent upgrades. one of each. no refunds.", color = Dim, fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Items, key = { it.id }) { item ->
                val owned = item.id in state.items
                Panel(Modifier.fillMaxWidth(), color = if (owned) CardHi else Card) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                            Text(item.desc, color = Dim, fontSize = 13.sp)
                            Text(item.mode, color = modeColor(item.mode), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        if (owned) {
                            Text("owned", color = Mint, fontWeight = FontWeight.Black, modifier = Modifier.width(88.dp), textAlign = TextAlign.Center)
                        } else {
                            ChunkyButton("${fmt(item.price)}c", Modifier.width(88.dp), enabled = state.coins >= item.price, height = 46.dp, sfx = null) {
                                if (vm.buyItem(item)) { fx.play(Sfx.BUY); fx.buzz(Buzz.SUCCESS) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================================ home extras ================================

@Composable
fun QuestPanel(vm: GameViewModel, state: GameState) {
    LaunchedEffect(Unit) { vm.refreshQuests() }
    val quests = state.questIds.mapNotNull { id -> QuestPool.firstOrNull { it.id == id } }
    if (quests.isEmpty()) return
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("daily quests", color = Gold, fontWeight = FontWeight.Black, fontSize = 18.sp)
            quests.forEach { q ->
                val prog = vm.questProgress(state, q)
                val done = q.id in state.questsDone
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(q.desc, color = if (done) Dim else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Canvas(Modifier.fillMaxWidth().height(10.dp).padding(top = 3.dp)) {
                            drawRoundRect(CardHi, cornerRadius = CornerRadius(10f))
                            drawRoundRect(if (done) Dim else Mint, size = Size(size.width * prog / q.target, size.height), cornerRadius = CornerRadius(10f))
                        }
                        Text("$prog/${q.target}", color = Dim, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    when {
                        done -> Text("done", color = Dim, fontWeight = FontWeight.Black, modifier = Modifier.width(84.dp), textAlign = TextAlign.Center)
                        prog >= q.target -> ChunkyButton("+${q.reward}c", Modifier.width(84.dp), color = Mint, height = 40.dp, sfx = Sfx.COIN) { vm.collectQuest(q) }
                        else -> Text("+${q.reward}c", color = Gold, fontWeight = FontWeight.Bold, modifier = Modifier.width(84.dp), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewsTicker(state: GameState) {
    val news = remember(state.totalSpent, state.yells, state.timesCaught, state.bingoWins) {
        listOf(
            "local idiot has spent ${fmt(state.totalSpent)} coins on tickets",
            "stand owner yelled at customers ${state.yells} times, police \"not interested\"",
            "bush near lot stand seen with eyes. again.",
            "grannies win ${maxOf(0, state.bingoPlayed - state.bingoWins)} bingo games, demand respect",
            "pigeon named \"the void\" refuses interviews",
            "raccoon union strikes behind lot stand",
            "cashier finds 14 defects on a brand new ticket, calls it \"a slow day\"",
            "scientists confirm: the 7 does look a little smug",
            "man caught ${state.timesCaught} times stealing tickets, still has not learned",
            "breaking: coin worth exactly nothing, economists stunned",
        ).shuffled().joinToString("     ·     ")
    }
    Text(
        news, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .background(Gold, RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp)
            .basicMarquee(iterations = Int.MAX_VALUE),
    )
}
