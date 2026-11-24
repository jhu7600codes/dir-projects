package com.patternbox.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.patternbox.core.Match
import com.patternbox.core.Mode
import com.patternbox.core.Tournament

/** bracket view. "version" bumps force a redraw after the tournament object changes */
@Composable
fun TournamentScreen(
    state: GameState,
    t: Tournament,
    padding: PaddingValues,
    version: Int,
    onWatch: (FightConfig) -> Unit,
    onChanged: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    @Suppress("UNUSED_EXPRESSION") version

    fun recordSim(r: Int, m: Int) {
        val (a, b) = t.pair(r, m)
        val w = Match.simulate(a, b, t.map)
        t.record(r, m, w)
        val winner = if (w == 0) a else b
        val loser = if (w == 0) b else a
        state.record(setOfNotNull(winner.rosterId.takeIf { it >= 0 }), setOfNotNull(loser.rosterId.takeIf { it >= 0 }), emptyMap())
    }

    val champ = t.champion
    LaunchedEffect(champ) {
        if (champ != null && !t.cupAwarded) {
            t.cupAwarded = true
            if (champ.rosterId >= 0) state.record(emptySet(), emptySet(), emptyMap(), cupFor = champ.rosterId)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            val next = t.next()
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (champ != null) {
                        MiniFighter(champ.loadout, Modifier.size(110.dp), team = 4)
                        Text("${champ.name} is the champion", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        if (champ.rosterId >= 0) Text("+1 cup", color = cs.primary)
                    } else if (next != null) {
                        val (r, m) = next
                        val (a, b) = t.pair(r, m)
                        Text(t.roundName(r), style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                MiniFighter(a.loadout, Modifier.size(72.dp), 0); Text(a.name, maxLines = 1)
                            }
                            Text("vs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                MiniFighter(b.loadout, Modifier.size(72.dp), 1); Text(b.name, maxLines = 1)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onWatch(FightConfig(Mode.TOURNAMENT, listOf(a, b), t.map, tournamentMatch = r to m)) }) { Text("watch") }
                            OutlinedButton(onClick = { recordSim(r, m); onChanged() }) { Text("simulate") }
                            TextButton(onClick = {
                                while (true) { val n = t.next() ?: break; recordSim(n.first, n.second) }
                                onChanged()
                            }) { Text("sim all") }
                        }
                    }
                }
            }
        }
        for (r in 1 until t.rounds.size) {
            item {
                Text(t.roundName(r - 1), style = MaterialTheme.typography.titleSmall)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (m in t.rounds[r].indices) {
                        val a = t.rounds[r - 1][m * 2]
                        val b = t.rounds[r - 1][m * 2 + 1]
                        val w = t.rounds[r][m]
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                BracketName(a?.let { t.players[it].name } ?: "?", w != null && w == a, w != null && w != a, Modifier.weight(1f))
                                Text("vs", color = cs.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
                                BracketName(b?.let { t.players[it].name } ?: "?", w != null && w == b, w != null && w != b, Modifier.weight(1f), end = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BracketName(name: String, won: Boolean, lost: Boolean, modifier: Modifier, end: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    Text(
        name, modifier, maxLines = 1,
        fontWeight = if (won) FontWeight.Bold else FontWeight.Normal,
        color = if (lost) cs.onSurfaceVariant.copy(alpha = .5f) else if (won) cs.primary else cs.onSurface,
        textAlign = if (end) TextAlign.End else TextAlign.Start,
    )
}
