package com.patternbox.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patternbox.core.Loadout
import com.patternbox.core.Presets

@Composable
fun RosterScreen(state: GameState, padding: PaddingValues, onOpen: (Long) -> Unit) {
    var presets by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current
    val cs = MaterialTheme.colorScheme

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 4.dp,
            bottom = padding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = { presets = true }, label = { Text("presets") })
                AssistChip(onClick = {
                    val d = Loadout.decode(clipboard.getText()?.text.orEmpty())
                    msg = if (d != null) { state.add(d.autoName, d); "added from clipboard" } else "clipboard has no fighter code"
                }, label = { Text("paste code") })
                Text("${state.roster.size} fighters", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            }
            msg?.let { Text(it, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant) }
        }
        items(state.roster, key = { it.id }) { f -> RosterCard(state, f, onOpen) }
        if (state.roster.isEmpty()) item {
            Text("no fighters yet. tap new to draw one, or grab some presets.", color = cs.onSurfaceVariant)
        }
    }

    if (presets) AlertDialog(
        onDismissRequest = { presets = false },
        title = { Text("presets") },
        text = {
            LazyColumn(Modifier.heightIn(max = 460.dp)) {
                items(Presets.all) { (name, l) ->
                    ListItem(
                        headlineContent = { Text(name) },
                        supportingContent = { Text("${l.weaponName} · ${l.ability.label}") },
                        leadingContent = { MiniFighter(l, Modifier.size(48.dp)) },
                        modifier = Modifier.clickable {
                            state.add(name, l); msg = "added $name"
                        },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { presets = false }) { Text("done") } },
    )
}

@Composable
private fun RosterCard(state: GameState, f: RosterFighter, onOpen: (Long) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current
    var menu by remember { mutableStateOf(false) }
    val l = f.loadout
    ElevatedCard(onClick = { onOpen(f.id) }, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            MiniFighter(l, Modifier.size(76.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(f.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${l.weaponName} · ${l.armourName}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1)
                Text(
                    "${l.stats.hp} hp · ${l.stats.dmg} dmg · ${l.ability.label}",
                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1,
                )
                Text(
                    "${f.wins}w ${f.losses}l · ${f.kos} kos" + if (f.cups > 0) " · ${f.cups} cup${if (f.cups == 1) "" else "s"}" else "",
                    style = MaterialTheme.typography.labelMedium, color = cs.primary,
                )
            }
            Column {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "more") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("copy code") }, onClick = { clipboard.setText(AnnotatedString(l.encode())); menu = false })
                    DropdownMenuItem(text = { Text("duplicate") }, onClick = { state.duplicate(f.id); menu = false })
                    DropdownMenuItem(text = { Text("delete") }, onClick = { state.delete(f.id); menu = false })
                }
            }
        }
    }
}
