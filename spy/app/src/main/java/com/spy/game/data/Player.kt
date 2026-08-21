package com.spy.game.data

/**
 * A single player in the current game.
 *
 * [id] is a stable index assigned at game start (position in the shuffled
 * player order) and is used everywhere else in the game state -- votes,
 * elimination, the reveal cursor -- instead of the name, since two players
 * are allowed to share a name.
 */
data class Player(
    val id: Int,
    val name: String,
    val isSpy: Boolean = false,
    val isEliminated: Boolean = false,
)
