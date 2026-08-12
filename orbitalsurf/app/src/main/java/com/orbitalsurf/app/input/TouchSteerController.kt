package com.orbitalsurf.app.input

import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.sign

/**
 * Converts raw touch events from `GameSurfaceView` into [GameInputState] updates: horizontal
 * drag maps to a continuous lateral axis, recentering to 0 the moment the finger lifts (like a
 * self-centering stick -- release and the ball goes straight; there are no lanes to snap to).
 * A short, low-movement touch counts as a jump tap instead of a drag.
 */
class TouchSteerController(private val inputState: GameInputState) {
    private var startX = 0f
    private var startY = 0f
    private var startTimeMillis = 0L
    private var movedPastSlop = false

    fun onTouchEvent(event: MotionEvent, viewWidthPx: Int) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                startTimeMillis = System.currentTimeMillis()
                movedPastSlop = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - startX
                val dy = event.y - startY
                if (abs(dx) > TAP_SLOP_PX || abs(dy) > TAP_SLOP_PX) movedPastSlop = true

                // A small deadzone right around the start point so tiny finger jitter while
                // trying to hold a straight line doesn't register as steering input at all.
                val deadzoned = if (abs(dx) <= DEADZONE_PX) 0f else dx - (DEADZONE_PX * sign(dx))
                val halfWidth = (viewWidthPx / 2).coerceAtLeast(1)
                val axis = deadzoned / (halfWidth * DRAG_SENSITIVITY)
                inputState.lateralAxis = axis.toDouble().coerceIn(-1.0, 1.0)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val elapsedMillis = System.currentTimeMillis() - startTimeMillis
                if (!movedPastSlop && elapsedMillis < TAP_TIME_MS) {
                    inputState.requestJump()
                }
                inputState.lateralAxis = 0.0
            }
        }
    }

    private companion object {
        const val TAP_SLOP_PX = 24f
        const val TAP_TIME_MS = 250L
        const val DEADZONE_PX = 16f
        // Fraction of half the screen width that counts as a "full" steering deflection --
        // higher = a longer, more deliberate drag needed for full lock, less twitchy than
        // requiring only a quarter of the screen width.
        const val DRAG_SENSITIVITY = 0.85f
    }
}
