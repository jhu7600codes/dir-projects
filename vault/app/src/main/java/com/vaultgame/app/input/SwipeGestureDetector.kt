package com.vaultgame.app.input

import android.view.MotionEvent
import com.vaultgame.core.physics.PlayerAction
import kotlin.math.abs

/**
 * Converts a raw touch gesture into at most one [PlayerAction] per finger-down/up cycle: swipe
 * left/right switches lanes, swipe up jumps, swipe down slides. Fires on [MotionEvent.ACTION_UP]
 * once the drag clears [MIN_SWIPE_DISTANCE_PX] in whichever axis moved further -- a tap with no
 * real movement is ignored rather than treated as a swipe.
 */
class SwipeGestureDetector(private val onAction: (PlayerAction) -> Unit) {
    private var startX = 0f
    private var startY = 0f
    private var tracking = false

    fun onTouchEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                tracking = true
            }
            MotionEvent.ACTION_UP -> {
                if (tracking) resolveSwipe(event.x - startX, event.y - startY)
                tracking = false
            }
            MotionEvent.ACTION_CANCEL -> tracking = false
        }
    }

    private fun resolveSwipe(dx: Float, dy: Float) {
        val action = when {
            abs(dx) < MIN_SWIPE_DISTANCE_PX && abs(dy) < MIN_SWIPE_DISTANCE_PX -> null
            abs(dx) >= abs(dy) -> if (dx > 0) PlayerAction.MOVE_RIGHT else PlayerAction.MOVE_LEFT
            else -> if (dy > 0) PlayerAction.SLIDE else PlayerAction.JUMP
        }
        action?.let(onAction)
    }

    private companion object {
        const val MIN_SWIPE_DISTANCE_PX = 60f
    }
}
