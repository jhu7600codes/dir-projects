package app.krala.ui.providers

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.krala.R
import app.krala.data.provider.Presets
import app.krala.data.provider.Provider
import app.krala.ui.settings.BackTopBar
import app.krala.ui.theme.LocalHaptics
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun ProvidersScreen(vm: ProvidersViewModel, onBack: () -> Unit, onEdit: (String) -> Unit, onAdd: (String?) -> Unit) {
    val providers by vm.providers.collectAsStateWithLifecycle()
    val h = LocalHaptics.current
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var order by remember { mutableStateOf(providers) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(providers) { if (!dragging) order = providers }
    var showPresets by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIdx = order.indexOfFirst { it.id == from.key }
        val toIdx = order.indexOfFirst { it.id == to.key }
        if (fromIdx >= 0 && toIdx >= 0) {
            order = order.toMutableList().apply { add(toIdx, removeAt(fromIdx)) }
            h.tick()
        }
    }

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.providers), onBack, scroll) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { h.click(); showPresets = true }, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text(stringResource(R.string.add_provider)) })
        },
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
    ) { pad ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 96.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "hint") {
                Text(
                    stringResource(R.string.providers_hint), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            items(order, key = { it.id }) { p ->
                ReorderableItem(reorderState, key = p.id) { isDragging ->
                    val handleModifier = Modifier.draggableHandle(
                        onDragStarted = { dragging = true; h.long() },
                        onDragStopped = { dragging = false; vm.reorder(order.map { it.id }); h.confirm() },
                    )
                    ProviderCard(
                        p = p,
                        index = order.indexOf(p),
                        dragging = isDragging,
                        onClick = { onEdit(p.id) },
                        onToggle = { vm.setEnabled(p.id, it) },
                        handleModifier = handleModifier,
                    )
                }
            }
        }
    }

    if (showPresets) {
        ModalBottomSheet(onDismissRequest = { showPresets = false }) {
            Text(stringResource(R.string.add_provider), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                items(Presets.all) { pr ->
                    ListItem(
                        headlineContent = { Text(pr.name) },
                        supportingContent = { Text(if (pr.needsKey) pr.keyHint else stringResource(R.string.no_key_needed), maxLines = 2) },
                        leadingContent = { Icon(if (pr.needsKey) Icons.Rounded.Key else Icons.Rounded.AutoAwesome, null) },
                        modifier = Modifier.clickable { showPresets = false; onAdd(pr.id) },
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.custom_provider)) },
                        supportingContent = { Text(stringResource(R.string.custom_provider_hint)) },
                        leadingContent = { Icon(Icons.Rounded.Tune, null) },
                        modifier = Modifier.clickable { showPresets = false; onAdd(null) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(p: Provider, index: Int, dragging: Boolean, onClick: () -> Unit, onToggle: (Boolean) -> Unit, handleModifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val now = System.currentTimeMillis()
    val limited = p.entity.rateLimitedUntil > now
    ElevatedCard(
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(containerColor = if (dragging) cs.secondaryContainer else cs.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (dragging) 8.dp else 1.dp),
        modifier = Modifier.fillMaxWidth().graphicsLayer { if (dragging) { scaleX = 1.02f; scaleY = 1.02f } }.animateContentSize(),
    ) {
        Row(Modifier.padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.DragHandle, stringResource(R.string.reorder), modifier = handleModifier.padding(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = cs.primary, modifier = Modifier.padding(end = 8.dp))
                    Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                }
                Text(
                    (p.firstModel ?: stringResource(R.string.no_model)) + " · " + stringResource(R.string.models_count, p.models.size),
                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1,
                )
                val status = when {
                    limited -> stringResource(R.string.rate_limited_for, ((p.entity.rateLimitedUntil - now) / 1000).coerceAtLeast(1))
                    p.entity.lastError != null && now - p.entity.lastErrorAt < 3_600_000 -> p.entity.lastError
                    p.entity.lastLatencyMs > 0 -> stringResource(R.string.last_latency, p.entity.lastLatencyMs) +
                        (p.entity.remainingRequests?.let { " · " + stringResource(R.string.remaining_requests, it) } ?: "")
                    else -> null
                }
                status?.let {
                    Text(
                        it, style = MaterialTheme.typography.labelSmall, maxLines = 2,
                        color = if (limited || p.entity.lastError != null && now - p.entity.lastErrorAt < 3_600_000) cs.error else cs.outline,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Switch(p.entity.enabled, onCheckedChange = onToggle)
        }
    }
}
