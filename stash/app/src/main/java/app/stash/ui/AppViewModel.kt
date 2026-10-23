package app.stash.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.stash.data.Library
import app.stash.data.db.PlaylistWithCount
import app.stash.data.db.Track
import app.stash.data.prefs.AppSettings
import app.stash.data.prefs.Settings
import app.stash.download.Downloads
import app.stash.importer.ImportQueue
import app.stash.player.PlayerConnection
import app.stash.recs.Recommender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * app-wide actions every screen needs (play, queue, download, like, add to playlist),
 * handed down through [LocalActions] instead of being re-injected into every screen.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    val player: PlayerConnection,
    val downloads: Downloads,
    val library: Library,
    private val recommender: Recommender,
    private val imports: ImportQueue,
    val prefs: Settings,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = prefs.flow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val playlists: StateFlow<List<PlaylistWithCount>> =
        library.playlists.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    fun toast(msg: String) {
        _messages.tryEmit(msg)
    }

    init {
        player.connect()
    }

    fun play(tracks: List<Track>, index: Int = 0) = player.play(tracks, index)
    fun shuffle(tracks: List<Track>) = player.play(tracks, shuffle = true)

    /** plays the song, then youtube music's radio for it */
    fun startRadio(track: Track) {
        toast("Starting radio")
        val started = player.play(listOf(track))
        viewModelScope.launch {
            val similar = runCatching { recommender.radioFor(track) }.getOrDefault(emptyList())
            started?.join() // the song itself has to be queued before the radio goes after it
            if (similar.isEmpty()) toast("Couldn't find similar songs") else player.addToQueue(similar)
        }
    }

    /** every song that couldn't be found anywhere, across all playlists and liked songs */
    val missingCount: StateFlow<Int> =
        library.playlists.observeUnmatchedCount().stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun rescanMissing() {
        imports.rescanMissing()
        toast("Searching for missing songs again in the background")
    }

    fun playNext(track: Track) {
        player.playNext(track); toast("Playing next")
    }

    fun addToQueue(tracks: List<Track>) {
        player.addToQueue(tracks); toast(if (tracks.size == 1) "Added to queue" else "${tracks.size} added to queue")
    }

    fun download(tracks: List<Track>) {
        downloads.enqueue(tracks)
        toast(if (tracks.size == 1) "Downloading" else "Downloading ${tracks.size} songs")
    }

    fun removeDownload(track: Track) {
        downloads.delete(track); toast("Removed download")
    }

    fun setLiked(track: Track, liked: Boolean) = library.setLiked(track, liked)

    fun addToPlaylist(playlistId: Long, name: String, tracks: List<Track>) {
        library.addToPlaylist(playlistId, tracks); toast("Added to $name")
    }

    fun newPlaylistWith(name: String, tracks: List<Track>) {
        viewModelScope.launch {
            library.createPlaylistWith(name, tracks).join()
            toast("Created $name")
        }
    }
}

val LocalActions = staticCompositionLocalOf<AppViewModel> { error("No AppViewModel") }
