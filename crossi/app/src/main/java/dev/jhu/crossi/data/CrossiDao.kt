package dev.jhu.crossi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CrossiDao {
    @Query("SELECT * FROM level_progress WHERE language = :language")
    fun levelProgress(language: String): Flow<List<LevelProgressEntity>>

    @Query("SELECT * FROM level_progress WHERE language = :language AND levelId = :levelId")
    suspend fun levelProgress(language: String, levelId: Int): LevelProgressEntity?

    @Upsert
    suspend fun upsert(progress: LevelProgressEntity)

    @Query("SELECT * FROM power_ups")
    fun powerUps(): Flow<List<PowerUpEntity>>

    @Query("SELECT * FROM power_ups WHERE type = :type")
    suspend fun powerUp(type: String): PowerUpEntity?

    @Upsert
    suspend fun upsert(powerUp: PowerUpEntity)

    @Query("SELECT * FROM streak WHERE id = 0")
    fun streak(): Flow<StreakEntity?>

    @Query("SELECT * FROM streak WHERE id = 0")
    suspend fun streakNow(): StreakEntity?

    @Upsert
    suspend fun upsert(streak: StreakEntity)

    @Query("SELECT * FROM daily_results WHERE date = :date AND language = :language")
    fun dailyResults(date: String, language: String): Flow<List<DailyResultEntity>>

    @Query("SELECT * FROM daily_results WHERE date = :date AND language = :language")
    suspend fun dailyResultsNow(date: String, language: String): List<DailyResultEntity>

    @Query("SELECT * FROM daily_results WHERE completed = 1 ORDER BY date DESC LIMIT 60")
    fun recentDailies(): Flow<List<DailyResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: DailyResultEntity)

    @Query("SELECT * FROM high_scores WHERE language = :language AND difficulty = :difficulty ORDER BY score DESC LIMIT 10")
    fun highScores(language: String, difficulty: String): Flow<List<HighScoreEntity>>

    @Query("SELECT MAX(score) FROM high_scores WHERE language = :language AND difficulty = :difficulty AND runId != :exceptRun")
    suspend fun bestScore(language: String, difficulty: String, exceptRun: String): Int?

    @Upsert
    suspend fun upsert(score: HighScoreEntity)

    @Query("SELECT * FROM free_roam_runs WHERE `key` = :key")
    fun run(key: String): Flow<FreeRoamRunEntity?>

    @Query("SELECT * FROM free_roam_runs WHERE `key` = :key")
    suspend fun runNow(key: String): FreeRoamRunEntity?

    @Upsert
    suspend fun upsert(run: FreeRoamRunEntity)

    @Query("DELETE FROM free_roam_runs WHERE `key` = :key")
    suspend fun deleteRun(key: String)

    @Query("SELECT * FROM saved_games WHERE modeKey = :key")
    suspend fun savedGame(key: String): SavedGameEntity?

    @Query("SELECT * FROM saved_games WHERE modeKey LIKE :prefix || '%' ORDER BY updatedAt DESC LIMIT 1")
    fun latestSavedGame(prefix: String): Flow<SavedGameEntity?>

    @Upsert
    suspend fun upsert(game: SavedGameEntity)

    @Query("DELETE FROM saved_games WHERE modeKey = :key")
    suspend fun deleteSavedGame(key: String)
}
