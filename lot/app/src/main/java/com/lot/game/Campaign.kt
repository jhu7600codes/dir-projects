package com.lot.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CommissionerDude = Dude(Color(0xFFF1C27D), Color(0xFF1B1B2F), Color(0xFF9E9E9E), 2, Gold, "the commissioner", true, 1.35f)
private val MomDude = Dude(Color(0xFFFFD7B5), Pink, Color(0xFF6B3E26), 0, Pink, "mom", false, 1.05f)

private fun DrawScope.drawSpeaker(who: String, t: Float) {
    val w = size.width
    val h = size.height
    when (who) {
        "raccoon" -> drawRaccoon(Offset(w * 0.45f, h * 0.65f), h * 0.6f, t, false)
        else -> {
            val d = when (who) {
                "mom" -> MomDude
                "cashier" -> CashierDude
                "granny" -> grannyDude(0)
                else -> CommissionerDude
            }
            drawDude(d, w / 2, h * 1.02f, h * 0.95f, if (who == "commissioner") Mood.GRUMPY else Mood.HAPPY, talk = t % 2f < 1.2f, t = t)
        }
    }
}

private fun screenFor(s: String): Screen = when (s) {
    "player" -> Screen.Player
    "host" -> Screen.Host
    "dumpster" -> Screen.Dumpster
    "stealer" -> Screen.Stealer
    "bingo" -> Screen.Bingo
    "derby" -> Screen.Derby
    "slots" -> Screen.Slots
    "inspector" -> Screen.Inspector
    "items" -> Screen.Items
    else -> Screen.Boss
}

@Composable
fun CampaignScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    LaunchedEffect(state.campaignLevel) { vm.campaignStart() }
    val inf = rememberInfiniteTransition(label = "camp")
    val t by inf.animateFloat(0f, 20f, infiniteRepeatable(tween(20000)), label = "t")
    val glow by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "g")
    val list = rememberLazyListState()
    val cur = state.campaignLevel
    LaunchedEffect(Unit) {
        // jump near the current level: header + chapter headers before it
        val idx = 1 + (cur.coerceAtMost(Campaign.lastIndex)) + Campaign.getOrNull(cur.coerceAtMost(Campaign.lastIndex))!!.chapter + 1
        list.scrollToItem((idx - 2).coerceAtLeast(0))
    }

    Column(Modifier.fillMaxSize()) {
        LotTopBar("campaign", state.coins, { nav.back() })
        LazyColumn(state = list, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Column {
                    Text("the rise of a lottery legend", color = Gold, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text("${cur.coerceAtMost(Campaign.size)}/${Campaign.size} levels done", color = Dim, fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    Canvas(Modifier.fillMaxWidth().height(12.dp)) {
                        drawRoundRect(CardHi, cornerRadius = CornerRadius(12f))
                        drawRoundRect(Gold, size = Size(size.width * cur / Campaign.size, size.height), cornerRadius = CornerRadius(12f))
                    }
                }
            }
            Chapters.forEachIndexed { ci, name ->
                item(key = "ch$ci") {
                    Text("chapter ${ci + 1}: $name", color = modeColor("campaign"), fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                }
                Campaign.forEachIndexed { li, lv ->
                    if (lv.chapter != ci) return@forEachIndexed
                    item(key = "lv$li") {
                        when {
                            li < cur -> LevelRow(li, lv.title, "done · +${fmt(lv.reward)}c", Mint, true)
                            li > cur -> LevelRow(li, if (li == cur + 1) lv.title else "???", "locked", Dim, false)
                            else -> CurrentLevel(vm, state, nav, lv, t, glow)
                        }
                    }
                }
            }
            if (cur >= Campaign.size) {
                item {
                    Panel(Modifier.fillMaxWidth(), color = CardHi) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("THE END", color = Gold, fontWeight = FontWeight.Black, fontSize = 28.sp)
                            Text(
                                "you beat the commissioner and took over the lottery. the first thing you did was raise ticket prices. you've become what you fought. classic.",
                                color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelRow(i: Int, title: String, sub: String, color: Color, done: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(40.dp)) {
            drawCircle(if (done) Mint else CardHi, size.minDimension / 2)
            drawCircle(Ink, size.minDimension / 2, style = Stroke(4f))
            label(if (done) "✓" else "${i + 1}", center.x, center.y, size.minDimension * 0.45f, if (done) Ink else Dim)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = if (done) Color.White else Dim, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(sub, color = color, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CurrentLevel(vm: GameViewModel, state: GameState, nav: Nav, lv: CampaignLevel, t: Float, glow: Float) {
    val prog = vm.campaignProgress(state)
    val ready = prog >= lv.target
    Panel(Modifier.fillMaxWidth(), color = CardHi) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(86.dp)) {
                    drawCircle(Gold.copy(alpha = 0.15f * glow), size.minDimension / 2)
                    drawSpeaker(lv.speaker, t)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("level ${state.campaignLevel + 1}: ${lv.title}", color = Gold, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text(
                        lv.story, color = Ink, fontSize = 14.sp,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(8.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("goal: ${lv.goal}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Canvas(Modifier.fillMaxWidth().height(14.dp).padding(top = 4.dp)) {
                drawRoundRect(Card, cornerRadius = CornerRadius(12f))
                drawRoundRect(if (ready) Gold else Mint, size = Size(size.width * prog / lv.target, size.height), cornerRadius = CornerRadius(12f))
            }
            Text("${fmt(prog)}/${fmt(lv.target)} · reward ${fmt(lv.reward)}c", color = Dim, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChunkyButton(if (lv.screen == "boss") "enter his office" else "go", Modifier.weight(1f), color = Sky, sfx = Sfx.WHOOSH) {
                    nav.go(screenFor(lv.screen))
                }
                ChunkyButton("complete", Modifier.weight(1f), color = Gold, enabled = ready, sfx = Sfx.BIG_WIN) {
                    vm.campaignComplete()
                }
            }
        }
    }
}

private val accusations = listOf(
    "you yelled at my customers", "you stole tickets from a BUSH", "you dug through MY trash",
    "you made grannies cry", "you bet on the void", "your sock is filthy", "you know too much",
)

@Composable
fun BossScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    var attempt by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<Boolean?>(null) }
    var confetti by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            LotTopBar("the commissioner", state.coins, { nav.back() })
            Text(
                "final boss · ${accusations.size} accusations · no mercy",
                color = Pink, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp),
            )
            Box(Modifier.weight(1f)) {
                if (result == null) {
                    key(attempt) {
                        FightStage(
                            accusations, false, "lawyer" in state.items,
                            boss = CommissionerDude, extraLines = 0, fixedHp = if ("lawyer" in state.items) 12 else 14,
                            opener = "so. you're the one asking questions.",
                            challenge = "the lottery is MINE. defend yourself!",
                        ) { won, _ ->
                            result = won
                            if (won) {
                                vm.flag("boss")
                                confetti++
                            }
                        }
                    }
                } else {
                    Column(
                        Modifier.fillMaxSize().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Canvas(Modifier.size(160.dp)) {
                            drawDude(CommissionerDude, size.width / 2, size.height, size.height * 0.9f, if (result == true) Mood.ANGRY else Mood.HAPPY)
                        }
                        Text(
                            if (result == true) "you beat the commissioner!" else "he had you thrown out.",
                            color = if (result == true) Gold else Pink, fontWeight = FontWeight.Black, fontSize = 24.sp, textAlign = TextAlign.Center,
                        )
                        Text(
                            if (result == true) "go back to the campaign to finish it." else "he says you can try again whenever. he's very confident.",
                            color = Dim, fontSize = 15.sp, textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        if (result == true) {
                            ChunkyButton("back to campaign", Modifier.fillMaxWidth()) { nav.back() }
                        } else {
                            ChunkyButton("fight again", Modifier.fillMaxWidth(), color = Pink) { result = null; attempt++ }
                        }
                    }
                }
            }
        }
        Confetti(confetti)
    }
}
