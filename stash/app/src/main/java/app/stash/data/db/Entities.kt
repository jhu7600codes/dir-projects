package app.stash.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DownloadState { NONE, QUEUED, DOWNLOADING, DONE, FAILED }

/**
 * One song from any provider. Search results use this too before they're saved, so every
 * library column has a default. [id] is "provider:sourceId" so the same song from the same
 * provider never shows up twice.
 */
@Entity(tableName = "tracks", indices = [Index("artist"), Index("album"), Index("lastPlayedAt")])
data class Track(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long = 0,
    val artworkUrl: String? = null,
    /** which [app.stash.providers.MusicProvider] this came from */
    val provider: String,
    /** provider-specific locator: a page url for yt-dlp sources, an api id otherwise */
    val sourceId: String,
    /** a stable (non-expiring) direct audio url when the provider has one, e.g. jamendo or archive.org */
    val streamHint: String? = null,

    // fallback: the provider that actually worked last time the original one failed
    val resolvedProvider: String? = null,
    val resolvedSourceId: String? = null,
    val resolvedStreamHint: String? = null,

    // offline
    val localPath: String? = null,
    val downloadState: DownloadState = DownloadState.NONE,
    val downloadProgress: Float = 0f,
    val downloadError: String? = null,

    // library
    val inLibrary: Boolean = false,
    val liked: Boolean = false,
    val addedAt: Long = 0,
    val lastPlayedAt: Long? = null,
    val playCount: Int = 0,
) {
    val isDownloaded get() = downloadState == DownloadState.DONE && localPath != null

    companion object {
        fun makeId(provider: String, sourceId: String) = "$provider:$sourceId"
    }
}

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val coverUrl: String? = null,
    /** "local", "spotify" or "apple" */
    val origin: String = "local",
    val originId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    indices = [Index("trackId")],
)
data class PlaylistTrack(
    val playlistId: Long,
    val trackId: String,
    val position: Int,
)

/** an imported song none of the providers could find, kept so it can be fixed by hand */
@Entity(tableName = "unmatched", indices = [Index("playlistId")])
data class UnmatchedTrack(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long = 0,
    val position: Int,
)

data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val description: String?,
    val coverUrl: String?,
    val origin: String,
    val updatedAt: Long,
    val trackCount: Int,
    val downloadedCount: Int,
    /** first track's art, used as the cover when the playlist has none */
    val firstArtwork: String?,
)

data class AlbumSummary(
    val album: String,
    val artist: String,
    @ColumnInfo(name = "artworkUrl") val artworkUrl: String?,
    val trackCount: Int,
)

data class ArtistSummary(
    val artist: String,
    val artworkUrl: String?,
    val trackCount: Int,
)
