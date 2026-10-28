package dev.jhu.crossi.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jhu.crossi.data.LanguageManager
import dev.jhu.crossi.data.Settings
import dev.jhu.crossi.di.AppContainer
import dev.jhu.crossi.game.BossTwist
import dev.jhu.crossi.game.CheckResult
import dev.jhu.crossi.game.Difficulty
import dev.jhu.crossi.game.GameEngine
import dev.jhu.crossi.game.GameMode
import dev.jhu.crossi.game.GameState
import dev.jhu.crossi.game.LevelCatalog
import dev.jhu.crossi.game.PowerUpType
import dev.jhu.crossi.game.Rewards
import dev.jhu.crossi.game.Rules
import dev.jhu.crossi.game.Scoring
import dev.jhu.crossi.game.Seeds
import dev.jhu.crossi.game.Status
import dev.jhu.crossi.data.ContentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Phase {
    data object Loading : Phase
    data object Error : Phase
    data object Ready : Phase
}

data class ResultInfo(
    val won: Boolean,
    val state: GameState,
    val stars: Int? = null,
    val score: Int? = null,
    val runScore: Int? = null,
    val newBest: Boolean = false,
    val rewards: List<PowerUpType> = emptyList(),
    val next: GameMode? = null,
    val retry: GameMode? = null,
)

sealed interface GameEvent {
    data class Checked(val result: CheckResult) : GameEvent
    data object NoneLeft : GameEvent
    data object CantUse : GameEvent
    data object Typed : GameEvent
}

class GameViewModel(val mode: GameMode, private val c: AppContainer) : ViewModel() {
    val language = LanguageManager.current()

    private val _phase = MutableStateFlow<Phase>(Phase.Loading)
    val phase: StateFlow<Phase> = _phase.asStateFlow()

    private val _game = MutableStateFlow<GameState?>(null)
    val game: StateFlow<GameState?> = _game.asStateFlow()

    private val _result = MutableStateFlow<ResultInfo?>(null)
    val result: StateFlow<ResultInfo?> = _result.asStateFlow()

    /** Boss levels show their twists once before you start. */
    private val _bossIntro = MutableStateFlow<List<BossTwist>>(emptyList())
    val bossIntro: StateFlow<List<BossTwist>> = _bossIntro.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<GameEvent> = _events

    val settings: StateFlow<Settings> = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
    val inventory: StateFlow<Map<PowerUpType, Int>> =
        c.games.inventory.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private var ticker: Job? = null
    private var saveJob: Job? = null

    init {
        load()
    }

    fun load() {
        _phase.value = Phase.Loading
        viewModelScope.launch {
            try {
                val saved = c.games.load(language, mode)?.takeIf { it.status == Status.PLAYING }
                val state = saved ?: newGame().also { fresh ->
                    if (fresh.rules.twists.isNotEmpty()) _bossIntro.value = fresh.rules.twists
                }
                _game.value = state
                _result.value = null
                _phase.value = Phase.Ready
            } catch (e: Exception) {
                _phase.value = Phase.Error
            }
        }
    }

    private suspend fun newGame(): GameState = when (mode) {
        is GameMode.Level -> {
            val puzzle = c.content.level(language, mode.number)
            val spec = LevelCatalog.spec(mode.number)
            val rules = Rules(spec.twists, LevelCatalog.pickups(spec, puzzle.entries.size))
            GameEngine.start(puzzle, rules, Seeds.level(mode.number, language) xor System.nanoTime())
        }
        is GameMode.FreeRoam -> GameEngine.start(c.content.freeRoamPuzzle(language, mode.difficulty))
        is GameMode.Daily -> {
            val seed = Seeds.daily(mode.date, mode.difficulty, language, ContentRepository.dictionaryVersion(language))
            GameEngine.start(c.content.generate(language, mode.difficulty, seed))
        }
    }

    fun dismissBossIntro() {
        _bossIntro.value = emptyList()
    }

    /** Timer only runs while the screen is in front. */
    fun setActive(active: Boolean) {
        ticker?.cancel()
        if (!active) {
            _game.value?.let { saveNow(it) }
            return
        }
        ticker = viewModelScope.launch {
            while (true) {
                delay(TICK_MS)
                if (_bossIntro.value.isEmpty()) update { GameEngine.tick(it, TICK_MS) }
            }
        }
    }

    // ---- input ----

    fun type(letter: Char) {
        update { GameEngine.type(it, letter, settings.value.inputPrefs) }
        _events.tryEmit(GameEvent.Typed)
    }

    fun backspace() {
        update(GameEngine::backspace)
        _events.tryEmit(GameEvent.Typed)
    }

    fun tap(cell: Int) = update { GameEngine.tap(it, cell) }
    fun toggleDirection() = update(GameEngine::toggleDirection)
    fun selectEntry(entry: Int) = update { GameEngine.selectEntry(it, entry) }
    fun moveEntry(delta: Int) = update { GameEngine.moveEntry(it, delta) }
    fun togglePencil() = update(GameEngine::togglePencil)

    fun finish() {
        val state = _game.value ?: return
        if (!GameEngine.canFinish(state)) return
        val (next, result) = GameEngine.finish(state)
        _events.tryEmit(GameEvent.Checked(result))
        update { next }
    }

    fun giveUp() = update(GameEngine::giveUp)

    fun usePowerUp(type: PowerUpType) {
        val state = _game.value ?: return
        if ((inventory.value[type] ?: 0) <= 0) {
            _events.tryEmit(GameEvent.NoneLeft)
            return
        }
        val next = GameEngine.use(state, type)
        if (next == null) {
            _events.tryEmit(GameEvent.CantUse)
            return
        }
        viewModelScope.launch {
            if (c.games.consume(type)) update { next }
        }
    }

    private fun update(change: (GameState) -> GameState) {
        val before = _game.value ?: return
        val after = change(before)
        if (after === before) return
        _game.value = after
        if (before.status == Status.PLAYING && after.status != Status.PLAYING) {
            onGameOver(after)
        } else {
            scheduleSave(after)
        }
    }

    private fun scheduleSave(state: GameState) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            c.games.save(language, mode, state)
        }
    }

    private fun saveNow(state: GameState) {
        if (state.status != Status.PLAYING) return
        saveJob?.cancel()
        c.appScope.launch { c.games.save(language, mode, state) }
    }

    /** Records the result, hands out rewards, and builds the result card. Runs in the app scope so leaving can't cut it off. */
    private fun onGameOver(state: GameState) {
        saveJob?.cancel()
        ticker?.cancel()
        val won = state.status == Status.WON
        c.appScope.launch {
            c.games.deleteSave(language, mode)
            val info = when (mode) {
                is GameMode.Level -> {
                    val spec = LevelCatalog.spec(mode.number)
                    if (won) {
                        val stars = Scoring.stars(state)
                        c.games.recordLevel(language, mode.number, stars, state.elapsedMs, state.checksUsed)
                        val rewards = GameEngine.earnedPickups(state) + if (spec.isBoss) Rewards.BOSS_BUNDLE else emptyList()
                        c.games.grant(rewards)
                        ResultInfo(
                            won = true, state = state, stars = stars, rewards = rewards,
                            next = if (mode.number < LevelCatalog.COUNT) GameMode.Level(mode.number + 1) else null,
                        )
                    } else {
                        ResultInfo(won = false, state = state, retry = mode)
                    }
                }
                is GameMode.FreeRoam -> {
                    if (won) {
                        val score = Scoring.score(state, mode.difficulty)
                        val run = c.games.recordFreeRoamClear(language, mode.difficulty, score)
                        c.games.grant(run.rewards)
                        ResultInfo(
                            won = true, state = state, score = score, runScore = run.runScore,
                            newBest = run.newBest, rewards = run.rewards, next = mode,
                        )
                    } else {
                        c.games.endRun(language, mode.difficulty)
                        ResultInfo(won = false, state = state, retry = mode)
                    }
                }
                is GameMode.Daily -> {
                    if (won) {
                        val score = Scoring.score(state, mode.difficulty)
                        val rewards = c.games.recordDaily(language, mode.date, mode.difficulty, state.elapsedMs, state.checksUsed, score)
                        c.games.grant(rewards)
                        ResultInfo(won = true, state = state, score = score, rewards = rewards, next = nextDaily(mode))
                    } else {
                        c.games.recordDailyFailed(language, mode.date, mode.difficulty)
                        ResultInfo(won = false, state = state, next = nextDaily(mode))
                    }
                }
            }
            _result.value = info
        }
    }

    private fun nextDaily(mode: GameMode.Daily): GameMode? =
        Difficulty.entries.getOrNull(mode.difficulty.ordinal + 1)?.let { GameMode.Daily(mode.date, it) }

    override fun onCleared() {
        _game.value?.let { saveNow(it) }
    }

    companion object {
        private const val TICK_MS = 1000L
        private const val SAVE_DEBOUNCE_MS = 600L
    }
}
