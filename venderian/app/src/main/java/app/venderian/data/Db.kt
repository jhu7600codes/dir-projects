package app.venderian.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.venderian.srs.SrsState
import kotlinx.coroutines.flow.Flow

/** one flashcard. id is "glyph:<letter id>" or "word:<venderian word>" */
@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val ease: Double = 2.5,
    val intervalDays: Int = 0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val dueAt: Long,
    val introducedAt: Long,
) {
    val key: String get() = id.substringAfter(':')
    val srs: SrsState get() = SrsState(ease, intervalDays, reps, lapses)

    companion object {
        const val GLYPH = "glyph"
        const val WORD = "word"
    }
}

/** xp earned per day; a day with a row counts toward the streak */
@Entity(tableName = "days")
data class DayEntity(@PrimaryKey val epochDay: Long, val xp: Int)

@Entity(tableName = "lessons")
data class LessonEntity(@PrimaryKey val lesson: Int, val completedAt: Long)

/** a finished course lesson (lesson, challenge or gold), by its id in course.json */
@Entity(tableName = "course")
data class CourseEntity(@PrimaryKey val id: String, val completedAt: Long, val mistakes: Int)

@Dao
interface CardDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(cards: List<CardEntity>)

    @Update
    suspend fun update(card: CardEntity)

    @Query("SELECT * FROM cards WHERE dueAt <= :now ORDER BY dueAt LIMIT :limit")
    suspend fun due(now: Long, limit: Int): List<CardEntity>

    @Query("SELECT COUNT(*) FROM cards WHERE dueAt <= :now")
    fun dueCount(now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE kind = :kind AND introducedAt >= :since")
    suspend fun introducedSince(kind: String, since: Long): Int

    @Query("SELECT id FROM cards")
    suspend fun ids(): List<String>

    @Query("SELECT * FROM cards")
    fun all(): Flow<List<CardEntity>>
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM days ORDER BY epochDay")
    fun days(): Flow<List<DayEntity>>

    @Query("INSERT OR IGNORE INTO days (epochDay, xp) VALUES (:day, 0)")
    suspend fun ensureDay(day: Long)

    @Query("UPDATE days SET xp = xp + :xp WHERE epochDay = :day")
    suspend fun addToDay(day: Long, xp: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun completeLesson(lesson: LessonEntity)

    @Query("SELECT * FROM lessons")
    fun lessons(): Flow<List<LessonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun completeCourse(lesson: CourseEntity)

    @Query("SELECT * FROM course")
    fun course(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM course")
    suspend fun courseNow(): List<CourseEntity>
}

@Database(entities = [CardEntity::class, DayEntity::class, LessonEntity::class, CourseEntity::class], version = 3)
abstract class VenderianDb : RoomDatabase() {
    abstract fun cards(): CardDao
    abstract fun progress(): ProgressDao

    companion object {
        /** 2: the course */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `course` (`id` TEXT NOT NULL, `completedAt` INTEGER NOT NULL, `mistakes` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        /** 3: challenges and gold lessons got stories, so finished ones have to be done again */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM course WHERE id LIKE '%-challenge' OR id LIKE '%-gold'")
            }
        }

        fun create(context: Context) =
            Room.databaseBuilder(context, VenderianDb::class.java, "venderian.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }
}
