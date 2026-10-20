package com.jhu.smashrooms

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class BattleScreen(g: GameView) : MenuScreen(g) {
    private val b = Battle(g.settings.copy(cpus = g.settings.cpus.toMutableList()))
    private val touchPad = TouchControls(g)
    private val pad = Pad()
    private var paused = false
    private var autoPaused = false

    // camera, in world units
    private var camX = 0f
    private var camY = -250f
    private var camW = 1700f
    private var k = 1f
    private var ox = 0f
    private var oy = 0f

    private val hurt = LightingColorFilter(0xff8080.toInt(), 0x700000)
    private val white = LightingColorFilter(0xffffff, 0x606060)
    private val gold = LightingColorFilter(0xffeecc.toInt(), 0x403000)
    private val path = Path()
    private val dst = RectF()

    override fun enter() = Assets.music(b.stage.def.music, 0.55f)

    fun autoPause() {
        if (b.over == 0) { paused = true; autoPaused = true }
    }

    fun togglePause() {
        if (b.over > 0) return
        paused = !paused
        Assets.play("locker", 0.5f)
    }

    override fun back(): Boolean {
        togglePause()
        return true
    }

    override fun layout() {
        buttons.clear()
        val s = g.s
        if (paused) {
            val bw = 280f * s
            val x = g.w / 2 - bw / 2
            var y = g.h * 0.42f
            for ((label, fn) in listOf<Pair<String, () -> Unit>>(
                "resume" to { paused = false },
                "restart" to { g.go(BattleScreen(g)) },
                "quit" to { g.go(SelectScreen(g)) },
            )) {
                buttons.add(Button(RectF(x, y, x + bw, y + 56f * s), label, label == "resume", fn))
                y += 68f * s
            }
        } else {
            buttons.add(Button(RectF(20f * s, 12f * s, 88f * s, 56f * s), "||") { togglePause() })
        }
    }

    override fun touch(e: MotionEvent) {
        layout()
        if (paused) { super.touch(e); return }
        // the pause button only reacts to a single finger tap on it
        if (e.actionMasked == MotionEvent.ACTION_DOWN && buttons[0].rect.contains(e.x, e.y)) {
            togglePause()
            return
        }
        touchPad.touch(e)
    }

    override fun update() {
        if (paused) return
        pad.clear()
        touchPad.fill(pad)
        g.keys.fill(pad)
        updateWith(pad)
    }

    fun updateWith(p: Pad) {
        touchPad.superReady = b.p1.fear >= 100f && !b.p1.dead
        b.update(p)
        if (b.over > 150) g.go(ResultsScreen(g, b))
        camera()
    }

    private fun camera() {
        val live = b.fighters.filter { !it.dead }
        var minX = b.stage.left - 100f
        var maxX = b.stage.right + 100f
        var minY = b.stage.main.y - 450f
        var maxY = b.stage.main.y + 150f
        for (f in live) {
            minX = min(minX, f.x - 250f); maxX = max(maxX, f.x + 250f)
            minY = min(minY, f.y - f.h - 220f); maxY = max(maxY, f.y + 120f)
        }
        minX = max(minX, b.stage.blastL + 100f)
        maxX = min(maxX, b.stage.blastR - 100f)
        minY = max(minY, b.stage.blastT + 100f)
        maxY = min(maxY, b.stage.blastB - 50f)
        val aspect = g.w / g.h
        val tw = max(max(maxX - minX, (maxY - minY) * aspect), 1300f).coerceAtMost(3000f)
        camW += (tw - camW) * 0.06f
        camX += ((minX + maxX) / 2 - camX) * 0.08f
        camY += ((minY + maxY) / 2 - camY) * 0.08f
    }

    private fun sx(x: Float) = (x - camX) * k + g.w / 2 + ox
    private fun sy(y: Float) = (y - camY) * k + g.h / 2 + oy

    override fun draw(c: Canvas) {
        layout()
        k = g.w / camW
        ox = if (b.shake > 0.5f) (Random.nextFloat() - 0.5f) * b.shake * g.s else 0f
        oy = if (b.shake > 0.5f) (Random.nextFloat() - 0.5f) * b.shake * g.s else 0f
        background(c)
        stage(c)
        for (it in b.items) item(c, it)
        for (p in b.projs) proj(c, p)
        for (f in b.fighters) fighter(c, f)
        for (e in b.fx) effect(c, e)
        overlays(c)
        hud(c)
        if (!paused && b.over == 0) touchPad.draw(c)
        countdown(c)
        if (paused) pauseMenu(c) else drawButtons(c)
    }

    // ---------- world ----------

    private fun background(c: Canvas) {
        c.drawColor(b.stage.def.tint)
        val bmp = Assets.img(b.stage.def.bg)
        val scale = max(g.w / bmp.width, g.h / bmp.height) * 1.15f
        val w = bmp.width * scale
        val h = bmp.height * scale
        val px = -camX * 0.04f * g.s
        val py = -(camY + 250f) * 0.03f * g.s
        dst.set((g.w - w) / 2 + px, (g.h - h) / 2 + py, (g.w + w) / 2 + px, (g.h + h) / 2 + py)
        paint.shader = null
        paint.colorFilter = null
        paint.alpha = 255
        c.drawBitmap(bmp, null, dst, paint)
        paint.style = Paint.Style.FILL
        paint.color = if (b.stage.def.dark) 0xd0000000.toInt() else 0xa0000000.toInt()
        c.drawRect(0f, 0f, g.w, g.h, paint)
    }

    private fun stage(c: Canvas) {
        val s = g.s
        for (p in b.stage.plats) {
            val l = sx(p.x1)
            val r = sx(p.x2)
            val t = sy(p.y)
            paint.shader = null
            paint.style = Paint.Style.FILL
            if (p.solid) {
                val bot = sy(p.y + p.thick)
                paint.color = Pal.WALL2
                c.drawRect(l, t, r, bot, paint)
                // wallpaper stripes
                paint.color = Pal.WALL
                var x = l + 30f * k
                while (x < r) { c.drawRect(x, t + 20f * k, x + 8f * k, bot, paint); x += 60f * k }
                // red carpet with brass trim, like the-rooms corridor
                paint.color = Pal.CARPET
                c.drawRect(l, t, r, t + 18f * k, paint)
                paint.color = Pal.CARPET2
                c.drawRect(l, t + 18f * k, r, t + 24f * k, paint)
                paint.color = Pal.BRASS
                c.drawRect(l, t + 24f * k, r, t + 27f * k, paint)
                // door plate in the middle
                val mx = (l + r) / 2
                dst.set(mx - 50f * k, t + 60f * k, mx + 50f * k, t + 90f * k)
                c.drawRoundRect(dst, 3f * k, 3f * k, paint)
                paint.color = 0xff1a1408.toInt()
                paint.typeface = Assets.mono
                paint.textSize = 18f * k
                paint.textAlign = Paint.Align.CENTER
                c.drawText("A-" + (b.stage.def.name.length * 37 % 1000).toString().padStart(3, '0'), mx, t + 82f * k, paint)
            } else {
                paint.color = 0xcc17151a.toInt()
                c.drawRect(l, t, r, t + 14f * k, paint)
                paint.color = Pal.CARPET
                c.drawRect(l, t, r, t + 5f * k, paint)
                paint.color = 0x55d8b25a
                c.drawRect(l, t + 12f * k, r, t + 14f * k, paint)
            }
        }
        // respawn door
        if (b.fighters.any { it.dead && it.stocks > 0 && it.respawn < 40 } || b.fighters.any { it.invuln > 120 }) {
            paint.color = 0xff9a6a40.toInt()
            dst.set(sx(-40f), sy(-560f), sx(40f), sy(-420f))
            c.drawRect(dst, paint)
            paint.color = Pal.BRASS
            c.drawCircle(sx(25f), sy(-490f), 4f * s, paint)
        }
    }

    private fun item(c: Canvas, it: Item) {
        if (it.life < 120 && it.life % 10 < 5) return
        val x = sx(it.x)
        val y = sy(it.y) + sin(b.t * 0.1f) * 4f * k - 20f * k
        val s = k
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        when (it.kind) {
            Item.VITAMINS -> {
                paint.color = 0xffe07a1f.toInt()
                dst.set(x - 16f * s, y - 20f * s, x + 16f * s, y + 20f * s); c.drawRoundRect(dst, 6f * s, 6f * s, paint)
                paint.color = Color.WHITE
                dst.set(x - 18f * s, y - 30f * s, x + 18f * s, y - 18f * s); c.drawRoundRect(dst, 3f * s, 3f * s, paint)
                dst.set(x - 12f * s, y - 6f * s, x + 12f * s, y + 8f * s); c.drawRect(dst, paint)
            }
            Item.GOLD -> {
                paint.color = Pal.BRASS
                c.drawCircle(x, y, 20f * s, paint)
                paint.color = 0xfff3d98a.toInt()
                c.drawCircle(x - 5f * s, y - 5f * s, 8f * s, paint)
            }
            else -> {
                paint.color = 0x44fff6c8
                path.reset(); path.moveTo(x + 18f * s, y); path.lineTo(x + 90f * s, y - 40f * s); path.lineTo(x + 90f * s, y + 40f * s); path.close()
                c.drawPath(path, paint)
                paint.color = 0xff55575c.toInt()
                dst.set(x - 26f * s, y - 9f * s, x + 18f * s, y + 9f * s); c.drawRoundRect(dst, 4f * s, 4f * s, paint)
                paint.color = 0xfffff6c8.toInt()
                c.drawCircle(x + 18f * s, y, 10f * s, paint)
            }
        }
    }

    private fun proj(c: Canvas, p: Proj) {
        val x = sx(p.x)
        val y = sy(p.y)
        paint.shader = null
        paint.colorFilter = null
        val sp = p.sprite
        if (sp != null) {
            val bmp = Assets.img(sp)
            val h = p.size * k
            val w = h * bmp.width / bmp.height
            c.save()
            c.rotate(p.rot, x, y)
            if (p.vx < 0f && p.spinV == 0f) c.scale(-1f, 1f, x, y)
            dst.set(x - w / 2, y - h / 2, x + w / 2, y + h / 2)
            paint.alpha = p.alpha
            c.drawBitmap(bmp, null, dst, paint)
            paint.alpha = 255
            c.restore()
            return
        }
        paint.style = Paint.Style.FILL
        when (p.shape) {
            1 -> star(c, x, y, p.size * 0.5f * k, p.rot, p.color)
            2 -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 5f * k
                paint.color = p.color
                val dir = if (p.vx >= 0) 1f else -1f
                for (i in 0..2) {
                    val rr = (p.r + i * 14f) * k
                    dst.set(x - rr, y - rr, x + rr, y + rr)
                    paint.alpha = 220 - i * 60
                    c.drawArc(dst, if (dir > 0) -50f else 130f, 100f, false, paint)
                }
                paint.alpha = 255
            }
            3 -> {
                paint.color = 0xff000000.toInt()
                c.drawCircle(x, y, p.r * k, paint)
                paint.color = 0x66000000
                c.drawCircle(x - p.vx * k, y - p.vy * k, p.r * 0.8f * k, paint)
            }
            else -> {
                paint.color = p.color
                paint.alpha = 90
                c.drawCircle(x, y, p.r * 1.6f * k, paint)
                paint.alpha = 255
                c.drawCircle(x, y, p.r * k, paint)
            }
        }
    }

    private fun star(c: Canvas, x: Float, y: Float, r: Float, rot: Float, color: Int) {
        path.reset()
        for (i in 0 until 10) {
            val a = Math.toRadians((rot + i * 36f - 90f).toDouble())
            val rr = if (i % 2 == 0) r else r * 0.45f
            val px = x + cos(a).toFloat() * rr
            val py = y + sin(a).toFloat() * rr
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        paint.style = Paint.Style.FILL
        paint.color = color
        c.drawPath(path, paint)
    }

    private fun fighter(c: Canvas, f: Fighter) {
        if (f.dead) return
        val s = g.s
        val d = f.def
        val bmp = Assets.img(d.spriteFor(f, g.frame))
        var x = sx(f.x)
        var y = sy(f.y)
        var alpha = 255
        if (f.invis > 0) alpha = if (f.human) 80 else 22
        if (f.invuln > 0 && f.invuln % 8 < 4) alpha = alpha / 2
        if (f.hitlag > 0 && f.hitstun > 0) x += (Random.nextFloat() - 0.5f) * 10f * k

        // anim per look
        val t = g.frame + f.slot * 31
        var sxk = 1f + f.squash
        var syk = 1f - f.squash
        when (d.look) {
            Look.BOB -> y += sin(t * 0.12f) * 4f * k
            Look.FLOAT -> y += (sin(t * 0.07f) * 8f - 10f) * k
            Look.PULSE -> { val p = 1f + sin(t * 0.15f) * 0.05f; sxk *= p; syk *= p }
            Look.JITTER, Look.FACES -> if (t % 9 < 2) { x += (Random.nextFloat() - 0.5f) * 8f * k; y += (Random.nextFloat() - 0.5f) * 6f * k }
            Look.STILL -> if (f.grounded && abs(f.vx) > 1f) y -= abs(sin(f.walkT)) * 6f * k
        }

        // shadow
        if (f.grounded && alpha > 100) {
            paint.shader = null
            paint.colorFilter = null
            paint.style = Paint.Style.FILL
            paint.color = 0x66000000
            dst.set(x - f.w * 0.45f * k, y - 6f * k, x + f.w * 0.45f * k, y + 6f * k)
            c.drawOval(dst, paint)
        }

        val h = f.h * 1.08f * k
        val w = h * bmp.width / bmp.height

        // afterimages on dashes
        if (f.trail > 0 && alpha > 100) {
            for (i in 1..3) {
                paint.alpha = 70 - i * 18
                paint.colorFilter = null
                val tx = x - f.vx * i * 2.2f * k
                drawSprite(c, bmp, tx, y, w, h, sxk, syk, f)
            }
        }

        paint.alpha = alpha
        paint.colorFilter = when {
            f.flash > 0 && f.flash % 4 < 2 -> if (f.hitstun > 0 || f.stun > 0) hurt else white
            f.armor > 0 && g.frame % 10 < 5 -> gold
            f.bonus > 1f && g.frame % 20 < 10 -> gold
            else -> null
        }
        drawSprite(c, bmp, x, y, w, h, sxk, syk, f)
        paint.colorFilter = null
        paint.alpha = 255

        // shield bubble
        if (f.shielding) {
            paint.style = Paint.Style.FILL
            paint.color = Pal.SLOTS[f.slot] and 0x00ffffff or 0x55000000
            c.drawCircle(x, sy(f.cy), (f.h * 0.35f + f.shield * 0.35f) * k, paint)
        }

        // tag + status icons above the head
        val headY = sy(f.y - f.h) - 18f * s
        if (alpha > 60 || f.human) {
            paint.style = Paint.Style.FILL
            paint.color = Pal.SLOTS[f.slot]
            path.reset()
            path.moveTo(x - 9f * s, headY - 14f * s); path.lineTo(x + 9f * s, headY - 14f * s); path.lineTo(x, headY - 2f * s); path.close()
            c.drawPath(path, paint)
            paint.typeface = Assets.mono
            paint.textSize = 14f * s
            paint.textAlign = Paint.Align.CENTER
            c.drawText(if (f.human) "P1" else "CPU${f.slot}", x, headY - 20f * s, paint)
        }
        var iconY = headY - 64f * s
        fun icon(name: String) {
            val bi = Assets.img(name)
            val ih = 46f * s
            dst.set(x - ih / 2, iconY - ih / 2, x + ih / 2, iconY + ih / 2)
            c.drawBitmap(bi, null, dst, paint)
            iconY -= 50f * s
        }
        if (f.curse > 0 && (f.curse > 30 || f.curse % 6 < 3)) icon("a90_stop")
        if (f.halt > 0) icon("a90b_halt")
        else if (f.proceed > 0) icon("a90b_proceed")
        if (f.reverse > 0) {
            paint.color = 0xff38d9ff.toInt()
            paint.typeface = Assets.mono
            paint.textSize = 16f * s
            c.drawText(if (g.frame % 20 < 10) "<- ->" else "-> <-", x, iconY, paint)
            iconY -= 24f * s
        }
        if (f.stun > 0 || f.shieldBreak > 0) {
            paint.color = Pal.BRASS
            for (i in 0..2) {
                val a = g.frame * 0.15f + i * 2.094f
                c.drawCircle(x + cos(a) * 26f * s, headY + 4f * s + sin(a) * 8f * s, 5f * s, paint)
            }
        }
    }

    private fun drawSprite(c: Canvas, bmp: android.graphics.Bitmap, x: Float, y: Float, w: Float, h: Float, sxk: Float, syk: Float, f: Fighter) {
        c.save()
        val cy = y - h / 2
        c.translate(x, cy)
        val rot = (if (f.hitstun > 0 && f.tumble) f.spin else f.spin * 0.3f) + f.lean * f.facing * 0.6f
        c.rotate(rot)
        c.scale(sxk * f.facing, syk)
        dst.set(-w / 2, -h / 2, w / 2, h / 2)
        c.drawBitmap(bmp, null, dst, paint)
        c.restore()
    }

    private fun effect(c: Canvas, e: Fx) {
        val a = e.life.toFloat() / e.max
        paint.shader = null
        paint.colorFilter = null
        when (e.kind) {
            Fx.DOT -> {
                paint.style = Paint.Style.FILL
                paint.color = e.color
                paint.alpha = (255 * a).toInt()
                c.drawCircle(sx(e.x), sy(e.y), e.size * k * (0.5f + a * 0.5f), paint)
            }
            Fx.RING -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 6f * k * a
                paint.color = e.color
                paint.alpha = (220 * a).toInt()
                c.drawCircle(sx(e.x), sy(e.y), e.size * k * (1.2f - a * 0.7f), paint)
            }
            Fx.TEXT -> {
                paint.style = Paint.Style.FILL
                paint.typeface = Assets.caveat
                paint.textSize = e.size * g.s
                paint.textAlign = Paint.Align.CENTER
                paint.color = 0xff000000.toInt()
                paint.alpha = (255 * min(1f, a * 2f)).toInt()
                c.drawText(e.text ?: "", sx(e.x) + 2f, sy(e.y) + 2f, paint)
                paint.color = e.color
                paint.alpha = (255 * min(1f, a * 2f)).toInt()
                c.drawText(e.text ?: "", sx(e.x), sy(e.y), paint)
            }
            Fx.SLASH -> {
                val o = e.owner ?: return
                if (o.dead) return
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeWidth = 14f * k * a
                paint.color = e.color
                paint.alpha = (200 * a).toInt()
                val cx = sx(o.x + e.x * o.facing)
                val cy = sy(o.cy + e.y)
                val r = e.size * k
                dst.set(cx - r, cy - r, cx + r, cy + r)
                val from = if (o.facing > 0) e.a0 else 180f - e.a1
                c.drawArc(dst, from, e.a1 - e.a0, false, paint)
                paint.strokeCap = Paint.Cap.BUTT
            }
            Fx.LINE -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = e.size * k * a
                paint.color = e.color
                paint.alpha = (255 * a).toInt()
                c.drawLine(sx(e.x), sy(e.y), sx(e.x - e.vx * 3f), sy(e.y - e.vy * 3f), paint)
            }
            Fx.SPRITE -> {
                val bmp = Assets.img(e.sprite ?: return)
                val h = e.size * k * (1.1f - a * 0.1f)
                val w = h * bmp.width / bmp.height
                dst.set(sx(e.x) - w / 2, sy(e.y) - h / 2, sx(e.x) + w / 2, sy(e.y) + h / 2)
                paint.alpha = (200 * a).toInt()
                c.drawBitmap(bmp, null, dst, paint)
                paint.alpha = 255
            }
            Fx.BEAM -> {
                paint.style = Paint.Style.FILL
                paint.color = e.color
                paint.alpha = (255 * a).toInt()
                if (e.a1 == 0f) {
                    val x2 = e.x + e.a0
                    dst.set(sx(min(e.x, x2)), sy(e.y) - e.size * k / 2 * a, sx(max(e.x, x2)), sy(e.y) + e.size * k / 2 * a)
                } else {
                    dst.set(sx(e.x), sy(e.y), sx(e.x + e.size), sy(e.y + e.a0))
                }
                c.drawRect(dst, paint)
                paint.alpha = 255
            }
            Fx.HAND -> {
                // a black hand coming out of the floor
                val grow = min(1f, (e.max - e.life) / 6f) * (if (e.life < 8) e.life / 8f else 1f)
                val bx = sx(e.x)
                val by = sy(e.y)
                val hh = e.size * k * grow
                val hw = 34f * k
                paint.style = Paint.Style.FILL
                paint.color = 0xff050505.toInt()
                dst.set(bx - hw / 2, by - hh, bx + hw / 2, by)
                c.drawRoundRect(dst, hw / 2, hw / 2, paint)
                for (i in -2..2) {
                    val fx = bx + i * hw * 0.28f
                    dst.set(fx - hw * 0.12f, by - hh - 30f * k * grow, fx + hw * 0.12f, by - hh + 10f * k)
                    c.drawRoundRect(dst, hw * 0.12f, hw * 0.12f, paint)
                }
            }
        }
        paint.alpha = 255
    }

    // ---------- screen overlays ----------

    private fun overlays(c: Canvas) {
        val s = g.s
        paint.colorFilter = null
        // lights out: you only see around yourself, like a flashlight
        val darkStage = b.stage.def.dark && (b.t % 300 > 260 || b.t % 300 < 6)
        if (b.darkT > 0 || darkStage) {
            val me = b.p1
            val cx = if (me.dead) g.w / 2 else sx(me.x)
            val cy = if (me.dead) g.h / 2 else sy(me.cy)
            val rad = (if (b.darkT > 0) 260f else 340f) * s
            paint.shader = RadialGradient(cx, cy, rad, intArrayOf(0x00000000, 0xf0000000.toInt()), floatArrayOf(0.35f, 1f), Shader.TileMode.CLAMP)
            paint.style = Paint.Style.FILL
            c.drawRect(0f, 0f, g.w, g.h, paint)
            paint.shader = null
            b.darkBy?.takeIf { b.darkT > 0 && !it.dead }?.let { by ->
                // a-120's eyes still show in the dark
                paint.color = 0xccc8d4ff.toInt()
                c.drawCircle(sx(by.x) - 14f * k, sy(by.cy) - 10f * k, 6f * k, paint)
                c.drawCircle(sx(by.x) + 14f * k, sy(by.cy) - 10f * k, 6f * k, paint)
            }
        }
        if (b.glitchT > 0) {
            paint.style = Paint.Style.FILL
            repeat(6) {
                paint.color = if (Random.nextBoolean()) 0x5538d9ff else 0x55ff3b3b
                val y = Random.nextFloat() * g.h
                c.drawRect(0f, y, g.w, y + Random.nextFloat() * 30f * s, paint)
            }
        }
        if (b.roomFlashT > 0) {
            paint.alpha = (255 * b.roomFlashT / 40f).toInt()
            cover(c, Assets.img(b.roomFlash ?: "a200_room"), paint.alpha)
        }
        if (b.flashT > 0) {
            paint.style = Paint.Style.FILL
            paint.color = b.flashColor
            paint.alpha = (Color.alpha(b.flashColor) * b.flashT / b.flashMax.toFloat()).toInt()
            c.drawRect(0f, 0f, g.w, g.h, paint)
            paint.alpha = 255
        }
        val js = b.jumpscare
        if (b.jumpscareT > 0 && js != null) {
            paint.style = Paint.Style.FILL
            paint.color = 0xff000000.toInt()
            c.drawRect(0f, 0f, g.w, g.h, paint)
            val bmp = Assets.img(js)
            val sz = g.h * (0.9f + (36 - b.jumpscareT) * 0.01f)
            val jx = (Random.nextFloat() - 0.5f) * 30f * s
            val jy = (Random.nextFloat() - 0.5f) * 30f * s
            dst.set(g.w / 2 - sz / 2 + jx, g.h / 2 - sz / 2 + jy, g.w / 2 + sz / 2 + jx, g.h / 2 + sz / 2 + jy)
            c.drawBitmap(bmp, null, dst, paint)
        }
    }

    private fun hud(c: Canvas) {
        val s = g.s
        val n = b.fighters.size
        val pw = 210f * s
        val gap = 16f * s
        val total = n * pw + (n - 1) * gap
        // top of the screen, the bottom belongs to your thumbs
        val y0 = 10f * s
        val x0 = (g.w - total) / 2
        for ((i, f) in b.fighters.withIndex()) {
            val x = x0 + i * (pw + gap)
            paint.shader = null
            paint.colorFilter = null
            paint.style = Paint.Style.FILL
            paint.color = 0xaa0d0c0e.toInt()
            dst.set(x, y0, x + pw, y0 + 80f * s)
            c.drawRoundRect(dst, 4f * s, 4f * s, paint)
            paint.color = Pal.SLOTS[f.slot]
            c.drawRect(x, y0, x + 5f * s, y0 + 80f * s, paint)
            paint.alpha = if (f.stocks > 0) 255 else 90
            fit(c, Assets.img(f.def.spriteFor(f, g.frame)), RectF(x + 10f * s, y0 + 6f * s, x + 70f * s, y0 + 66f * s), paint.alpha)
            val dmgCol = when {
                f.stocks <= 0 -> Pal.DIM
                f.dmg < 50f -> Pal.FG
                f.dmg < 100f -> 0xffffb36b.toInt()
                f.dmg < 150f -> 0xffff6b4a.toInt()
                else -> 0xffff2a2a.toInt()
            }
            val jig = if (f.flash > 0) (Random.nextFloat() - 0.5f) * 6f * s else 0f
            title(c, if (f.stocks > 0) "${f.dmg.toInt()}%" else "out", x + 78f * s + jig, y0 + 50f * s, 48f, dmgCol)
            mono(c, f.def.name, x + 80f * s, y0 + 16f * s, 12f, Pal.DIM)
            // stocks
            if (!(b.settings.training && f.slot != 0)) {
                paint.color = Pal.SLOTS[f.slot]
                for (k in 0 until min(f.stocks, 5)) c.drawCircle(x + 160f * s + (k % 3) * 14f * s, y0 + 14f * s + (k / 3) * 14f * s, 5f * s, paint)
            }
            // fear meter
            paint.color = Pal.WALL2
            c.drawRect(x + 10f * s, y0 + 70f * s, x + pw - 10f * s, y0 + 75f * s, paint)
            paint.color = if (f.fear >= 100f && g.frame % 16 < 8) Pal.BRASS else Pal.CARPET
            c.drawRect(x + 10f * s, y0 + 70f * s, x + 10f * s + (pw - 20f * s) * f.fear / 100f, y0 + 75f * s, paint)
        }
        if (b.settings.training) {
            mono(c, "training · the cpu doesn't fight back", g.w / 2, 112f * s, 13f, Pal.DIM, Paint.Align.CENTER)
        }
    }

    private fun countdown(c: Canvas) {
        val s = g.s
        val cd = b.countdown
        val text = when {
            cd > 180 -> null
            cd > 120 -> "3"
            cd > 60 -> "2"
            cd > 0 -> "1"
            b.t < 260 && b.over == 0 -> "go!"
            b.over > 0 -> "game!"
            else -> null
        } ?: return
        val size = if (text == "game!") 160f else 140f
        title(c, text, g.w / 2 + 4f * s, g.h * 0.45f + 4f * s, size, 0xff000000.toInt(), Paint.Align.CENTER)
        title(c, text, g.w / 2, g.h * 0.45f, size, if (text == "game!" || text == "go!") Pal.CARPET else Pal.FG, Paint.Align.CENTER)
    }

    private fun pauseMenu(c: Canvas) {
        val s = g.s
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = 0xcc0d0c0e.toInt()
        c.drawRect(0f, 0f, g.w, g.h, paint)
        plate(c, "paused", g.w / 2 - 50f * s, g.h * 0.3f)
        if (autoPaused) mono(c, "tap resume when you're ready", g.w / 2, g.h * 0.36f, 14f, Pal.DIM, Paint.Align.CENTER)
        mono(c, "attack: jab/tilts, flick + attack: smash, special + direction: 4 specials", g.w / 2, g.h * 0.85f, 13f, Pal.DIM, Paint.Align.CENTER)
        mono(c, "shield + direction: dodge, full fear bar: super button appears", g.w / 2, g.h * 0.85f + 20f * s, 13f, Pal.DIM, Paint.Align.CENTER)
        drawButtons(c)
    }
}
