package app.stash.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun get(id: String): Track?

    @Query("SELECT * FROM tracks WHERE id IN (:ids)")
    suspend fun getAll(ids: List<String>): List<Track>

    @Query("SELECT * FROM tracks WHERE id = :id")
    fun observe(id: String): Flow<Track?>

    @Query("SELECT * FROM tracks WHERE id IN (:ids)")
    fun observeAll(ids: List<String>): Flow<List<Track>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(track: Track): Long

    @Update
    suspend fun update(track: Track)

    /** saves a search result without clobbering library/download state of an existing row */
    @Transaction
    suspend fun ensure(track: Track): Track {
        insertIgnore(track)
        return get(track.id) ?: track
    }

    @Query("UPDATE tracks SET inLibrary = 1, addedAt = CASE WHEN addedAt = 0 THEN :now ELSE addedAt END WHERE id = :id")
    suspend fun addToLibrary(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE tracks SET inLibrary = 0, liked = 0 WHERE id = :id")
    suspend fun removeFromLibrary(id: String)

    @Query("UPDATE tracks SET liked = :liked, inLibrary = CASE WHEN :liked THEN 1 ELSE inLibrary END, addedAt = CASE WHEN addedAt = 0 THEN :now ELSE addedAt END WHERE id = :id")
    suspend fun setLiked(id: String, liked: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE tracks SET lastPlayedAt = :now, playCount = playCount + 1 WHERE id = :id")
    suspend fun markPlayed(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE tracks SET resolvedProvider = :provider, resolvedSourceId = :sourceId, resolvedStreamHint = :hint WHERE id = :id")
    suspend fun setResolved(id: String, provider: String?, sourceId: String?, hint: String?)

    @Query("UPDATE tracks SET downloadState = :state, downloadProgress = :progress, downloadError = :error WHERE id = :id")
    suspend fun setDownloadState(id: String, state: DownloadState, progress: Float = 0f, error: String? = null)

    @Query("UPDATE tracks SET downloadProgress = :progress WHERE id = :id")
    suspend fun setDownloadProgress(id: String, progress: Float)

    @Query("UPDATE tracks SET downloadState = 'DONE', downloadProgress = 1, downloadError = NULL, localPath = :path, inLibrary = 1 WHERE id = :id")
    suspend fun markDownloaded(id: String, path: String)

    @Query("UPDATE tracks SET downloadState = 'NONE', downloadProgress = 0, downloadError = NULL, localPath = NULL WHERE id = :id")
    suspend fun clearDownload(id: String)

    @Query("SELECT * FROM tracks WHERE inLibrary = 1 ORDER BY addedAt DESC")
    fun observeLibrary(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE liked = 1 ORDER BY addedAt DESC")
    fun observeLiked(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE downloadState = 'DONE' ORDER BY title COLLATE NOCASE")
    fun observeDownloaded(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE downloadState IN ('QUEUED', 'DOWNLOADING', 'FAILED') ORDER BY downloadState = 'DOWNLOADING' DESC, downloadState = 'FAILED', addedAt")
    fun observeDownloadQueue(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE downloadState IN ('QUEUED', 'DOWNLOADING')")
    suspend fun pendingDownloads(): List<Track>

    @Query("SELECT * FROM tracks WHERE downloadState = 'FAILED'")
    suspend fun failedDownloads(): List<Track>

    @Query("SELECT * FROM tracks WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE playCount > 0 ORDER BY playCount DESC, lastPlayedAt DESC LIMIT :limit")
    fun observeMostPlayed(limit: Int = 20): Flow<List<Track>>

    @Query(
        """SELECT album, artist, MAX(artworkUrl) AS artworkUrl, COUNT(*) AS trackCount FROM tracks
           WHERE inLibrary = 1 AND album IS NOT NULL AND album != ''
           GROUP BY album, artist ORDER BY album COLLATE NOCASE"""
    )
    fun observeAlbums(): Flow<List<AlbumSummary>>

    @Query(
        """SELECT artist, MAX(artworkUrl) AS artworkUrl, COUNT(*) AS trackCount FROM tracks
           WHERE inLibrary = 1 GROUP BY artist ORDER BY artist COLLATE NOCASE"""
    )
    fun observeArtists(): Flow<List<ArtistSummary>>

    @Query("SELECT * FROM tracks WHERE album = :album AND artist = :artist ORDER BY addedAt")
    fun observeAlbum(album: String, artist: String): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE artist = :artist AND (inLibrary = 1 OR lastPlayedAt IS NOT NULL) ORDER BY playCount DESC, title COLLATE NOCASE")
    fun observeArtist(artist: String): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE inLibrary = 1 AND (title LIKE '%' || :q || '%' OR artist LIKE '%' || :q || '%' OR album LIKE '%' || :q || '%') LIMIT 30")
    suspend fun searchLibrary(q: String): List<Track>
}

@Dao
interface PlaylistDao {
    @Insert
    suspend fun insert(playlist: Playlist): Long

    @Update
    suspend fun update(playlist: Playlist)

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun get(id: Long): Playlist?

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observe(id: Long): Flow<Playlist?>

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id")
    suspend fun clearTracks(id: Long)

    @Query("DELETE FROM unmatched WHERE playlistId = :id")
    suspend fun clearUnmatched(id: Long)

    @Transaction
    suspend fun deleteFully(id: Long) {
        clearTracks(id)
        clearUnmatched(id)
        delete(id)
    }

    @Query("UPDATE playlists SET name = :name, updatedAt = :now WHERE id = :id")
    suspend fun rename(id: Long, name: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE playlists SET updatedAt = :now WHERE id = :id")
    suspend fun touch(id: Long, now: Long = System.currentTimeMillis())

    @Query(
        """SELECT p.id, p.name, p.description, p.coverUrl, p.origin, p.updatedAt,
           (SELECT COUNT(*) FROM playlist_tracks pt WHERE pt.playlistId = p.id) AS trackCount,
           (SELECT COUNT(*) FROM playlist_tracks pt JOIN tracks t ON t.id = pt.trackId
              WHERE pt.playlistId = p.id AND t.downloadState = 'DONE') AS downloadedCount,
           (SELECT t.artworkUrl FROM playlist_tracks pt JOIN tracks t ON t.id = pt.trackId
              WHERE pt.playlistId = p.id ORDER BY pt.position LIMIT 1) AS firstArtwork
           FROM playlists p ORDER BY p.updatedAt DESC"""
    )
    fun observeAll(): Flow<List<PlaylistWithCount>>

    @Query("SELECT t.* FROM tracks t JOIN playlist_tracks pt ON pt.trackId = t.id WHERE pt.playlistId = :id ORDER BY pt.position")
    fun observeTracks(id: Long): Flow<List<Track>>

    @Query("SELECT t.* FROM tracks t JOIN playlist_tracks pt ON pt.trackId = t.id WHERE pt.playlistId = :id ORDER BY pt.position")
    suspend fun tracks(id: Long): List<Track>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = :id")
    suspend fun nextPosition(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrack(ref: PlaylistTrack)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrack(playlistId: Long, trackId: String)

    @Query("UPDATE playlist_tracks SET position = :position WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun setPosition(playlistId: Long, trackId: String, position: Int)

    @Insert
    suspend fun insertUnmatched(track: UnmatchedTrack): Long

    @Query("SELECT * FROM unmatched WHERE playlistId = :id ORDER BY position")
    fun observeUnmatched(id: Long): Flow<List<UnmatchedTrack>>

    @Query("SELECT * FROM unmatched WHERE id = :id")
    suspend fun getUnmatched(id: Long): UnmatchedTrack?

    @Query("DELETE FROM unmatched WHERE id = :id")
    suspend fun deleteUnmatched(id: Long)
}
