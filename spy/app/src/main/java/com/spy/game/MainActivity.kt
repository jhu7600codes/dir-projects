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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spy.game.data.GamePhase
import com.spy.game.ui.screens.DemoScreen
import com.spy.game.ui.screens.EliminationScreen
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
    // The tutorial/demo lives outside GamePhase entirely -- it's a scripted
    // walkthrough reachable from Setup, not a state the real game ever
    // enters, so it's just a plain boolean overlaying the phase switch below.
    var showDemo by remember { mutableStateOf(false) }

    if (showDemo) {
        DemoScreen(onDone = { showDemo = false })
        return
    }

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
                onShowDemo = { showDemo = true },
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
                hintRoundNumber = viewModel.hintRoundNumber,
                discussionStarted = viewModel.discussionStarted,
                timerSeconds = viewModel.timerSeconds,
                onAdvanceHintRound = viewModel::advanceHintRound,
                onStartDiscussion = viewModel::startDiscussion,
                onCallMeetingEarly = viewModel::callMeeting,
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

            GamePhase.ELIMINATION -> EliminationScreen(
                eliminatedPlayer = viewModel.lastOutcome?.eliminatedPlayer,
                onFinished = viewModel::finishElimination,
            )

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
