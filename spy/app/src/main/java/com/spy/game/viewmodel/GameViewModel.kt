package com.spy.game.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spy.game.data.DEFAULT_TIMER_SECONDS
import com.spy.game.data.GamePhase
import com.spy.game.data.Player
import com.spy.game.data.VoteOutcome
import com.spy.game.data.WordBank
import com.spy.game.data.WordEntry
import com.spy.game.data.Winner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Single source of truth for a game of Spy. Everything is in-memory only --
 * there's no backend and nothing is persisted, so a process death just means
 * a new game.
 *
 * This is an [AndroidViewModel] (not a plain [androidx.lifecycle.ViewModel])
 * solely so [WordBank.loadDictionary] has a [android.content.Context] to
 * read `assets/words_ru.txt` from.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    init {
        WordBank.loadDictionary(application)
    }

    var phase by mutableStateOf(GamePhase.SETUP)
        private set

    var players by mutableStateOf<List<Player>>(emptyList())
        private set

    var wordEntry by mutableStateOf<WordEntry?>(null)
        private set

    /** Index into [players] of whose turn it is in the reveal (pass-the-phone) phase. */
    var revealIndex by mutableStateOf(0)
        private set

    var timerSeconds by mutableStateOf(DEFAULT_TIMER_SECONDS)
        private set

    var timerRunning by mutableStateOf(false)
        private set

    private var timerJob: Job? = null

    /**
     * How many words each hint is allowed to be in the current round of
     * [GamePhase.PLAY], before discussion has started -- 1 in the first
     * round, 2 in the next, and so on for as long as nobody's ready to
     * accuse anyone yet.
     */
    var hintRoundNumber by mutableStateOf(1)
        private set

    /** True once the big red button has been pressed and the 3-minute discussion timer is running. */
    var discussionStarted by mutableStateOf(false)
        private set

    /** Active players at the moment the current meeting was called, in voting order. */
    var voteOrder by mutableStateOf<List<Player>>(emptyList())
        private set

    var voterIndex by mutableStateOf(0)
        private set

    /** voterId -> targetId, where a null target means that voter skipped. */
    var votes by mutableStateOf<Map<Int, Int?>>(emptyMap())
        private set

    var lastOutcome by mutableStateOf<VoteOutcome?>(null)
        private set

    var winner by mutableStateOf<Winner?>(null)
        private set

    val activePlayers: List<Player> get() = players.filterNot { it.isEliminated }

    val currentRevealPlayer: Player? get() = players.getOrNull(revealIndex)

    val currentVoter: Player? get() = voteOrder.getOrNull(voterIndex)

    fun startGame(names: List<String>) {
        val shuffled = names.map { it.trim() }.filter { it.isNotEmpty() }.shuffled()
        val spyIndex = shuffled.indices.random()
        players = shuffled.mapIndexed { index, name ->
            Player(id = index, name = name, isSpy = index == spyIndex)
        }
        wordEntry = WordBank.random()
        revealIndex = 0
        resetDiscussion()
        lastOutcome = null
        winner = null
        phase = GamePhase.REVEAL
    }

    fun advanceReveal() {
        if (revealIndex < players.size - 1) {
            revealIndex += 1
        } else {
            phase = GamePhase.PLAY
        }
    }

    /** "Никто не догадался" -- nobody's ready to accuse anyone, so hints get one word longer. */
    fun advanceHintRound() {
        if (!discussionStarted) hintRoundNumber += 1
    }

    /**
     * The big red button: "I think I know who the spy is." Starts the
     * 3-minute discussion timer, which auto-calls the meeting at zero.
     */
    fun startDiscussion() {
        if (discussionStarted) return
        discussionStarted = true
        timerSeconds = DEFAULT_TIMER_SECONDS
        startTimer()
    }

    private fun startTimer() {
        if (timerRunning || timerSeconds <= 0) return
        timerRunning = true
        timerJob = viewModelScope.launch {
            while (timerRunning && timerSeconds > 0) {
                delay(1000)
                if (!timerRunning) break
                timerSeconds -= 1
            }
            if (timerSeconds <= 0) {
                timerRunning = false
                callMeeting()
            }
        }
    }

    private fun pauseTimer() {
        timerRunning = false
        timerJob?.cancel()
        timerJob = null
    }

    /** Resets the whole hint-round/discussion state back to a fresh round 1 -- called at the start of every PLAY phase. */
    private fun resetDiscussion() {
        pauseTimer()
        timerSeconds = DEFAULT_TIMER_SECONDS
        hintRoundNumber = 1
        discussionStarted = false
    }

    /** "Голосовать досрочно" during discussion, or the automatic call when the timer hits zero. */
    fun callMeeting() {
        pauseTimer()
        voteOrder = activePlayers
        voterIndex = 0
        votes = emptyMap()
        phase = GamePhase.VOTE
    }

    /** [targetId] null means the current voter chose to skip. */
    fun castVote(targetId: Int?) {
        val voter = currentVoter ?: return
        votes = votes + (voter.id to targetId)
        if (voterIndex < voteOrder.size - 1) {
            voterIndex += 1
        } else {
            tally()
        }
    }

    private fun tally() {
        val counts = mutableMapOf<Int, Int>()
        votes.values.filterNotNull().forEach { targetId ->
            counts[targetId] = (counts[targetId] ?: 0) + 1
        }
        val maxVotes = counts.values.maxOrNull() ?: 0
        val topTargets = counts.filterValues { it == maxVotes }.keys

        // A tie for the top spot, or everyone skipping, means nobody is eliminated.
        val eliminatedId = if (maxVotes > 0 && topTargets.size == 1) topTargets.first() else null
        val eliminated = players.find { it.id == eliminatedId }

        if (eliminated != null) {
            players = players.map { p -> if (p.id == eliminated.id) p.copy(isEliminated = true) else p }
        }

        lastOutcome = VoteOutcome(
            eliminatedPlayer = eliminated,
            wasSpy = eliminated?.isSpy == true,
            remainingActiveCount = activePlayers.size,
            totalPlayerCount = players.size,
        )
        // ELIMINATION plays a short "thrown out" animation, then calls
        // finishElimination() itself to move on to RESULT -- see EliminationScreen.
        phase = GamePhase.ELIMINATION
    }

    /** Called once the elimination animation has finished playing. */
    fun finishElimination() {
        phase = GamePhase.RESULT
    }

    /** Called from the result screen's "Далее" button. Decides win/continue. */
    fun proceedFromResult() {
        val outcome = lastOutcome ?: return
        when {
            outcome.wasSpy -> {
                winner = Winner.CIVILIANS
                phase = GamePhase.END
            }
            outcome.remainingActiveCount <= 2 -> {
                winner = Winner.SPY
                phase = GamePhase.END
            }
            else -> {
                resetDiscussion()
                phase = GamePhase.PLAY
            }
        }
    }

    fun newGame() {
        resetDiscussion()
        players = emptyList()
        wordEntry = null
        revealIndex = 0
        voteOrder = emptyList()
        voterIndex = 0
        votes = emptyMap()
        lastOutcome = null
        winner = null
        phase = GamePhase.SETUP
    }

    override fun onCleared() {
        pauseTimer()
        super.onCleared()
    }
}
