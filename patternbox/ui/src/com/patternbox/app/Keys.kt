package com.patternbox.app

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/**
 * keyboard controls. the window forwards every key here (desktop main / activity),
 * whichever fight screen is open plugs in a handler.
 */
object KeyBus {
    var handler: ((KeyEvent) -> Boolean)? = null
    fun dispatch(e: KeyEvent): Boolean = handler?.invoke(e) ?: false
}

enum class Act { LEFT, RIGHT, JUMP, HIT, ABILITY }

private val P1 = mapOf(
    Key.A to Act.LEFT, Key.D to Act.RIGHT, Key.W to Act.JUMP, Key.Spacebar to Act.JUMP,
    Key.F to Act.HIT, Key.G to Act.ABILITY,
)
private val P2 = mapOf(
    Key.DirectionLeft to Act.LEFT, Key.DirectionRight to Act.RIGHT, Key.DirectionUp to Act.JUMP,
    Key.Enter to Act.HIT, Key.NumPadEnter to Act.HIT, Key.L to Act.HIT,
    Key.ShiftRight to Act.ABILITY, Key.K to Act.ABILITY,
)

/** which player slot and action a key means. with one player both sets drive player 1 */
fun keyAction(key: Key, players: Int): Pair<Int, Act>? {
    P1[key]?.let { return 0 to it }
    P2[key]?.let { return (if (players >= 2) 1 else 0) to it }
    return null
}

/** true while held, false when let go; null for key repeats/other events */
fun KeyEvent.pressed(): Boolean? = when (type) {
    KeyEventType.KeyDown -> true
    KeyEventType.KeyUp -> false
    else -> null
}

fun keyHints(players: Int): String = if (players >= 2)
    "p1: a d move, w jump, f hit, g ability      p2: arrows, enter hit, right shift ability"
else "move: a d / arrows   jump: w / space / up   hit: f / enter   ability: g / right shift"
