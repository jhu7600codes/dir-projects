package com.lot.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun HomeScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    var settings by remember { mutableStateOf(false) }
    val inf = rememberInfiniteTransition(label = "home")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "t")

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            CoinPill(state.coins)
        }
        LotLogo()
        Text("the fake money lottery", color = Dim, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
            "rank: ${rankOf(state.badges.size)}", color = Ink, fontWeight = FontWeight.Black, fontSize = 13.sp,
            modifier = Modifier
                .padding(top = 6.dp)
                .background(modeColor("campaign"), androidx.compose.foundation.shape.RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 3.dp),
        )
        Spacer(Modifier.height(10.dp))
        NewsTicker(state)
        Spacer(Modifier.height(12.dp))
        QuestPanel(vm, state)
        Spacer(Modifier.height(12.dp))

        CampaignCard(state, t) { nav.go(Screen.Campaign) }
        Spacer(Modifier.height(12.dp))
        val modes = listOf(
        ModeEntry("player", "buy tickets, scratch stuff, lose fake money", Gold, art = { w, h, tt ->
            drawDude(Dude(Color(0xFFFFD7B5), Pink, Color(0xFF6B3E26), 1, Mint, "", false, 1f), w * 0.5f, h * 1.05f, h * 0.95f, Mood.HAPPY, wobble = tt * 2 - 1, holdTicket = true, t = tt * 10)
            drawSymbol(Sym.STAR, Offset(w * 0.18f, h * 0.35f), h * 0.35f)
            drawSymbol(Sym.SEVEN, Offset(w * 0.82f, h * 0.4f), h * 0.4f)
        }, go = Screen.Player),
        ModeEntry("host", "run the stand. sell. yell. regret.", Pink, art = { w, h, tt ->
            drawStand(w * 0.12f, w * 0.88f, h * 0.3f, h * 0.68f, h * 1.0f)
            drawDude(HostDude.copy(shirt = Gold), w * 0.5f, h * 0.7f, h * 0.45f, Mood.GRUMPY, talk = tt > 0.5f, t = tt * 10)
        }, go = Screen.Host),
        ModeEntry("stealer", "spy with binoculars, snatch winning tickets", Mint, art = { w, h, tt ->
            drawBush(Offset(w * 0.32f, h * 0.65f), h * 0.42f, true, tt * 2 - 1, tt * 20)
            // binoculars
            val c = Offset(w * 0.7f, h * 0.5f)
            circleO(c + Offset(-h * 0.16f, 0f), h * 0.15f, Color(0xFF333344), h * 0.03f)
            circleO(c + Offset(h * 0.16f, 0f), h * 0.15f, Color(0xFF333344), h * 0.03f)
            drawCircle(Sky.copy(alpha = 0.7f), h * 0.09f, c + Offset(-h * 0.16f, 0f))
            drawCircle(Sky.copy(alpha = 0.7f), h * 0.09f, c + Offset(h * 0.16f, 0f))
        }, go = Screen.Stealer),
        ModeEntry("bingo", "you vs three very fast grannies", Sky, art = { w, h, tt ->
            drawDude(grannyDude(0), w * 0.3f, h * 1.05f, h * 0.9f, Mood.GRUMPY, t = tt * 10)
            drawBall(Offset(w * 0.72f, h * 0.35f), h * 0.2f, 42)
            drawBall(Offset(w * 0.78f, h * 0.75f), h * 0.16f, 7)
        }, go = Screen.Bingo),
        ModeEntry("dumpster", "dig through trash for thrown away winners", Color(0xFF9CCC65), art = { w, h, tt ->
            drawTrashBag(Offset(w * 0.3f, h * 0.55f), h * 0.7f, Color(0xFF2A2830), (tt - 0.5f) * 0.6f)
            drawRaccoon(Offset(w * 0.68f, h * 0.65f), h * 0.45f, tt * 5f, false)
        }, go = Screen.Dumpster),
        ModeEntry("derby", "bet on pigeons. lose to pigeons.", Color(0xFFFF8C42), art = { w, h, tt ->
            drawPigeon(Offset(w * 0.35f, h * 0.4f), h * 0.4f, tt, 1f)
            drawPigeon(Offset(w * 0.62f, h * 0.7f), h * 0.4f, 1f - tt, 1f, Color(0xFF3A3A44))
            for (i in 0 until 6) drawRect(if (i % 2 == 0) Color.White else Ink, Offset(w * 0.88f, i * h / 6), Size(w * 0.04f, h / 6))
        }, go = Screen.Derby),
        ModeEntry("slots", "stop the reels yourself. blame yourself.", Color(0xFFE53935), art = { w, h, tt ->
            drawRoundRect(Color(0xFFE53935), Offset(w * 0.15f, h * 0.15f), Size(w * 0.6f, h * 0.75f), androidx.compose.ui.geometry.CornerRadius(16f))
            for (i in 0 until 3) {
                drawRect(Color.White, Offset(w * (0.2f + i * 0.18f), h * 0.32f), Size(w * 0.15f, h * 0.4f))
                drawSymbol(Sym.SEVEN, Offset(w * (0.275f + i * 0.18f), h * 0.52f), h * 0.3f)
            }
            drawLine(Ink, Offset(w * 0.8f, h * 0.5f), Offset(w * 0.85f, h * (0.15f + tt * 0.2f)), 8f)
            circleO(Offset(w * 0.85f, h * (0.15f + tt * 0.2f)), h * 0.09f, Pink, 4f)
        }, go = Screen.Slots),
        ModeEntry("inspector", "spot fake tickets before they spot you", Color(0xFF26C6DA), art = { w, h, tt ->
            drawInspectTicket(realTicket(1234), Offset(w * 0.12f, h * 0.2f), Size(w * 0.6f, h * 0.55f))
            drawCircle(Color(0xFFBFE6FF).copy(alpha = 0.5f), h * 0.22f, Offset(w * (0.55f + tt * 0.15f), h * 0.5f))
            drawCircle(Ink, h * 0.22f, Offset(w * (0.55f + tt * 0.15f), h * 0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(6f))
        }, go = Screen.Inspector),

            ModeEntry("rain", "catch falling tickets", Color(0xFF7986CB), art = { w, h, tt ->
                for (i in 0 until 4) {
                    val x = w * (0.2f + i * 0.2f)
                    val y = h * ((0.15f + i * 0.17f + tt * 0.3f) % 0.8f)
                    drawRoundRect(Color(0xFFFFF3D6), Offset(x - 12f, y - 7f), Size(24f, 14f), androidx.compose.ui.geometry.CornerRadius(3f))
                }
                outlined(androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.3f, h * 0.75f); lineTo(w * 0.7f, h * 0.75f); lineTo(w * 0.65f, h * 0.95f); lineTo(w * 0.35f, h * 0.95f); close()
                }, Color(0xFFB5651D), 4f)
            }, go = Screen.Rain),
            ModeEntry("hi-lo", "higher or lower. or broke.", Color(0xFFFFB74D), art = { w, h, tt ->
                drawBall(Offset(w * 0.35f, h * 0.5f), h * 0.3f, 17)
                label(if (tt > 0.5f) "▲" else "▼", w * 0.72f, h * 0.5f, h * 0.45f, if (tt > 0.5f) Pink else Sky, Ink)
            }, go = Screen.Hilo),
        )
        modes.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { m -> ModeTile(m, t, Modifier.weight(1f)) { nav.go(m.go) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChunkyButton("badges ${state.badges.size}/${Badges.size}", Modifier.weight(1f), color = Sky) { nav.go(Screen.Badges) }
            ChunkyButton("items ${state.items.size}/${Items.size}", Modifier.weight(1f), color = Pink) { nav.go(Screen.Items) }
            ChunkyButton("settings", Modifier.weight(1f), color = CardHi, textColor = Color.White) { settings = true }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (settings) SettingsDialog(vm, state) { settings = false }
}

@Composable
private fun ModeCard(
    title: String, sub: String, color: Color, t: Float,
    art: DrawScope.(Float, Float, Float) -> Unit,
    onClick: () -> Unit,
) {
    val fx = LocalFx.current
    Panel(
        Modifier
            .fillMaxWidth()
            .clickable { fx.play(Sfx.WHOOSH); fx.buzz(Buzz.LIGHT); onClick() },
        color = Card,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(
                Modifier
                    .size(110.dp, 86.dp)
            ) {
                drawRoundRect(color.copy(alpha = 0.18f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(30f))
                art(size.width, size.height, t)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = color, fontWeight = FontWeight.Black, fontSize = 28.sp)
                Text(sub, color = Dim, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SettingsDialog(vm: GameViewModel, state: GameState, onClose: () -> Unit) {
    var confirmReset by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onClose) {
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("settings", color = Gold, fontWeight = FontWeight.Black, fontSize = 24.sp)
                Spacer(Modifier.height(10.dp))
                SettingRow("sound effects", state.sound) { vm.setSound(it) }
                SettingRow("haptics", state.haptics) { vm.setHaptics(it) }
                Spacer(Modifier.height(8.dp))
                Text("show coin \"value\" in", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("for fun only. coins are fake and can't be cashed out.", color = Dim, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Currencies.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { c ->
                            val on = c.code == state.currency
                            Text(
                                c.code, color = if (on) Ink else Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (on) Gold else CardHi, androidx.compose.foundation.shape.RoundedCornerShape(50))
                                    .clickable { vm.setCurrency(c.code) }
                                    .padding(vertical = 6.dp),
                            )
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Text("1 coin ≈ ${coinValue(1, state.currency)}", color = Mint, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                ChunkyButton("reset all progress", Modifier.fillMaxWidth(), color = Color(0xFFFF6B6B)) { confirmReset = true }
                Spacer(Modifier.height(8.dp))
                ChunkyButton("done", Modifier.fillMaxWidth()) { onClose() }
                Spacer(Modifier.height(8.dp))
                Text("all money is fake. please don't gamble irl, it's way less funny.", color = Dim, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("wipe everything?") },
            text = { Text("coins, stats, badges and tutorials all go back to zero.") },
            confirmButton = { TextButton({ vm.resetAll(); confirmReset = false; onClose() }) { Text("wipe it") } },
            dismissButton = { TextButton({ confirmReset = false }) { Text("nah") } },
            containerColor = Card,
        )
    }
}

@Composable
private fun SettingRow(label: String, on: Boolean, set: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Switch(on, set, colors = SwitchDefaults.colors(checkedTrackColor = Gold, checkedThumbColor = Ink))
    }
}

@Composable
fun BadgesScreen(state: GameState, nav: Nav) {
    Column(Modifier.fillMaxSize()) {
        LotTopBar("badges ${state.badges.size}/${Badges.size}", null, { nav.back() })
        LazyVerticalGrid(
            GridCells.Fixed(3),
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(Badges, key = { it.id }) { b ->
                val got = b.id in state.badges
                Panel(Modifier.fillMaxWidth(), color = if (got) CardHi else Card) {
                    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1.2f)) {
                            Canvas(Modifier.fillMaxSize()) {
                                drawMedal(center + Offset(0f, size.height * 0.15f), size.minDimension * 0.3f, modeColor(b.mode), !got)
                            }
                        }
                        Text(
                            if (got) b.name else "???", color = if (got) Color.White else Dim,
                            fontWeight = FontWeight.Black, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 2,
                        )
                        Text(b.desc, color = Dim, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
                        Text(b.mode, color = modeColor(b.mode), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

class ModeEntry(
    val title: String, val sub: String, val color: Color,
    val art: DrawScope.(Float, Float, Float) -> Unit, val go: Screen,
)

fun rankOf(badges: Int) = when {
    badges >= 65 -> "the house"
    badges >= 55 -> "lottery legend"
    badges >= 45 -> "fake money mogul"
    badges >= 35 -> "stand regular"
    badges >= 25 -> "lottery enjoyer"
    badges >= 15 -> "scratch rookie"
    badges >= 5 -> "ticket goblin"
    else -> "nobody (yet)"
}

@Composable
private fun ModeTile(m: ModeEntry, t: Float, modifier: Modifier, onClick: () -> Unit) {
    val fx = LocalFx.current
    Panel(
        modifier.clickable { fx.play(Sfx.WHOOSH); fx.buzz(Buzz.LIGHT); onClick() },
        color = Card,
    ) {
        Column(Modifier.padding(10.dp)) {
            Canvas(Modifier.fillMaxWidth().height(78.dp)) {
                drawRoundRect(m.color.copy(alpha = 0.18f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f))
                m.art(this, size.width, size.height, t)
            }
            Spacer(Modifier.height(6.dp))
            Text(m.title, color = m.color, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text(m.sub, color = Dim, fontSize = 12.sp, maxLines = 2, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun CampaignCard(state: GameState, t: Float, onClick: () -> Unit) {
    val fx = LocalFx.current
    val lv = Campaign.getOrNull(state.campaignLevel)
    Panel(
        Modifier
            .fillMaxWidth()
            .clickable { fx.play(Sfx.WHOOSH); fx.buzz(Buzz.LIGHT); onClick() },
        color = CardHi,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(90.dp, 80.dp)) {
                drawRoundRect(modeColor("campaign").copy(alpha = 0.2f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f))
                drawDude(CommissionerDude, size.width / 2, size.height * 1.05f, size.height * 0.9f, Mood.GRUMPY, wobble = (t - 0.5f) * 0.6f, t = t * 10)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("campaign", color = modeColor("campaign"), fontWeight = FontWeight.Black, fontSize = 26.sp)
                Text(
                    if (lv == null) "complete! you are the lottery now." else "chapter ${lv.chapter + 1} · level ${state.campaignLevel + 1}: ${lv.title}",
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                )
                Text("${state.campaignLevel.coerceAtMost(Campaign.size)}/${Campaign.size} levels", color = Dim, fontSize = 12.sp)
            }
        }
    }
}
