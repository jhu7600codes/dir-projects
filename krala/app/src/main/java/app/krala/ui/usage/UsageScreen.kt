package app.krala.ui.usage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.krala.R
import app.krala.data.db.UsageDao
import app.krala.data.db.UsageEntity
import app.krala.ui.components.EmptyState
import app.krala.ui.settings.BackTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class UsageViewModel @Inject constructor(private val dao: UsageDao) : ViewModel() {
    val today: Long = LocalDate.now().toEpochDay()
    val usage: StateFlow<List<UsageEntity>> = dao.observeSince(today - 29).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    fun clear() = viewModelScope.launch { dao.clear() }
}

data class ProviderStats(
    val id: String, val name: String, val requests: Int, val successes: Int, val failures: Int, val rateLimits: Int,
    val promptTokens: Long, val completionTokens: Long, val avgLatency: Long, val avgFirstToken: Long, val perDay: Map<Long, Int>,
)

@Composable
fun UsageScreen(vm: UsageViewModel, onBack: () -> Unit) {
    val usage by vm.usage.collectAsStateWithLifecycle()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var confirm by remember { mutableStateOf(false) }
    val stats = remember(usage) {
        usage.groupBy { it.providerId }.map { (id, rows) ->
            val ok = rows.sumOf { it.successes }
            ProviderStats(
                id, rows.last().providerName, rows.sumOf { it.requests }, ok, rows.sumOf { it.failures }, rows.sumOf { it.rateLimits },
                rows.sumOf { it.promptTokens }, rows.sumOf { it.completionTokens },
                if (ok > 0) rows.sumOf { it.totalLatencyMs } / ok else 0, if (ok > 0) rows.sumOf { it.firstTokenMs } / ok else 0,
                rows.associate { it.day to it.requests },
            )
        }.sortedByDescending { it.requests }
    }
    Scaffold(
        topBar = {
            BackTopBar(stringResource(R.string.usage), onBack, scroll) {
                IconButton(onClick = { confirm = true }, enabled = usage.isNotEmpty()) { Icon(Icons.Rounded.DeleteSweep, stringResource(R.string.clear)) }
            }
        },
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
    ) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 32.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text(stringResource(R.string.usage_explainer), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp)) }
            if (stats.isEmpty()) item { EmptyState(stringResource(R.string.usage_empty), stringResource(R.string.usage_empty_body)) }
            items(stats, key = { it.id }) { s -> StatsCard(s, vm.today) }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.clear_usage_q)) },
            confirmButton = { TextButton(onClick = { confirm = false; vm.clear() }) { Text(stringResource(R.string.clear)) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun StatsCard(s: ProviderStats, today: Long) {
    val cs = MaterialTheme.colorScheme
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = cs.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(s.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(stringResource(R.string.stat_requests), s.requests.toString())
                Stat(stringResource(R.string.stat_success), if (s.requests > 0) "${s.successes * 100 / s.requests}%" else "–")
                Stat(stringResource(R.string.stat_rate_limits), s.rateLimits.toString())
                Stat(stringResource(R.string.stat_latency), if (s.avgLatency > 0) "${s.avgLatency / 100 / 10.0}s" else "–")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.stat_tokens, s.promptTokens, s.completionTokens) +
                    if (s.avgFirstToken > 0) " · " + stringResource(R.string.stat_first_token, s.avgFirstToken) else "",
                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            val max = (s.perDay.values.maxOrNull() ?: 1).coerceAtLeast(1)
            val bar = cs.primary
            val track = cs.surfaceContainerHighest
            Canvas(Modifier.fillMaxWidth().height(56.dp)) {
                val days = 30
                val w = size.width / days
                for (i in 0 until days) {
                    val day = today - (days - 1 - i)
                    val v = s.perDay[day] ?: 0
                    val hgt = if (v == 0) 3f else (size.height * v / max).coerceAtLeast(6f)
                    drawRoundRect(
                        color = if (v == 0) track else bar,
                        topLeft = Offset(i * w + w * 0.15f, size.height - hgt),
                        size = Size(w * 0.7f, hgt),
                        cornerRadius = CornerRadius(w * 0.3f),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.days_ago_30), style = MaterialTheme.typography.labelSmall, color = cs.outline)
                Text(stringResource(R.string.group_today), style = MaterialTheme.typography.labelSmall, color = cs.outline)
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
