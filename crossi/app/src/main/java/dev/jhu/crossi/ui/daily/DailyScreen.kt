package dev.jhu.crossi.ui.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import dev.jhu.crossi.data.DailyResultEntity
import dev.jhu.crossi.data.LanguageManager
import dev.jhu.crossi.di.AppContainer
import dev.jhu.crossi.game.Difficulty
import dev.jhu.crossi.game.GameMode
import dev.jhu.crossi.game.Rewards
import dev.jhu.crossi.game.Streak
import dev.jhu.crossi.game.Streaks
import dev.jhu.crossi.ui.common.CrossiTopBar
import dev.jhu.crossi.ui.common.crossiViewModel
import dev.jhu.crossi.ui.common.formatTime
import dev.jhu.crossi.ui.common.label
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class DailyViewModel(c: AppContainer) : ViewModel() {
    val today: String = LocalDate.now().toString()
    val results: StateFlow<List<DailyResultEntity>> =
        c.games.dailyResults(today, LanguageManager.current()).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val streak: StateFlow<Streak> = c.games.streak.stateIn(viewModelScope, SharingStarted.Eagerly, Streak())
    val recent: StateFlow<List<DailyResultEntity>> =
        c.games.recentDailies.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}

@Composable
fun DailyScreen(onBack: () -> Unit, onPlay: (GameMode) -> Unit) {
    val vm = crossiViewModel { DailyViewModel(it) }
    val results by vm.results.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val today = LocalDate.parse(vm.today)
    val current = Streaks.displayed(streak, today)

    Scaffold(topBar = { CrossiTopBar(stringResource(R.string.daily), onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.size(8.dp))
                        Column {
                            Text(stringResource(R.string.streak_days, current), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.best_streak, streak.best), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Text(stringResource(R.string.next_milestone, Rewards.nextMilestone(current)), style = MaterialTheme.typography.bodySmall)
                    // last two weeks, filled dot = cleared a daily that day
                    val cleared = recent.map { it.date }.toSet()
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 13 downTo 0) {
                            val day = today.minusDays(i.toLong()).toString()
                            Surface(
                                Modifier.size(14.dp),
                                shape = CircleShape,
                                color = if (day in cleared) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHighest,
                            ) {}
                        }
                    }
                }
            }
            Text(stringResource(R.string.today) + " · " + vm.today, style = MaterialTheme.typography.titleMedium)
            for (difficulty in Difficulty.entries) {
                val result = results.firstOrNull { it.difficulty == difficulty.name }
                DailyCard(difficulty, result) { onPlay(GameMode.Daily(vm.today, difficulty)) }
            }
        }
    }
}

@Composable
private fun DailyCard(difficulty: Difficulty, result: DailyResultEntity?, onPlay: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(difficulty.label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.grid_size, difficulty.size), style = MaterialTheme.typography.bodyMedium)
                if (result?.completed == true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("${stringResource(R.string.cleared)} · ${formatTime(result.timeMs)} · ${result.score}")
                    }
                }
            }
            if (result?.completed == true) FilledTonalButton(onClick = onPlay) { Text(stringResource(R.string.play)) }
            else Button(onClick = onPlay) { Text(stringResource(R.string.play)) }
        }
    }
}
