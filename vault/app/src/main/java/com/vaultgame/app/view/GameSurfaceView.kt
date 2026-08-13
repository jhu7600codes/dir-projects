package com.vaultgame.app.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import com.vaultgame.app.input.SwipeGestureDetector
import com.vaultgame.app.render.GameRenderer
import com.vaultgame.core.session.GameSession

/**
 * A plain [View] (not a GLSurfaceView/SurfaceView) driven by [Choreographer] frame callbacks.
 * This game's whole visual identity is flat-color procedural 2D shapes with a cheap pseudo-3D
 * projection (see [GameRenderer]) rather than real 3D geometry, so a Canvas on the UI thread is
 * simple, correct, and plenty fast -- there's no need for a GL context or a second render thread.
 */
class GameSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var session: GameSession? = null
    private var renderer: GameRenderer? = null
    private var swipeDetector: SwipeGestureDetector? = null
    private var running = false
    private var lastFrameTimeNanos: Long = 0L
    private var elapsedSeconds: Double = 0.0
    private var onFrame: ((GameSession) -> Unit)? = null

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            val dtSeconds = if (lastFrameTimeNanos == 0L) {
                1.0 / 60.0
            } else {
                ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000.0).coerceIn(0.0, MAX_FRAME_DT)
            }
            lastFrameTimeNanos = frameTimeNanos
            elapsedSeconds += dtSeconds

            val activeSession = session
            if (activeSession != null) {
                activeSession.tick(dtSeconds)
                onFrame?.invoke(activeSession)
                invalidate()
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    /** Must be called once before this view starts ticking/drawing. [onFrame] fires once per
     * tick, after physics, before the frame is drawn -- the host activity uses it to refresh the
     * HUD and to notice game-over. */
    fun start(session: GameSession, renderer: GameRenderer, onFrame: (GameSession) -> Unit) {
        this.session = session
        this.renderer = renderer
        this.onFrame = onFrame
        this.swipeDetector = SwipeGestureDetector { action -> session.queueAction(action) }
    }

    fun resumeLoop() {
        if (running) return
        running = true
        lastFrameTimeNanos = 0L
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun pauseLoop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    override fun onDraw(canvas: Canvas) {
        val activeSession = session ?: return
        renderer?.draw(canvas, activeSession, width, height, elapsedSeconds)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        swipeDetector?.onTouchEvent(event)
        return true
    }

    override fun onDetachedFromWindow() {
        pauseLoop()
        super.onDetachedFromWindow()
    }

    private companion object {
        /** Clamp huge dt spikes (app backgrounded mid-frame, debugger pause, etc.) so the
         * player never teleports through a whole segment's obstacles in one tick. */
        const val MAX_FRAME_DT = 0.1
    }
}
