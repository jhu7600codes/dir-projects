package com.orbitalsurf.app.input

import com.orbitalsurf.core.physics.SteerInput
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The bridge between touch events (delivered on the UI thread, via `TouchSteerController`)
 * and the physics tick (consumed on the GL render thread, via `GameRenderer.onDrawFrame`).
 * `lateralAxis` is a continuously-updated level (last-value-wins is fine for a steering axis);
 * `jumpPressed` is consume-once so a single tap yields exactly one jump, not a jump held every
 * frame until the next touch event arrives.
 */
class GameInputState {
    @Volatile
    var lateralAxis: Double = 0.0

    private val jumpRequested = AtomicBoolean(false)

    fun requestJump() {
        jumpRequested.set(true)
    }

    /** Called once per physics tick; returns and clears the pending jump flag. */
    fun consumeInput(): SteerInput {
        val jump = jumpRequested.getAndSet(false)
        return SteerInput(lateralAxis = lateralAxis, jumpPressed = jump)
    }
}
