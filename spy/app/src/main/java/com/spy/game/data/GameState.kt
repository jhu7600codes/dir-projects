package com.spy.game.data

/** Which screen the game is currently on. */
enum class GamePhase {
    SETUP,
    REVEAL,
    PLAY,
    VOTE,
    RESULT,
    END,
}

/** Who won, once the game has reached [GamePhase.END]. */
enum class Winner {
    CIVILIANS,
    SPY,
}

/** Outcome of a single vote round, computed in [GamePhase.RESULT]. */
data class VoteOutcome(
    val eliminatedPlayer: Player?,
    val wasSpy: Boolean,
    val remainingActiveCount: Int,
    val totalPlayerCount: Int,
)

/** Default length of the discussion timer in [GamePhase.PLAY], in seconds. */
const val DEFAULT_TIMER_SECONDS = 180
