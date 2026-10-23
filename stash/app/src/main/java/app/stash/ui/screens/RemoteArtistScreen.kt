package app.stash.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.stash.data.db.Track
import app.stash.providers.ProviderRegistry
import app.stash.ui.components.originName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** an artist found in search (soundcloud / audius / youtube), with their own uploads */
@HiltViewModel
class RemoteArtistViewModel @Inject constructor(
    saved: SavedStateHandle,
    registry: ProviderRegistry,
) : ViewModel() {
    val provider: String = saved["provider"] ?: ""
    val name: String = saved["name"] ?: ""
    val art: String? = saved.get<String>("art")?.ifBlank { null }
    val tracks = MutableStateFlow<List<Track>?>(null)
    val error = MutableStateFlow<String?>(null)

    init {
        val id: String = saved["id"] ?: ""
        viewModelScope.launch {
            runCatching { registry.get(provider)?.artistTracks(id).orEmpty() }
                .onSuccess { tracks.value = it }
                .onFailure { error.value = it.message; tracks.value = emptyList() }
        }
    }
}

@Composable
fun RemoteArtistScreen() {
    val vm: RemoteArtistViewModel = hiltViewModel()
    val tracks by vm.tracks.collectAsState()
    val error by vm.error.collectAsState()
    val list = tracks
    if (list == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (list.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(error?.let { "Couldn't load ${vm.name}: $it" } ?: "No songs from ${vm.name} yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    CollectionScreen(
        title = vm.name,
        kind = "Artist on ${originName(vm.provider)}",
        tracks = list,
        artwork = vm.art ?: list.firstNotNullOfOrNull { it.artworkUrl },
        artShape = CircleShape,
        artIcon = Icons.Rounded.Person,
    )
}
