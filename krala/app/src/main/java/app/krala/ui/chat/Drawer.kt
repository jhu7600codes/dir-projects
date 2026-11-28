package app.krala.ui.chat

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.krala.R
import app.krala.data.db.ConversationEntity
import app.krala.ui.components.KralaLogo
import app.krala.ui.theme.LocalHaptics
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class ChatGroup(val label: Int) {
    PINNED(R.string.group_pinned), TODAY(R.string.group_today), YESTERDAY(R.string.group_yesterday),
    WEEK(R.string.group_week), MONTH(R.string.group_month), OLDER(R.string.group_older)
}

fun groupOf(c: ConversationEntity, today: LocalDate): ChatGroup {
    if (c.pinned) return ChatGroup.PINNED
    val d = Instant.ofEpochMilli(c.lastMessageAt).atZone(ZoneId.systemDefault()).toLocalDate()
    return when {
        !d.isBefore(today) -> ChatGroup.TODAY
        d == today.minusDays(1) -> ChatGroup.YESTERDAY
        d.isAfter(today.minusDays(7)) -> ChatGroup.WEEK
        d.isAfter(today.minusDays(30)) -> ChatGroup.MONTH
        else -> ChatGroup.OLDER
    }
}

@Composable
fun ChatDrawer(
    conversations: List<ConversationEntity>,
    currentId: String?,
    search: String,
    searchMatches: Set<String>,
    onSearch: (String) -> Unit,
    onOpen: (String) -> Unit,
    onNewChat: () -> Unit,
    onNewIncognito: () -> Unit,
    onRename: (ConversationEntity) -> Unit,
    onTogglePin: (ConversationEntity) -> Unit,
    onExport: (ConversationEntity) -> Unit,
    onDelete: (ConversationEntity) -> Unit,
    onSettings: () -> Unit,
    onMemory: () -> Unit,
    onAccount: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val h = LocalHaptics.current
    val today = remember { LocalDate.now() }
    val filtered = remember(conversations, search, searchMatches) {
        val q = search.trim()
        conversations.filter { c ->
            (c.title.isNotBlank() || c.lastMessageAt > c.createdAt) &&
                (q.isEmpty() || c.title.contains(q, ignoreCase = true) || c.id in searchMatches)
        }
    }
    val grouped = remember(filtered) { filtered.groupBy { groupOf(it, today) }.toSortedMap(compareBy { it.ordinal }) }

    ModalDrawerSheet(drawerContainerColor = cs.surfaceContainerLow, modifier = Modifier.widthIn(max = 340.dp)) {
        Column(Modifier.fillMaxHeight()) {
            Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                KralaLogo(size = 30.dp)
                Text("krala", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 10.dp).weight(1f))
                IconButton(onClick = { h.click(); onNewIncognito() }) { Icon(Icons.Rounded.VisibilityOff, stringResource(R.string.new_incognito)) }
            }
            OutlinedTextField(
                value = search,
                onValueChange = onSearch,
                placeholder = { Text(stringResource(R.string.search_chats)) },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = if (search.isNotEmpty()) ({ IconButton(onClick = { onSearch("") }) { Icon(Icons.Rounded.Close, stringResource(R.string.clear)) } }) else null,
                singleLine = true,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
            )
            FilledTonalButton(
                onClick = { h.click(); onNewChat() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.new_chat))
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 8.dp)) {
                if (grouped.isEmpty()) item {
                    Text(
                        stringResource(if (search.isBlank()) R.string.no_chats_yet else R.string.no_matches),
                        style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
                grouped.forEach { (group, list) ->
                    item(key = "h-" + group.name) {
                        Text(
                            stringResource(group.label), style = MaterialTheme.typography.labelMedium, color = cs.primary,
                            modifier = Modifier.padding(start = 28.dp, top = 14.dp, bottom = 4.dp).animateItem(),
                        )
                    }
                    items(list, key = { it.id }) { c ->
                        DrawerChatRow(
                            c, selected = c.id == currentId,
                            onOpen = { onOpen(c.id) }, onRename = { onRename(c) }, onTogglePin = { onTogglePin(c) },
                            onExport = { onExport(c) }, onDelete = { onDelete(c) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
            HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.5f))
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                DrawerFooterButton(Icons.Rounded.Psychology, stringResource(R.string.memory)) { onMemory() }
                DrawerFooterButton(Icons.Rounded.AccountCircle, stringResource(R.string.account)) { onAccount() }
                DrawerFooterButton(Icons.Rounded.Settings, stringResource(R.string.settings)) { onSettings() }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun DrawerFooterButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val h = LocalHaptics.current
    TextButton(onClick = { h.click(); onClick() }) {
        Icon(icon, null, Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, maxLines = 1)
    }
}

@Composable
private fun DrawerChatRow(
    c: ConversationEntity,
    selected: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val h = LocalHaptics.current
    var menu by remember { mutableStateOf(false) }
    Box(modifier.padding(horizontal = 12.dp, vertical = 1.dp)) {
        Surface(
            shape = CircleShape,
            color = if (selected) cs.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent,
            modifier = Modifier.fillMaxWidth().clip(CircleShape).combinedClickable(
                onClick = { h.click(); onOpen() },
                onLongClick = { h.long(); menu = true },
            ),
        ) {
            Row(Modifier.padding(start = 16.dp, end = 4.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                if (c.incognito) Icon(Icons.Rounded.VisibilityOff, null, Modifier.size(16.dp).padding(end = 2.dp), tint = cs.onSurfaceVariant)
                if (c.pinned) Icon(Icons.Rounded.PushPin, null, Modifier.size(16.dp), tint = cs.primary)
                Text(
                    c.title.ifBlank { stringResource(R.string.untitled) },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (selected) cs.onSecondaryContainer else cs.onSurface,
                    modifier = Modifier.weight(1f).padding(start = if (c.pinned || c.incognito) 8.dp else 0.dp),
                )
                IconButton(onClick = { h.click(); menu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.MoreVert, stringResource(R.string.more), Modifier.size(18.dp), tint = cs.onSurfaceVariant)
                }
            }
        }
        DropdownMenu(menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, null) }, onClick = { menu = false; onRename() })
            DropdownMenuItem(
                text = { Text(stringResource(if (c.pinned) R.string.unpin else R.string.pin)) },
                leadingIcon = { Icon(Icons.Rounded.PushPin, null) }, onClick = { menu = false; onTogglePin() },
            )
            DropdownMenuItem(text = { Text(stringResource(R.string.export)) }, leadingIcon = { Icon(Icons.Rounded.IosShare, null) }, onClick = { menu = false; onExport() })
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete), color = cs.error) },
                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = cs.error) }, onClick = { menu = false; onDelete() },
            )
        }
    }
}
