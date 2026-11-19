package com.patternbox.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patternbox.core.GameMap
import com.patternbox.core.Mode

@Composable
fun PlayScreen(state: GameState, padding: PaddingValues, onFriends: () -> Unit = {}, onMode: (Mode) -> Unit) {
    val cs = MaterialTheme.colorScheme
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "${state.matchesPlayed} matches played · ${state.totalKos} kos · best survival wave ${state.bestWave}",
                style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant,
            )
        }
        item {
            ElevatedCard(
                onClick = onFriends, modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = cs.primaryContainer),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("play with friends", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = cs.onPrimaryContainer)
                    Text("wifi multiplayer, up to 6 phones", style = MaterialTheme.typography.bodyMedium, color = cs.onPrimaryContainer)
                }
            }
        }
        items(Mode.values().toList()) { m ->
            ElevatedCard(onClick = { onMode(m) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val showcase = state.roster.getOrNull(m.ordinal % state.roster.size.coerceAtLeast(1))
                    if (showcase != null) MiniFighter(showcase.loadout, Modifier.size(56.dp), team = m.ordinal)
                    Column(Modifier.weight(1f)) {
                        Text(m.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(m.blurb, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** pick fighters, who controls them and the map */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(state: GameState, mode: Mode, padding: PaddingValues, onStart: (FightConfig) -> Unit, onTournament: (List<RosterFighter>, GameMap, Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    var count by remember { mutableIntStateOf(when (mode) { Mode.FFA, Mode.KOTH, Mode.BOSS -> 4; Mode.SANDBOX -> 2; else -> mode.minFighters }) }
    if (mode == Mode.TOURNAMENT) {
        TournamentSetup(state, padding, onTournament)
        return
    }
    val picks = remember {
        mutableStateListOf<Long?>().apply {
            val ids = state.roster.map { it.id }
            repeat(6) { add(ids.getOrNull(it)) }
        }
    }
    // control per slot: 0 = bot, 1 = pad 1, 2 = pad 2
    val control = remember { mutableStateListOf(1, 0, 0, 0, 0, 0) }
    var map by remember { mutableStateOf(if (mode == Mode.SANDBOX) GameMap.CRATES else GameMap.FLAT) }
    var choosing by remember { mutableStateOf<Int?>(null) }

    Column(
        Modifier.verticalScroll(rememberScrollState())
            .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(mode.blurb, color = cs.onSurfaceVariant)
        if (mode.minFighters != mode.maxFighters) {
            Text("fighters", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (n in mode.minFighters..mode.maxFighters) FilterChip(selected = count == n, onClick = { count = n }, label = { Text("$n") })
            }
        }
        for (i in 0 until count) {
            val f = picks[i]?.let { state.byId(it) }
            val team = when (mode) { Mode.TEAMS -> i % 2; Mode.SURVIVAL -> 0; Mode.BOSS -> if (i == 0) 1 else 0; else -> i }
            OutlinedCard(onClick = { choosing = i }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (f != null) MiniFighter(f.loadout, Modifier.size(56.dp), team)
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (mode) {
                                Mode.TEAMS -> "team ${i % 2 + 1}"; Mode.SURVIVAL -> "you"
                                Mode.BOSS -> if (i == 0) "the boss" else "hero $i"
                                else -> "slot ${i + 1}"
                            },
                            style = MaterialTheme.typography.labelMedium, color = teamColor(cs, team),
                        )
                        Text(f?.displayName ?: "random", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    }
                }
                Row(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((c, label) in listOf(0 to "bot", 1 to "pad 1", 2 to "pad 2")) {
                        FilterChip(selected = control[i] == c, onClick = {
                            // a pad can only drive one fighter
                            if (c != 0) for (j in control.indices) if (control[j] == c) control[j] = 0
                            control[i] = c
                        }, label = { Text(label) })
                    }
                }
            }
        }
        Text("map", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (m in GameMap.values()) FilterChip(selected = map == m, onClick = { map = m }, label = { Text(m.label) })
        }
        Text(map.blurb, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        Button(onClick = {
            val entries = (0 until count).map { i ->
                (picks[i]?.let { state.byId(it) } ?: state.roster.randomOrNull())?.entry()
                    ?: com.patternbox.core.Loadout.random().let { com.patternbox.core.Entry(it.autoName, it) }
            }
            val humans = listOf(1, 2).mapNotNull { pad -> (0 until count).firstOrNull { control[it] == pad } }
            onStart(FightConfig(mode, entries, map, humans))
        }, modifier = Modifier.fillMaxWidth()) { Text("fight", style = MaterialTheme.typography.titleMedium) }
    }

    choosing?.let { slot ->
        FighterPicker(state, onPick = { picks[slot] = it; choosing = null }, onDismiss = { choosing = null })
    }
}

@Composable
internal fun FighterPicker(state: GameState, onPick: (Long?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("pick a fighter") },
        text = {
            LazyColumn(Modifier.heightIn(max = 460.dp)) {
                item { ListItem(headlineContent = { Text("random from roster") }, modifier = Modifier.clickable { onPick(null) }) }
                items(state.roster, key = { it.id }) { f ->
                    ListItem(
                        headlineContent = { Text(f.displayName) },
                        supportingContent = { Text("${f.loadout.weaponName} · ${f.wins}w ${f.losses}l") },
                        leadingContent = { MiniFighter(f.loadout, Modifier.size(44.dp)) },
                        modifier = Modifier.clickable { onPick(f.id) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("cancel") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TournamentSetup(state: GameState, padding: PaddingValues, onStart: (List<RosterFighter>, GameMap, Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val chosen = remember { mutableStateListOf<Long>().apply { addAll(state.roster.take(2).map { it.id }) } }
    var size by remember { mutableIntStateOf(8) }
    var map by remember { mutableStateOf(GameMap.FLAT) }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("pick up to $size of your fighters. empty spots get filled with presets.", color = cs.onSurfaceVariant)
                Text("bracket", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (n in listOf(4, 8)) FilterChip(selected = size == n, onClick = { size = n; while (chosen.size > n) chosen.removeAt(chosen.lastIndex) }, label = { Text("$n fighters") })
                }
                Text("map", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (m in GameMap.values()) FilterChip(selected = map == m, onClick = { map = m }, label = { Text(m.label) })
                }
                Button(onClick = { onStart(chosen.mapNotNull { state.byId(it) }, map, size) }, modifier = Modifier.fillMaxWidth()) {
                    Text("start tournament (${chosen.size}/$size picked)")
                }
            }
        }
        items(state.roster, key = { it.id }) { f ->
            val on = f.id in chosen
            ListItem(
                headlineContent = { Text(f.displayName) },
                supportingContent = { Text(if (f.cups > 0) "${f.cups} cups" else "${f.wins}w ${f.losses}l") },
                leadingContent = { MiniFighter(f.loadout, Modifier.size(44.dp)) },
                trailingContent = { FilterChip(selected = on, onClick = { if (on) chosen.remove(f.id) else if (chosen.size < size) chosen.add(f.id) }, label = { Text(if (on) "in" else "add") }) },
            )
        }
    }
}
