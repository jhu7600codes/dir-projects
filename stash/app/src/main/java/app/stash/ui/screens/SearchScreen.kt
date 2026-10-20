package app.stash.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.stash.data.Library
import app.stash.data.db.Track
import app.stash.providers.MusicProvider
import app.stash.providers.ProviderRegistry
import app.stash.ui.LocalActions
import app.stash.ui.LocalNav
import app.stash.ui.components.TrackRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderResults(val loading: Boolean = true, val tracks: List<Track> = emptyList(), val error: String? = null)

data class SearchUi(
    val query: String = "",
    val providers: List<MusicProvider> = emptyList(),
    val filter: String? = null, // null = all
    val local: List<Track> = emptyList(),
    val results: Map<String, ProviderResults> = emptyMap(),
    val searched: String = "",
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val registry: ProviderRegistry,
    private val library: Library,
) : ViewModel() {
    private val _ui = MutableStateFlow(SearchUi())
    val ui: StateFlow<SearchUi> = _ui
    private var job: Job? = null
    private var debounce: Job? = null

    init {
        viewModelScope.launch { _ui.update { it.copy(providers = registry.active()) } }
    }

    fun setQuery(q: String, immediate: Boolean = false) {
        _ui.update { it.copy(query = q) }
        debounce?.cancel()
        if (q.isBlank()) {
            job?.cancel()
            _ui.update { it.copy(results = emptyMap(), local = emptyList(), searched = "") }
            return
        }
        debounce = viewModelScope.launch {
            if (!immediate) delay(900)
            search()
        }
    }

    fun setFilter(id: String?) {
        _ui.update { it.copy(filter = id) }
        if (_ui.value.searched.isNotBlank()) search(force = true)
    }

    fun search(force: Boolean = false) {
        val q = _ui.value.query.trim()
        if (q.isEmpty() || (!force && q == _ui.value.searched)) return
        job?.cancel()
        job = viewModelScope.launch {
            val providers = registry.active().also { p -> _ui.update { it.copy(providers = p) } }
            val targets = _ui.value.filter?.let { f -> providers.filter { it.id == f } } ?: providers
            _ui.update { s -> s.copy(searched = q, results = targets.associate { it.id to ProviderResults() }) }
            launch {
                val local = runCatching { library.tracks.searchLibrary(q) }.getOrDefault(emptyList())
                _ui.update { it.copy(local = local) }
            }
            val limit = if (_ui.value.filter == null) 6 else 30
            targets.forEach { p ->
                launch {
                    val r = runCatching { p.search(q, limit) }
                    _ui.update { s ->
                        s.copy(
                            results = s.results + (p.id to ProviderResults(
                                loading = false,
                                tracks = r.getOrDefault(emptyList()),
                                error = r.exceptionOrNull()?.message,
                            ))
                        )
                    }
                }
            }
        }
    }
}

private val browseTiles = listOf(
    "pop", "hip hop", "rock", "electronic", "lofi", "jazz", "classical", "indie",
    "phonk", "r&b", "metal", "ambient", "russian rap", "k-pop", "soundtracks", "chill",
)

@Composable
fun SearchScreen(fixUnmatchedId: Long? = null, initialQuery: String = "") {
    val vm: SearchViewModel = hiltViewModel()
    val ui by vm.ui.collectAsState()
    val actions = LocalActions.current
    val nav = LocalNav.current
    val keyboard = LocalSoftwareKeyboardController.current
    val current by actions.player.state.collectAsState()
    val picking = fixUnmatchedId != null

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && ui.query.isBlank()) vm.setQuery(initialQuery, immediate = true)
    }

    fun onPick(list: List<Track>, index: Int) {
        if (fixUnmatchedId != null) {
            actions.library.resolveUnmatched(fixUnmatchedId, list[index])
            actions.toast("matched")
            nav.popBackStack()
        } else {
            actions.play(list, index)
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (picking) {
                IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "back") }
            }
            TextField(
                value = ui.query,
                onValueChange = { vm.setQuery(it) },
                placeholder = { Text(if (picking) "find the right song" else "what do you want to listen to?") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    if (ui.query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Rounded.Close, "clear") }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); vm.search(force = true) }),
                modifier = Modifier.weight(1f),
            )
        }

        if (ui.providers.isNotEmpty()) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = ui.filter == null, onClick = { vm.setFilter(null) }, label = { Text("everywhere") }) }
                items(ui.providers, key = { it.id }) { p ->
                    FilterChip(selected = ui.filter == p.id, onClick = { vm.setFilter(p.id) }, label = { Text(p.displayName) })
                }
            }
        }

        if (ui.searched.isBlank()) {
            if (!picking) BrowseGrid(onPick = { vm.setQuery(it, immediate = true) })
            return@Column
        }

        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (ui.local.isNotEmpty() && !picking) {
                item { ResultHeader("in your library", null) }
                itemsIndexed(ui.local, key = { _, t -> "local:${t.id}" }) { i, t ->
                    TrackRow(t, onClick = { onPick(ui.local, i) }, isCurrent = current.currentId == t.id)
                }
            }
            ui.providers.filter { ui.results.containsKey(it.id) }.forEach { p ->
                val r = ui.results.getValue(p.id)
                item(key = "h:${p.id}") {
                    ResultHeader(p.displayName, if (ui.filter == null && r.tracks.size >= 6) ({ vm.setFilter(p.id) }) else null)
                }
                when {
                    r.loading -> item(key = "l:${p.id}") {
                        Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        }
                    }
                    r.error != null -> item(key = "e:${p.id}") {
                        Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(r.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                    }
                    r.tracks.isEmpty() -> item(key = "n:${p.id}") {
                        Text("nothing here", Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> itemsIndexed(r.tracks, key = { _, t -> "${p.id}:${t.id}" }) { i, t ->
                        TrackRow(
                            t, onClick = { onPick(r.tracks, i) }, isCurrent = current.currentId == t.id,
                            trailing = if (picking) null else ({
                                IconButton(onClick = { actions.download(listOf(t)) }) {
                                    Icon(Icons.Rounded.Download, "download", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultHeader(title: String, onMore: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (onMore != null) TextButton(onClick = onMore) { Text("more") }
    }
}

/** spotify's "browse all", with tonal material you containers instead of fixed colors */
@Composable
private fun BrowseGrid(onPick: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val palette = listOf(
        scheme.primaryContainer to scheme.onPrimaryContainer,
        scheme.tertiaryContainer to scheme.onTertiaryContainer,
        scheme.secondaryContainer to scheme.onSecondaryContainer,
        scheme.surfaceContainerHighest to scheme.onSurface,
    )
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("browse all", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 6.dp)) }
        items(browseTiles.chunked(2)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { genre ->
                    val (bg, fg) = palette[(browseTiles.indexOf(genre) + browseTiles.indexOf(genre) / 2) % palette.size]
                    Surface(
                        onClick = { onPick(genre) },
                        color = bg,
                        contentColor = fg,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.weight(1f).height(92.dp),
                    ) {
                        Box(Modifier.padding(14.dp)) {
                            Text(genre, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
