package dev.jhu.crossi.ui.powerups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dev.jhu.crossi.R
import dev.jhu.crossi.di.AppContainer
import dev.jhu.crossi.game.PowerUpType
import dev.jhu.crossi.ui.common.CrossiTopBar
import dev.jhu.crossi.ui.common.crossiViewModel
import dev.jhu.crossi.ui.common.description
import dev.jhu.crossi.ui.common.icon
import dev.jhu.crossi.ui.common.label
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PowerUpsViewModel(c: AppContainer) : ViewModel() {
    val inventory: StateFlow<Map<PowerUpType, Int>> =
        c.games.inventory.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
}

@Composable
fun PowerUpsScreen(onBack: () -> Unit) {
    val vm = crossiViewModel { PowerUpsViewModel(it) }
    val inventory by vm.inventory.collectAsStateWithLifecycle()

    Scaffold(topBar = { CrossiTopBar(stringResource(R.string.power_ups), onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (type in PowerUpType.entries) {
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(type.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(type.label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(type.description), style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("×${inventory[type] ?: 0}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    }
                }
            }
            Text(stringResource(R.string.earn_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            for (line in listOf(R.string.earn_streak, R.string.earn_all_dailies, R.string.earn_pickups, R.string.earn_bosses, R.string.earn_free_roam)) {
                Text("• " + stringResource(line), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
