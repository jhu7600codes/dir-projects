package dev.jhu.crossi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_progress", primaryKeys = ["language", "levelId"])
data class LevelProgressEntity(
    val language: String,
    val levelId: Int,
    val stars: Int,
    val bestTimeMs: Long,
    val checksUsed: Int,
    val completedAt: Long,
)

@Entity(tableName = "power_ups")
data class PowerUpEntity(@PrimaryKey val type: String, val count: Int)

@Entity(tableName = "streak")
data class StreakEntity(
    @PrimaryKey val id: Int = 0,
    val current: Int,
    val best: Int,
    val lastDate: String?,
)

@Entity(tableName = "daily_results", primaryKeys = ["date", "difficulty", "language"])
data class DailyResultEntity(
    val date: String,
    val difficulty: String,
    val language: String,
    val completed: Boolean,
    val timeMs: Long,
    val checksUsed: Int,
    val score: Int,
)

@Entity(tableName = "high_scores")
data class HighScoreEntity(
    @PrimaryKey val runId: String,
    val language: String,
    val difficulty: String,
    val score: Int,
    val puzzles: Int,
    val achievedAt: Long,
)

/** The free roam run in progress per language + difficulty. */
@Entity(tableName = "free_roam_runs")
data class FreeRoamRunEntity(
    @PrimaryKey val key: String,
    val runId: String,
    val score: Int,
    val cleared: Int,
)

@Entity(tableName = "saved_games")
data class SavedGameEntity(
    @PrimaryKey val modeKey: String,
    val modeJson: String,
    val stateJson: String,
    val updatedAt: Long,
)
