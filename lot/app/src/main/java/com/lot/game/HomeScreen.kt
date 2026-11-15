package com.lot.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
        Spacer(Modifier.height(18.dp))

        ModeCard("player", "buy tickets, scratch stuff, lose fake money", Gold, t, art = { w, h, tt ->
            drawDude(Dude(Color(0xFFFFD7B5), Pink, Color(0xFF6B3E26), 1, Mint, "", false, 1f), w * 0.5f, h * 1.05f, h * 0.95f, Mood.HAPPY, wobble = tt * 2 - 1, holdTicket = true, t = tt * 10)
            drawSymbol(Sym.STAR, Offset(w * 0.18f, h * 0.35f), h * 0.35f)
            drawSymbol(Sym.SEVEN, Offset(w * 0.82f, h * 0.4f), h * 0.4f)
        }) { nav.go(Screen.Player) }
        Spacer(Modifier.height(12.dp))
        ModeCard("host", "run the stand. sell. yell. regret.", Pink, t, art = { w, h, tt ->
            drawStand(w * 0.12f, w * 0.88f, h * 0.3f, h * 0.68f, h * 1.0f)
            drawDude(HostDude.copy(shirt = Gold), w * 0.5f, h * 0.7f, h * 0.45f, Mood.GRUMPY, talk = tt > 0.5f, t = tt * 10)
        }) { nav.go(Screen.Host) }
        Spacer(Modifier.height(12.dp))
        ModeCard("stealer", "spy with binoculars, snatch winning tickets", Mint, t, art = { w, h, tt ->
            drawBush(Offset(w * 0.32f, h * 0.65f), h * 0.42f, true, tt * 2 - 1, tt * 20)
            // binoculars
            val c = Offset(w * 0.7f, h * 0.5f)
            circleO(c + Offset(-h * 0.16f, 0f), h * 0.15f, Color(0xFF333344), h * 0.03f)
            circleO(c + Offset(h * 0.16f, 0f), h * 0.15f, Color(0xFF333344), h * 0.03f)
            drawCircle(Sky.copy(alpha = 0.7f), h * 0.09f, c + Offset(-h * 0.16f, 0f))
            drawCircle(Sky.copy(alpha = 0.7f), h * 0.09f, c + Offset(h * 0.16f, 0f))
        }) { nav.go(Screen.Stealer) }

        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChunkyButton("badges ${state.badges.size}/${Badges.size}", Modifier.weight(1f), color = Sky) { nav.go(Screen.Badges) }
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
