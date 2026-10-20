package com.jhu.smashrooms

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** what a fighter is told to do this frame. humans and cpus both fill one of these. */
class Pad {
    var x = 0f
    var y = 0f
    var atk = false
    var spc = false
    var jmp = false
    var shd = false
    var sup = false

    var atkP = false; private set
    var spcP = false; private set
    var jmpP = false; private set
    var shdP = false; private set
    var supP = false; private set
    /** frames left on a fast stick flick, sign = direction. smash attacks and tap jump read these */
    var flickX = 0; private set
    var flickY = 0; private set

    private var pa = false
    private var ps = false
    private var pj = false
    private var pd = false
    private var pu = false
    private var lowX = 0
    private var lowY = 0
    private var px = 0f
    private var py = 0f

    fun clear() {
        x = 0f; y = 0f; atk = false; spc = false; jmp = false; shd = false; sup = false
    }

    fun latch(tapJump: Boolean) {
        atkP = atk && !pa; spcP = spc && !ps; jmpP = jmp && !pj; shdP = shd && !pd; supP = sup && !pu
        pa = atk; ps = spc; pj = jmp; pd = shd; pu = sup
        if (abs(x) < 0.3f) lowX = 0 else lowX++
        if (abs(y) < 0.3f) lowY = 0 else lowY++
        if (flickX != 0) flickX -= Integer.signum(flickX)
        if (flickY != 0) flickY -= Integer.signum(flickY)
        if (abs(x) > 0.8f && abs(px) <= 0.8f && lowX <= 5) flickX = if (x > 0) 6 else -6
        if (abs(y) > 0.8f && abs(py) <= 0.8f && lowY <= 5) flickY = if (y > 0) 6 else -6
        if (tapJump && y < -0.8f && py >= -0.8f && lowY <= 5) jmpP = true
        px = x; py = y
    }

    /** anything at all being pushed. a-90's stop curse punishes this */
    fun anything() = abs(x) > 0.35f || abs(y) > 0.5f || atk || spc || jmp || shd

    fun copyFrom(o: Pad) {
        x = o.x; y = o.y; atk = o.atk; spc = o.spc; jmp = o.jmp; shd = o.shd; sup = o.sup
    }
}

/** keyboard + gamepad for player 1 */
class KeyPad {
    private val held = HashSet<Int>()
    var ax = 0f
    var ay = 0f

    fun key(e: KeyEvent): Boolean {
        val k = e.keyCode
        if (k !in MAPPED) return false
        if (e.action == KeyEvent.ACTION_DOWN) held.add(k) else if (e.action == KeyEvent.ACTION_UP) held.remove(k)
        return true
    }

    fun motion(e: MotionEvent): Boolean {
        if (e.source and InputDevice.SOURCE_JOYSTICK != InputDevice.SOURCE_JOYSTICK || e.action != MotionEvent.ACTION_MOVE) return false
        ax = e.getAxisValue(MotionEvent.AXIS_X).let { if (abs(it) < 0.2f) 0f else it }
        ay = e.getAxisValue(MotionEvent.AXIS_Y).let { if (abs(it) < 0.2f) 0f else it }
        val hx = e.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hy = e.getAxisValue(MotionEvent.AXIS_HAT_Y)
        if (hx != 0f) ax = hx
        if (hy != 0f) ay = hy
        return true
    }

    private fun h(vararg ks: Int) = ks.any { it in held }

    fun fill(p: Pad) {
        var x = ax
        var y = ay
        if (h(KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT)) x = -1f
        if (h(KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT)) x = 1f
        if (h(KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP)) y = -1f
        if (h(KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DPAD_DOWN)) y = 1f
        if (abs(x) > abs(p.x)) p.x = x
        if (abs(y) > abs(p.y)) p.y = y
        p.atk = p.atk || h(KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_BUTTON_X)
        p.spc = p.spc || h(KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_BUTTON_B)
        p.jmp = p.jmp || h(KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_Y)
        p.shd = p.shd || h(KeyEvent.KEYCODE_L, KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1)
        p.sup = p.sup || h(KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.KEYCODE_BUTTON_L2)
    }

    companion object {
        val MAPPED = setOf(
            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_S,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_L, KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_ENTER,
        )
    }
}

/** on screen stick + buttons, drawn with the-rooms' own touch icons */
class TouchControls(private val g: GameView) {
    private class Btn(val name: String, val icon: String, val label: String) {
        var cx = 0f; var cy = 0f; var r = 0f
        var down = false
        val ids = HashSet<Int>()
    }

    private val atk = Btn("atk", "touch_use", "attack")
    private val spc = Btn("spc", "touch_item", "special")
    private val jmp = Btn("jmp", "touch_run", "jump")
    private val shd = Btn("shd", "touch_crouch", "shield")
    private val sup = Btn("sup", "touch_item", "super")
    private val btns = listOf(atk, spc, jmp, shd, sup)
    var superReady = false

    private var stickId = -1
    private var baseX = 0f
    private var baseY = 0f
    private var knobX = 0f
    private var knobY = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val rect = RectF()

    private fun layout() {
        val s = g.s
        val r = 62f * s
        val rx = g.w - 120f * s
        val by = g.h - 130f * s
        atk.cx = rx; atk.cy = by; atk.r = r
        spc.cx = rx - 150f * s; spc.cy = by + 30f * s; spc.r = r
        jmp.cx = rx + 10f * s; jmp.cy = by - 150f * s; jmp.r = r * 0.85f
        shd.cx = rx - 140f * s; shd.cy = by - 120f * s; shd.r = r * 0.75f
        sup.cx = rx - 270f * s; sup.cy = by - 60f * s; sup.r = r * 0.8f
    }

    fun touch(e: MotionEvent) {
        layout()
        val ai = e.actionIndex
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> down(e.getPointerId(ai), e.getX(ai), e.getY(ai))
            MotionEvent.ACTION_MOVE -> for (i in 0 until e.pointerCount) move(e.getPointerId(i), e.getX(i), e.getY(i))
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> up(e.getPointerId(ai))
            MotionEvent.ACTION_CANCEL -> {
                stickId = -1
                btns.forEach { it.ids.clear(); it.down = false }
            }
        }
    }

    private fun hitBtn(x: Float, y: Float): Btn? =
        btns.filter { it !== sup || superReady }.minByOrNull { hypot(it.cx - x, it.cy - y) / it.r }
            ?.takeIf { hypot(it.cx - x, it.cy - y) < it.r * 1.35f }

    private fun down(id: Int, x: Float, y: Float) {
        if (x < g.w * 0.42f && stickId == -1) {
            stickId = id
            baseX = x; baseY = y; knobX = x; knobY = y
            return
        }
        hitBtn(x, y)?.let { it.ids.add(id); it.down = true }
    }

    private fun move(id: Int, x: Float, y: Float) {
        if (id == stickId) {
            val max = 90f * g.s
            var dx = x - baseX
            var dy = y - baseY
            val d = hypot(dx, dy)
            if (d > max * 1.6f) {
                // drag the base along so the stick never feels stuck
                baseX = x - dx / d * max * 1.6f
                baseY = y - dy / d * max * 1.6f
                dx = x - baseX; dy = y - baseY
            }
            knobX = x; knobY = y
            return
        }
        // sliding a thumb between buttons works like on a real pad
        val b = hitBtn(x, y)
        for (o in btns) if (o !== b && o.ids.remove(id)) o.down = o.ids.isNotEmpty()
        if (b != null) { b.ids.add(id); b.down = true }
    }

    private fun up(id: Int) {
        if (id == stickId) { stickId = -1; return }
        for (b in btns) if (b.ids.remove(id)) b.down = b.ids.isNotEmpty()
    }

    fun fill(p: Pad) {
        if (stickId != -1) {
            val max = 90f * g.s
            p.x = ((knobX - baseX) / max).coerceIn(-1f, 1f)
            p.y = ((knobY - baseY) / max).coerceIn(-1f, 1f)
        }
        p.atk = atk.down; p.spc = spc.down; p.jmp = jmp.down; p.shd = shd.down; p.sup = sup.down && superReady
    }

    fun draw(c: Canvas) {
        layout()
        val s = g.s
        if (stickId != -1) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f * s
            paint.color = 0x55ece6dc
            c.drawCircle(baseX, baseY, 90f * s, paint)
            val max = 90f * s
            val dx = knobX - baseX
            val dy = knobY - baseY
            val d = hypot(dx, dy)
            val k = if (d > max) max / d else 1f
            val kx = baseX + dx * k
            val ky = baseY + dy * k
            val kr = 46f * s
            rect.set(kx - kr, ky - kr, kx + kr, ky + kr)
            paint.alpha = 200
            c.drawBitmap(Assets.img("touch_knob"), null, rect, paint)
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f * s
            paint.color = 0x30ece6dc
            c.drawCircle(150f * s, g.h - 150f * s, 90f * s, paint)
        }
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Assets.mono
        for (b in btns) {
            if (b === sup && !superReady) continue
            paint.style = Paint.Style.FILL
            paint.color = when {
                b === sup -> if (b.down || g.frame / 8 % 2 == 0L) 0xeea3161c.toInt() else 0x88a3161c.toInt()
                b.down -> 0x88d8b25a.toInt()
                else -> 0x44000000
            }
            c.drawCircle(b.cx, b.cy, b.r, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f * s
            paint.color = if (b === sup) 0xffd8b25a.toInt() else 0x88ece6dc.toInt()
            c.drawCircle(b.cx, b.cy, b.r, paint)
            val ir = b.r * 0.55f
            rect.set(b.cx - ir, b.cy - ir - 6f * s, b.cx + ir, b.cy + ir - 6f * s)
            paint.alpha = if (b.down) 255 else 190
            c.save()
            if (b === jmp) c.rotate(-90f, rect.centerX(), rect.centerY())
            c.drawBitmap(Assets.img(b.icon), null, rect, paint)
            c.restore()
            paint.style = Paint.Style.FILL
            paint.color = 0xddece6dc.toInt()
            paint.textSize = 15f * s
            c.drawText(b.label, b.cx, b.cy + b.r * 0.78f, paint)
        }
    }

    companion object {
        fun clamp(v: Float, a: Float, b: Float) = max(a, min(b, v))
    }
}
