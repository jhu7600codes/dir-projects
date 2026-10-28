package dev.jhu.crossi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LevelProgressEntity::class, PowerUpEntity::class, StreakEntity::class, DailyResultEntity::class,
        HighScoreEntity::class, FreeRoamRunEntity::class, SavedGameEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class CrossiDatabase : RoomDatabase() {
    abstract fun dao(): CrossiDao

    companion object {
        fun create(context: Context): CrossiDatabase =
            Room.databaseBuilder(context, CrossiDatabase::class.java, "crossi.db").build()
    }
}
