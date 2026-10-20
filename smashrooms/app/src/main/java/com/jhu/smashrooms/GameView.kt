package com.jhu.smashrooms

import android.content.Context
import android.graphics.Canvas
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

/** one screen at a time, all on a fixed 60hz tick. touch and keys are handled under the same lock as update/draw. */
abstract class Screen(val g: GameView) {
    open fun update() {}
    abstract fun draw(c: Canvas)
    open fun touch(e: MotionEvent) {}
    /** true = handled */
    open fun back(): Boolean = false
    open fun enter() {}
}

class GameView(ctx: Context) : SurfaceView(ctx), SurfaceHolder.Callback {
    val lock = Any()
    var w = 1280f
    var h = 720f
    /** ui scale, 1 = 720p tall */
    val s get() = h / 720f

    val keys = KeyPad()
    var settings = Settings()

    private var screen: Screen = TitleScreen(this)
    private var thread: Thread? = null
    @Volatile private var running = false
    @Volatile private var hasSurface = false
    var frame = 0L

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun go(next: Screen) {
        screen = next
        next.enter()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        hasSurface = true
        startThread()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        synchronized(lock) {
            w = width.toFloat(); h = height.toFloat()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        hasSurface = false
        stopThread()
    }

    fun resumeLoop() {
        if (hasSurface) startThread()
    }

    fun pauseLoop() {
        synchronized(lock) { (screen as? BattleScreen)?.autoPause() }
        stopThread()
    }

    private fun startThread() {
        if (running) return
        running = true
        screen.enter()
        thread = Thread({ loop() }, "game").also { it.start() }
    }

    private fun stopThread() {
        running = false
        thread?.join(500)
        thread = null
    }

    private fun loop() {
        val step = 1_000_000_000L / 60
        var last = System.nanoTime()
        var acc = 0L
        while (running) {
            val now = System.nanoTime()
            acc += now - last
            last = now
            if (acc > step * 5) acc = step * 5
            synchronized(lock) {
                while (acc >= step) {
                    screen.update()
                    frame++
                    acc -= step
                }
            }
            if (!hasSurface) break
            val c = try { holder.lockHardwareCanvas() } catch (_: Exception) { null } ?: continue
            try {
                synchronized(lock) { screen.draw(c) }
            } finally {
                try { holder.unlockCanvasAndPost(c) } catch (_: Exception) {}
            }
            val spare = step - (System.nanoTime() - now)
            if (spare > 2_000_000) Thread.sleep(spare / 1_000_000 - 1)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        synchronized(lock) { screen.touch(event) }
        return true
    }

    fun back(): Boolean = synchronized(lock) { screen.back() }

    fun key(e: KeyEvent): Boolean = synchronized(lock) {
        val used = keys.key(e)
        if (used && e.action == KeyEvent.ACTION_DOWN && e.repeatCount == 0) {
            if (e.keyCode == KeyEvent.KEYCODE_BUTTON_START || e.keyCode == KeyEvent.KEYCODE_ESCAPE) {
                if (!screen.back()) (screen as? BattleScreen)?.togglePause()
            }
        }
        used
    }

    fun joystick(e: MotionEvent): Boolean = synchronized(lock) { keys.motion(e) }
}

data class Settings(
    var p1: Int = 0,
    var cpus: MutableList<Int> = mutableListOf(-1),
    var level: Int = 5,
    var stocks: Int = 3,
    var stage: Int = 0,
    var items: Boolean = true,
    var training: Boolean = false,
)
