package com.patternbox.app

import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.patternbox.core.Arena
import com.patternbox.core.Entry
import com.patternbox.core.Event
import com.patternbox.core.GameMap
import com.patternbox.core.Mode
import com.patternbox.core.NetClient
import com.patternbox.core.NetHost
import com.patternbox.core.Snapshot
import kotlin.concurrent.thread

/** modes that make sense with friends */
val LAN_MODES = listOf(Mode.VERSUS, Mode.TEAMS, Mode.FFA, Mode.KOTH, Mode.BOSS, Mode.SANDBOX)

@Composable
fun LanHomeScreen(padding: PaddingValues, onHost: () -> Unit, onJoin: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.padding(top = padding.calculateTopPadding()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("everyone needs to be on the same wifi (or one phone's hotspot). one phone hosts, the rest join.", color = cs.onSurfaceVariant)
        ElevatedCard(onClick = onHost, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("host a game", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("your phone runs the fight, up to 5 friends join", color = cs.onSurfaceVariant)
            }
        }
        ElevatedCard(onClick = onJoin, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("join a game", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("find a friend's game on the wifi, or type their ip", color = cs.onSurfaceVariant)
            }
        }
    }
}

/** fighter picked for online play, shared by host and join screens */
@Composable
private fun MyFighterRow(state: GameState, id: Long?, onPick: (Long) -> Unit) {
    var choosing by remember { mutableStateOf(false) }
    val f = id?.let { state.byId(it) } ?: state.roster.firstOrNull()
    OutlinedCard(onClick = { choosing = true }, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (f != null) MiniFighter(f.loadout, Modifier.size(56.dp))
            Column(Modifier.weight(1f)) {
                Text("your fighter", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(f?.displayName ?: "make a fighter first", style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
            Text("change", color = MaterialTheme.colorScheme.primary)
        }
    }
    if (choosing) FighterPicker(state, onPick = { if (it != null) onPick(it); choosing = false }, onDismiss = { choosing = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HostLobbyScreen(state: GameState, host: NetHost, padding: PaddingValues, onFight: (FightConfig) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    var myId by remember { mutableStateOf(state.roster.firstOrNull()?.id) }
    var mode by remember { mutableStateOf(Mode.FFA) }
    var map by remember { mutableStateOf(GameMap.FLAT) }
    var bots by remember { mutableIntStateOf(0) }
    val ips = remember { localIps() }
    val me = myId?.let { state.byId(it) } ?: state.roster.firstOrNull()

    DisposableEffect(host) {
        val main = Handler(Looper.getMainLooper())
        host.onChange = { main.post { version++ } }
        val ad = Advertiser(ctx, me?.displayName ?: "game", host.localPort).also { it.start() }
        onDispose { host.onChange = {}; ad.stop() }
    }
    val peers = remember(version) { host.readyPeers }
    LaunchedEffect(version, mode, map, myId) {
        host.sendLobby(me?.displayName ?: "host", me?.loadout?.encode() ?: "", mode, map)
    }

    Column(
        Modifier.verticalScroll(rememberScrollState())
            .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("game is open", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("friends should see it in \"join a game\". if not, they can type:", color = cs.onSurfaceVariant)
                for (ip in ips) Text(ip, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleLarge)
                if (ips.isEmpty()) Text("no wifi address found, turn on wifi or a hotspot", color = cs.error)
            }
        }
        MyFighterRow(state, myId) { myId = it }

        Text("players (${peers.size + 1 + bots})", style = MaterialTheme.typography.titleSmall)
        ListItem(headlineContent = { Text(me?.displayName ?: "you") }, supportingContent = { Text("you, the host") },
            leadingContent = { me?.let { MiniFighter(it.loadout, Modifier.size(44.dp)) } })
        for (p in peers) {
            val l = p.loadout
            ListItem(headlineContent = { Text(p.name) }, supportingContent = { Text(l?.weaponName ?: "") },
                leadingContent = { l?.let { MiniFighter(it, Modifier.size(44.dp), team = 1) } })
        }
        if (peers.isEmpty()) Text("waiting for friends to join...", color = cs.onSurfaceVariant)

        Text("mode", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (m in LAN_MODES) FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.label) })
        }
        Text(mode.blurb, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        Text("map", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (m in GameMap.values()) FilterChip(selected = map == m, onClick = { map = m }, label = { Text(m.label) })
        }
        Text("extra bots", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (n in 0..4) FilterChip(selected = bots == n, onClick = { bots = n }, label = { Text("$n") })
        }

        val people = 1 + peers.size
        val need = when (mode) { Mode.VERSUS -> 2; Mode.TEAMS -> 4; else -> 2 }
        val cap = when (mode) { Mode.VERSUS -> 2; Mode.TEAMS -> 4; else -> 6 }
        val total = minOf(cap, people + bots).coerceAtLeast(need)
        Text(
            when {
                people > cap -> "${people - cap} friend(s) will watch, ${mode.label} only fits $cap"
                people + bots < need -> "${need - people - bots} bot(s) get added to fill it up"
                else -> "$total fighters"
            },
            style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
        )
        Button(enabled = me != null, onClick = {
            val mine = me ?: return@Button
            val entries = mutableListOf(mine.entry())
            val pairs = mutableListOf<Pair<com.patternbox.core.Peer, Int>>()
            for (p in peers) {
                if (entries.size >= total) break
                val l = p.loadout ?: continue
                pairs += p to entries.size
                entries += Entry(p.name, l)
            }
            while (entries.size < total) {
                val b = state.roster.random()
                entries += Entry(b.displayName + " (bot)", b.loadout)
            }
            onFight(FightConfig(mode, entries, map, humans = listOf(0), net = host, peers = pairs))
        }, modifier = Modifier.fillMaxWidth()) { Text("start", style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
fun JoinScreen(state: GameState, client: NetClient, padding: PaddingValues, onStarted: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    var myId by remember { mutableStateOf(state.roster.firstOrNull()?.id) }
    var ip by remember { mutableStateOf("") }
    var connecting by remember { mutableStateOf(false) }
    val discovery = remember { Discovery(ctx) }
    val me = myId?.let { state.byId(it) } ?: state.roster.firstOrNull()

    DisposableEffect(client) {
        val main = Handler(Looper.getMainLooper())
        client.onChange = { main.post { version++ } }
        discovery.start()
        onDispose { client.onChange = {}; discovery.stop() }
    }
    // jump into the fight whenever the host starts one
    var seenStart by remember { mutableIntStateOf(client.startCount) }
    LaunchedEffect(version) {
        if (client.startCount != seenStart && client.start != null) { seenStart = client.startCount; onStarted() }
    }

    fun connect(host: String, port: Int) {
        val mine = me ?: return
        connecting = true
        thread {
            client.connect(host, mine.displayName, mine.loadout.encode(), port)
            Handler(Looper.getMainLooper()).post { connecting = false; version++ }
        }
    }

    @Suppress("UNUSED_EXPRESSION") version
    Column(
        Modifier.verticalScroll(rememberScrollState())
            .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (client.connected) {
            val lobby = client.lobby
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("you're in", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("waiting for the host to start", color = cs.onSurfaceVariant)
                    if (lobby != null) {
                        Text("${lobby.mode.label} on ${lobby.map.label}", color = cs.primary)
                        for ((i, pl) in lobby.players.withIndex()) {
                            Text((if (i == lobby.you) "you: " else if (i == 0) "host: " else "") + pl.first)
                        }
                    }
                    if (client.start != null) Button(onClick = onStarted) { Text("back to the fight") }
                }
            }
            return@Column
        }
        MyFighterRow(state, myId) { myId = it }
        client.error?.let { Text(it, color = cs.error) }
        Text("games on this wifi", style = MaterialTheme.typography.titleSmall)
        if (discovery.games.isEmpty()) Text("looking... (make sure the host pressed \"host a game\")", color = cs.onSurfaceVariant)
        for (g in discovery.games) {
            OutlinedCard(onClick = { connect(g.host, g.port) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(g.name, style = MaterialTheme.typography.titleMedium)
                        Text(g.host, fontFamily = FontFamily.Monospace, color = cs.onSurfaceVariant)
                    }
                    Text("join", color = cs.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text("or type the host's ip", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                ip, { ip = it.trim() }, singleLine = true, placeholder = { Text("192.168.1.23") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.weight(1f),
            )
            Button(enabled = ip.isNotBlank() && !connecting && me != null, onClick = { connect(ip, com.patternbox.core.Proto.PORT) }) {
                Text(if (connecting) "..." else "join")
            }
        }
    }
}

/** a friend's phone: draws the host's arena and sends buttons back */
@Composable
fun ClientFightScreen(state: GameState, sfx: Sfx?, client: NetClient, padding: PaddingValues, onLeave: () -> Unit) {
    val start = client.start ?: return
    val startCount = client.startCount
    val arena = remember(startCount) { Arena().also { it.remote = true } }
    var frame by remember { mutableLongStateOf(0L) }
    var extra by remember { mutableStateOf("") }
    var lost by remember { mutableStateOf(false) }
    val view = LocalView.current
    val soundOn by rememberUpdatedState(state.sound)
    val haptics by rememberUpdatedState(state.haptics)
    val mine = start.yourFighter

    LaunchedEffect(startCount) {
        var lastSent = 0L
        var lastBits = -1
        while (true) {
            withFrameNanos { now ->
                client.latestSnap.getAndSet(null)?.let { bytes ->
                    val (events, line) = runCatching { Snapshot.apply(arena, bytes) }.getOrDefault(emptyList<Event>() to extra)
                    extra = line
                    var big = false
                    for (e in events) {
                        if (soundOn) sfx?.play(e)
                        if (e is Event.Boom || e == Event.Ko) big = true
                    }
                    if (haptics && events.any { it !is Event.Jump && it !is Event.Shoot }) {
                        view.performHapticFeedback(if (big) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.CONTEXT_CLICK)
                    }
                }
                val f = arena.fighters.getOrNull(mine)
                if (f != null) {
                    val i = f.input
                    val bits = (if (i.left) 1 else 0) or (if (i.right) 2 else 0) or (if (i.jump) 4 else 0)
                    // send on change, and a keepalive now and then
                    if (bits != lastBits || now - lastSent > 200_000_000L) {
                        client.sendInput(i.left, i.right, i.jump)
                        lastBits = bits; lastSent = now
                    }
                }
                if (!client.connected) lost = true
                frame = now
            }
        }
    }

    Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ArenaView(arena, null, { frame }, state.screenShake, extra.ifEmpty { "connected" }, Modifier.weight(1f).fillMaxWidth())
            if (mine >= 0) {
                Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    ControlPad(
                        team = arena.fighters.getOrNull(mine)?.team ?: 0,
                        size = state.controlSize.dp,
                        label = null,
                        fighter = { arena.fighters.getOrNull(mine) },
                        frame = { frame },
                        onTouch = {},
                        onSwing = { client.swing(); arena.fighters.getOrNull(mine)?.input?.let { client.sendInput(it.left, it.right, it.jump) } },
                        onAbility = { client.ability(); arena.fighters.getOrNull(mine)?.input?.let { client.sendInput(it.left, it.right, it.jump) } },
                    )
                }
            } else {
                Text("you're watching this one", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val w = arena.winnerName
        if (lost || (w != null && arena.winT > 1.2f)) {
            ElevatedCard(Modifier.align(Alignment.Center).padding(24.dp).widthIn(max = 420.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (lost) {
                        Text("disconnected", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text(client.error ?: "the host left", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(if (w.isNullOrEmpty()) "double ko" else "$w wins", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text("waiting for the host to start the next one", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = onLeave) { Text("leave") }
                }
            }
        }
    }
}
