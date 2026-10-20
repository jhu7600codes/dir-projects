package app.stash.ui.screens

import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stash.data.db.DownloadState
import app.stash.ui.LocalActions
import app.stash.ui.LocalNav
import app.stash.ui.components.Artwork
import app.stash.ui.components.EmptyState
import app.stash.ui.components.SectionHeader
import app.stash.ui.components.TrackRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen() {
    val actions = LocalActions.current
    val nav = LocalNav.current
    val context = LocalContext.current
    val dao = actions.library.tracks
    val queue by remember { dao.observeDownloadQueue() }.collectAsState(initial = emptyList())
    val done by remember { dao.observeDownloaded() }.collectAsState(initial = emptyList())
    val settings by actions.settings.collectAsState()
    val current by actions.player.state.collectAsState()
    val used = remember(done.size) { actions.downloads.usedBytes() }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("downloads") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "back") } },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 24.dp)) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("${done.size} songs offline", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${Formatter.formatShortFileSize(context, used)} used on this phone",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Wifi, null, Modifier.size(20.dp))
                            Text("download on wi-fi only", Modifier.weight(1f).padding(start = 10.dp))
                            Switch(
                                checked = settings?.wifiOnlyDownloads == true,
                                onCheckedChange = { actions.downloads.setWifiOnly(it) },
                            )
                        }
                    }
                }
            }

            if (queue.isNotEmpty()) {
                item {
                    SectionHeader("queue (${queue.size})") {
                        if (queue.any { it.downloadState == DownloadState.FAILED }) {
                            TextButton(onClick = { actions.downloads.retryAllFailed() }) { Text("retry failed") }
                        }
                        TextButton(onClick = { actions.downloads.cancelAll() }) { Text("cancel all") }
                    }
                }
                items(queue, key = { "q:${it.id}" }) { t ->
                    ListItem(
                        headlineContent = { Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = {
                            Column {
                                Text(
                                    when (t.downloadState) {
                                        DownloadState.DOWNLOADING -> "${(t.downloadProgress * 100).toInt()}% · ${t.artist}"
                                        DownloadState.FAILED -> "failed: ${t.downloadError ?: "unknown error"}"
                                        else -> if (t.downloadError != null) "retrying soon · ${t.downloadError}" else
                                            if (settings?.wifiOnlyDownloads == true) "waiting for wi-fi · ${t.artist}" else "waiting · ${t.artist}"
                                    },
                                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    color = if (t.downloadState == DownloadState.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (t.downloadState == DownloadState.DOWNLOADING) {
                                    Spacer(Modifier.height(6.dp))
                                    LinearProgressIndicator(progress = { t.downloadProgress }, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        },
                        leadingContent = { Artwork(t.artworkUrl, Modifier.size(52.dp)) },
                        trailingContent = {
                            Row {
                                if (t.downloadState == DownloadState.FAILED) {
                                    IconButton(onClick = { actions.downloads.retry(t) }) { Icon(Icons.Rounded.Refresh, "retry") }
                                }
                                IconButton(onClick = { actions.downloads.cancel(t) }) { Icon(Icons.Rounded.Close, "cancel") }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }

            item { SectionHeader("downloaded") }
            if (done.isEmpty()) item {
                EmptyState(Icons.Rounded.DownloadForOffline, "nothing here yet", "downloaded songs play without internet and never buffer.")
            }
            itemsIndexed(done, key = { _, t -> "d:${t.id}" }) { i, t ->
                TrackRow(t, onClick = { actions.play(done, i) }, isCurrent = current.currentId == t.id)
            }
        }
    }
}
