package app.krala.ui.plugins

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.krala.R
import app.krala.core.PluginSpec
import app.krala.data.db.PluginEntity
import app.krala.data.plugins.PluginRepository
import app.krala.ui.chat.PluginEditDialog
import app.krala.ui.components.EmptyState
import app.krala.ui.settings.BackTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PluginsViewModel @Inject constructor(private val repo: PluginRepository) : ViewModel() {
    val plugins: StateFlow<List<PluginEntity>> = repo.all.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val making = MutableStateFlow(false)
    val draft = MutableStateFlow<PluginSpec?>(null)
    val error = MutableStateFlow<String?>(null)

    fun make(request: String) = viewModelScope.launch {
        making.value = true; error.value = null
        runCatching { repo.make(request) }.onSuccess { draft.value = it }.onFailure { error.value = it.message }
        making.value = false
    }
    fun save(spec: PluginSpec, id: String?) = viewModelScope.launch { repo.save(spec, id) }
    fun toggle(p: PluginEntity, on: Boolean) = viewModelScope.launch { repo.setEnabled(p, on) }
    fun delete(p: PluginEntity) = viewModelScope.launch { repo.delete(p.id) }
}

@Composable
fun PluginsScreen(vm: PluginsViewModel, onBack: () -> Unit) {
    val plugins by vm.plugins.collectAsStateWithLifecycle()
    val making by vm.making.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var ask by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PluginEntity?>(null) }

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.plugins), onBack, scroll) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { ask = true }, icon = {
                if (making) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Rounded.Add, null)
            }, text = { Text(stringResource(R.string.plugin_new)) })
        },
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
    ) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 96.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Text(stringResource(R.string.plugins_explainer), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp)) }
            error?.let { e -> item { Text(e, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp)) } }
            if (plugins.isEmpty()) item { EmptyState(stringResource(R.string.plugins_empty), stringResource(R.string.plugins_empty_body)) }
            items(plugins, key = { it.id }) { p ->
                ElevatedCard(onClick = { editing = p }, colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Extension, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("/" + p.name + if (p.web) "  · web" else "", style = MaterialTheme.typography.titleMedium)
                            Text(p.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                        Switch(p.enabled, onCheckedChange = { vm.toggle(p, it) })
                    }
                }
            }
        }
    }
    if (ask) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { ask = false },
            title = { Text(stringResource(R.string.plugin_new)) },
            text = {
                Column {
                    Text(stringResource(R.string.plugin_describe), style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(text, { text = it }, minLines = 3, placeholder = { Text(stringResource(R.string.plugin_example)) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            },
            confirmButton = { TextButton(onClick = { ask = false; vm.make(text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.plugin_make)) } },
            dismissButton = {
                Row {
                    TextButton(onClick = { ask = false; vm.draft.value = PluginSpec("", "", "", false) }) { Text(stringResource(R.string.plugin_by_hand)) }
                    TextButton(onClick = { ask = false }) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
    }
    draft?.let { d -> PluginEditDialog(d, isNew = true, onSave = { vm.save(it, null) }, onDelete = null, onDismiss = { vm.draft.value = null }) }
    editing?.let { p ->
        PluginEditDialog(PluginSpec(p.name, p.description, p.instructions, p.web), isNew = false, onSave = { vm.save(it, p.id) }, onDelete = { vm.delete(p) }, onDismiss = { editing = null })
    }
}
