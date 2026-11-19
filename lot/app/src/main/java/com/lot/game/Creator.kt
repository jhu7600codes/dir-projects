package com.lot.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val paperColors = listOf(0xFFFFE08A, 0xFFF3E9D2, 0xFFCFE3F5, 0xFFDDF7E9, 0xFFFFE1EC, 0xFFE6E0D0, 0xFFD8FBFF, 0xFF3B2F4A)
private val accentColors = listOf(0xFFFF5C8A, 0xFF4BE3AC, 0xFF6EC6FF, 0xFFFFC93C, 0xFF8E6CFF, 0xFFFF8C42, 0xFFE53935, 0xFF2E9E5B)
private val ticketPrices = listOf(2, 5, 10, 20, 50, 100, 250, 500, 1000)
private val modePrices = listOf(5, 10, 25, 50, 100, 500)
private val hosts = listOf("host", "granny", "cashier", "commissioner", "raccoon", "pigeon", "you")

fun DrawScope.drawHostChar(who: String, t: Float, talk: Boolean = false) {
    val w = size.width
    val h = size.height
    when (who) {
        "raccoon" -> drawRaccoon(Offset(w * 0.45f, h * 0.65f), h * 0.6f, t, false)
        "pigeon" -> drawPigeon(Offset(w / 2, h * 0.6f), h * 0.6f, kotlin.math.abs(sin(t * 6f)), 1f)
        else -> {
            val d = when (who) {
                "granny" -> grannyDude(0)
                "cashier" -> CashierDude
                "commissioner" -> CommissionerDude
                "you" -> YouDude
                else -> HostDude
            }
            drawDude(d, w / 2, h * 1.02f, h * 0.95f, Mood.HAPPY, talk = talk, t = t)
        }
    }
}

@Composable
private fun lotFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
    focusedBorderColor = Gold, unfocusedBorderColor = Dim, cursorColor = Gold,
    focusedLabelColor = Gold, unfocusedLabelColor = Dim,
)

@Composable
private fun ColorRow(colors: List<Long>, selected: Long, onPick: (Long) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        colors.forEach { c ->
            Box(
                Modifier
                    .size(32.dp)
                    .background(Color(c), CircleShape)
                    .border(if (c == selected) 3.dp else 1.5.dp, if (c == selected) Color.White else Ink, CircleShape)
                    .clickable { onPick(c) }
            )
        }
    }
}

@Composable
private fun ChipRow(options: List<String>, selected: String, onPick: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { o ->
            val on = o == selected
            Text(
                o, color = if (on) Ink else Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .background(if (on) Gold else CardHi, RoundedCornerShape(50))
                    .clickable { onPick(o) }
                    .padding(vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun Label(text: String) = Text(text, color = Gold, fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))

@Composable
fun CreatorScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        LotTopBar("creator studio", state.coins, { nav.back() })
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChunkyButton("tickets", Modifier.weight(1f), color = if (tab == 0) Gold else CardHi, textColor = if (tab == 0) Ink else Color.White, height = 46.dp) { tab = 0 }
            ChunkyButton("modes", Modifier.weight(1f), color = if (tab == 1) Gold else CardHi, textColor = if (tab == 1) Ink else Color.White, height = 46.dp) { tab = 1 }
        }
        if (tab == 0) TicketCreator(vm, state) else ModeCreator(vm, state, nav)
    }
}

// ================================ ticket creator ================================

@Composable
private fun TicketCreator(vm: GameViewModel, state: GameState) {
    val fx = LocalFx.current
    var name by remember { mutableStateOf("") }
    var price by remember { mutableIntStateOf(10) }
    var chance by remember { mutableFloatStateOf(0.3f) }
    val syms = remember { mutableStateListOf(Sym.SOCK, Sym.CHERRY, Sym.BELL, Sym.STAR, Sym.SEVEN) }
    var paper by remember { mutableLongStateOf(paperColors[0]) }
    var accent by remember { mutableLongStateOf(accentColors[0]) }
    var saved by remember { mutableStateOf<String?>(null) }
    val draft = CustomTicket(0, name.ifBlank { "my ticket" }, price, chance, syms.toList(), paper, accent)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("design your own scratch ticket. it shows up in the player shop.", color = Dim, fontSize = 13.sp)
        // preview
        Spacer(Modifier.height(8.dp))
        if (syms.size == 5) TicketPreview(draft)
        Label("name")
        OutlinedTextField(
            name, { name = it.take(22) }, Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("e.g. lucky socks deluxe", color = Dim) }, colors = lotFieldColors(),
        )
        Label("price")
        ChipRow(ticketPrices.map { "$it" }, "$price") { price = it.toInt() }
        Label("win chance: ${(chance * 100).toInt()}%  (lower = bigger prizes)")
        Slider(
            chance, { chance = it }, valueRange = 0.1f..0.5f, steps = 7,
            colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold),
        )
        Label("symbols (tap in order: most common first, rarest last)")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Sym.entries.forEach { s ->
                val idx = syms.indexOf(s)
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .background(if (idx >= 0) CardHi else Card, RoundedCornerShape(10.dp))
                        .border(2.dp, if (idx >= 0) Gold else Ink, RoundedCornerShape(10.dp))
                        .clickable {
                            if (idx >= 0) syms.remove(s) else if (syms.size < 5) syms.add(s)
                            fx.play(Sfx.CLICK)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize().padding(4.dp)) { drawSymbol(s, center, size.width * 0.8f) }
                    if (idx >= 0) Text("${idx + 1}", color = Gold, fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp))
                }
            }
        }
        if (syms.size < 5) Text("pick ${5 - syms.size} more", color = Pink, fontSize = 12.sp)
        Label("paper")
        ColorRow(paperColors, paper) { paper = it }
        Label("stripe")
        ColorRow(accentColors, accent) { accent = it }
        Spacer(Modifier.height(16.dp))
        ChunkyButton("save ticket", Modifier.fillMaxWidth(), color = Mint, enabled = syms.size == 5 && name.isNotBlank(), sfx = Sfx.BUY) {
            vm.saveTicket(draft.copy(id = System.currentTimeMillis(), name = name.trim()))
            saved = "\"${name.trim()}\" is now in the shop!"
            name = ""
        }
        saved?.let { Text(it, color = Mint, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)) }

        if (state.customTickets.isNotEmpty()) {
            Label("your tickets")
            state.customTickets.forEach { t ->
                Panel(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(28.dp).background(Color(t.paper), RoundedCornerShape(6.dp)).border(2.dp, Color(t.accent), RoundedCornerShape(6.dp)))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${t.price}c · ${(t.chance * 100).toInt()}% · top ${fmt(t.prizes().last())}", color = Dim, fontSize = 12.sp)
                        }
                        ShareButton(vm, state, "ticket", t.name, t.toJson())
                        Spacer(Modifier.width(6.dp))
                        ChunkyButton("x", Modifier.width(44.dp), color = Pink, height = 40.dp) { vm.deleteTicket(t.id) }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TicketPreview(t: CustomTicket) {
    val prizes = t.prizes()
    Panel(Modifier.fillMaxWidth(), color = Color(t.paper)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.fillMaxWidth().background(Color(t.accent), RoundedCornerShape(10.dp)).padding(6.dp),
                contentAlignment = Alignment.Center,
            ) { Text(t.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp) }
            Text("${t.price}c · match 3 to win", color = if (Color(t.paper).luminance() < 0.4f) Color.White else Ink, fontSize = 13.sp, modifier = Modifier.padding(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                t.syms.forEachIndexed { i, s ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Canvas(Modifier.size(34.dp)) { drawSymbol(s, center, size.width * 0.9f) }
                        Text(fmt(prizes[i]), color = if (Color(t.paper).luminance() < 0.4f) Color.White else Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun Color.luminance() = 0.299f * red + 0.587f * green + 0.114f * blue

// ================================ mode creator ================================

private val templateNames = mapOf(
    "draw" to "number draw", "wheel" to "wheel", "flip" to "coin flips", "dice" to "dice",
    "cups" to "cup shuffle", "race" to "race", "boxes" to "mystery boxes",
)

@Composable
private fun ModeCreator(vm: GameViewModel, state: GameState, nav: Nav) {
    var name by remember { mutableStateOf("") }
    var template by remember { mutableStateOf("draw") }
    var host by remember { mutableStateOf("host") }
    var color by remember { mutableLongStateOf(accentColors[0]) }
    var phrase by remember { mutableStateOf("") }
    var price by remember { mutableIntStateOf(10) }
    var pool by remember { mutableFloatStateOf(20f) }
    var picks by remember { mutableFloatStateOf(3f) }
    var draws by remember { mutableFloatStateOf(5f) }
    var segments by remember { mutableFloatStateOf(8f) }
    var flips by remember { mutableFloatStateOf(3f) }
    var dice by remember { mutableFloatStateOf(2f) }
    var cups by remember { mutableFloatStateOf(3f) }
    var swaps by remember { mutableFloatStateOf(6f) }
    var runners by remember { mutableFloatStateOf(4f) }
    var runnerType by remember { mutableStateOf("pigeon") }
    var boxes by remember { mutableFloatStateOf(9f) }
    var bombs by remember { mutableFloatStateOf(2f) }
    var clock by remember { mutableFloatStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        var last = 0L
        while (true) androidx.compose.runtime.withFrameNanos { now -> if (last != 0L) clock += (now - last) / 1e9f; last = now }
    }
    val p = picks.toInt()
    val d = draws.toInt().coerceIn(p, pool.toInt() - 1)
    val draft = CustomMode(
        0, name.ifBlank { "my mode" }, template, color, host, phrase.ifBlank { "welcome to my game!" }, price,
        pool.toInt(), p, d, segments.toInt() / 2 * 2, flips.toInt(),
        dice.toInt(), cups.toInt(), swaps.toInt(), runners.toInt(), runnerType, boxes.toInt(),
        bombs.toInt().coerceIn(1, boxes.toInt() - 2), state.nickname,
    )
    val sc = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("build your own game mode. it shows up on the home screen.", color = Dim, fontSize = 13.sp)
        Label("name")
        OutlinedTextField(name, { name = it.take(22) }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("e.g. grandma's revenge", color = Dim) }, colors = lotFieldColors())
        Label("game type")
        templateNames.values.toList().chunked(4).forEach { row ->
            ChipRow(row, templateNames.getValue(template)) { v -> template = templateNames.entries.first { it.value == v }.key }
            Spacer(Modifier.height(6.dp))
        }
        when (template) {
            "draw" -> {
                Label("numbers 1 to ${pool.toInt()}")
                Slider(pool, { pool = it; if (draws >= it) draws = it - 1 }, valueRange = 10f..50f, steps = 39, colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold))
                Label("player picks $p")
                Slider(picks, { picks = it; if (draws < it) draws = it }, valueRange = 1f..6f, steps = 4, colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold))
                Label("balls drawn $d")
                Slider(draws, { draws = it }, valueRange = 1f..10f, steps = 8, colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold))
            }
            "wheel" -> {
                Label("wheel slices ${segments.toInt() / 2 * 2}")
                Slider(segments, { segments = it }, valueRange = 4f..12f, steps = 7, colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold))
            }
            "flip" -> {
                Label("flips in a row to win: ${flips.toInt()}")
                Slider(flips, { flips = it }, valueRange = 1f..8f, steps = 6, colors = sc)
            }
            "dice" -> {
                Label("number of dice: ${dice.toInt()}")
                Slider(dice, { dice = it }, valueRange = 1f..3f, steps = 1, colors = sc)
            }
            "cups" -> {
                Label("cups: ${cups.toInt()}")
                Slider(cups, { cups = it }, valueRange = 3f..6f, steps = 2, colors = sc)
                Label("shuffles: ${swaps.toInt()} (more = harder to follow)")
                Slider(swaps, { swaps = it }, valueRange = 3f..20f, steps = 16, colors = sc)
            }
            "race" -> {
                Label("runners: ${runners.toInt()}")
                Slider(runners, { runners = it }, valueRange = 3f..6f, steps = 2, colors = sc)
                Label("who races")
                ChipRow(RunnerTypes, runnerType) { runnerType = it }
            }
            else -> {
                Label("boxes: ${boxes.toInt()}")
                Slider(boxes, { boxes = it; if (bombs > it - 2) bombs = it - 2 }, valueRange = 4f..12f, steps = 7, colors = sc)
                Label("bombs: ${bombs.toInt().coerceIn(1, boxes.toInt() - 2)}")
                Slider(bombs, { bombs = it }, valueRange = 1f..(boxes - 2f).coerceAtLeast(2f), colors = sc)
            }
        }
        Label("entry price")
        ChipRow(modePrices.map { "$it" }, "$price") { price = it.toInt() }
        Label("host")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            hosts.forEach { hh ->
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(0.8f)
                        .background(if (hh == host) CardHi else Card, RoundedCornerShape(10.dp))
                        .border(2.dp, if (hh == host) Gold else Ink, RoundedCornerShape(10.dp))
                        .clickable { host = hh },
                ) { Canvas(Modifier.fillMaxSize().padding(2.dp)) { drawHostChar(hh, clock) } }
            }
        }
        Label("host catchphrase")
        OutlinedTextField(phrase, { phrase = it.take(60) }, Modifier.fillMaxWidth(), placeholder = { Text("welcome to my game!", color = Dim) }, colors = lotFieldColors())
        Label("color")
        ColorRow(accentColors, color) { color = it }
        Spacer(Modifier.height(10.dp))
        Panel(Modifier.fillMaxWidth(), color = CardHi) {
            Text(modeSummary(draft), color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(12.dp))
        }
        Spacer(Modifier.height(12.dp))
        ChunkyButton("save mode", Modifier.fillMaxWidth(), color = Mint, enabled = name.isNotBlank(), sfx = Sfx.BUY) {
            vm.saveMode(draft.copy(id = System.currentTimeMillis(), name = name.trim(), catchphrase = phrase.trim().ifBlank { "welcome to my game!" }))
            name = ""; phrase = ""
        }
        if (state.customModes.isNotEmpty()) {
            Label("your modes")
            state.customModes.forEach { m ->
                Panel(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(28.dp).background(Color(m.color), CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${templateNames[m.template]} · ${m.price}c", color = Dim, fontSize = 12.sp)
                        }
                        ChunkyButton("play", Modifier.width(64.dp), color = Mint, height = 40.dp) { vm.currentMode = m; nav.go(Screen.CustomPlay) }
                        Spacer(Modifier.width(6.dp))
                        ShareButton(vm, state, "mode", m.name, m.toJson())
                        Spacer(Modifier.width(6.dp))
                        ChunkyButton("x", Modifier.width(44.dp), color = Pink, height = 40.dp) { vm.deleteMode(m.id) }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

fun modeSummary(m: CustomMode): String = when (m.template) {
    "dice" -> "roll ${m.dice} di${if (m.dice > 1) "ce" else "e"}, call the total.\n" +
        "rare totals pay more, up to ${fmt((m.dice..m.dice * 6).maxOf { dicePayout(m, it) })}c"
    "cups" -> "follow the ball under ${m.cups} cups through ${m.swaps} shuffles.\nfind it: ${fmt(pickOnePayout(m, m.cups))}c"
    "race" -> "${m.runners} ${m.runnerType}s race, pick the winner.\nwin: ${fmt(pickOnePayout(m, m.runners))}c"
    "boxes" -> "${m.boxes} boxes, ${m.bombs} have a bomb.\nevery safe box grows the pot, cash out any time."
    "draw" -> {
        val pr = drawPrizes(m)
        "pick ${m.picks} of 1-${m.pool}, ${m.draws} balls drawn.\n" +
            pr.mapIndexedNotNull { k, v -> if (v > 0) "$k match${if (k > 1) "es" else ""}: ${fmt(v)}c" else null }.joinToString("\n")
    }
    "wheel" -> "spin a ${m.segments} slice wheel. half the slices pay nothing.\nbest slice: ${fmt(m.price * wheelMults(m).max() / 100)}c"
    else -> "call ${m.flips} coin flip${if (m.flips > 1) "s" else ""} in a row.\nget them all right: ${fmt(flipPayout(m))}c"
}

// ================================ playing a custom mode ================================

fun DrawScope.drawSegWheel(c: Offset, r: Float, rot: Float, labels: List<String>, colors: List<Color>) {
    val seg = 360f / labels.size
    labels.forEachIndexed { i, l ->
        val start = -90f + i * seg + rot
        drawArc(colors[i % colors.size], start, seg, true, c - Offset(r, r), Size(r * 2, r * 2))
        drawArc(Ink, start, seg, true, c - Offset(r, r), Size(r * 2, r * 2), style = Stroke(r * 0.03f))
        val a = Math.toRadians((start + seg / 2).toDouble())
        val tp = c + Offset((cos(a) * r * 0.66f).toFloat(), (sin(a) * r * 0.66f).toFloat())
        label(l, tp.x, tp.y, r * 0.14f, Ink)
    }
    circleO(c, r * 0.12f, Gold, r * 0.03f)
    drawCircle(Ink, r, c, style = Stroke(r * 0.05f))
}

@Composable
fun CustomPlayScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val m = vm.currentMode
    if (m == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { nav.back() }
        return
    }
    val fx = LocalFx.current
    val scope = rememberCoroutineScope()
    var clock by remember { mutableFloatStateOf(0f) }
    var talk by remember { mutableStateOf(true) }
    var line by remember { mutableStateOf(m.catchphrase) }
    var busy by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        var last = 0L
        while (true) androidx.compose.runtime.withFrameNanos { now -> if (last != 0L) clock += (now - last) / 1e9f; last = now }
    }
    androidx.compose.runtime.LaunchedEffect(line) { talk = true; delay(1400); talk = false }

    fun pay(): Boolean {
        if (state.coins < m.price) return false
        vm.update { it.copy(coins = it.coins - m.price, customPlays = it.customPlays + 1) }
        fx.play(Sfx.BUY)
        return true
    }

    fun result(win: Int) {
        if (win > 0) {
            vm.update { it.copy(coins = it.coins + win) }
            line = "you won ${fmt(win)}c! ${listOf("i'm shocked", "lucky you", "this game is broken", "don't tell anyone").random()}"
            fx.play(Sfx.BIG_WIN); fx.buzz(Buzz.SUCCESS); confetti++
        } else {
            line = listOf("you lose! ${m.catchphrase}", "nothing! classic.", "better luck never", "the house thanks you").random()
            fx.play(Sfx.LOSE); fx.buzz(Buzz.FAIL)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            LotTopBar(m.name, state.coins, { nav.back() })
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(84.dp).background(Color(m.color).copy(alpha = 0.25f), RoundedCornerShape(16.dp))) { drawHostChar(m.host, clock, talk) }
                Spacer(Modifier.width(10.dp))
                Text(
                    line, color = Ink, fontWeight = FontWeight.Black, fontSize = 15.sp,
                    modifier = Modifier.weight(1f).background(Color.White, RoundedCornerShape(14.dp)).border(2.5.dp, Ink, RoundedCornerShape(14.dp)).padding(10.dp),
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                when (m.template) {
                    "draw" -> DrawGame(m, state, busy, { busy = it }, { pay() }, { line = it }, { result(it) })
                    "wheel" -> {
                        val mults = remember(m.id) { wheelMults(m) }
                        val rot = remember { Animatable(0f) }
                        val cols = listOf(Color(m.color), Color.White)
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                            Canvas(Modifier.weight(1f).aspectRatio(1f)) {
                                drawSegWheel(center, size.minDimension * 0.45f, rot.value, mults.map { if (it == 0) "lol" else "x${"%.1f".format(it / 100f)}" }, cols)
                                val top = Offset(center.x, center.y - size.minDimension * 0.45f)
                                outlined(Path().apply { moveTo(top.x - 22f, top.y - 30f); lineTo(top.x + 22f, top.y - 30f); lineTo(top.x, top.y + 16f); close() }, Pink, 5f)
                            }
                            ChunkyButton("spin (${m.price}c)", Modifier.fillMaxWidth(), color = Color(m.color), enabled = !busy && state.coins >= m.price, sfx = null) {
                                if (!pay()) return@ChunkyButton
                                busy = true
                                val idx = Random.nextInt(mults.size)
                                val seg = 360f / mults.size
                                val base = rot.value - rot.value % 360f
                                val target = base + 360f * 5 - (idx * seg + seg / 2)
                                line = "round and round it goes..."
                                scope.launch {
                                    rot.animateTo(target, tween(3500, easing = CubicBezierEasing(0.1f, 0.7f, 0.2f, 1f)))
                                    result(m.price * mults[idx] / 100)
                                    busy = false
                                }
                            }
                        }
                    }
                    "dice" -> DiceGame(m, state, busy, { busy = it }, { pay() }, { line = it }, { result(it) })
                    "cups" -> CupsGame(m, state, busy, { busy = it }, { pay() }, { line = it }, { result(it) })
                    "race" -> RaceGame(m, state, busy, { busy = it }, { pay() }, { line = it }, { result(it) })
                    "boxes" -> BoxesGame(m, state, busy, { busy = it }, { pay() }, { line = it }, { result(it) })
                    else -> FlipGame(m, state, busy, { busy = it }, { pay() }, { line = it }, { result(it) })
                }
            }
        }
        Confetti(confetti)
    }
}

@Composable
private fun DrawGame(
    m: CustomMode, state: GameState, busy: Boolean, setBusy: (Boolean) -> Unit,
    pay: () -> Boolean, say: (String) -> Unit, result: (Int) -> Unit,
) {
    val fx = LocalFx.current
    val scope = rememberCoroutineScope()
    val picks = remember { mutableStateListOf<Int>() }
    val drawn = remember { mutableStateListOf<Int>() }
    val prizes = remember(m.id) { drawPrizes(m) }
    Column(Modifier.fillMaxSize()) {
        Text("pick ${m.picks} · ${picks.size}/${m.picks}", color = Dim, fontWeight = FontWeight.Bold)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            (1..m.pool).chunked(7).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { n ->
                        val on = n in picks
                        Canvas(
                            Modifier.weight(1f).aspectRatio(1f).clickable(enabled = !busy) {
                                if (on) picks.remove(n) else if (picks.size < m.picks) picks.add(n)
                                drawn.clear(); fx.play(Sfx.BALL)
                            }
                        ) {
                            val r = size.minDimension * 0.42f
                            if (on || n in drawn) drawBall(center, r, n, on && n in drawn)
                            else { drawCircle(CardHi, r, center); label("$n", center.x, center.y, r * 0.8f, Dim) }
                        }
                    }
                    repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Text("drawn: ${drawn.joinToString(" ")}", color = Gold, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(vertical = 6.dp))
        ChunkyButton("play (${m.price}c)", Modifier.fillMaxWidth(), color = Color(m.color), enabled = !busy && picks.size == m.picks && state.coins >= m.price, sfx = null) {
            if (!pay()) return@ChunkyButton
            setBusy(true)
            drawn.clear()
            say("drawing ${m.draws} balls...")
            scope.launch {
                val balls = (1..m.pool).shuffled().take(m.draws)
                for (b in balls) {
                    delay(600)
                    drawn.add(b)
                    fx.play(if (b in picks) Sfx.COIN else Sfx.BALL)
                }
                delay(300)
                val hits = drawn.count { it in picks }
                result(prizes.getOrElse(hits) { 0 })
                setBusy(false)
            }
        }
    }
}

@Composable
private fun FlipGame(
    m: CustomMode, state: GameState, busy: Boolean, setBusy: (Boolean) -> Unit,
    pay: () -> Boolean, say: (String) -> Unit, result: (Int) -> Unit,
) {
    val fx = LocalFx.current
    val scope = rememberCoroutineScope()
    var inRound by remember { mutableStateOf(false) }
    var correct by remember { mutableIntStateOf(0) }
    var face by remember { mutableStateOf(true) } // true = heads
    val spin = remember { Animatable(0f) }

    fun call(heads: Boolean) {
        setBusy(true)
        scope.launch {
            val outcome = Random.nextBoolean()
            spin.snapTo(0f)
            fx.play(Sfx.WHOOSH)
            spin.animateTo(6f, tween(900))
            face = outcome
            if (outcome == heads) {
                correct++
                fx.play(Sfx.COIN); fx.buzz(Buzz.LIGHT)
                if (correct >= m.flips) {
                    inRound = false
                    result(flipPayout(m))
                } else say("right! ${m.flips - correct} more to go")
            } else {
                inRound = false
                result(0)
            }
            setBusy(false)
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$correct/${m.flips} correct · win ${fmt(flipPayout(m))}c", color = Gold, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(180.dp).graphicsLayer { scaleX = cos(spin.value * Math.PI.toFloat()).let { if (kotlin.math.abs(it) < 0.05f) 0.05f else kotlin.math.abs(it) } }) {
                drawCoin(center, size.minDimension * 0.45f)
                label(if (face) "H" else "T", center.x, center.y + size.minDimension * 0.25f, size.minDimension * 0.18f, Color(0xFFB07D00))
            }
        }
        if (inRound) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChunkyButton("heads", Modifier.weight(1f), color = Gold, enabled = !busy, sfx = null) { call(true) }
                ChunkyButton("tails", Modifier.weight(1f), color = Sky, enabled = !busy, sfx = null) { call(false) }
            }
        } else {
            ChunkyButton("play (${m.price}c)", Modifier.fillMaxWidth(), color = Color(m.color), enabled = !busy && state.coins >= m.price, sfx = null) {
                if (!pay()) return@ChunkyButton
                correct = 0
                inRound = true
                say("call it!")
            }
        }
    }
}
