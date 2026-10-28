package dev.jhu.crossi.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jhu.crossi.R

@Composable
fun HomeScreen(
    onLevels: () -> Unit,
    onFreeRoam: () -> Unit,
    onDaily: () -> Unit,
    onPowerUps: () -> Unit,
    onSettings: () -> Unit,
    onHowToPlay: () -> Unit,
) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onHowToPlay) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, stringResource(R.string.how_to_play))
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, stringResource(R.string.settings))
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.tagline), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeButton(R.string.levels, Icons.Filled.GridOn, onLevels, primary = true)
                HomeButton(R.string.free_roam, Icons.Filled.AllInclusive, onFreeRoam)
                HomeButton(R.string.daily, Icons.Filled.CalendarToday, onDaily)
                HomeButton(R.string.power_ups, Icons.Filled.Bolt, onPowerUps)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HomeButton(label: Int, icon: ImageVector, onClick: () -> Unit, primary: Boolean = false) {
    val modifier = Modifier.fillMaxWidth().height(56.dp)
    val content: @Composable () -> Unit = {
        Icon(icon, null)
        Spacer(Modifier.padding(start = 8.dp))
        Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
    }
    if (primary) Button(onClick, modifier) { content() } else FilledTonalButton(onClick, modifier) { content() }
}
