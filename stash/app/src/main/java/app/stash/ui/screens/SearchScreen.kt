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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import app.stash.providers.Matcher
import app.stash.providers.MusicProvider
import app.stash.providers.ProviderRegistry
import app.stash.recs.Recommender
import app.stash.ui.LocalActions
import app.stash.ui.LocalNav
import app.stash.ui.components.TrackRow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderResults(val loading: Boolean = true, val tracks: List<Track> = emptyList(), val error: String? = null)

data class SearchUi(
    val query: String = "",
    val providers: List<MusicProvider> = emptyList(),
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

    fun search(force: Boolean = false) {
        val q = _ui.value.query.trim()
        if (q.isEmpty() || (!force && q == _ui.value.searched)) return
        job?.cancel()
        job = viewModelScope.launch {
            val providers = registry.active().also { p -> _ui.update { it.copy(providers = p) } }
            val targets = providers
            _ui.update { s -> s.copy(searched = q, results = targets.associate { it.id to ProviderResults() }) }
            launch {
                val local = runCatching { library.tracks.searchLibrary(q) }.getOrDefault(emptyList())
                _ui.update { it.copy(local = local) }
            }
            val limit = 15
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
    "Pop", "Hip hop", "Rock", "Electronic", "Lo-fi", "Jazz", "Classical", "Indie",
    "Phonk", "R&B", "Metal", "Ambient", "Russian rap", "K-Pop", "Soundtracks", "Chill",
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
            actions.toast("Matched")
            nav.popBackStack()
        } else {
            actions.play(list, index)
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (picking) {
                IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            }
            TextField(
                value = ui.query,
                onValueChange = { vm.setQuery(it) },
                placeholder = { Text(if (picking) "Find the right song" else "What do you want to listen to?") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    if (ui.query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Rounded.Close, "Clear") }
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

        if (ui.searched.isBlank()) {
            if (!picking) BrowseGrid(onPick = { vm.setQuery(it, immediate = true) })
            return@Column
        }

        val loading = ui.results.values.any { it.loading }
        val songs = remember(ui.results, ui.local) { mergeResults(ui.searched, ui.local, ui.providers, ui.results) }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (!loading && songs.isEmpty()) item {
                val error = ui.results.values.firstNotNullOfOrNull { it.error }
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (error != null) Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        if (error != null) "Search didn't work: $error" else "Nothing found",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            itemsIndexed(songs, key = { _, t -> t.id }) { i, t ->
                TrackRow(
                    t, onClick = { onPick(songs, i) }, isCurrent = current.currentId == t.id,
                    trailing = if (picking) null else ({
                        IconButton(onClick = { actions.download(listOf(t)) }) {
                            Icon(Icons.Rounded.Download, "Download", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }),
                )
            }
        }
    }
}

/**
 * one list of songs from every source: your library first, then the best matches for what
 * you typed, the same song from two sources shown once (the earlier source wins)
 */
private fun mergeResults(
    query: String,
    local: List<Track>,
    providers: List<MusicProvider>,
    results: Map<String, ProviderResults>,
): List<Track> {
    val words = Matcher.normalize(query).split(' ').filter { it.isNotEmpty() }
    fun relevance(t: Track): Double {
        if (words.isEmpty()) return 0.0
        val text = Matcher.normalize("${t.artist} ${t.title}")
        return words.count { it in text }.toDouble() / words.size
    }
    val remote = providers.flatMapIndexed { rank, p ->
        results[p.id]?.tracks.orEmpty().mapIndexed { i, t -> Triple(t, rank, i) }
    }.sortedWith(
        compareByDescending<Triple<Track, Int, Int>> { relevance(it.first) }
            .thenBy { it.third } // keep each source's own ranking…
            .thenBy { it.second } // …and prefer earlier sources on ties
    ).map { it.first }
    return (local + remote).distinctBy { Recommender.key(it) }
}

/** spotify's "Browse all", with tonal material you containers instead of fixed colors */
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
        item { Text("Browse all", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 6.dp)) }
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
