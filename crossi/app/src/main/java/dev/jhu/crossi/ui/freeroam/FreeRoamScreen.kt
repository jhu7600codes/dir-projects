package dev.jhu.crossi.ui.freeroam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jhu.crossi.R
import dev.jhu.crossi.data.FreeRoamRunEntity
import dev.jhu.crossi.data.HighScoreEntity
import dev.jhu.crossi.data.LanguageManager
import dev.jhu.crossi.di.AppContainer
import dev.jhu.crossi.game.Difficulty
import dev.jhu.crossi.game.GameMode
import dev.jhu.crossi.ui.common.CrossiTopBar
import dev.jhu.crossi.ui.common.crossiViewModel
import dev.jhu.crossi.ui.common.label
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class FreeRoamViewModel(private val c: AppContainer) : ViewModel() {
    private val language = LanguageManager.current()
    fun run(difficulty: Difficulty): Flow<FreeRoamRunEntity?> = c.games.run(language, difficulty)
    fun highScores(difficulty: Difficulty): Flow<List<HighScoreEntity>> = c.games.highScores(language, difficulty)

    fun endRun(difficulty: Difficulty) = viewModelScope.launch {
        c.games.endRun(language, difficulty)
        c.games.deleteSave(language, GameMode.FreeRoam(difficulty))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeRoamScreen(onBack: () -> Unit, onPlay: (GameMode) -> Unit) {
    val vm = crossiViewModel { FreeRoamViewModel(it) }
    var selected by remember { mutableStateOf(Difficulty.NORMAL) }

    Scaffold(topBar = { CrossiTopBar(stringResource(R.string.free_roam), onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (difficulty in Difficulty.entries) {
                val run by vm.run(difficulty).collectAsState(initial = null)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(difficulty.label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.grid_size, difficulty.size), style = MaterialTheme.typography.bodyMedium)
                            }
                            Button(onClick = { onPlay(GameMode.FreeRoam(difficulty)) }) {
                                Text(stringResource(if (run != null) R.string.continue_game else R.string.play))
                            }
                        }
                        run?.let { r ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.free_roam_run, r.score, r.cleared), modifier = Modifier.weight(1f))
                                TextButton(onClick = { vm.endRun(difficulty) }) { Text(stringResource(R.string.end_run)) }
                            }
                        }
                    }
                }
            }

            Text(stringResource(R.string.high_scores), style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Difficulty.entries.forEachIndexed { i, d ->
                    SegmentedButton(
                        selected = selected == d,
                        onClick = { selected = d },
                        shape = SegmentedButtonDefaults.itemShape(i, Difficulty.entries.size),
                    ) { Text(stringResource(d.label)) }
                }
            }
            val scores by vm.highScores(selected).collectAsState(initial = emptyList())
            if (scores.isEmpty()) {
                Text(stringResource(R.string.no_scores), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            scores.forEachIndexed { i, s ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${i + 1}.", fontWeight = FontWeight.Bold)
                    Text(s.score.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.puzzles_count, s.puzzles), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
