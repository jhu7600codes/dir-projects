package com.patternbox.app

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(state: GameState, padding: PaddingValues) {
    val cs = MaterialTheme.colorScheme
    var confirm by remember { mutableStateOf(false) }
    Column(
        Modifier.verticalScroll(rememberScrollState())
            .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
    ) {
        Toggle("sound", "hits, booms, clangs, all made from maths", state.sound, state::changeSound)
        Toggle("vibration", "buzz on hits and pattern dots", state.haptics, state::changeHaptics)
        Toggle("damage numbers", "the little -12 popups", state.damageNumbers, state::changeDamageNumbers)
        Toggle("screen shake", "turn off if it makes you dizzy", state.screenShake, state::changeScreenShake)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Toggle("wallpaper colours", "material you. fighters and the app take your wallpaper colours", state.dynamicColor, state::changeDynamicColor)
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("theme", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (m in ThemeMode.values()) FilterChip(selected = state.themeMode == m, onClick = { state.changeThemeMode(m) }, label = { Text(m.label) })
            }
            Text("button size", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((v, n) in listOf(52 to "small", 64 to "medium", 76 to "big", 88 to "huge")) FilterChip(selected = state.controlSize == v, onClick = { state.changeControlSize(v) }, label = { Text(n) })
            }
            Text("game speed", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (v in listOf(.5f, .75f, 1f, 1.25f, 1.5f, 2f)) FilterChip(selected = state.gameSpeed == v, onClick = { state.changeGameSpeed(v) }, label = { Text("${v}x") })
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("stats", style = MaterialTheme.typography.titleSmall)
            Text("${state.matchesPlayed} matches, ${state.totalKos} kos, best survival wave ${state.bestWave}", color = cs.onSurfaceVariant)
            val top = state.roster.maxByOrNull { it.wins * 3 + it.cups * 10 }
            if (top != null && top.wins > 0) Text("best fighter: ${top.displayName} (${top.wins} wins, ${top.cups} cups)", color = cs.onSurfaceVariant)
            OutlinedButton(onClick = { confirm = true }) { Text("reset stats") }
            Text("patternbox ${BuildConfigVersion.NAME}", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("reset all stats?") },
        text = { Text("wins, losses, kos, cups and best wave go back to zero. fighters stay.") },
        confirmButton = { TextButton(onClick = { state.resetStats(); confirm = false }) { Text("reset") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("cancel") } },
    )
}

object BuildConfigVersion { const val NAME = "0.2.3" }

@Composable
private fun Toggle(title: String, sub: String, value: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(sub) },
        trailingContent = { Switch(checked = value, onCheckedChange = onChange) },
        modifier = Modifier,
    )
}
