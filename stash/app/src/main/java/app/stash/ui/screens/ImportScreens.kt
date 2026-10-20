package app.stash.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import app.stash.importer.AppleMusic
import app.stash.importer.ExportifyCsv
import app.stash.importer.SpotifyLink
import dagger.hilt.android.qualifiers.ApplicationContext
import app.stash.importer.ImportManager
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
    private val spotify: SpotifyLink,
    private val apple: AppleMusic,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val error = MutableStateFlow<String?>(null)

    /** a spotify or apple music link (or any shared text containing one) */
    fun cleanLink(text: String) = spotify.findLink(text) ?: apple.findLink(text) ?: text.trim()

    fun importLink(link: String): Boolean {
        error.value = null
        return when {
            "spotify" in link -> { manager.start(listOf { spotify.playlist(link) }, "spotify"); true }
            "music.apple.com" in link -> { manager.start(listOf { apple.playlist(link) }, "apple music"); true }
            else -> { error.value = "paste a spotify or apple music playlist link"; false }
        }
    }

    /** reads the picked exportify csv / zip files up front, then imports each playlist in turn */
    fun importFiles(uris: List<Uri>): Boolean {
        error.value = null
        val playlists = uris.flatMap { uri ->
            runCatching {
                context.contentResolver.openInputStream(uri)!!.use { ExportifyCsv.read(displayName(uri), it) }
            }.onFailure { error.value = "couldn't read ${displayName(uri)}: ${it.message}" }.getOrDefault(emptyList())
        }
        if (playlists.isEmpty()) {
            if (error.value == null) error.value = "no songs found, is that an exportify csv?"
            return false
        }
        manager.start(playlists.map { p -> suspend { p } }, "spotify")
        return true
    }

    private fun displayName(uri: Uri): String =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "playlist.csv"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(sharedLink: String?) {
    val vm: ImportViewModel = hiltViewModel()
    val nav = LocalNav.current
    val context = LocalContext.current
    val error by vm.error.collectAsState()
    var link by remember { mutableStateOf(sharedLink?.let(vm::cleanLink) ?: "") }
    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty() && vm.importFiles(uris)) nav.navigate(Routes.IMPORT_PROGRESS)
    }
    val busy by vm.manager.progress.collectAsState()

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
            if (busy.running) item {
                FilledTonalButton(onClick = { nav.navigate(Routes.IMPORT_PROGRESS) }, modifier = Modifier.fillMaxWidth()) {
                    Text("an import is running, see progress")
                }
            }
            item {
                ServiceCard(R.drawable.ic_spotify, "playlist link", Color(0xFF1DB954)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.ic_apple_music), null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("spotify or apple music, the playlist has to be public. share → copy link, or share it straight to stash.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = link, onValueChange = { link = it }, singleLine = true,
                        placeholder = { Text("https://open.spotify.com/playlist/…") },
                        shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(
                        enabled = link.isNotBlank() && !busy.running,
                        onClick = { if (vm.importLink(vm.cleanLink(link))) nav.navigate(Routes.IMPORT_PROGRESS) },
                    ) { Text("import") }
                    Text(
                        "spotify links stop at the first 100 songs, use exportify below for bigger ones.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            item {
                ServiceCard(R.drawable.ic_spotify, "exportify", Color(0xFF1DB954)) {
                    Text(
                        "for private playlists, liked songs, or everything at once: open exportify.net, log in with spotify, " +
                            "hit export (or export all for a zip), then pick the files here.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://exportify.net")))
                        }) { Text("open exportify") }
                        FilledTonalButton(
                            enabled = !busy.running,
                            onClick = { pickFiles.launch(arrayOf("text/*", "application/zip", "application/octet-stream")) },
                        ) { Text("pick csv / zip") }
                    }
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
