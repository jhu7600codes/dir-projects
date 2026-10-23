package app.stash.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stash.data.db.DownloadState
import app.stash.data.db.LIKED_PLAYLIST_ID
import app.stash.data.db.Track
import app.stash.data.db.UnmatchedTrack
import app.stash.ui.LocalActions
import app.stash.ui.LocalNav
import app.stash.ui.Routes
import app.stash.ui.components.Artwork
import app.stash.ui.components.LocalExtraTrackActions
import app.stash.ui.components.NewPlaylistDialog
import app.stash.ui.components.SheetAction
import app.stash.ui.components.TrackRow
import app.stash.ui.components.formatDuration

data class MenuEntry(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/**
 * shared layout for playlists, albums, artists, liked songs: a big centered cover
 * (google play music style) with tonal download / shuffle buttons and a morphing play button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    title: String,
    kind: String,
    tracks: List<Track>,
    artwork: Any?,
    artShape: Shape = MaterialTheme.shapes.extraLarge,
    artIcon: ImageVector = Icons.AutoMirrored.Rounded.QueueMusic,
    menu: List<MenuEntry> = emptyList(),
    extraTrackActions: (index: Int, Track) -> List<SheetAction> = { _, _ -> emptyList() },
    numbered: Boolean = false,
    /** where each track sits in the source playlist, so [missing] songs can be shown in their spot */
    positions: List<Int>? = null,
    missing: List<UnmatchedTrack> = emptyList(),
) {
    val actions = LocalActions.current
    val nav = LocalNav.current
    val current by actions.player.state.collectAsState()
    val list = rememberLazyListState()
    val scrolled by remember { derivedStateOf { list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 600 } }
    val titleAlpha by animateFloatAsState(if (scrolled) 1f else 0f, label = "title")
    var showMenu by remember { mutableStateOf(false) }

    val playingHere = tracks.any { it.id == current.currentId }
    val downloaded = tracks.count { it.downloadState == DownloadState.DONE }
    val downloading = tracks.any { it.downloadState == DownloadState.QUEUED || it.downloadState == DownloadState.DOWNLOADING }
    val totalMs = tracks.sumOf { it.durationMs }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, Modifier.alpha(titleAlpha), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "back") } },
                actions = {
                    if (menu.isNotEmpty()) Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Rounded.MoreVert, "more") }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            menu.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.label) }, leadingIcon = { Icon(m.icon, null) },
                                    onClick = { showMenu = false; m.onClick() },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (scrolled) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
                ),
            )
        },
    ) { padding ->
        val rows = remember(tracks, positions, missing) { mergeRows(tracks, positions, missing) }
            LazyColumn(state = list, contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 24.dp)) {
                item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Artwork(artwork, Modifier.size(232.dp), artShape, artIcon)
                        Spacer(Modifier.height(20.dp))
                        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            buildList {
                                add(kind)
                                add("${tracks.size} songs")
                                if (totalMs > 0) add(formatDuration(totalMs))
                                if (downloaded > 0) add("$downloaded offline")
                            }.joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilledTonalIconButton(
                                onClick = {
                                    when {
                                        tracks.isEmpty() -> Unit
                                        downloaded == tracks.size -> actions.toast("all downloaded")
                                        else -> actions.download(tracks)
                                    }
                                },
                                modifier = Modifier.size(52.dp),
                            ) {
                                when {
                                    tracks.isNotEmpty() && downloaded == tracks.size -> Icon(Icons.Rounded.DownloadDone, "downloaded")
                                    downloading -> CircularProgressIndicator(
                                        progress = { if (tracks.isEmpty()) 0f else downloaded.toFloat() / tracks.size },
                                        modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp,
                                    )
                                    else -> Icon(Icons.Rounded.Download, "download all")
                                }
                            }
                            FilledIconButton(
                                onClick = { if (playingHere) actions.player.togglePlay() else actions.play(tracks) },
                                modifier = Modifier.size(width = 104.dp, height = 64.dp),
                                shape = MaterialTheme.shapes.large,
                                enabled = tracks.isNotEmpty(),
                            ) {
                                Icon(
                                    if (playingHere && current.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    "play", Modifier.size(34.dp),
                                )
                            }
                            FilledTonalIconButton(
                                onClick = { actions.shuffle(tracks) }, modifier = Modifier.size(52.dp), enabled = tracks.isNotEmpty(),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(),
                            ) { Icon(Icons.Rounded.Shuffle, "shuffle") }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
                if (missing.isNotEmpty()) item(key = "missing-note") {
                    Text(
                        "${missing.size} couldn't be found, they're greyed out below. tap one to search for it.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
                items(rows, key = { r -> if (r is CollectionRow.Song) "s:${r.index}" else "u:${(r as CollectionRow.Missing).u.id}" }) { r ->
                    when (r) {
                        is CollectionRow.Song -> {
                            val t = tracks[r.index]
                            CompositionLocalProvider(LocalExtraTrackActions provides { extraTrackActions(r.index, it) }) {
                                TrackRow(
                                    t, onClick = { actions.play(tracks, r.index) }, isCurrent = current.currentId == t.id,
                                    index = r.index, showArt = !numbered,
                                )
                            }
                        }
                        is CollectionRow.Missing -> MissingRow(r.u)
                    }
                }
            }
    }
}

@Composable
fun PlaylistScreen(id: Long) {
    val actions = LocalActions.current
    val nav = LocalNav.current
    val dao = actions.library.playlists
    val playlist by remember(id) { dao.observe(id) }.collectAsState(initial = null)
    val entries by remember(id) { dao.observeEntries(id) }.collectAsState(initial = emptyList())
    val unmatched by remember(id) { dao.observeUnmatched(id) }.collectAsState(initial = emptyList())
    val tracks = remember(entries) { entries.map { it.track } }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val p = playlist ?: return

    CollectionScreen(
        title = p.name,
        kind = when (p.origin) {
            "local" -> "playlist"
            "mix" -> "made for you"
            else -> "from ${p.origin}"
        },
        tracks = tracks,
        positions = entries.map { it.position },
        missing = unmatched,
        artwork = p.coverUrl ?: tracks.firstOrNull()?.artworkUrl,
        menu = listOf(
            MenuEntry("rename", Icons.Rounded.Edit) { renaming = true },
            MenuEntry("remove downloads", Icons.Rounded.RemoveCircleOutline) { actions.downloads.delete(tracks.filter { it.isDownloaded }) },
            MenuEntry("delete playlist", Icons.Rounded.Delete) { deleting = true },
        ),
        extraTrackActions = { i, _ ->
            listOf(SheetAction("remove from this playlist", Icons.Rounded.RemoveCircleOutline) {
                actions.library.removeFromPlaylist(id, entries[i].position)
            })
        },
    )

    if (renaming) NewPlaylistDialog(
        title = "rename playlist", initial = p.name,
        onDismiss = { renaming = false },
        onCreate = { actions.library.rename(id, it) },
    )
    if (deleting) AlertDialog(
        onDismissRequest = { deleting = false },
        title = { Text("delete ${p.name}?") },
        text = { Text("the songs stay in your library and downloads aren't touched.") },
        confirmButton = {
            TextButton(onClick = { deleting = false; actions.library.deletePlaylist(id); nav.popBackStack() }) { Text("delete") }
        },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("cancel") } },
    )
}

@Composable
fun AlbumScreen(name: String, artist: String) {
    val dao = LocalActions.current.library.tracks
    val tracks by remember(name, artist) { dao.observeAlbum(name, artist) }.collectAsState(initial = emptyList())
    val nav = LocalNav.current
    CollectionScreen(
        title = name, kind = "album · $artist", tracks = tracks,
        artwork = tracks.firstNotNullOfOrNull { it.artworkUrl }, numbered = true,
        menu = listOf(MenuEntry("go to artist", Icons.Rounded.Person) { nav.navigate(Routes.artist(artist)) }),
    )
}

@Composable
fun ArtistScreen(name: String) {
    val dao = LocalActions.current.library.tracks
    val tracks by remember(name) { dao.observeArtist(name) }.collectAsState(initial = emptyList())
    CollectionScreen(
        title = name, kind = "artist", tracks = tracks,
        artwork = tracks.firstNotNullOfOrNull { it.artworkUrl }, artShape = CircleShape, artIcon = Icons.Rounded.Person,
    )
}

@Composable
fun LikedScreen() {
    val library = LocalActions.current.library
    val tracks by remember { library.tracks.observeLiked() }.collectAsState(initial = emptyList())
    val unmatched by remember { library.playlists.observeUnmatched(LIKED_PLAYLIST_ID) }.collectAsState(initial = emptyList())
    CollectionScreen(
        title = "liked songs", kind = "playlist", tracks = tracks, artwork = null, artIcon = Icons.Rounded.Favorite,
        missing = unmatched,
    )
}

private sealed interface CollectionRow {
    data class Song(val index: Int) : CollectionRow
    data class Missing(val u: UnmatchedTrack) : CollectionRow
}

/** songs and not-found songs in source order; without positions the missing ones go last */
private fun mergeRows(tracks: List<Track>, positions: List<Int>?, missing: List<UnmatchedTrack>): List<CollectionRow> {
    if (positions == null) return tracks.indices.map { CollectionRow.Song(it) } + missing.map { CollectionRow.Missing(it) }
    return (tracks.indices.map { positions[it] to CollectionRow.Song(it) } + missing.map { it.position to CollectionRow.Missing(it) })
        .sortedBy { it.first }
        .map { it.second }
}

@Composable
private fun MissingRow(u: UnmatchedTrack) {
    val actions = LocalActions.current
    val nav = LocalNav.current
    ListItem(
        headlineContent = { Text(u.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(listOfNotNull(u.artist, "not found", formatDuration(u.durationMs).ifEmpty { null }).joinToString(" · "),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.HelpOutline, null, tint = MaterialTheme.colorScheme.outline)
            }
        },
        trailingContent = {
            IconButton(onClick = { actions.library.dismissUnmatched(u.id) }) { Icon(Icons.Rounded.Close, "remove") }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            supportingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
        ),
        modifier = Modifier.clickable { nav.navigate(Routes.fix(u.id, "${u.artist} ${u.title}")) },
    )
}
