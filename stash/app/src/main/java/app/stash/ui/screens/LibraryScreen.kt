package app.stash.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.stash.ui.LocalActions
import app.stash.ui.LocalNav
import app.stash.ui.Routes
import app.stash.ui.components.Artwork
import app.stash.ui.components.EmptyState
import app.stash.ui.components.NewPlaylistDialog
import app.stash.ui.components.TrackRow
import kotlinx.coroutines.launch

private enum class LibFilter(val label: String) { PLAYLISTS("playlists"), SONGS("songs"), ALBUMS("albums"), ARTISTS("artists"), DOWNLOADED("downloaded") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen() {
    val actions = LocalActions.current
    val nav = LocalNav.current
    val dao = actions.library.tracks
    val scope = rememberCoroutineScope()
    var filter by rememberSaveable { mutableStateOf(LibFilter.PLAYLISTS) }
    var creating by remember { mutableStateOf(false) }
    val playlists by actions.playlists.collectAsState()
    val songs by remember { dao.observeLibrary() }.collectAsState(initial = emptyList())
    val liked by remember { dao.observeLiked() }.collectAsState(initial = emptyList())
    val albums by remember { dao.observeAlbums() }.collectAsState(initial = emptyList())
    val artists by remember { dao.observeArtists() }.collectAsState(initial = emptyList())
    val downloaded by remember { dao.observeDownloaded() }.collectAsState(initial = emptyList())
    val current by actions.player.state.collectAsState()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("your library") },
                actions = {
                    IconButton(onClick = { nav.navigate(Routes.import()) }) { Icon(Icons.Rounded.SwapHoriz, "import playlists") }
                    IconButton(onClick = { creating = true }) { Icon(Icons.Rounded.Add, "new playlist") }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 24.dp)) {
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LibFilter.entries) { f ->
                        FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
                    }
                }
                Spacer(Modifier.size(8.dp))
            }

            when (filter) {
                LibFilter.PLAYLISTS -> {
                    item {
                        LibraryRow(
                            title = "liked songs", subtitle = "${liked.size} songs", art = null,
                            onClick = { nav.navigate(Routes.LIKED) },
                        ) { PinnedIcon(Icons.Rounded.Favorite) }
                    }
                    items(playlists, key = { it.id }) { p ->
                        val sub = buildList {
                            add("playlist")
                            add("${p.trackCount} songs")
                            if (p.downloadedCount > 0) add(if (p.downloadedCount == p.trackCount) "downloaded" else "${p.downloadedCount} offline")
                            if (p.origin != "local") add("from ${p.origin}")
                        }.joinToString(" · ")
                        LibraryRow(p.name, sub, p.coverUrl ?: p.firstArtwork, onClick = { nav.navigate(Routes.playlist(p.id)) })
                    }
                    if (playlists.isEmpty()) item {
                        EmptyState(Icons.AutoMirrored.Rounded.QueueMusic, "no playlists yet", "make one, or bring yours over from spotify or apple music.") {
                            TextButton(onClick = { nav.navigate(Routes.import()) }) { Text("import playlists") }
                        }
                    }
                }

                LibFilter.SONGS -> {
                    if (songs.isEmpty()) item {
                        EmptyState(Icons.Rounded.LibraryMusic, "no saved songs", "long-press any song and pick \"save to library\".")
                    } else item {
                        Row(Modifier.padding(horizontal = 12.dp)) {
                            TextButton(onClick = { actions.shuffle(songs) }) {
                                Icon(Icons.Rounded.Shuffle, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("shuffle all")
                            }
                        }
                    }
                    itemsIndexed(songs, key = { _, t -> t.id }) { i, t ->
                        TrackRow(t, onClick = { actions.play(songs, i) }, isCurrent = current.currentId == t.id)
                    }
                }

                LibFilter.ALBUMS -> {
                    if (albums.isEmpty()) item { EmptyState(Icons.Rounded.Album, "no albums", "songs with album info show up here, grouped.") }
                    items(albums, key = { "${it.album}|${it.artist}" }) { a ->
                        LibraryRow(a.album, "album · ${a.artist}", a.artworkUrl, onClick = { nav.navigate(Routes.album(a.album, a.artist)) })
                    }
                }

                LibFilter.ARTISTS -> {
                    if (artists.isEmpty()) item { EmptyState(Icons.Rounded.Person, "no artists", "save some songs and their artists land here.") }
                    items(artists, key = { it.artist }) { a ->
                        LibraryRow(a.artist, "${a.trackCount} songs", a.artworkUrl, circle = true, onClick = { nav.navigate(Routes.artist(a.artist)) })
                    }
                }

                LibFilter.DOWNLOADED -> {
                    if (downloaded.isEmpty()) item {
                        EmptyState(Icons.Rounded.DownloadDone, "nothing offline yet", "hit download on a song, album or playlist.")
                    } else item {
                        Row(Modifier.padding(horizontal = 12.dp)) {
                            TextButton(onClick = { actions.shuffle(downloaded) }) {
                                Icon(Icons.Rounded.Shuffle, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("shuffle offline")
                            }
                            TextButton(onClick = { nav.navigate(Routes.DOWNLOADS) }) { Text("manage downloads") }
                        }
                    }
                    itemsIndexed(downloaded, key = { _, t -> t.id }) { i, t ->
                        TrackRow(t, onClick = { actions.play(downloaded, i) }, isCurrent = current.currentId == t.id)
                    }
                }
            }
        }
    }

    if (creating) NewPlaylistDialog(
        onDismiss = { creating = false },
        onCreate = { name -> scope.launch { nav.navigate(Routes.playlist(actions.library.createPlaylist(name))) } },
    )
}

@Composable
private fun PinnedIcon(icon: ImageVector) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small, modifier = Modifier.size(56.dp)) {
        Icon(icon, null, Modifier.padding(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
fun LibraryRow(
    title: String,
    subtitle: String,
    art: Any?,
    onClick: () -> Unit,
    circle: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = leading ?: {
            Artwork(art, Modifier.size(56.dp), if (circle) CircleShape else MaterialTheme.shapes.small,
                if (circle) Icons.Rounded.Person else Icons.AutoMirrored.Rounded.QueueMusic)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 4.dp),
    )
}
