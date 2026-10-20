package app.stash.ui.screens

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.stash.R
import app.stash.data.prefs.Settings
import app.stash.importer.AppleMusic
import app.stash.importer.ImportManager
import app.stash.importer.RemotePlaylistInfo
import app.stash.importer.SourcePlaylist
import app.stash.importer.Spotify
import app.stash.ui.LocalActions
import app.stash.ui.LocalNav
import app.stash.ui.Routes
import app.stash.ui.components.Artwork
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    val manager: ImportManager,
    private val spotify: Spotify,
    private val apple: AppleMusic,
    private val settings: Settings,
) : ViewModel() {
    val spotifyPlaylists = MutableStateFlow<List<RemotePlaylistInfo>?>(null)
    val error = MutableStateFlow<String?>(null)

    fun loginSpotify(context: Context) = viewModelScope.launch {
        error.value = null
        runCatching { spotify.authorizeUri() }
            .onSuccess { CustomTabsIntent.Builder().build().launchUrl(context, it) }
            .onFailure { error.value = it.message }
    }

    fun loadSpotify() = viewModelScope.launch {
        error.value = null
        if (!settings.current().spotifyConnected) return@launch
        runCatching { spotify.playlists() }
            .onSuccess { spotifyPlaylists.value = it }
            .onFailure { error.value = it.message }
    }

    fun importSpotify(selected: List<RemotePlaylistInfo>, liked: Boolean) {
        val loaders = buildList<suspend () -> SourcePlaylist> {
            if (liked) add { spotify.likedSongs() }
            selected.forEach { info -> add { spotify.playlist(info) } }
        }
        manager.start(loaders, "spotify")
    }

    fun importApple(link: String) {
        manager.start(listOf { apple.playlist(link) }, "apple music")
    }

    fun findAppleLink(text: String) = apple.findLink(text)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(sharedLink: String?) {
    val vm: ImportViewModel = hiltViewModel()
    val nav = LocalNav.current
    val context = LocalContext.current
    val settings by LocalActions.current.settings.collectAsState()
    val error by vm.error.collectAsState()
    var link by remember { mutableStateOf(sharedLink?.let(vm::findAppleLink) ?: sharedLink ?: "") }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("bring your music") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    "every song gets matched against youtube, soundcloud, bandcamp and the open sources, in that order. " +
                        "anything that can't be found is listed so you can fix it by hand.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                ServiceCard(R.drawable.ic_spotify, "spotify", Color(0xFF1DB954)) {
                    val s = settings
                    when {
                        s == null -> Unit
                        s.spotifyClientId.isBlank() -> {
                            Text("needs your own spotify app's client id first (free, developer.spotify.com). set the redirect uri to stash://spotify-callback.",
                                style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { nav.navigate(Routes.SETTINGS) }) { Text("open settings") }
                        }
                        !s.spotifyConnected -> FilledTonalButton(onClick = { vm.loginSpotify(context) }) { Text("log in with spotify") }
                        else -> FilledTonalButton(onClick = { nav.navigate(Routes.IMPORT_SPOTIFY) }) { Text("pick playlists") }
                    }
                }
            }
            item {
                ServiceCard(R.drawable.ic_apple_music, "apple music", Color(0xFFFA243C)) {
                    Text("paste a playlist link (share → copy link in apple music), or share it straight to stash.",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = link, onValueChange = { link = it }, singleLine = true,
                        placeholder = { Text("https://music.apple.com/…/playlist/…") },
                        shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(
                        enabled = link.contains("music.apple.com"),
                        onClick = { vm.importApple(link.trim()); nav.navigate(Routes.IMPORT_PROGRESS) },
                    ) { Text("import") }
                }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        }
    }
}

@Composable
private fun ServiceCard(logo: Int, name: String, brand: Color, content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MaterialTheme.shapes.medium, color = brand.copy(alpha = 0.14f), modifier = Modifier.size(52.dp)) {
                    Box(contentAlignment = Alignment.Center) { Image(painterResource(logo), name, Modifier.size(30.dp)) }
                }
                Spacer(Modifier.width(14.dp))
                Text(name, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyPlaylistsScreen() {
    val vm: ImportViewModel = hiltViewModel()
    val nav = LocalNav.current
    val playlists by vm.spotifyPlaylists.collectAsState()
    val error by vm.error.collectAsState()
    var selected by remember { mutableStateOf(setOf<String>()) }
    var liked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadSpotify() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("spotify playlists") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "back") } },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Button(
                    enabled = selected.isNotEmpty() || liked,
                    onClick = {
                        vm.importSpotify(playlists.orEmpty().filter { it.id in selected }, liked)
                        nav.navigate(Routes.IMPORT_PROGRESS) { popUpTo(Routes.IMPORT_SPOTIFY) { inclusive = true } }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                ) { Text("import ${selected.size + if (liked) 1 else 0}") }
            }
        },
    ) { padding ->
        val list = playlists
        when {
            error != null -> Box(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            list == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> LazyColumn(contentPadding = padding) {
                item {
                    ListItem(
                        headlineContent = { Text("liked songs") },
                        leadingContent = {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small, modifier = Modifier.size(52.dp)) {
                                Icon(Icons.Rounded.Favorite, null, Modifier.padding(14.dp))
                            }
                        },
                        trailingContent = { Checkbox(checked = liked, onCheckedChange = { liked = it }) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { liked = !liked },
                    )
                }
                items(list, key = { it.id }) { p ->
                    val on = p.id in selected
                    ListItem(
                        headlineContent = { Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(listOfNotNull("${p.trackCount} songs", p.owner).joinToString(" · ")) },
                        leadingContent = { Artwork(p.coverUrl, Modifier.size(52.dp)) },
                        trailingContent = { Checkbox(checked = on, onCheckedChange = { selected = if (it) selected + p.id else selected - p.id }) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { selected = if (on) selected - p.id else selected + p.id },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportProgressScreen() {
    val vm: ImportViewModel = hiltViewModel()
    val nav = LocalNav.current
    val p by vm.manager.progress.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (p.finished) "import done" else "importing") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "back") } },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = padding.calculateTopPadding(), bottom = 24.dp)) {
            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painterResource(if (p.origin == "spotify") R.drawable.ic_spotify else R.drawable.ic_apple_music),
                                null, Modifier.size(28.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(p.playlistName.ifBlank { "…" }, style = MaterialTheme.typography.titleLarge,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.height(16.dp))
                        if (p.total > 0) {
                            LinearProgressIndicator(progress = { p.done.toFloat() / p.total }, modifier = Modifier.fillMaxWidth().height(8.dp))
                        } else if (p.running) {
                            LinearProgressIndicator(Modifier.fillMaxWidth().height(8.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("${p.done} of ${p.total} checked · ${p.matched} found · ${p.unmatched.size} missing",
                            style = MaterialTheme.typography.bodyMedium)
                        if (p.current.isNotBlank()) Text(p.current, style = MaterialTheme.typography.bodySmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                        p.error?.let {
                            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (p.running) TextButton(onClick = { vm.manager.cancel() }) { Text("stop") }
                            p.playlistId?.let { id ->
                                FilledTonalButton(onClick = { nav.navigate(Routes.playlist(id)) }) {
                                    if (p.finished) Icon(Icons.Rounded.CheckCircle, null, Modifier.size(18.dp))
                                    if (p.finished) Spacer(Modifier.width(6.dp))
                                    Text("open playlist")
                                }
                            }
                        }
                    }
                }
            }
            if (p.unmatched.isNotEmpty()) {
                item {
                    Text("couldn't match", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 4.dp))
                    Text("these are saved on the playlist page, tap them there to search by hand.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(p.unmatched) { t ->
                    ListItem(
                        headlineContent = { Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(t.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = { Artwork(t.artworkUrl, Modifier.size(44.dp)) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }
    }
}
