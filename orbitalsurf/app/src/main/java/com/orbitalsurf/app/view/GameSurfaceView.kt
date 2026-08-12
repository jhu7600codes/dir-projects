package com.orbitalsurf.app.view

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import com.orbitalsurf.app.input.GameInputState
import com.orbitalsurf.app.input.TouchSteerController
import com.orbitalsurf.app.render.GameRenderer

/** Thin GLSurfaceView subclass: wires touch events to a [TouchSteerController] and hosts a [GameRenderer]. */
class GameSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {

    private var touchController: TouchSteerController? = null

    /** Must be called once before this view starts rendering. */
    fun start(renderer: GameRenderer, inputState: GameInputState) {
        setEGLContextClientVersion(2)
        // Explicit config: RGBA8888 + a 16-bit depth buffer (no stencil) -- the default
        // chooser isn't guaranteed to include a depth buffer, and this game relies on
        // GL_DEPTH_TEST for correct occlusion between buildings/ball/pickups.
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        touchController = TouchSteerController(inputState)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        touchController?.onTouchEvent(event, width)
        return true
    }
}
