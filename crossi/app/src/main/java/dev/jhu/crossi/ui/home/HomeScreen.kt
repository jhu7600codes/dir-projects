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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dev.jhu.crossi.R
import dev.jhu.crossi.data.LanguageManager
import dev.jhu.crossi.di.AppContainer
import dev.jhu.crossi.game.GameMode
import dev.jhu.crossi.game.PowerUpType
import dev.jhu.crossi.game.Streaks
import dev.jhu.crossi.ui.common.crossiViewModel
import dev.jhu.crossi.ui.common.icon
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class HomeViewModel(c: AppContainer) : ViewModel() {
    val continueMode: StateFlow<GameMode?> =
        c.games.latestSave(LanguageManager.current()).stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val streak: StateFlow<Int> = c.games.streak.map { Streaks.displayed(it, LocalDate.now()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val inventory: StateFlow<Map<PowerUpType, Int>> =
        c.games.inventory.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
}

@Composable
fun HomeScreen(
    onContinue: (GameMode) -> Unit,
    onLevels: () -> Unit,
    onFreeRoam: () -> Unit,
    onDaily: () -> Unit,
    onPowerUps: () -> Unit,
    onSettings: () -> Unit,
    onHowToPlay: () -> Unit,
) {
    val vm = crossiViewModel { HomeViewModel(it) }
    val continueMode by vm.continueMode.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val inventory by vm.inventory.collectAsStateWithLifecycle()

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.secondary)
                Text(stringResource(R.string.streak_days, streak), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
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
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (type in PowerUpType.entries) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(type.icon, null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(2.dp))
                        Text((inventory[type] ?: 0).toString(), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                continueMode?.let { mode ->
                    HomeButton(R.string.continue_game, Icons.Filled.PlayArrow, { onContinue(mode) }, style = 0)
                }
                HomeButton(R.string.levels, Icons.Filled.GridOn, onLevels, style = if (continueMode == null) 0 else 1)
                HomeButton(R.string.daily, Icons.Filled.CalendarToday, onDaily, style = 1)
                HomeButton(R.string.free_roam, Icons.Filled.AllInclusive, onFreeRoam, style = 1)
                HomeButton(R.string.power_ups, Icons.Filled.Bolt, onPowerUps, style = 2)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HomeButton(label: Int, icon: ImageVector, onClick: () -> Unit, style: Int) {
    val modifier = Modifier.fillMaxWidth().height(56.dp)
    val content: @Composable () -> Unit = {
        Icon(icon, null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
    }
    when (style) {
        0 -> Button(onClick, modifier) { content() }
        1 -> FilledTonalButton(onClick, modifier) { content() }
        else -> OutlinedButton(onClick, modifier) { content() }
    }
}
