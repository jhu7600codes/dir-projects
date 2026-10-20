package com.jhu.smashrooms

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.sin

class TitleScreen(g: GameView) : MenuScreen(g) {
    override fun enter() = Assets.music("menu", 0.7f)

    override fun layout() {
        buttons.clear()
        val s = g.s
        val bw = 300f * s
        val bh = 56f * s
        val x = g.w / 2 - bw / 2
        var y = g.h * 0.56f
        fun add(label: String, primary: Boolean = false, fn: () -> Unit) {
            buttons.add(Button(RectF(x, y, x + bw, y + bh), label, primary, fn))
            y += bh + 12f * s
        }
        add("fight", true) { g.settings.training = false; g.go(SelectScreen(g)) }
        add("training") { g.settings.training = true; g.go(SelectScreen(g)) }
        add("entities") { g.go(RosterScreen(g, 0)) }
        val sw = 140f * s
        buttons.add(Button(RectF(g.w - sw - 20f * s, 20f * s, g.w - 20f * s, 20f * s + 44f * s), if (Assets.musicOn) "music on" else "music off") {
            Assets.musicOn = !Assets.musicOn
        })
        buttons.add(Button(RectF(g.w - sw - 20f * s, 74f * s, g.w - 20f * s, 74f * s + 44f * s), if (Assets.sfxOn) "sfx on" else "sfx off") {
            Assets.sfxOn = !Assets.sfxOn
        })
        buttons.add(Button(RectF(g.w - sw - 20f * s, g.h - 64f * s, g.w - 20f * s, g.h - 20f * s), "credits") { g.go(CreditsScreen(g)) })
    }

    override fun draw(c: Canvas) {
        layout()
        backdrop(c, "s_lobby", 0xa8)
        val s = g.s
        // flickering fluorescent tube
        val on = g.frame % 420 !in 380L..384L && g.frame % 420 !in 390L..392L
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = if (on) Pal.FLUOR else 0x33e9f1ea
        r.set(g.w / 2 - 170f * s, 22f * s, g.w / 2 + 170f * s, 28f * s)
        c.drawRoundRect(r, 3f * s, 3f * s, paint)
        val logo = Assets.img("logo")
        val lw = minOf(g.w * 0.62f, 640f * s)
        val lh = lw * logo.height / logo.width
        r.set(g.w / 2 - lw / 2, 50f * s, g.w / 2 + lw / 2, 50f * s + lh)
        c.drawBitmap(logo, null, r, paint)
        title(c, "smash edition", g.w / 2 + lw * 0.32f, 50f * s + lh + 44f * s, 48f, 0xffff3b3b.toInt(), Paint.Align.CENTER)
        mono(c, "an unofficial doors x the rooms fighting game, made by jhu", g.w / 2, g.h * 0.56f - 14f * s, 14f, Pal.DIM, Paint.Align.CENTER)
        drawButtons(c)
        // a couple of entities lurking in the corners
        val bob = sin(g.frame * 0.05f) * 6f * s
        fit(c, Assets.img("figure_body"), RectF(30f * s, g.h - 300f * s + bob, 230f * s, g.h - 20f * s + bob), 210)
        fit(c, Assets.img("rush_face"), RectF(g.w - 400f * s, g.h * 0.55f - bob, g.w - 200f * s, g.h * 0.55f + 200f * s - bob), 180)
    }

    override fun back(): Boolean = false
}

class SelectScreen(g: GameView) : MenuScreen(g) {
    private val set get() = g.settings
    /** -1 = you, otherwise cpu index */
    private var picking = -1
    private val cells = ArrayList<RectF>()

    override fun enter() = Assets.music("menu", 0.7f)

    override fun layout() {
        buttons.clear()
        cells.clear()
        val s = g.s
        val cols = 8
        val pad = 10f * s
        val top = 70f * s
        val gw = g.w - 60f * s
        val cw = (gw - pad * (cols - 1)) / cols
        val ch = minOf(cw, (g.h * 0.5f - pad) / 2)
        for (i in Roster.ALL.indices) {
            val x = 30f * s + (i % cols) * (cw + pad)
            val y = top + (i / cols) * (ch + pad)
            cells.add(RectF(x, y, x + cw, y + ch))
        }
        val by = g.h - 70f * s
        val bh = 52f * s
        var x = 30f * s
        fun add(label: String, w: Float, primary: Boolean = false, fn: () -> Unit) {
            buttons.add(Button(RectF(x, by, x + w * s, by + bh), label, primary, fn))
            x += w * s + 10f * s
        }
        add("back", 110f) { g.go(TitleScreen(g)) }
        if (!set.training) {
            add("+ cpu", 110f) { if (set.cpus.size < 3) set.cpus.add(-1) }
            add("- cpu", 110f) { if (set.cpus.size > 1) { set.cpus.removeAt(set.cpus.lastIndex); picking = -1 } }
            add("level ${set.level}", 140f) { set.level = set.level % 9 + 1 }
            add("stocks ${set.stocks}", 150f) { set.stocks = set.stocks % 5 + 1 }
            add(if (set.items) "items on" else "items off", 150f) { set.items = !set.items }
        } else {
            while (set.cpus.size > 1) set.cpus.removeAt(set.cpus.lastIndex)
        }
        buttons.add(Button(RectF(g.w - 200f * s, by, g.w - 30f * s, by + bh), "next", true) { g.go(StageScreen(g)) })

        // player slots
        val n = 1 + set.cpus.size
        val sw = 190f * s
        val sy = g.h - 230f * s
        for (i in 0 until n) {
            val sx = 30f * s + i * (sw + 14f * s)
            val rect = RectF(sx, sy, sx + sw, sy + 140f * s)
            buttons.add(Button(rect, "") { picking = i - 1 })
        }
    }

    override fun tap(x: Float, y: Float) {
        val i = cells.indexOfFirst { it.contains(x, y) }
        if (i < 0) return
        Assets.play("door_open", 0.4f, 1.4f)
        if (picking < 0) set.p1 = i
        else set.cpus[picking] = if (set.cpus[picking] == i) -1 else i
    }

    override fun draw(c: Canvas) {
        layout()
        backdrop(c, "s_lobby", 0xc8)
        val s = g.s
        plate(c, if (set.training) "a-000 · training" else "a-001 · pick your entity", 30f * s, 46f * s)
        mono(c, if (picking < 0) "picking for: you" else "picking for: cpu ${picking + 1} (tap again for random)", g.w - 30f * s, 46f * s, 15f, Pal.BRASS, Paint.Align.RIGHT)
        val sel = if (picking < 0) set.p1 else set.cpus[picking]
        for ((i, cell) in cells.withIndex()) {
            val d = Roster.ALL[i]
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.color = if (i == sel) 0xff2a1214.toInt() else 0xcc17151a.toInt()
            c.drawRoundRect(cell, 4f * s, 4f * s, paint)
            val inner = RectF(cell.left + 8f * s, cell.top + 6f * s, cell.right - 8f * s, cell.bottom - 26f * s)
            fit(c, Assets.img(d.spriteFor(null, g.frame)), inner)
            mono(c, d.name, cell.centerX(), cell.bottom - 8f * s, 15f, if (i == sel) Pal.FG else Pal.DIM, Paint.Align.CENTER)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = (if (i == sel) 3f else 1f) * s
            paint.color = if (i == sel) Pal.SLOTS[picking + 1] else Pal.WALL2
            c.drawRoundRect(cell, 4f * s, 4f * s, paint)
        }
        drawButtons(c)
        // slots on top of their invisible buttons
        val n = 1 + set.cpus.size
        for (i in 0 until n) {
            val b = buttons[buttons.size - n + i].rect
            val id = if (i == 0) set.p1 else set.cpus[i - 1]
            paint.style = Paint.Style.FILL
            paint.color = 0xdd0d0c0e.toInt()
            c.drawRoundRect(b, 4f * s, 4f * s, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = (if (picking == i - 1) 3f else 1.5f) * s
            paint.color = Pal.SLOTS[i]
            c.drawRoundRect(b, 4f * s, 4f * s, paint)
            if (id >= 0) {
                val d = Roster.ALL[id]
                fit(c, Assets.img(d.spriteFor(null, g.frame)), RectF(b.left + 6f * s, b.top + 6f * s, b.left + 86f * s, b.bottom - 6f * s))
                title(c, d.name, b.left + 92f * s, b.top + 50f * s, 34f, Pal.FG)
            } else {
                title(c, "?", b.left + 46f * s, b.top + 90f * s, 70f, Pal.DIM, Paint.Align.CENTER)
                title(c, "random", b.left + 92f * s, b.top + 50f * s, 34f, Pal.DIM)
            }
            mono(c, if (i == 0) "you" else "cpu $i", b.left + 92f * s, b.top + 80f * s, 14f, Pal.SLOTS[i])
        }
    }

    override fun back(): Boolean { g.go(TitleScreen(g)); return true }
}

class StageScreen(g: GameView) : MenuScreen(g) {
    override fun layout() {
        buttons.clear()
        val s = g.s
        val list = Stage.ALL
        val cols = 3
        val pad = 14f * s
        val top = 80f * s
        val cw = (g.w - 60f * s - pad * (cols - 1)) / cols
        val ch = (g.h - top - 100f * s - pad * 2) / 3
        for (i in list.indices) {
            val x = 30f * s + (i % cols) * (cw + pad)
            val y = top + (i / cols) * (ch + pad)
            buttons.add(Button(RectF(x, y, x + cw, y + ch), "") { start(i) })
        }
        val by = g.h - 70f * s
        buttons.add(Button(RectF(30f * s, by, 170f * s, by + 52f * s), "back") { g.go(SelectScreen(g)) })
        buttons.add(Button(RectF(g.w - 230f * s, by, g.w - 30f * s, by + 52f * s), "random", true) { start(list.indices.random()) })
    }

    private fun start(i: Int) {
        g.settings.stage = i
        g.go(BattleScreen(g))
    }

    override fun draw(c: Canvas) {
        layout()
        backdrop(c, "s_dark", 0xc0)
        val s = g.s
        plate(c, "a-002 · pick a room", 30f * s, 52f * s)
        for ((i, st) in Stage.ALL.withIndex()) {
            val b = buttons[i]
            c.save()
            c.clipRect(b.rect)
            val bmp = Assets.img(st.bg)
            val k = maxOf(b.rect.width() / bmp.width, b.rect.height() / bmp.height)
            val w = bmp.width * k
            val h = bmp.height * k
            paint.alpha = if (b.down) 255 else 200
            c.drawBitmap(bmp, null, RectF(b.rect.centerX() - w / 2, b.rect.centerY() - h / 2, b.rect.centerX() + w / 2, b.rect.centerY() + h / 2), paint)
            paint.alpha = 255
            paint.style = Paint.Style.FILL
            paint.color = 0x99000000.toInt()
            c.drawRect(b.rect.left, b.rect.bottom - 40f * s, b.rect.right, b.rect.bottom, paint)
            c.restore()
            title(c, st.name, b.rect.left + 12f * s, b.rect.bottom - 10f * s, 32f, Pal.FG)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = (if (b.down) 3f else 1.5f) * s
            paint.color = if (b.down) Pal.BRASS else Pal.WALL2
            c.drawRect(b.rect, paint)
        }
        drawButtons(c)
    }

    override fun back(): Boolean { g.go(SelectScreen(g)); return true }
}

class RosterScreen(g: GameView, private var sel: Int) : MenuScreen(g) {
    override fun layout() {
        buttons.clear()
        val s = g.s
        val rows = 8
        val bw = 140f * s
        val bh = (g.h - 140f * s) / rows - 8f * s
        for (i in Roster.ALL.indices) {
            val x = 30f * s + (i / rows) * (bw + 10f * s)
            val y = 70f * s + (i % rows) * (bh + 8f * s)
            buttons.add(Button(RectF(x, y, x + bw, y + bh), Roster.ALL[i].name, i == sel) { sel = i })
        }
        buttons.add(Button(RectF(30f * s, g.h - 62f * s, 170f * s, g.h - 14f * s), "back") { g.go(TitleScreen(g)) })
    }

    override fun draw(c: Canvas) {
        layout()
        backdrop(c, "s_tall", 0xd0)
        val s = g.s
        plate(c, "a-003 · entities", 30f * s, 46f * s)
        drawButtons(c)
        val d = Roster.ALL[sel]
        val left = 340f * s
        val bob = sin(g.frame * 0.06f) * 6f * s
        fit(c, Assets.img(d.spriteFor(null, g.frame)), RectF(left, 80f * s + bob, left + 260f * s, 340f * s + bob))
        title(c, d.name, left + 290f * s, 130f * s, 72f, d.color)
        mono(c, d.blurb, left + 292f * s, 165f * s, 16f, Pal.FG)
        val stats = listOf(
            "weight" to d.weight / 140f, "speed" to d.speed / 9.5f, "jump" to (d.jump * d.jumps) / 64f, "power" to (d.weight / 140f + 0.5f) / 1.5f,
        )
        for ((i, st) in stats.withIndex()) {
            val y = 200f * s + i * 30f * s
            mono(c, st.first, left + 292f * s, y + 12f * s, 14f, Pal.DIM)
            paint.style = Paint.Style.FILL
            paint.color = Pal.WALL2
            c.drawRect(left + 380f * s, y, left + 600f * s, y + 14f * s, paint)
            paint.color = d.color
            c.drawRect(left + 380f * s, y, left + 380f * s + 220f * s * st.second.coerceIn(0.05f, 1f), y + 14f * s, paint)
        }
        val moves = listOf("special" to d.specials[0], "side + special" to d.specials[1], "up + special" to d.specials[2], "down + special" to d.specials[3], "super (full fear)" to d.sup)
        var y = 370f * s
        for ((input, m) in moves) {
            mono(c, input, left, y, 13f, Pal.BRASS)
            title(c, m.name, left + 190f * s, y + 4f * s, 30f, Pal.FG)
            mono(c, m.desc, left + 190f * s, y + 24f * s, 13f, Pal.DIM)
            y += 58f * s
        }
    }

    override fun back(): Boolean { g.go(TitleScreen(g)); return true }
}

class CreditsScreen(g: GameView) : MenuScreen(g) {
    override fun layout() {
        buttons.clear()
        val s = g.s
        buttons.add(Button(RectF(30f * s, g.h - 70f * s, 170f * s, g.h - 18f * s), "back") { g.go(TitleScreen(g)) })
    }

    override fun draw(c: Canvas) {
        layout()
        backdrop(c, "s_wires", 0xd8)
        val s = g.s
        plate(c, "a-999 · credits", 40f * s, 56f * s)
        val lines = listOf(
            "smash rooms" to "made by jhulian (jhu7600codes)",
            "the rooms" to "every image, sound and the font come from the-rooms build",
            "rooms" to "nicorocks5555: a-60, a-90, a-120, a-200 (the happy scribble)",
            "doors" to "lsplash and rediblesqw: rush, ambush, eyes, screech, figure, seek",
            "the rooms: revisited" to "eclipse games: a-60b, a-90b",
            "font" to "caveat by the caveat project authors (sil ofl 1.1)",
            "note" to "unofficial fan game. not affiliated with roblox. art and sounds are personal use only.",
        )
        var y = 120f * s
        for ((a, b) in lines) {
            title(c, a, 40f * s, y, 34f, Pal.FG)
            mono(c, b, 40f * s, y + 26f * s, 15f, Pal.DIM)
            y += 72f * s
        }
        drawButtons(c)
    }

    override fun back(): Boolean { g.go(TitleScreen(g)); return true }
}

class ResultsScreen(g: GameView, private val b: Battle) : MenuScreen(g) {
    override fun enter() = Assets.music("death", 0.7f)

    override fun layout() {
        buttons.clear()
        val s = g.s
        val by = g.h - 74f * s
        buttons.add(Button(RectF(g.w - 230f * s, by, g.w - 30f * s, by + 54f * s), "rematch", true) { g.go(BattleScreen(g)) })
        buttons.add(Button(RectF(g.w - 460f * s, by, g.w - 250f * s, by + 54f * s), "new picks") { g.go(SelectScreen(g)) })
        buttons.add(Button(RectF(30f * s, by, 200f * s, by + 54f * s), "title") { g.go(TitleScreen(g)) })
    }

    override fun draw(c: Canvas) {
        layout()
        backdrop(c, b.stage.def.bg, 0xc8)
        val s = g.s
        val w = b.winner
        // winner first, then everyone else in reverse order of going out
        val order = listOfNotNull(w) + b.koOrder.reversed().filter { it !== w }
        if (w != null) {
            val bob = sin(g.frame * 0.06f) * 8f * s
            fit(c, Assets.img(w.def.spriteFor(w, g.frame)), RectF(60f * s, 90f * s + bob, 420f * s, 450f * s + bob))
            plate(c, if (w.human) "you win" else "cpu ${w.slot} wins", 60f * s, 70f * s)
            title(c, w.def.name, 240f * s, 520f * s, 90f, w.def.color, Paint.Align.CENTER)
        } else title(c, "no contest", 240f * s, 300f * s, 80f, Pal.DIM, Paint.Align.CENTER)

        var y = 110f * s
        val x = 500f * s
        mono(c, "place", x, y, 14f, Pal.BRASS)
        mono(c, "entity", x + 90f * s, y, 14f, Pal.BRASS)
        mono(c, "kos", x + 360f * s, y, 14f, Pal.BRASS)
        mono(c, "falls", x + 450f * s, y, 14f, Pal.BRASS)
        mono(c, "dmg", x + 550f * s, y, 14f, Pal.BRASS)
        for ((i, f) in order.withIndex()) {
            y += 70f * s
            title(c, "${i + 1}", x + 10f * s, y, 48f, if (i == 0) Pal.BRASS else Pal.DIM)
            fit(c, Assets.img(f.def.spriteFor(f, g.frame)), RectF(x + 80f * s, y - 50f * s, x + 140f * s, y + 6f * s))
            title(c, f.def.name, x + 150f * s, y - 6f * s, 34f, Pal.FG)
            mono(c, if (f.human) "you" else "cpu ${f.slot}", x + 150f * s, y + 14f * s, 12f, Pal.SLOTS[f.slot])
            title(c, "${f.kos}", x + 370f * s, y, 40f, Pal.FG)
            title(c, "${f.falls}", x + 460f * s, y, 40f, Pal.FG)
            title(c, "${f.dmg.toInt()}%", x + 550f * s, y, 40f, Pal.FG)
        }
        drawButtons(c)
    }

    override fun back(): Boolean { g.go(TitleScreen(g)); return true }
}
