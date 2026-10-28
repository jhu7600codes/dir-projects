package dev.jhu.crossi.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dev.jhu.crossi.R
import dev.jhu.crossi.data.LanguageManager
import dev.jhu.crossi.data.Settings
import dev.jhu.crossi.data.SettingsRepository
import dev.jhu.crossi.data.ThemeMode
import dev.jhu.crossi.di.AppContainer
import dev.jhu.crossi.game.Language
import dev.jhu.crossi.ui.common.CrossiTopBar
import dev.jhu.crossi.ui.common.crossiViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(c: AppContainer) : ViewModel() {
    private val repo: SettingsRepository = c.settings
    val settings: StateFlow<Settings> = repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    fun edit(block: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { repo.block() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm = crossiViewModel { SettingsViewModel(it) }
    val s by vm.settings.collectAsStateWithLifecycle()
    val language = LanguageManager.current()

    Scaffold(topBar = { CrossiTopBar(stringResource(R.string.settings), onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Section(R.string.language)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = listOf(Language.EN to "English", Language.RU to "Русский")
                options.forEachIndexed { i, (lang, name) ->
                    SegmentedButton(
                        selected = language == lang,
                        onClick = { if (language != lang) LanguageManager.set(lang) },
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    ) { Text(name) }
                }
            }

            Section(R.string.section_look)
            Text(stringResource(R.string.theme))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = listOf(ThemeMode.DARK to R.string.theme_dark, ThemeMode.LIGHT to R.string.theme_light, ThemeMode.SYSTEM to R.string.theme_system)
                options.forEachIndexed { i, (mode, label) ->
                    SegmentedButton(
                        selected = s.themeMode == mode,
                        onClick = { vm.edit { setThemeMode(mode) } },
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    ) { Text(stringResource(label)) }
                }
            }
            Toggle(R.string.dynamic_color, s.dynamicColor) { vm.edit { setDynamicColor(it) } }

            Section(R.string.section_game)
            Toggle(R.string.auto_advance, s.autoAdvance) { vm.edit { setAutoAdvance(it) } }
            Toggle(R.string.skip_filled, s.skipFilled) { vm.edit { setSkipFilled(it) } }
            Toggle(R.string.jump_next_clue, s.jumpToNextClue) { vm.edit { setJumpToNextClue(it) } }
            Toggle(R.string.highlight_crossing, s.highlightCrossing) { vm.edit { setHighlightCrossing(it) } }
            Toggle(R.string.show_timer, s.showTimer) { vm.edit { setShowTimer(it) } }
            Toggle(R.string.haptics, s.haptics) { vm.edit { setHaptics(it) } }

            Text(
                stringResource(R.string.credits),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

@Composable
private fun Section(label: Int) {
    Text(
        stringResource(label),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun Toggle(label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
