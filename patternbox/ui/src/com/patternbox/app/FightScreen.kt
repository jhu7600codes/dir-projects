package com.patternbox.app

import androidx.compose.ui.input.key.key
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawBehind
import com.patternbox.core.Ability
import com.patternbox.core.Entry
import com.patternbox.core.Fighter
import com.patternbox.core.GameMap
import com.patternbox.core.Gravity
import com.patternbox.core.Match
import com.patternbox.core.Mode
import com.patternbox.core.Tool
import com.patternbox.core.NetHost
import com.patternbox.core.Peer
import com.patternbox.core.Snapshot
import com.patternbox.core.applyPeerInput

/** everything needed to start a fight. humans = indices into entries driven by the on-screen pads */
data class FightConfig(
    val mode: Mode,
    val entries: List<Entry>,
    val map: GameMap,
    val humans: List<Int> = emptyList(),
    val tournamentMatch: Pair<Int, Int>? = null,
    /** hosting a wifi game: every friend's phone and the fighter index they drive */
    val net: NetHost? = null,
    val peers: List<Pair<Peer, Int>> = emptyList(),
)

private val FIGHT_TOOLS = listOf(Tool.GRAB, Tool.BOOM, Tool.POKE, Tool.FIRE, Tool.FREEZE, Tool.ZAP, Tool.HEAL)

@Composable
fun FightScreen(
    state: GameState,
    config: FightConfig,
    padding: PaddingValues,
    onDone: (winner: Int?) -> Unit,
    onExit: () -> Unit,
) {
    var round by remember { mutableIntStateOf(0) }
    val match = remember(round, config) {
        Match(config.mode, config.entries, config.map).also {
            it.arena.showDamage = state.damageNumbers
            it.arena.spawnLoadout = {
                state.roster.randomOrNull()?.let { f -> f.loadout to f.displayName }
                    ?: com.patternbox.core.Loadout.random().let { l -> l to l.autoName }
            }
        }
    }
    val arena = match.arena
    var started by remember(match) { mutableStateOf(false) }
    var tool by remember { mutableStateOf<Tool?>(Tool.GRAB) }
    var slow by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var gravity by remember(match) { mutableStateOf(Gravity.EARTH) }
    var frame by remember { mutableLongStateOf(0L) }
    var botFlags by remember(match) { mutableStateOf(List(config.entries.size) { i -> i !in config.humans && config.peers.none { it.second == i } }) }
    val pending = remember(match) { mutableListOf<com.patternbox.core.Event>() }
    var netFrame by remember(match) { mutableIntStateOf(0) }
    var showResult by remember(match) { mutableStateOf(false) }
    var recorded by remember(match) { mutableStateOf(false) }
    val fx = LocalPlatform.current.feedback
    val speed by rememberUpdatedState(state.gameSpeed)
    val haptics by rememberUpdatedState(state.haptics)
    val soundOn by rememberUpdatedState(state.sound)
    val sandbox = config.mode == Mode.SANDBOX

    fun humanFighter(slot: Int): Fighter? {
        val idx = config.humans.getOrNull(slot) ?: return null
        return arena.fighters.getOrNull(idx)
    }

    LaunchedEffect(match) {
        state.justUnlocked = emptyList()
        var last = 0L
        var acc = 0f
        while (true) {
            withFrameNanos { now ->
                if (!started) {
                    // first frame: the canvas has measured the arena by now, so place everything for real
                    started = arena.width > 0
                    if (started) {
                        match.start()
                        gravity = arena.gravity
                        arena.fighters.forEachIndexed { i, f -> f.bot = botFlags.getOrElse(i) { true } }
                        config.net?.sendStart(config.mode, config.map, config.entries) { p -> config.peers.firstOrNull { it.first === p }?.second ?: -1 }
                    }
                }
                if (last != 0L && started && !paused) {
                    val dt = ((now - last) / 1e9f).coerceAtMost(.1f)
                    acc += dt * (if (slow) .3f else 1f) * speed
                    while (acc >= 1f / 60f) {
                        for ((p, i) in config.peers) arena.fighters.getOrNull(i)?.let { applyPeerInput(arena, it, p) }
                        match.step(); acc -= 1f / 60f
                    }
                }
                val net = config.net
                if (net != null && started) {
                    pending += arena.events
                    // ~30 snapshots a second is plenty on wifi
                    if (netFrame++ % 2 == 0) {
                        net.sendSnapshot(Snapshot.write(arena, pending.toList(), modeLine(config.mode, match, if (paused) "paused" else null)))
                        pending.clear()
                    }
                }
                last = now
                frame = now
                if (arena.events.isNotEmpty()) {
                    var bigHit = false
                    for (e in arena.events) {
                        if (soundOn) fx.play(e)
                        if (e is com.patternbox.core.Event.Boom || e == com.patternbox.core.Event.Ko) bigHit = true
                        if (e is com.patternbox.core.Event.Hit && e.dmg > 12) bigHit = true
                    }
                    if (haptics && arena.events.any { it !is com.patternbox.core.Event.Jump && it !is com.patternbox.core.Event.Shoot }) {
                        fx.buzz(bigHit)
                    }
                    arena.events.clear()
                }
                if (match.over && !showResult && arena.winT > 1.2f) showResult = true
            }
        }
    }

    // keyboard (pc, or a phone with a keyboard plugged in)
    androidx.compose.runtime.DisposableEffect(match) {
        KeyBus.handler = handler@{ ev ->
            val down = ev.pressed() ?: return@handler false
            val (slot, act) = keyAction(ev.key, config.humans.size) ?: return@handler false
            val idx = config.humans.getOrNull(slot) ?: return@handler false
            val f = arena.fighters.getOrNull(idx) ?: return@handler false
            if (f.bot) { f.bot = false; botFlags = botFlags.toMutableList().also { it[idx] = false } }
            when (act) {
                Act.LEFT -> f.input.left = down
                Act.RIGHT -> f.input.right = down
                Act.JUMP -> f.input.jump = down
                Act.HIT -> if (down) arena.swing(f)
                Act.ABILITY -> if (down) arena.useAbility(f)
            }
            true
        }
        onDispose { KeyBus.handler = null }
    }

    // save results once per match
    LaunchedEffect(showResult) {
        if (!showResult || recorded) return@LaunchedEffect
        recorded = true
        val ids = config.entries.map { it.rosterId }
        val kos = arena.fighters.mapIndexedNotNull { i, f -> ids.getOrNull(i)?.takeIf { it >= 0 }?.let { it to f.kos } }.toMap()
        val wonIdx = match.winners().toSet()
        state.notePlayed(config.mode, online = config.net != null, humanWonSpecial = config.humans.any { it in wonIdx })
        when (config.mode) {
            Mode.SURVIVAL -> {
                state.recordWave(match.wave - 1)
                state.record(emptySet(), emptySet(), kos)
            }
            else -> {
                val w = match.winners().toSet()
                val winners = ids.filterIndexed { i, id -> id >= 0 && i in w }.toSet()
                val losers = ids.filterIndexed { i, id -> id >= 0 && i !in w }.toSet()
                state.record(winners, losers, kos)
            }
        }
    }

    Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val extra = when (config.mode) {
                Mode.SURVIVAL -> "wave ${match.wave}   best ${state.bestWave}"
                else -> modeLine(config.mode, match, null)
            }
            ArenaView(arena, tool, { frame }, state.screenShake, if (paused) "paused" else extra, Modifier.weight(1f).fillMaxWidth())

            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (t in if (sandbox) Tool.values().toList() else FIGHT_TOOLS) {
                    FilterChip(selected = tool == t, onClick = { tool = if (tool == t) null else t }, label = { Text(t.label) })
                }
                VerticalDivider(Modifier.padding(vertical = 8.dp).size(1.dp, 24.dp))
                FilterChip(selected = paused, onClick = { paused = !paused }, label = { Text("pause") })
                FilterChip(selected = slow, onClick = { slow = !slow }, label = { Text("slowmo") })
                AssistChip(onClick = {
                    gravity = Gravity.values()[(gravity.ordinal + 1) % Gravity.values().size]; arena.gravity = gravity
                }, label = { Text("gravity: ${gravity.label}") })
                AssistChip(onClick = { arena.chaos() }, label = { Text("chaos") })
                AssistChip(onClick = { arena.healAll() }, label = { Text("heal all") })
                for ((slot, idx) in config.humans.withIndex()) {
                    FilterChip(selected = botFlags.getOrElse(idx) { true }, onClick = {
                        val f = arena.fighters.getOrNull(idx) ?: return@FilterChip
                        f.bot = !f.bot
                        botFlags = botFlags.toMutableList().also { it[idx] = f.bot }
                    }, label = { Text("p${slot + 1} autopilot") })
                }
                AssistChip(onClick = { round++ }, label = { Text("restart") })
            }

            if (config.humans.isNotEmpty() && !LocalPlatform.current.touchControls) {
                Text(keyHints(config.humans.size), Modifier.padding(bottom = 10.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (config.humans.isNotEmpty() && LocalPlatform.current.touchControls) {
                // one full width row per player, p2 on top so p1 sits under the thumbs
                Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (slot in config.humans.indices.reversed()) {
                        val idx = config.humans[slot]
                        ControlPad(
                            team = arena.fighters.getOrNull(idx)?.team ?: idx,
                            size = state.controlSize.dp,
                            label = if (config.humans.size > 1) "p${slot + 1}" else null,
                            fighter = { humanFighter(slot) },
                            frame = { frame },
                            onTouch = {
                                val f = humanFighter(slot) ?: return@ControlPad
                                if (f.bot) { f.bot = false; botFlags = botFlags.toMutableList().also { it[idx] = false } }
                            },
                            onSwing = { humanFighter(slot)?.let { arena.swing(it) } },
                            onAbility = { humanFighter(slot)?.let { arena.useAbility(it) } },
                        )
                    }
                }
            }
        }

        AnimatedVisibility(showResult, Modifier.align(Alignment.Center), enter = fadeIn() + slideInVertically { it / 3 }) {
            ResultCard(
                match, config, state,
                onRematch = { round++ },
                onContinue = { onDone(arena.winnerTeam) },
                onExit = onExit,
            )
        }
    }
}

/** the little status line under the arena, also sent to friends' phones */
fun modeLine(mode: Mode, match: Match, override: String?): String = override ?: when (mode) {
    Mode.SURVIVAL -> "wave ${match.wave}"
    Mode.SANDBOX -> "${match.arena.fighters.count { !it.dead }} alive, ${match.arena.props.size} props"
    Mode.KOTH -> "first to ${match.goal.toInt()} in the zone"
    Mode.BOSS -> "take down the boss"
    else -> match.map.label
}

@Composable
private fun ResultCard(match: Match, config: FightConfig, state: GameState, onRematch: () -> Unit, onContinue: () -> Unit, onExit: () -> Unit) {
    val arena = match.arena
    val cs = MaterialTheme.colorScheme
    ElevatedCard(Modifier.padding(24.dp).widthIn(max = 420.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val title = when {
                config.mode == Mode.SURVIVAL -> "survived ${match.wave - 1} wave${if (match.wave - 1 == 1) "" else "s"}"
                arena.winnerName.isNullOrEmpty() -> "double ko"
                else -> "${arena.winnerName} wins"
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            if (config.mode == Mode.SURVIVAL) Text("best: ${state.bestWave}", color = cs.onSurfaceVariant)
            for (a in state.justUnlocked) {
                Text("achievement: ${a.title}", color = cs.primary, fontWeight = FontWeight.Bold)
            }
            for (f in arena.fighters.filter { config.mode != Mode.SURVIVAL || it.team == 0 }.sortedByDescending { it.dmgDealt }.take(6)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(teamColor(cs, f.team)))
                    Text(f.name, Modifier.weight(1f), maxLines = 1)
                    Text("${f.kos} ko  ${f.dmgDealt.toInt()} dmg", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                if (config.tournamentMatch != null) {
                    Button(onClick = onContinue) { Text("back to bracket") }
                } else {
                    Button(onClick = onRematch) { Text("rematch") }
                    OutlinedButton(onClick = onExit) { Text("done") }
                }
            }
        }
    }
}

@Composable
internal fun ControlPad(
    team: Int,
    size: Dp,
    label: String?,
    fighter: () -> Fighter?,
    frame: () -> Long,
    onTouch: () -> Unit,
    onSwing: () -> Unit,
    onAbility: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var l by remember { mutableStateOf(false) }
    var r by remember { mutableStateOf(false) }
    var j by remember { mutableStateOf(false) }
    fun push() {
        onTouch()
        fighter()?.input?.let { it.left = l; it.right = r; it.jump = j }
    }
    Surface(shape = RoundedCornerShape(minOf(size / 2, 36.dp) + 8.dp), color = teamContainer(cs, team), modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.padding(8.dp)) {
            val f = fighter()
            frame()
            val ab = f?.loadout?.ability ?: Ability.NONE
            val hasAb = ab.cooldown > 0
            // widths in "units": 2 wide move buttons, jump, ability, a bigger hit button, plus a gap for the label
            val units = 1.25f * 2 + 1f + (if (hasAb) 1f else 0f) + 1.2f + .5f
            val gaps = 8.dp * (if (hasAb) 5 else 4)
            val unit = minOf(size, (maxWidth - gaps) / units)
            // narrow phones: buttons get taller instead of wider, so bigger settings still help
            val h = maxOf(size, unit)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HoldButton(team, unit * 1.25f, h, round = false, onHold = { l = it; push() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "left", Modifier.size(unit * .6f), tint = it)
                }
                HoldButton(team, unit * 1.25f, h, round = false, onHold = { r = it; push() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "right", Modifier.size(unit * .6f), tint = it)
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (label != null) Text(label, style = MaterialTheme.typography.labelLarge, color = onTeamContainer(cs, team))
                }
                HoldButton(team, unit, h, onHold = { j = it; push() }) {
                    Icon(Icons.Filled.KeyboardArrowUp, "jump", Modifier.size(unit * .55f), tint = it)
                }
                if (hasAb) {
                    val ready = 1f - ((f?.abilityCd ?: 0f) / ab.cooldown).coerceIn(0f, 1f)
                    HoldButton(team, unit, h, fill = ready, onHold = { if (it) { onTouch(); onAbility() } }) {
                        Text(ab.label.substringBefore(' ').take(6), color = it.copy(alpha = if (ready >= 1f) 1f else .5f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
                HoldButton(team, unit * 1.2f, h, strong = true, onHold = { if (it) { onTouch(); onSwing() } }) {
                    Text("hit", color = it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

/** a button that reports press and release, so holding left keeps walking */
@Composable
private fun HoldButton(
    team: Int,
    width: Dp,
    height: Dp,
    round: Boolean = true,
    strong: Boolean = false,
    fill: Float = 1f,
    onHold: (Boolean) -> Unit,
    content: @Composable (tint: Color) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var down by remember { mutableStateOf(false) }
    // pointerInput below lives across recompositions, so always call the newest onHold.
    // without this, rematch left the buttons driving the old match's fighters
    val hold by rememberUpdatedState(onHold)
    val c = teamColor(cs, team)
    val bg = when {
        down -> c
        strong -> c.copy(alpha = .4f)
        else -> c.copy(alpha = .14f)
    }
    Box(
        Modifier
            .size(width, height)
            .clip(if (round) RoundedCornerShape(50) else RoundedCornerShape(30))
            .background(bg)
            .drawBehind {
                // cooldown: fills up from the bottom
                if (fill < 1f && !down) drawRect(c.copy(alpha = .35f), topLeft = androidx.compose.ui.geometry.Offset(0f, this.size.height * (1 - fill)))
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val d = awaitFirstDown()
                    d.consume(); down = true; hold(true)
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == d.id } ?: break
                        if (!ch.pressed) break
                    }
                    down = false; hold(false)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        content(if (down) cs.surface else onTeamContainer(cs, team))
    }
}
