package app.stash.importer

import app.stash.data.db.Playlist
import app.stash.data.db.PlaylistDao
import app.stash.data.db.PlaylistTrack
import app.stash.data.db.TrackDao
import app.stash.data.db.UnmatchedTrack
import app.stash.providers.Resolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import javax.inject.Singleton

data class ImportProgress(
    val running: Boolean = false,
    val origin: String = "",
    val playlistName: String = "",
    val playlistId: Long? = null,
    val total: Int = 0,
    val done: Int = 0,
    val matched: Int = 0,
    val current: String = "",
    val unmatched: List<SourceTrack> = emptyList(),
    val error: String? = null,
    val finished: Boolean = false,
)

/**
 * matches an outside playlist song by song against the providers and builds a local playlist.
 * runs in its own scope so it keeps going if you leave the progress screen.
 */
@Singleton
class ImportManager @Inject constructor(
    private val resolver: Resolver,
    private val tracks: TrackDao,
    private val playlists: PlaylistDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _progress = MutableStateFlow(ImportProgress())
    val progress: StateFlow<ImportProgress> = _progress

    /** imports each playlist in turn; the progress screen follows whichever one is running */
    fun start(loaders: List<suspend () -> SourcePlaylist>, origin: String) {
        if (job?.isActive == true || loaders.isEmpty()) return
        job = scope.launch {
            loaders.forEachIndexed { i, load ->
                val queued = if (loaders.size > 1) " (${i + 1} of ${loaders.size})" else ""
                _progress.value = ImportProgress(running = true, origin = origin, current = "loading playlist…$queued")
                try {
                    run(load())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _progress.update { it.copy(running = false, error = e.message ?: "import failed", finished = true) }
                }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        _progress.update { it.copy(running = false, finished = true, current = "cancelled") }
    }

    fun reset() {
        if (job?.isActive != true) _progress.value = ImportProgress()
    }

    private suspend fun run(source: SourcePlaylist) {
        val playlistId = playlists.insert(
            Playlist(name = source.name, coverUrl = source.coverUrl, origin = source.origin, originId = source.originId)
        )
        _progress.update {
            it.copy(playlistName = source.name, playlistId = playlistId, total = source.tracks.size, current = "")
        }

        val gate = Semaphore(PARALLEL)
        kotlinx.coroutines.coroutineScope {
            source.tracks.mapIndexed { index, st ->
                async {
                    gate.withPermit {
                        _progress.update { it.copy(current = "${st.artist} - ${st.title}") }
                        val hit = runCatching { resolver.match(st.title, st.artist, st.durationMs) }.getOrNull()
                        if (hit != null) {
                            // keep the nicer metadata from the source service
                            val saved = tracks.ensure(
                                hit.copy(
                                    title = st.title,
                                    artist = st.artist,
                                    album = st.album ?: hit.album,
                                    artworkUrl = st.artworkUrl ?: hit.artworkUrl,
                                    durationMs = if (st.durationMs > 0) st.durationMs else hit.durationMs,
                                )
                            )
                            tracks.addToLibrary(saved.id)
                            playlists.insertTrack(PlaylistTrack(playlistId, saved.id, index))
                            _progress.update { it.copy(done = it.done + 1, matched = it.matched + 1) }
                        } else {
                            playlists.insertUnmatched(
                                UnmatchedTrack(
                                    playlistId = playlistId, title = st.title, artist = st.artist,
                                    album = st.album, durationMs = st.durationMs, position = index,
                                )
                            )
                            _progress.update { it.copy(done = it.done + 1, unmatched = it.unmatched + st) }
                        }
                    }
                }
            }.awaitAll()
        }
        playlists.touch(playlistId)
        _progress.update { it.copy(running = false, finished = true, current = "") }
    }

    companion object {
        private const val PARALLEL = 3
    }
}
