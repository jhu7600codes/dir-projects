package com.patternbox.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.patternbox.core.Loadout
import com.patternbox.core.Part
import com.patternbox.core.randomPattern
import kotlin.math.roundToInt

@Composable
fun BuildScreen(state: GameState, padding: PaddingValues) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                "draw like an android lock screen. skipping over an unused dot grabs it too. longer weapon hits harder but swings slower, more armour blocks more but makes you heavy.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(listOf(0, 1)) { i -> FighterCard(state, i) }
    }
}

@Composable
fun fighterPalette(i: Int) = FighterPalette(
    body = playerColor(i),
    metal = MaterialTheme.colorScheme.outline,
    blade = MaterialTheme.colorScheme.inverseSurface,
    bladeOutline = MaterialTheme.colorScheme.surfaceContainerLowest,
)

@Composable
private fun FighterCard(state: GameState, i: Int) {
    val l = state.loadouts[i]
    val s = l.stats
    val clipboard = LocalClipboardManager.current
    var codeMsg by remember { mutableStateOf<String?>(null) }
    val pal = fighterPalette(i)

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "p${i + 1}",
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(playerContainer(i))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    color = onPlayerContainer(i),
                    style = MaterialTheme.typography.labelLarge,
                )
                OutlinedTextField(
                    value = state.names[i],
                    onValueChange = { state.rename(i, it.take(24)) },
                    singleLine = true,
                    label = { Text("name") },
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (part in Part.values()) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(part.name.lowercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        PatternPad(
                            pattern = l[part],
                            minDots = part.minDots,
                            lineColor = if (part == Part.ARMOUR) MaterialTheme.colorScheme.outline else playerColor(i),
                            onPattern = { state.set(i, state.loadouts[i].with(part, it)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            when (part) {
                                Part.BODY -> "${s.hp} hp"
                                Part.WEAPON -> l.weaponName
                                Part.ARMOUR -> l.armourName
                            },
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                        TextButton(onClick = { state.set(i, state.loadouts[i].with(part, randomPattern(part.minDots, part.maxRandom))) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("random")
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Canvas(
                    Modifier
                        .size(112.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    translate(size.width / 2, size.height / 2 + 4) {
                        scale(size.width / 120f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                            drawFighter(l, pal)
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    StatLine("health", "${s.hp}")
                    StatLine("speed", "%.2f".format(s.speed))
                    StatLine("jump", "%.1f".format(s.jump))
                    StatLine("damage", "${s.dmg}")
                    StatLine("swing", "%.2fs".format(s.swing))
                    StatLine("block", "${(s.def * 100).roundToInt()}% x ${s.plates}")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = { clipboard.setText(AnnotatedString(l.encode())); codeMsg = "copied ${l.encode()}" },
                    label = { Text(l.encode(), fontFamily = FontFamily.Monospace) },
                    leadingIcon = { Icon(Icons.Filled.Share, null, Modifier.size(18.dp)) },
                )
                TextButton(onClick = {
                    val txt = clipboard.getText()?.text.orEmpty()
                    val d = Loadout.decode(txt)
                    if (d != null) { state.set(i, d); codeMsg = "loaded" } else codeMsg = "clipboard has no fighter code"
                }) { Text("paste") }
                Spacer(Modifier.weight(1f))
                FilledTonalButton(onClick = {
                    val r = Loadout.random()
                    state.set(i, r); state.rename(i, r.autoName)
                }) {
                    Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                }
            }
            codeMsg?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StatLine(k: String, v: String) {
    Row {
        Text(k, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(v, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
    Spacer(Modifier.height(0.dp))
}
