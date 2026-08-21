package com.spy.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spy.game.data.GamePhase
import com.spy.game.ui.screens.EndScreen
import com.spy.game.ui.screens.PlayScreen
import com.spy.game.ui.screens.ResultScreen
import com.spy.game.ui.screens.RevealScreen
import com.spy.game.ui.screens.SetupScreen
import com.spy.game.ui.screens.VoteScreen
import com.spy.game.ui.theme.SpyTheme
import com.spy.game.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    SpyApp()
                }
            }
        }
    }
}

@Composable
private fun SpyApp(viewModel: GameViewModel = viewModel()) {
    // The whole game is one state machine driven by GamePhase -- every
    // screen below just renders viewModel's current state and reports
    // user actions back to it, with no navigation back-stack to manage.
    AnimatedContent(
        targetState = viewModel.phase,
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
        label = "game-phase",
    ) { phase ->
        when (phase) {
            GamePhase.SETUP -> SetupScreen(
                onStartGame = viewModel::startGame,
            )

            GamePhase.REVEAL -> {
                val player = viewModel.currentRevealPlayer
                if (player != null) {
                    RevealScreen(
                        player = player,
                        wordEntry = viewModel.wordEntry,
                        playerNumber = viewModel.revealIndex + 1,
                        totalPlayers = viewModel.players.size,
                        onNext = viewModel::advanceReveal,
                    )
                }
            }

            GamePhase.PLAY -> PlayScreen(
                activePlayers = viewModel.activePlayers,
                timerSeconds = viewModel.timerSeconds,
                timerRunning = viewModel.timerRunning,
                onToggleTimer = viewModel::toggleTimer,
                onResetTimer = viewModel::resetTimer,
                onCallMeeting = viewModel::callMeeting,
            )

            GamePhase.VOTE -> {
                val voter = viewModel.currentVoter
                if (voter != null) {
                    VoteScreen(
                        voter = voter,
                        candidates = viewModel.voteOrder.filter { it.id != voter.id },
                        voterNumber = viewModel.voterIndex + 1,
                        totalVoters = viewModel.voteOrder.size,
                        onVote = viewModel::castVote,
                    )
                }
            }

            GamePhase.RESULT -> {
                val outcome = viewModel.lastOutcome
                if (outcome != null) {
                    ResultScreen(
                        outcome = outcome,
                        onContinue = viewModel::proceedFromResult,
                    )
                }
            }

            GamePhase.END -> {
                val winner = viewModel.winner
                if (winner != null) {
                    EndScreen(
                        winner = winner,
                        wordEntry = viewModel.wordEntry,
                        spy = viewModel.players.find { it.isSpy },
                        onNewGame = viewModel::newGame,
                    )
                }
            }
        }
    }
}
