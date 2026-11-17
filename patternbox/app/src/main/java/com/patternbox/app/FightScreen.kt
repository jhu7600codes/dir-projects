package com.patternbox.app

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patternbox.core.Ability
import com.patternbox.core.Entry
import com.patternbox.core.Fighter
import com.patternbox.core.GameMap
import com.patternbox.core.Gravity
import com.patternbox.core.Match
import com.patternbox.core.Mode
import com.patternbox.core.Tool

/** everything needed to start a fight. humans = indices into entries driven by the on-screen pads */
data class FightConfig(
    val mode: Mode,
    val entries: List<Entry>,
    val map: GameMap,
    val humans: List<Int> = emptyList(),
    val tournamentMatch: Pair<Int, Int>? = null,
)

private val FIGHT_TOOLS = listOf(Tool.GRAB, Tool.BOOM, Tool.POKE, Tool.FIRE, Tool.FREEZE, Tool.ZAP, Tool.HEAL)

@Composable
fun FightScreen(
    state: GameState,
    sfx: Sfx?,
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
    var botFlags by remember(match) { mutableStateOf(List(config.entries.size) { it !in config.humans }) }
    var showResult by remember(match) { mutableStateOf(false) }
    var recorded by remember(match) { mutableStateOf(false) }
    val view = LocalView.current
    val speed by rememberUpdatedState(state.gameSpeed)
    val haptics by rememberUpdatedState(state.haptics)
    val soundOn by rememberUpdatedState(state.sound)
    val sandbox = config.mode == Mode.SANDBOX

    fun humanFighter(slot: Int): Fighter? {
        val idx = config.humans.getOrNull(slot) ?: return null
        return arena.fighters.getOrNull(idx)
    }

    LaunchedEffect(match) {
        var last = 0L
        var acc = 0f
        while (true) {
            withFrameNanos { now ->
                if (!started) {
                    // first frame: the canvas has measured the arena by now, so place everything for real
                    started = arena.width > 0
                    if (started) {
                        match.start()
                        arena.fighters.forEachIndexed { i, f -> f.bot = botFlags.getOrElse(i) { true } }
                    }
                }
                if (last != 0L && started && !paused) {
                    val dt = ((now - last) / 1e9f).coerceAtMost(.1f)
                    acc += dt * (if (slow) .3f else 1f) * speed
                    while (acc >= 1f / 60f) { match.step(); acc -= 1f / 60f }
                }
                last = now
                frame = now
                if (arena.events.isNotEmpty()) {
                    var bigHit = false
                    for (e in arena.events) {
                        if (soundOn) sfx?.play(e)
                        if (e is com.patternbox.core.Event.Boom || e == com.patternbox.core.Event.Ko) bigHit = true
                        if (e is com.patternbox.core.Event.Hit && e.dmg > 12) bigHit = true
                    }
                    if (haptics && arena.events.any { it !is com.patternbox.core.Event.Jump && it !is com.patternbox.core.Event.Shoot }) {
                        view.performHapticFeedback(if (bigHit) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.CONTEXT_CLICK)
                    }
                    arena.events.clear()
                }
                if (match.over && !showResult && arena.winT > 1.2f) showResult = true
            }
        }
    }

    // save results once per match
    LaunchedEffect(showResult) {
        if (!showResult || recorded) return@LaunchedEffect
        recorded = true
        val ids = config.entries.map { it.rosterId }
        val kos = arena.fighters.mapIndexedNotNull { i, f -> ids.getOrNull(i)?.takeIf { it >= 0 }?.let { it to f.kos } }.toMap()
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
                Mode.SANDBOX -> "${arena.fighters.count { !it.dead }} alive, ${arena.props.size} props"
                else -> config.map.label
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

            if (config.humans.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = if (config.humans.size == 1) Arrangement.Center else Arrangement.SpaceBetween,
                ) {
                    for (slot in config.humans.indices) {
                        val idx = config.humans[slot]
                        ControlPad(
                            team = if (config.mode == Mode.TEAMS) idx % 2 else idx,
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
private fun ControlPad(
    team: Int,
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
    Surface(shape = RoundedCornerShape(28.dp), color = teamContainer(cs, team)) {
        Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            HoldButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, team) { l = it; push() }
            HoldButton(Icons.Filled.KeyboardArrowUp, team) { j = it; push() }
            HoldButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, team) { r = it; push() }
            HoldButton(Icons.Filled.Star, team, strong = true) { if (it) { onTouch(); onSwing() } }
            val f = fighter()
            frame()
            val ab = f?.loadout?.ability ?: Ability.NONE
            if (ab.cooldown > 0) {
                val ready = (f?.abilityCd ?: 1f) <= 0f
                HoldButton(Icons.Filled.ThumbUp, team, strong = ready, dim = !ready) { if (it) { onTouch(); onAbility() } }
            }
        }
    }
}

@Composable
private fun HoldButton(icon: ImageVector, team: Int, strong: Boolean = false, dim: Boolean = false, onHold: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    var down by remember { mutableStateOf(false) }
    val c = teamColor(cs, team)
    val bg = when {
        down -> c
        strong -> c.copy(alpha = .35f)
        else -> Color.Transparent
    }
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val d = awaitFirstDown()
                    d.consume(); down = true; onHold(true)
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == d.id } ?: break
                        if (!ch.pressed) break
                    }
                    down = false; onHold(false)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = (if (down) cs.surface else onTeamContainer(cs, team)).copy(alpha = if (dim) .4f else 1f))
    }
}
