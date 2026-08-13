package com.vaultgame.core.physics

/** One discrete input gesture, decoded by the app's swipe detector and queued for the next tick. */
enum class PlayerAction {
    MOVE_LEFT,
    MOVE_RIGHT,
    JUMP,
    SLIDE,
}
