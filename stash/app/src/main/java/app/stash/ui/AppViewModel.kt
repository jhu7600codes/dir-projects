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
import app.stash.player.PlayerConnection
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
    settings: Settings,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settings.flow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

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

    fun playNext(track: Track) {
        player.playNext(track); toast("playing next")
    }

    fun addToQueue(tracks: List<Track>) {
        player.addToQueue(tracks); toast(if (tracks.size == 1) "added to queue" else "${tracks.size} added to queue")
    }

    fun download(tracks: List<Track>) {
        downloads.enqueue(tracks)
        toast(if (tracks.size == 1) "downloading" else "downloading ${tracks.size} songs")
    }

    fun removeDownload(track: Track) {
        downloads.delete(track); toast("removed download")
    }

    fun setLiked(track: Track, liked: Boolean) = library.setLiked(track, liked)

    fun addToPlaylist(playlistId: Long, name: String, tracks: List<Track>) {
        library.addToPlaylist(playlistId, tracks); toast("added to $name")
    }

    fun newPlaylistWith(name: String, tracks: List<Track>) {
        viewModelScope.launch {
            library.createPlaylistWith(name, tracks).join()
            toast("created $name")
        }
    }
}

val LocalActions = staticCompositionLocalOf<AppViewModel> { error("no AppViewModel") }
