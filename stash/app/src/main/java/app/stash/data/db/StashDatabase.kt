package app.stash.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Track::class, Playlist::class, PlaylistTrack::class, UnmatchedTrack::class],
    version = 1,
    exportSchema = false,
)
abstract class StashDatabase : RoomDatabase() {
    abstract fun tracks(): TrackDao
    abstract fun playlists(): PlaylistDao
}
