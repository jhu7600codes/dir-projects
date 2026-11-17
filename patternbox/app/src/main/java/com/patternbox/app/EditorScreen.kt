package com.patternbox.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patternbox.core.Ability
import com.patternbox.core.ArmourMat
import com.patternbox.core.Geo
import com.patternbox.core.Loadout
import com.patternbox.core.Part
import com.patternbox.core.WeaponMat
import com.patternbox.core.WeaponType
import com.patternbox.core.randomPattern
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(state: GameState, id: Long, padding: PaddingValues, onTry: (RosterFighter) -> Unit, onDeleted: () -> Unit) {
    val f = state.byId(id) ?: return
    val l = f.loadout
    val s = l.stats
    val cs = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current
    var msg by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    fun set(n: Loadout) = state.update(f.copy(loadout = n))

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AnimatedPreview(l, Modifier.fillMaxWidth().height(170.dp))

        OutlinedTextField(
            value = f.name,
            onValueChange = { state.update(f.copy(name = it.take(24))) },
            singleLine = true,
            label = { Text("name") },
            placeholder = { Text(l.autoName) },
            modifier = Modifier.fillMaxWidth(),
        )

        // the four lock screens
        for (row in listOf(listOf(Part.BODY, Part.WEAPON), listOf(Part.ARMOUR, Part.HAT))) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                for (part in row) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(part.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text(
                                when (part) {
                                    Part.BODY -> "${s.hp} hp"
                                    Part.WEAPON -> l.weaponName
                                    Part.ARMOUR -> l.armourName
                                    Part.HAT -> l.hatName
                                },
                                style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, maxLines = 1,
                            )
                        }
                        PatternPad(
                            pattern = l[part],
                            minDots = part.minDots.coerceAtLeast(if (part == Part.HAT) 2 else 0),
                            lineColor = when (part) {
                                Part.ARMOUR -> cs.outline
                                Part.HAT -> cs.onSurface
                                else -> cs.primary
                            },
                            onPattern = { set(l.with(part, it)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row {
                            TextButton(onClick = { set(l.with(part, randomPattern(part.minDots.coerceAtLeast(2), part.maxRandom))) }) { Text("random") }
                            if (part == Part.HAT && l.hat.isNotEmpty()) TextButton(onClick = { set(l.with(part, emptyList())) }) { Text("no hat") }
                        }
                    }
                }
            }
        }

        Picker("weapon type", WeaponType.values().toList(), l.weaponType, { it.label }, { it.blurb }) { set(l.copy(weaponType = it)) }
        Picker("weapon material", WeaponMat.values().toList(), l.weaponMat, { it.label }, { it.blurb }) { set(l.copy(weaponMat = it)) }
        Picker("armour material", ArmourMat.values().toList(), l.armourMat, { it.label }, { it.blurb }) { set(l.copy(armourMat = it)) }
        Picker("ability", Ability.values().toList(), l.ability, { it.label }, { a -> a.blurb + if (a.cooldown > 0) " (${a.cooldown.roundToInt()}s cooldown)" else "" }) { set(l.copy(ability = it)) }

        ElevatedCard {
            Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("stats", style = MaterialTheme.typography.titleSmall)
                StatBar("health", s.hp / 180f, "${s.hp}")
                StatBar("speed", s.speed / 6f, "%.1f".format(s.speed))
                StatBar("jump", s.jump / 13f, "%.1f".format(s.jump))
                StatBar("damage", s.dmg / 40f, "${s.dmg}")
                StatBar("swing speed", 1 - (s.swing / 2f), "%.2fs".format(s.swing))
                StatBar("block", s.def / .8f, "${(s.def * 100).roundToInt()}%")
                StatBar("knockback", s.kb / 4.5f, "x%.1f".format(s.kb))
                Spacer(Modifier.height(4.dp))
                Text("record: ${f.wins} wins, ${f.losses} losses, ${f.kos} kos, ${f.cups} cups", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { clipboard.setText(AnnotatedString(l.encode())); msg = "copied" },
                label = { Text(l.encode(), fontFamily = FontFamily.Monospace) },
                leadingIcon = { Icon(Icons.Filled.Share, null, Modifier.size(18.dp)) },
            )
            AssistChip(onClick = {
                val d = Loadout.decode(clipboard.getText()?.text.orEmpty())
                if (d != null) { set(d); msg = "loaded from clipboard" } else msg = "clipboard has no fighter code"
            }, label = { Text("paste code") })
        }
        msg?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant) }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { onTry(f) }) {
                Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("try it")
            }
            OutlinedButton(onClick = { val r = Loadout.random(); state.update(f.copy(loadout = r, name = r.autoName)) }) {
                Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("randomize all")
            }
            OutlinedButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Filled.Delete, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("delete")
            }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("delete ${f.displayName}?") },
        text = { Text("their record goes too. copy the code first if you want them back later.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; state.delete(f.id); onDeleted() }) { Text("delete") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("keep") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Picker(title: String, items: List<T>, selected: T, label: (T) -> String, blurb: (T) -> String, onPick: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (it in items) FilterChip(selected = it == selected, onClick = { onPick(it) }, label = { Text(label(it)) })
        }
        Text(blurb(selected), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatBar(name: String, frac: Float, value: String) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(name, Modifier.weight(.38f), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        Canvas(Modifier.weight(.47f).height(8.dp).clip(RoundedCornerShape(50)).background(cs.surfaceContainerHighest)) {
            drawRect(cs.primary, size = size.copy(width = size.width * frac.coerceIn(.03f, 1f)))
        }
        Text(value, Modifier.weight(.15f), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
    }
}

/** the fighter on a little stage, swinging forever */
@Composable
fun AnimatedPreview(l: Loadout, modifier: Modifier = Modifier, team: Int = 0) {
    val cs = MaterialTheme.colorScheme
    val pal = remember(cs, team) { paletteFor(cs, team) }
    val t by rememberInfiniteTransition(label = "swing").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    Canvas(modifier.clip(RoundedCornerShape(28.dp)).background(cs.surfaceContainerHigh)) {
        // swing during the first 40% of the loop, rest otherwise
        val p = (t / .4f).coerceIn(0f, 1f)
        val swing = if (t > .4f) Geo.REST_SWING else when {
            p < .3f -> -.9f - (p / .3f)
            p < .7f -> -1.9f + ((p - .3f) / .4f) * 3.4f
            else -> 1.5f - ((p - .7f) / .3f) * 2.4f
        }
        val thrust = if (l.weaponType == WeaponType.SPEAR && t < .4f) kotlin.math.sin(p * Math.PI).toFloat() * 40 else 0f
        val sw = if (l.weaponType == WeaponType.SPEAR) -.2f else if (l.weaponType == WeaponType.BOW) -.15f else swing
        val k = size.height / 130f
        translate(size.width / 2, size.height / 2 + 8 * k) {
            scale(k, pivot = Offset.Zero) {
                drawFighter(l, pal, FighterLook(weapon = Geo.weaponLocal(l.body, l.weapon, sw, thrust), hot = t in .12f..(.3f)))
            }
        }
    }
}

/** small static fighter for lists */
@Composable
fun MiniFighter(l: Loadout, modifier: Modifier = Modifier, team: Int = 0) {
    val cs = MaterialTheme.colorScheme
    val pal = remember(cs, team) { paletteFor(cs, team) }
    Canvas(modifier.clip(RoundedCornerShape(20.dp)).background(cs.surfaceContainerHighest)) {
        val k = size.height / 125f
        translate(size.width / 2, size.height / 2 + 6 * k) {
            scale(k, pivot = Offset.Zero) { drawFighter(l, pal) }
        }
    }
}
