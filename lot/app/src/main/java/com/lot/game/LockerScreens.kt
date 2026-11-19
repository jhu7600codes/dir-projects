package com.lot.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

// ================================ sharing ================================

@Composable
private fun NicknameDialog(onDone: (String?) -> Unit) {
    var nick by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onDone(null) },
        containerColor = Card,
        title = { Text("pick a nickname") },
        text = {
            Column {
                Text("it's shown next to everything you upload. don't use your real name.", color = Dim, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    nick, { nick = it.take(24) }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Gold, cursorColor = Gold),
                )
            }
        },
        confirmButton = { TextButton({ onDone(nick.trim().ifBlank { null }) }, enabled = nick.isNotBlank()) { Text("save") } },
        dismissButton = { TextButton({ onDone(null) }) { Text("cancel") } },
    )
}

/** uploads a creation to the lottery locker */
@Composable
fun ShareButton(vm: GameViewModel, state: GameState, kind: String, name: String, data: JSONObject, wide: Boolean = false) {
    val fx = LocalFx.current
    val scope = rememberCoroutineScope()
    var askNick by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    fun upload(nick: String) {
        busy = true
        scope.launch {
            result = try {
                Locker.publish(kind, name, nick, data, vm.ensureSecret())
                vm.update { it.copy(lockerUploads = it.lockerUploads + 1) }
                fx.play(Sfx.WIN)
                "\"$name\" is in the lottery locker now. everyone can see it."
            } catch (e: LockerException) {
                fx.play(Sfx.LOSE)
                "upload failed: ${e.message}"
            }
            busy = false
        }
    }

    ChunkyButton(
        if (busy) "..." else if (wide) "share to the lottery locker" else "share",
        if (wide) Modifier.fillMaxWidth() else Modifier.width(72.dp), color = Sky, height = if (wide) 50.dp else 40.dp, enabled = !busy,
    ) { if (state.nickname.isBlank()) askNick = true else upload(state.nickname) }

    if (askNick) NicknameDialog { n ->
        askNick = false
        if (n != null) { vm.setNickname(n); upload(n) }
    }
    result?.let { r ->
        AlertDialog(
            onDismissRequest = { result = null }, containerColor = Card,
            text = { Text(r, color = Color.White) },
            confirmButton = { TextButton({ result = null }) { Text("ok") } },
        )
    }
}

// ================================ lottery locker browser ================================

private fun DrawScope.drawLockerPreview(item: LockerItem, t: Float) {
    when (item.kind) {
        "mode" -> {
            val m = modeFromJson(item.data)
            drawRoundRect(Color(m.color).copy(alpha = 0.3f), cornerRadius = CornerRadius(20f))
            drawHostChar(m.host, t)
        }
        "ticket" -> {
            val tk = ticketFromJson(item.data) ?: return
            val tl = Offset(size.width * 0.06f, size.height * 0.18f)
            val sz = Size(size.width * 0.88f, size.height * 0.64f)
            drawRoundRect(Color(tk.paper), tl, sz, CornerRadius(10f))
            drawRect(Color(tk.accent), tl + Offset(0f, sz.height * 0.1f), Size(sz.width, sz.height * 0.2f))
            drawRoundRect(Ink, tl, sz, CornerRadius(10f), style = Stroke(4f))
            drawSymbol(tk.syms.last(), Offset(size.width * 0.5f, size.height * 0.6f), size.width * 0.35f)
        }
        else -> {
            val hat = cosmetic(item.data.optString("hat"))?.value?.toInt() ?: 1
            val shirt = cosmetic(item.data.optString("shirt"))?.value ?: 0xFF4BE3AC
            val d = Dude(Color(0xFFFFD7B5), Color(shirt), Color(0xFF6B3E26), hat, if (hat == 7) Gold else Pink, "", item.data.optBoolean("glasses"), 1f)
            drawRoundRect(CardHi, cornerRadius = CornerRadius(20f))
            drawDude(d, size.width / 2, size.height * 1.02f, size.height * 0.85f, Mood.HAPPY, t = t)
        }
    }
}

private fun describe(item: LockerItem): String = when (item.kind) {
    "mode" -> modeFromJson(item.data).let { m -> "\"${m.catchphrase}\"\n" + modeSummary(m) + "\nentry ${m.price}c" }
    "ticket" -> ticketFromJson(item.data)?.let { t -> "${t.price}c scratch ticket · ${(t.chance * 100).toInt()}% win chance · top prize ${fmt(t.prizes().last())}" } ?: "broken ticket"
    else -> listOfNotNull(
        cosmetic(item.data.optString("hat"))?.name, cosmetic(item.data.optString("shirt"))?.name,
        if (item.data.optBoolean("glasses")) "nerd glasses" else null,
    ).joinToString(" + ")
}

@Composable
fun LockerScreen(vm: GameViewModel, state: GameState, nav: Nav) {
    val fx = LocalFx.current
    val scope = rememberCoroutineScope()
    var kind by remember { mutableStateOf("all") }
    var sort by remember { mutableStateOf("new") }
    var search by remember { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val items = remember { mutableStateListOf<LockerItem>() }
    val mine = remember { mutableStateListOf<String>() }
    val liked = remember { mutableStateListOf<String>() }
    var toast by remember { mutableStateOf<String?>(null) }
    var reportTarget by remember { mutableStateOf<LockerItem?>(null) }
    var clock by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) withFrameNanos { now -> if (last != 0L) clock += (now - last) / 1e9f; last = now }
    }
    LaunchedEffect(toast) { if (toast != null) { delay(2200); toast = null } }

    LaunchedEffect(kind, sort, reload) {
        loading = true
        error = null
        try {
            if (state.lockerSecret.isNotBlank()) { mine.clear(); mine.addAll(Locker.mine(state.lockerSecret)) }
            val got = if (kind == "mine") {
                Locker.browse(null, "new").filter { it.id in mine }
            } else {
                Locker.browse(if (kind == "all") null else kind.removeSuffix("s"), sort, search)
            }
            items.clear(); items.addAll(got)
        } catch (e: LockerException) {
            error = e.message
        }
        loading = false
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            LotTopBar("lottery locker", state.coins, { nav.back() })
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("modes, tickets and outfits made by other players", color = Dim, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("all", "modes", "tickets", "outfits", "mine").forEach { k ->
                        Text(
                            k, color = if (k == kind) Ink else Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).background(if (k == kind) Gold else CardHi, RoundedCornerShape(50)).clickable { kind = k }.padding(vertical = 7.dp),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        search, { search = it.take(30) }, Modifier.weight(1f), singleLine = true,
                        placeholder = { Text("search names", color = Dim) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Gold, unfocusedBorderColor = Dim, cursorColor = Gold),
                    )
                    ChunkyButton("go", Modifier.width(56.dp), color = Gold, height = 50.dp) { reload++ }
                    ChunkyButton(if (sort == "new") "new" else "top", Modifier.width(64.dp), color = CardHi, textColor = Color.White, height = 50.dp) {
                        sort = if (sort == "new") "popular" else "new"
                    }
                }
            }
            when {
                loading -> Text("opening the locker...", color = Dim, modifier = Modifier.fillMaxWidth().padding(30.dp), textAlign = TextAlign.Center)
                error != null -> Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error ?: "", color = Pink, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    ChunkyButton("try again", Modifier.fillMaxWidth()) { reload++ }
                }
                items.isEmpty() -> Text(
                    if (kind == "mine") "you haven't shared anything yet. share from the creator studio or your profile." else "nothing here yet. be the first!",
                    color = Dim, modifier = Modifier.fillMaxWidth().padding(30.dp), textAlign = TextAlign.Center,
                )
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(items, key = { it.id }) { item ->
                        Panel(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Canvas(Modifier.size(70.dp)) { drawLockerPreview(item, clock) }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(item.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                                        Text("${item.kind} by ${item.author}", color = modeColor("locker"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("♥ ${item.likes} · ${item.downloads} got it", color = Dim, fontSize = 12.sp)
                                    }
                                }
                                Text(describe(item), color = Dim, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (item.kind == "mode") {
                                        ChunkyButton("play", Modifier.weight(1f), color = Mint, height = 42.dp) {
                                            vm.currentMode = modeFromJson(item.data).copy(author = item.author)
                                            nav.go(Screen.CustomPlay)
                                        }
                                    }
                                    val have = item.id in state.lockerSaved
                                    ChunkyButton(
                                        when { item.kind == "outfit" -> "wear"; have -> "got it"; else -> "get" },
                                        Modifier.weight(1f), color = Gold, height = 42.dp, enabled = item.kind == "outfit" || !have,
                                    ) {
                                        val m = vm.saveFromLocker(item)
                                        toast = m
                                        if (!have) scope.launch { Locker.bump(item.id, "download") }
                                    }
                                    if (item.id in mine) {
                                        ChunkyButton("delete", Modifier.weight(1f), color = Pink, height = 42.dp) {
                                            scope.launch {
                                                toast = try {
                                                    if (Locker.delete(item.id, state.lockerSecret)) { items.remove(item); "deleted" } else "couldn't delete it"
                                                } catch (e: LockerException) { e.message }
                                            }
                                        }
                                    } else {
                                        ChunkyButton(if (item.id in liked) "♥" else "♡", Modifier.width(52.dp), color = Pink, height = 42.dp, enabled = item.id !in liked) {
                                            liked.add(item.id)
                                            fx.play(Sfx.COIN)
                                            scope.launch { Locker.bump(item.id, "like") }
                                        }
                                        ChunkyButton("!", Modifier.width(44.dp), color = CardHi, textColor = Color.White, height = 42.dp) { reportTarget = item }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        toast?.let {
            Text(
                it, color = Ink, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp).background(Gold, RoundedCornerShape(16.dp)).padding(12.dp),
            )
        }
        reportTarget?.let { item ->
            AlertDialog(
                onDismissRequest = { reportTarget = null }, containerColor = Card,
                title = { Text("report \"${item.name}\"?") },
                text = { Text("for mean, gross or creepy stuff. things with enough reports get hidden.", color = Dim) },
                confirmButton = {
                    TextButton({
                        scope.launch { Locker.bump(item.id, "report") }
                        items.remove(item)
                        reportTarget = null
                        toast = "reported. thanks."
                    }) { Text("report") }
                },
                dismissButton = { TextButton({ reportTarget = null }) { Text("cancel") } },
            )
        }
    }
}

// ================================ new mode templates ================================

private fun DrawScope.drawDie(c: Offset, s: Float, v: Int) {
    drawRoundRect(Ink, c - Offset(s / 2 - 3f, s / 2 - 5f), Size(s, s), CornerRadius(s * 0.2f))
    drawRoundRect(Color.White, c - Offset(s / 2, s / 2), Size(s, s), CornerRadius(s * 0.2f))
    drawRoundRect(Ink, c - Offset(s / 2, s / 2), Size(s, s), CornerRadius(s * 0.2f), style = Stroke(s * 0.05f))
    val o = s * 0.25f
    val pips = when (v) {
        1 -> listOf(0 to 0)
        2 -> listOf(-1 to -1, 1 to 1)
        3 -> listOf(-1 to -1, 0 to 0, 1 to 1)
        4 -> listOf(-1 to -1, 1 to -1, -1 to 1, 1 to 1)
        5 -> listOf(-1 to -1, 1 to -1, 0 to 0, -1 to 1, 1 to 1)
        else -> listOf(-1 to -1, 1 to -1, -1 to 0, 1 to 0, -1 to 1, 1 to 1)
    }
    pips.forEach { (x, y) -> drawCircle(Ink, s * 0.09f, c + Offset(x * o, y * o)) }
}

@Composable
fun DiceGame(m: CustomMode, state: GameState, busy: Boolean, setBusy: (Boolean) -> Unit, pay: () -> Boolean, say: (String) -> Unit, result: (Int) -> Unit) {
    val fx = LocalFx.current
    val scope = rememberCoroutineScope()
    var pick by remember { mutableIntStateOf(m.dice * 7 / 2) }
    val faces = remember { mutableStateListOf<Int>().apply { repeat(m.dice) { add(Random.nextInt(1, 7)) } } }
    var wobble by remember { mutableFloatStateOf(0f) }
    Column(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val s = size.minDimension * 0.28f
            faces.forEachIndexed { i, v ->
                val cx = size.width * (i + 1) / (faces.size + 1)
                drawDie(Offset(cx + sin(wobble * 30f + i) * 10f * wobble, size.height / 2), s, v)
            }
        }
        Text("call the total", color = Dim, fontWeight = FontWeight.Bold)
        Column(Modifier.verticalScroll(rememberScrollState()).height(130.dp)) {
            (m.dice..m.dice * 6).toList().chunked(6).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { tot ->
                        Column(
                            Modifier.weight(1f).background(if (tot == pick) Gold else CardHi, RoundedCornerShape(10.dp)).clickable(enabled = !busy) { pick = tot }.padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("$tot", color = if (tot == pick) Ink else Color.White, fontWeight = FontWeight.Black)
                            Text("${fmt(dicePayout(m, tot))}c", color = if (tot == pick) Ink else Dim, fontSize = 9.sp)
                        }
                    }
                    repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        ChunkyButton("roll (${m.price}c)", Modifier.fillMaxWidth(), color = Color(m.color), enabled = !busy && state.coins >= m.price, sfx = null) {
            if (!pay()) return@ChunkyButton
            setBusy(true)
            say("rolling for a $pick...")
            scope.launch {
                repeat(12) {
                    for (i in faces.indices) faces[i] = Random.nextInt(1, 7)
                    wobble = 1f - it / 12f
                    fx.play(Sfx.CLICK)
                    delay(70)
                }
                wobble = 0f
                val total = faces.sum()
                result(if (total == pick) dicePayout(m, pick) else 0)
                setBusy(false)
            }
        }
    }
}

@Composable
fun CupsGame(m: CustomMode, state: GameState, busy: Boolean, setBusy: (Boolean) -> Unit, pay: () -> Boolean, say: (String) -> Unit, result: (Int) -> Unit) {
    val fx = LocalFx.current
    val scope = rememberCoroutineScope()
    val n = m.cups
    val slots = remember { IntArray(n) { it } } // cup -> slot
    var ballCup by remember { mutableIntStateOf(0) }
    var lifted by remember { mutableStateOf(setOf<Int>()) }
    var swapA by remember { mutableIntStateOf(-1) }
    var swapB by remember { mutableIntStateOf(-1) }
    val prog = remember { Animatable(0f) }
    var picking by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Canvas(
            Modifier.fillMaxWidth().weight(1f).clickable(enabled = picking) { }
        ) {
            @Suppress("UNUSED_EXPRESSION") tick
            val slotW = size.width / n
            val cy = size.height * 0.62f
            val cw = slotW * 0.7f
            for (cup in 0 until n) {
                var x = slots[cup] * slotW + slotW / 2
                var yOff = 0f
                if (cup == swapA || cup == swapB) {
                    val other = if (cup == swapA) swapB else swapA
                    val to = slots[other] * slotW + slotW / 2
                    x += (to - x) * prog.value
                    yOff = sin(prog.value * Math.PI.toFloat()) * size.height * 0.12f * if (cup == swapA) 1f else -1f
                }
                val up = if (cup in lifted) size.height * 0.25f else 0f
                if (cup == ballCup) circleO(Offset(x, cy + cw * 0.35f), cw * 0.16f, Pink, 4f)
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(x - cw * 0.5f, cy + cw * 0.5f - up + yOff); lineTo(x - cw * 0.35f, cy - cw * 0.5f - up + yOff)
                    lineTo(x + cw * 0.35f, cy - cw * 0.5f - up + yOff); lineTo(x + cw * 0.5f, cy + cw * 0.5f - up + yOff); close()
                }
                outlined(p, Color(m.color), 5f)
                drawRect(Color.White.copy(alpha = 0.3f), Offset(x - cw * 0.3f, cy - cw * 0.3f - up + yOff), Size(cw * 0.12f, cw * 0.6f))
            }
        }
        if (picking) {
            Text("which cup?", color = Gold, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (slot in 0 until n) {
                    ChunkyButton("${slot + 1}", Modifier.weight(1f), color = Color(m.color), height = 50.dp) {
                        picking = false
                        val cup = slots.indexOf(slot)
                        lifted = setOf(cup, ballCup)
                        tick++
                        result(if (cup == ballCup) pickOnePayout(m, n) else 0)
                        setBusy(false)
                    }
                }
            }
        } else {
            ChunkyButton("play (${m.price}c)", Modifier.fillMaxWidth(), color = Color(m.color), enabled = !busy && state.coins >= m.price, sfx = null) {
                if (!pay()) return@ChunkyButton
                setBusy(true)
                scope.launch {
                    ballCup = Random.nextInt(n)
                    lifted = setOf(ballCup); tick++
                    say("keep your eyes on the ball...")
                    delay(1100)
                    lifted = emptySet(); tick++
                    delay(400)
                    val dur = (520 - m.swaps * 18).coerceAtLeast(180)
                    repeat(m.swaps) {
                        var a = Random.nextInt(n)
                        var b = Random.nextInt(n - 1); if (b >= a) b++
                        if (Random.nextBoolean()) { val t = a; a = b; b = t }
                        swapA = a; swapB = b
                        prog.snapTo(0f)
                        fx.play(Sfx.WHOOSH)
                        prog.animateTo(1f, tween(dur, easing = FastOutSlowInEasing))
                        val t = slots[a]; slots[a] = slots[b]; slots[b] = t
                        swapA = -1; swapB = -1
                        prog.snapTo(0f)
                        tick++
                    }
                    say("so. where is it?")
                    picking = true
                }
            }
        }
    }
}

@Composable
fun RaceGame(m: CustomMode, state: GameState, busy: Boolean, setBusy: (Boolean) -> Unit, pay: () -> Boolean, say: (String) -> Unit, result: (Int) -> Unit) {
    val fx = LocalFx.current
    val scope = rememberCoroutineScope()
    val n = m.runners
    var pick by remember { mutableIntStateOf(0) }
    var t by remember { mutableFloatStateOf(0f) }
    val finish = remember { FloatArray(n) { 5f } }
    val waves = remember { IntArray(n) { 1 } }
    Column(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            drawRoundRect(Color(0xFF3B7A3A), cornerRadius = CornerRadius(24f))
            val lane = size.height / n
            val endX = size.width * 0.86f
            for (i in 0 until 12) drawRect(if (i % 2 == 0) Color.White else Ink, Offset(endX, i * size.height / 12), Size(size.width * 0.025f, size.height / 12))
            for (i in 0 until n) {
                val y = lane * i + lane / 2
                val p = (t / finish[i]).coerceIn(0f, 1f)
                val pp = (p + 0.04f * sin(2 * Math.PI.toFloat() * waves[i] * p)).coerceIn(0f, 1f)
                val x = size.width * 0.1f + (endX - size.width * 0.1f + 20f) * pp
                when (m.runnerType) {
                    "raccoon" -> drawRaccoon(Offset(x, y + lane * 0.1f), lane * 0.55f, t, false)
                    "granny" -> drawDude(grannyDude(i), x, y + lane * 0.48f, lane * 0.95f, Mood.GRUMPY, wobble = if (t > 0f && p < 1f) sin(t * 14f + i) * 0.5f else 0f, t = t)
                    else -> drawPigeon(Offset(x, y), lane * 0.5f, abs(sin(t * 16f + i)), 1f, ballColors[i % ballColors.size])
                }
                label("${i + 1}", size.width * 0.04f, y, lane * 0.3f, Color.White, Ink)
                if (i == pick) drawCircle(Gold, lane * 0.4f, Offset(x, y), style = Stroke(4f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
            for (i in 0 until n) {
                ChunkyButton("${i + 1}", Modifier.weight(1f), color = if (i == pick) Gold else CardHi, textColor = if (i == pick) Ink else Color.White, height = 46.dp, enabled = !busy) { pick = i }
            }
        }
        ChunkyButton("race (${m.price}c, win ${fmt(pickOnePayout(m, n))})", Modifier.fillMaxWidth(), color = Color(m.color), enabled = !busy && state.coins >= m.price, sfx = null) {
            if (!pay()) return@ChunkyButton
            setBusy(true)
            val winner = Random.nextInt(n)
            for (i in 0 until n) { finish[i] = if (i == winner) 5f else 5.3f + Random.nextFloat() * 2f; waves[i] = 1 + Random.nextInt(3) }
            say("and they're off!")
            fx.play(Sfx.DRUM)
            scope.launch {
                t = 0f
                var last = 0L
                while (t < 5f) withFrameNanos { now -> if (last != 0L) t += (now - last) / 1e9f; last = now }
                t = 5f
                result(if (winner == pick) pickOnePayout(m, n) else 0)
                setBusy(false)
            }
        }
    }
}

@Composable
fun BoxesGame(m: CustomMode, state: GameState, busy: Boolean, setBusy: (Boolean) -> Unit, pay: () -> Boolean, say: (String) -> Unit, result: (Int) -> Unit) {
    val fx = LocalFx.current
    var round by remember { mutableIntStateOf(0) }
    val bombs = remember(round) { (0 until m.boxes).shuffled().take(m.bombs).toSet() }
    val opened = remember(round) { mutableStateListOf<Int>() }
    var pot by remember(round) { mutableFloatStateOf(0f) }
    var playing by remember(round) { mutableStateOf(false) }
    var boom by remember(round) { mutableStateOf(false) }

    fun end(win: Int) {
        playing = false
        setBusy(false)
        result(win)
    }

    Column(Modifier.fillMaxSize()) {
        Text(if (playing) "pot: ${fmt(pot.toInt())}c" else " ", color = Gold, fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            (0 until m.boxes).chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { i ->
                        val open = i in opened || (!playing && (boom || opened.isNotEmpty()) && i in bombs)
                        Box(
                            Modifier.weight(1f).aspectRatio(1f)
                                .background(if (open) (if (i in bombs) Color(0xFF4A2030) else Color(0xFF2E5A3A)) else Color(m.color), RoundedCornerShape(12.dp))
                                .border(2.5.dp, Ink, RoundedCornerShape(12.dp))
                                .clickable(enabled = playing && i !in opened) {
                                    if (i in bombs) {
                                        boom = true
                                        fx.play(Sfx.CAUGHT); fx.buzz(Buzz.HEAVY)
                                        say("BOOM. the pot is gone.")
                                        end(0)
                                    } else {
                                        val closed = m.boxes - opened.size
                                        pot = (pot * boxStep(closed, m.bombs)).toFloat()
                                        opened.add(i)
                                        fx.play(Sfx.COIN); fx.buzz(Buzz.LIGHT)
                                        say("safe! pot is ${fmt(pot.toInt())}c")
                                        if (m.boxes - opened.size == m.bombs) end(pot.toInt())
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (open) {
                                Canvas(Modifier.fillMaxSize(0.6f)) {
                                    if (i in bombs) {
                                        circleO(center, size.minDimension * 0.4f, Ink, 3f)
                                        drawLine(Gold, center - Offset(0f, size.minDimension * 0.4f), center - Offset(-size.minDimension * 0.15f, size.minDimension * 0.6f), 5f)
                                    } else drawCoin(center, size.minDimension * 0.35f)
                                }
                            } else Text("?", color = Ink, fontWeight = FontWeight.Black, fontSize = 24.sp)
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        if (playing) {
            ChunkyButton("cash out ${fmt(pot.toInt())}c", Modifier.fillMaxWidth(), color = Mint, enabled = opened.isNotEmpty(), sfx = null) { end(pot.toInt()) }
        } else {
            ChunkyButton("play (${m.price}c)", Modifier.fillMaxWidth(), color = Color(m.color), enabled = !busy && state.coins >= m.price, sfx = null) {
                if (!pay()) return@ChunkyButton
                round++
                // the new round's state is created on the next composition, so set it up after
                setBusy(true)
            }
        }
    }
    LaunchedEffect(round) {
        if (round > 0) {
            pot = m.price.toFloat()
            playing = true
            say("pick a box. ${m.bombs} of them go boom.")
        }
    }
}
