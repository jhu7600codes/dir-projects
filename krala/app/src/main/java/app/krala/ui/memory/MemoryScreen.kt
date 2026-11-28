package app.krala.ui.memory

import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.krala.R
import app.krala.core.Tokens
import app.krala.data.db.MemoryEntryEntity
import app.krala.data.db.MemoryKind
import app.krala.data.memory.MemoryRepository
import app.krala.data.memory.MemoryScheduler
import app.krala.ui.components.EmptyState
import app.krala.ui.settings.BackTopBar
import app.krala.ui.theme.LocalHaptics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val repo: MemoryRepository,
    private val scheduler: MemoryScheduler,
) : ViewModel() {
    val entries: StateFlow<List<MemoryEntryEntity>> = repo.entries.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    fun save(e: MemoryEntryEntity, name: String, notes: String) = viewModelScope.launch { repo.edit(e.id, name, notes) }
    fun delete(e: MemoryEntryEntity) = viewModelScope.launch { repo.delete(e.id) }
    fun forgetAll() = viewModelScope.launch { repo.forgetEverything() }
    fun catchUp() = viewModelScope.launch { scheduler.sweep() }
}

private val fmt = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm").withZone(ZoneId.systemDefault())

@Composable
fun MemoryScreen(vm: MemoryViewModel, onBack: () -> Unit, onOpenChat: (String) -> Unit) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    val h = LocalHaptics.current
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var editing by remember { mutableStateOf<MemoryEntryEntity?>(null) }
    var confirmForget by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var showArchived by remember { mutableStateOf(false) }
    val live = entries.filter { it.archivedInto == null }
    val archived = entries.filter { it.archivedInto != null }
    val tokens = live.sumOf { Tokens.estimate(it.notes) }

    Scaffold(
        topBar = {
            BackTopBar(stringResource(R.string.memory), onBack, scroll) {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.more)) }
                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.memory_catch_up)) }, leadingIcon = { Icon(Icons.Rounded.Sync, null) }, onClick = { menu = false; vm.catchUp() })
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.forget_everything), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; confirmForget = true },
                        )
                    }
                }
            }
        },
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
    ) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 32.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.memory_explainer, live.size, tokens),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            if (entries.isEmpty()) item { EmptyState(stringResource(R.string.memory_empty), stringResource(R.string.memory_empty_body)) }
            items(live, key = { it.id }) { e -> EntryCard(e, onEdit = { editing = e }, onOpen = e.chatId?.let { { onOpenChat(it) } }, modifier = Modifier.animateItem()) }
            if (archived.isNotEmpty()) {
                item {
                    TextButton(onClick = { showArchived = !showArchived }) {
                        Icon(if (showArchived) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.archived_entries, archived.size))
                    }
                }
                if (showArchived) items(archived, key = { it.id }) { e ->
                    EntryCard(e, onEdit = { editing = e }, onOpen = e.chatId?.let { { onOpenChat(it) } }, archived = true, modifier = Modifier.animateItem())
                }
            }
        }
    }

    editing?.let { e ->
        var name by remember(e.id) { mutableStateOf(e.chatName) }
        var notes by remember(e.id) { mutableStateOf(e.notes) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(stringResource(R.string.edit_memory)) },
            text = {
                Column {
                    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.chat_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(R.string.notes)) }, minLines = 6, maxLines = 14, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(onClick = { vm.save(e, name, notes); h.confirm(); editing = null }) { Text(stringResource(R.string.save)) } },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.delete(e); h.long(); editing = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text(stringResource(R.string.delete))
                    }
                    TextButton(onClick = { editing = null }) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
    }
    if (confirmForget) {
        AlertDialog(
            onDismissRequest = { confirmForget = false },
            icon = { Icon(Icons.Rounded.DeleteForever, null) },
            title = { Text(stringResource(R.string.forget_everything_q)) },
            text = { Text(stringResource(R.string.forget_everything_body)) },
            confirmButton = {
                TextButton(onClick = { confirmForget = false; h.long(); vm.forgetAll() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.forget))
                }
            },
            dismissButton = { TextButton(onClick = { confirmForget = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun EntryCard(e: MemoryEntryEntity, onEdit: () -> Unit, onOpen: (() -> Unit)?, archived: Boolean = false, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    ElevatedCard(
        onClick = { expanded = !expanded },
        colors = CardDefaults.elevatedCardColors(containerColor = if (e.kind == MemoryKind.ARCHIVE) cs.tertiaryContainer.copy(alpha = 0.35f) else cs.surfaceContainerLow),
        modifier = modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (e.kind == MemoryKind.ARCHIVE) Icons.Rounded.Inventory2 else if (archived) Icons.Rounded.Archive else Icons.Rounded.ChatBubbleOutline,
                    null, Modifier.size(18.dp), tint = cs.primary,
                )
                Text(
                    if (e.kind == MemoryKind.ARCHIVE) stringResource(R.string.memory_archive) else e.chatName.ifBlank { stringResource(R.string.untitled) },
                    style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                Text(fmt.format(Instant.ofEpochMilli(e.updatedAt)), style = MaterialTheme.typography.labelSmall, color = cs.outline)
            }
            Text(
                e.notes, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp, end = 8.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (onOpen != null) TextButton(onClick = onOpen) { Text(stringResource(R.string.open_chat)) }
                TextButton(onClick = onEdit) { Text(stringResource(R.string.edit)) }
            }
        }
    }
}
