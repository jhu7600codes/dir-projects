package dev.jhu.crossi.data

import androidx.room.withTransaction
import dev.jhu.crossi.game.Difficulty
import dev.jhu.crossi.game.GameMode
import dev.jhu.crossi.game.GameState
import dev.jhu.crossi.game.Language
import dev.jhu.crossi.game.PowerUpType
import dev.jhu.crossi.game.Rewards
import dev.jhu.crossi.game.Streak
import dev.jhu.crossi.game.Streaks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.UUID

/** Everything the player has done: progress, power ups, streaks, scores, saved games. */
class GameRepository(private val db: CrossiDatabase) {
    private val dao = db.dao()

    // ---- levels ----

    fun levelProgress(language: Language): Flow<Map<Int, LevelProgressEntity>> =
        dao.levelProgress(language.code).map { list -> list.associateBy { it.levelId } }

    suspend fun recordLevel(language: Language, level: Int, stars: Int, timeMs: Long, checksUsed: Int) {
        val old = dao.levelProgress(language.code, level)
        dao.upsert(
            LevelProgressEntity(
                language = language.code,
                levelId = level,
                stars = maxOf(stars, old?.stars ?: 0),
                bestTimeMs = minOf(timeMs, old?.bestTimeMs ?: Long.MAX_VALUE),
                checksUsed = minOf(checksUsed, old?.checksUsed ?: Int.MAX_VALUE),
                completedAt = System.currentTimeMillis(),
            ),
        )
    }

    // ---- power ups ----

    val inventory: Flow<Map<PowerUpType, Int>> = dao.powerUps().map { list ->
        PowerUpType.entries.associateWith { type -> list.firstOrNull { it.type == type.name }?.count ?: 0 }
    }

    suspend fun grant(powerUps: List<PowerUpType>) {
        if (powerUps.isEmpty()) return
        db.withTransaction {
            powerUps.groupingBy { it }.eachCount().forEach { (type, n) ->
                val current = dao.powerUp(type.name)?.count ?: 0
                dao.upsert(PowerUpEntity(type.name, current + n))
            }
        }
    }

    /** Takes one, returns false if there were none. */
    suspend fun consume(type: PowerUpType): Boolean = db.withTransaction {
        val current = dao.powerUp(type.name)?.count ?: 0
        if (current <= 0) return@withTransaction false
        dao.upsert(PowerUpEntity(type.name, current - 1))
        true
    }

    // ---- dailies + streak ----

    val streak: Flow<Streak> = dao.streak().map { it?.let { s -> Streak(s.current, s.best, s.lastDate) } ?: Streak() }

    fun dailyResults(date: String, language: Language): Flow<List<DailyResultEntity>> =
        dao.dailyResults(date, language.code)

    val recentDailies: Flow<List<DailyResultEntity>> = dao.recentDailies()

    /** Saves a cleared daily, moves the streak, returns any power ups earned. */
    suspend fun recordDaily(
        language: Language,
        date: String,
        difficulty: Difficulty,
        timeMs: Long,
        checksUsed: Int,
        score: Int,
    ): List<PowerUpType> = db.withTransaction {
        val before = dao.dailyResultsNow(date, language.code)
        if (before.any { it.difficulty == difficulty.name && it.completed }) return@withTransaction emptyList()
        dao.insert(DailyResultEntity(date, difficulty.name, language.code, true, timeMs, checksUsed, score))
        val rewards = ArrayList<PowerUpType>()
        val old = dao.streakNow()?.let { Streak(it.current, it.best, it.lastDate) } ?: Streak()
        val updated = Streaks.onDailyCleared(old, LocalDate.parse(date))
        if (updated != old) {
            dao.upsert(StreakEntity(current = updated.current, best = updated.best, lastDate = updated.lastDate))
            if (updated.current != old.current) Rewards.streakMilestone(updated.current)?.let(rewards::addAll)
        }
        if (before.count { it.completed } == Difficulty.entries.size - 1) rewards += Rewards.ALL_DAILIES_BONUS
        rewards
    }

    suspend fun recordDailyFailed(language: Language, date: String, difficulty: Difficulty) {
        val before = dao.dailyResultsNow(date, language.code)
        if (before.none { it.difficulty == difficulty.name }) {
            dao.insert(DailyResultEntity(date, difficulty.name, language.code, false, 0, 0, 0))
        }
    }

    // ---- free roam ----

    private fun runKey(language: Language, difficulty: Difficulty) = "${language.code}:${difficulty.name}"

    fun run(language: Language, difficulty: Difficulty): Flow<FreeRoamRunEntity?> = dao.run(runKey(language, difficulty))

    fun highScores(language: Language, difficulty: Difficulty): Flow<List<HighScoreEntity>> =
        dao.highScores(language.code, difficulty.name)

    data class RunUpdate(val runScore: Int, val cleared: Int, val newBest: Boolean, val rewards: List<PowerUpType>)

    suspend fun recordFreeRoamClear(language: Language, difficulty: Difficulty, score: Int): RunUpdate =
        db.withTransaction {
            val key = runKey(language, difficulty)
            val run = dao.runNow(key) ?: FreeRoamRunEntity(key, UUID.randomUUID().toString(), 0, 0)
            val updated = run.copy(score = run.score + score, cleared = run.cleared + 1)
            dao.upsert(updated)
            val best = dao.bestScore(language.code, difficulty.name, run.runId) ?: 0
            dao.upsert(
                HighScoreEntity(run.runId, language.code, difficulty.name, updated.score, updated.cleared, System.currentTimeMillis()),
            )
            val rewards = Rewards.runRewards(run.score, updated.score).toMutableList()
            // beating your old best gives a bonus, once per run (the moment you pass it)
            if (best > 0 && run.score <= best && updated.score > best) rewards += PowerUpType.REVEAL_WORD
            RunUpdate(updated.score, updated.cleared, updated.score > best, rewards)
        }

    suspend fun endRun(language: Language, difficulty: Difficulty) = dao.deleteRun(runKey(language, difficulty))

    // ---- saved games ----

    suspend fun save(language: Language, mode: GameMode, state: GameState) {
        dao.upsert(
            SavedGameEntity(
                modeKey = mode.key(language),
                modeJson = json.encodeToString(GameMode.serializer(), mode),
                stateJson = json.encodeToString(GameState.serializer(), state),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun load(language: Language, mode: GameMode): GameState? =
        dao.savedGame(mode.key(language))?.let { runCatching { json.decodeFromString(GameState.serializer(), it.stateJson) }.getOrNull() }

    suspend fun deleteSave(language: Language, mode: GameMode) = dao.deleteSavedGame(mode.key(language))

    /** Most recent unfinished game in [language], for the home screen's continue button. */
    fun latestSave(language: Language): Flow<GameMode?> = dao.latestSavedGame("${language.code}:").map { entity ->
        entity?.let { runCatching { json.decodeFromString(GameMode.serializer(), it.modeJson) }.getOrNull() }
    }

    companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
