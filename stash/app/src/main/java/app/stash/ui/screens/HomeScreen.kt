package app.stash.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.stash.greet.GreetingAction
import app.stash.greet.GreetingViewModel
import app.stash.ui.LocalActions
import app.stash.ui.LocalNav
import app.stash.ui.Routes
import app.stash.ui.components.Artwork
import app.stash.ui.components.CoverCard
import app.stash.ui.components.EmptyState
import app.stash.ui.components.SectionHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val actions = LocalActions.current
    val nav = LocalNav.current
    val dao = actions.library.tracks
    val recent by remember { dao.observeRecent(20) }.collectAsState(initial = emptyList())
    val mostPlayed by remember { dao.observeMostPlayed(15) }.collectAsState(initial = emptyList())
    val downloaded by remember { dao.observeDownloaded() }.collectAsState(initial = emptyList())
    val liked by remember { dao.observeLiked() }.collectAsState(initial = emptyList())
    val playlists by actions.playlists.collectAsState()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val greetingVm: GreetingViewModel = hiltViewModel()
    val greeting by greetingVm.greeting.collectAsState()
    // re-check on every visit (the place lookup itself is cached), e.g. right after turning it on
    LaunchedEffect(Unit) { greetingVm.refresh() }
    val scope = rememberCoroutineScope()

    fun runAction(a: GreetingAction) {
        scope.launch {
            when (a) {
                is GreetingAction.PlayArtist -> actions.shuffle(dao.byArtist(a.artist))
                GreetingAction.OpenDownloads -> nav.navigate(Routes.DOWNLOADS)
                GreetingAction.ShuffleLiked -> actions.shuffle(dao.liked())
                GreetingAction.ShuffleDownloads -> actions.shuffle(dao.downloaded())
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(greeting.title) },
                actions = {
                    IconButton(onClick = { nav.navigate(Routes.DOWNLOADS) }) { Icon(Icons.Rounded.DownloadForOffline, "downloads") }
                    IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) { Icon(Icons.Rounded.Settings, "settings") }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        val empty = recent.isEmpty() && playlists.isEmpty() && downloaded.isEmpty() && liked.isEmpty()
        LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 24.dp)) {
            greeting.message?.let { msg ->
                item(key = "greeting") { GreetingCard(msg, greeting.action?.takeUnless { empty }, ::runAction) }
            }
            if (empty) {
                item {
                    EmptyState(
                        Icons.Rounded.Search,
                        "your stash is empty",
                        "search for something to play, or bring your playlists over from spotify or apple music.",
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { nav.navigate(Routes.SEARCH) }) { Text("search") }
                            OutlinedButton(onClick = { nav.navigate(Routes.import()) }) { Text("import playlists") }
                        }
                    }
                }
                return@LazyColumn
            }

            // spotify-style quick picks: a two column grid of compact tiles
            item {
                val tiles = buildList {
                    if (liked.isNotEmpty()) add(QuickTile("liked songs", null, Icons.Rounded.Favorite) { nav.navigate(Routes.LIKED) })
                    if (downloaded.isNotEmpty()) add(QuickTile("downloads", null, Icons.Rounded.Download) { nav.navigate(Routes.DOWNLOADS) })
                    playlists.take(8 - size).forEach { p ->
                        add(QuickTile(p.name, p.coverUrl ?: p.firstArtwork, null) { nav.navigate(Routes.playlist(p.id)) })
                    }
                }
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tiles.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { QuickPick(it, Modifier.weight(1f)) }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            if (recent.isNotEmpty()) {
                item { SectionHeader("recently played") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        itemsIndexed(recent, key = { _, t -> t.id }) { i, t ->
                            CoverCard(t.title, t.artist, t.artworkUrl, onClick = { actions.play(recent, i) })
                        }
                    }
                }
            }

            if (playlists.isNotEmpty()) {
                item { SectionHeader("your playlists") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(playlists, key = { it.id }) { p ->
                            CoverCard(p.name, "${p.trackCount} songs", p.coverUrl ?: p.firstArtwork, onClick = { nav.navigate(Routes.playlist(p.id)) })
                        }
                    }
                }
            }

            if (mostPlayed.size >= 3) {
                item { SectionHeader("on repeat") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        itemsIndexed(mostPlayed, key = { _, t -> t.id }) { i, t ->
                            CoverCard(t.title, "${t.playCount} plays", t.artworkUrl, onClick = { actions.play(mostPlayed, i) }, size = 120.dp)
                        }
                    }
                }
            }

            if (downloaded.isNotEmpty()) {
                item {
                    SectionHeader("ready offline") {
                        IconButton(onClick = { actions.shuffle(downloaded) }) { Icon(Icons.Rounded.Shuffle, "shuffle downloads") }
                    }
                }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        itemsIndexed(downloaded.take(30), key = { _, t -> t.id }) { i, t ->
                            CoverCard(t.title, t.artist, t.artworkUrl, onClick = { actions.play(downloaded, i) }, size = 120.dp)
                        }
                    }
                }
            }
        }
    }
}

private data class QuickTile(val title: String, val art: Any?, val icon: ImageVector?, val onClick: () -> Unit)

@Composable
private fun QuickPick(tile: QuickTile, modifier: Modifier) {
    Surface(
        onClick = tile.onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.height(56.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (tile.icon != null) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(56.dp)) {
                    Icon(tile.icon, null, Modifier.padding(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            } else {
                Artwork(tile.art, Modifier.size(56.dp), RectangleShape)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                tile.title, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
        }
    }
}

@Composable
private fun GreetingCard(message: String, action: GreetingAction?, onAction: (GreetingAction) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
    ) {
        Row(Modifier.padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f).padding(vertical = 10.dp),
            )
            if (action != null) TextButton(onClick = { onAction(action) }) { Text(action.label, maxLines = 1) }
        }
    }
}
