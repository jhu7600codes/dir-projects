package com.jhu.smashrooms

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent

/** the-rooms site palette: dark walls, red carpet, brass door plates */
object Pal {
    const val BG = 0xff0d0c0e.toInt()
    const val WALL = 0xff17151a.toInt()
    const val WALL2 = 0xff201d23.toInt()
    const val FG = 0xffece6dc.toInt()
    const val DIM = 0xff9c948a.toInt()
    const val CARPET = 0xffa3161c.toInt()
    const val CARPET2 = 0xff6e0e12.toInt()
    const val BRASS = 0xffd8b25a.toInt()
    const val FLUOR = 0xffe9f1ea.toInt()
    val SLOTS = intArrayOf(0xffff4a4a.toInt(), 0xff4aa8ff.toInt(), 0xff7dff8a.toInt(), 0xffd8b25a.toInt())
}

class Button(val rect: RectF, val label: String, val primary: Boolean = false, val onTap: () -> Unit) {
    var down = false
}

/** shared drawing + tap handling for menu screens */
abstract class MenuScreen(g: GameView) : Screen(g) {
    protected val buttons = ArrayList<Button>()
    protected val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    protected val r = RectF()
    private var downId = -1

    /** rebuild buttons for the current size, called every draw */
    abstract fun layout()

    open fun tap(x: Float, y: Float) {}

    override fun touch(e: MotionEvent) {
        val x = e.x
        val y = e.y
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downId = e.getPointerId(0)
                buttons.forEach { it.down = it.rect.contains(x, y) }
            }
            MotionEvent.ACTION_MOVE -> buttons.forEach { it.down = it.rect.contains(x, y) }
            MotionEvent.ACTION_UP -> {
                val hit = buttons.firstOrNull { it.rect.contains(x, y) }
                buttons.forEach { it.down = false }
                if (hit != null) {
                    Assets.play("flashlight_click", 0.6f)
                    hit.onTap()
                } else tap(x, y)
            }
            MotionEvent.ACTION_CANCEL -> buttons.forEach { it.down = false }
        }
    }

    protected fun backdrop(c: Canvas, bg: String = "web_menu", dim: Int = 0xb0) {
        c.drawColor(Pal.BG)
        cover(c, Assets.img(bg), 255)
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = (dim shl 24) or 0x0d0c0e
        c.drawRect(0f, 0f, g.w, g.h, paint)
        // red carpet stripe down the left, like the site
        val s = g.s
        paint.color = Pal.CARPET
        var y = 0f
        while (y < g.h) {
            c.drawRect(0f, y, 10f * s, y + 40f * s, paint)
            y += 44f * s
        }
    }

    protected fun cover(c: Canvas, b: Bitmap, alpha: Int) {
        val k = maxOf(g.w / b.width, g.h / b.height)
        val bw = b.width * k
        val bh = b.height * k
        r.set((g.w - bw) / 2, (g.h - bh) / 2, (g.w + bw) / 2, (g.h + bh) / 2)
        paint.alpha = alpha
        c.drawBitmap(b, null, r, paint)
        paint.alpha = 255
    }

    protected fun drawButtons(c: Canvas) {
        for (b in buttons) button(c, b)
    }

    protected fun button(c: Canvas, b: Button) {
        val s = g.s
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = when {
            b.primary && b.down -> 0xffc21d24.toInt()
            b.primary -> Pal.CARPET
            b.down -> 0x44d8b25a
            else -> 0x88000000.toInt()
        }
        c.drawRoundRect(b.rect, 4f * s, 4f * s, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * s
        paint.color = if (b.primary) Pal.CARPET else Pal.BRASS
        c.drawRoundRect(b.rect, 4f * s, 4f * s, paint)
        paint.style = Paint.Style.FILL
        paint.color = if (b.primary) Pal.FG else Pal.BRASS
        paint.typeface = Assets.mono
        paint.textSize = minOf(22f * s, b.rect.height() * 0.42f)
        paint.textAlign = Paint.Align.CENTER
        paint.letterSpacing = 0.08f
        c.drawText(b.label.uppercase(), b.rect.centerX(), b.rect.centerY() + paint.textSize * 0.36f, paint)
        paint.letterSpacing = 0f
    }

    protected fun plate(c: Canvas, text: String, x: Float, y: Float) {
        val s = g.s
        paint.typeface = Assets.mono
        paint.textSize = 15f * s
        paint.letterSpacing = 0.12f
        paint.textAlign = Paint.Align.LEFT
        val tw = paint.measureText(text.uppercase())
        paint.style = Paint.Style.FILL
        paint.color = Pal.BRASS
        r.set(x, y - 18f * s, x + tw + 20f * s, y + 6f * s)
        c.drawRoundRect(r, 2f * s, 2f * s, paint)
        paint.color = 0xff1a1408.toInt()
        c.drawText(text.uppercase(), x + 10f * s, y, paint)
        paint.letterSpacing = 0f
    }

    protected fun title(c: Canvas, text: String, x: Float, y: Float, size: Float, color: Int = Pal.FG, align: Paint.Align = Paint.Align.LEFT) {
        paint.style = Paint.Style.FILL
        paint.typeface = Assets.caveat
        paint.textSize = size * g.s
        paint.textAlign = align
        paint.color = color
        c.drawText(text, x, y, paint)
    }

    protected fun mono(c: Canvas, text: String, x: Float, y: Float, size: Float, color: Int = Pal.DIM, align: Paint.Align = Paint.Align.LEFT) {
        paint.style = Paint.Style.FILL
        paint.typeface = Assets.mono
        paint.textSize = size * g.s
        paint.textAlign = align
        paint.color = color
        c.drawText(text, x, y, paint)
    }

    /** sprite fit into a box, keeping aspect */
    protected fun fit(c: Canvas, b: Bitmap, box: RectF, alpha: Int = 255) {
        val k = minOf(box.width() / b.width, box.height() / b.height)
        val w = b.width * k
        val h = b.height * k
        val dst = RectF(box.centerX() - w / 2, box.centerY() - h / 2, box.centerX() + w / 2, box.centerY() + h / 2)
        paint.alpha = alpha
        c.drawBitmap(b, null, dst, paint)
        paint.alpha = 255
    }
}
