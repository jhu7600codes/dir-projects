package com.jhu.smashrooms

import kotlin.math.abs
import kotlin.random.Random

/** cpu player. level 1-9 changes how fast it reacts and how often it does something smart. */
class Ai(private val level: Int, private val passive: Boolean) {
    private var wait = 0
    private var hold = 0
    private var held = Pad()
    private var target: Fighter? = null
    private val smart get() = level / 9f

    fun think(f: Fighter, b: Battle, p: Pad) {
        if (f.dead) return
        if (passive) { idleTrainer(f, b, p); return }

        // status effects the cpu is supposed to respect (sometimes it doesn't, like a real player)
        if (f.curse > 0 && Random.nextFloat() < 0.6f + smart * 0.39f) return
        if (f.halt > 0 && Random.nextFloat() < 0.5f + smart * 0.45f) return

        if (offstage(f, b)) { recover(f, b, p); return }

        if (hold > 0) {
            hold--
            p.copyFrom(held)
            if (hold > 0) return
        }
        if (wait > 0) { wait--; drift(f, b, p); return }

        val t = pickTarget(f, b) ?: return
        val dx = t.x - f.x
        val dy = t.y - f.y
        val adx = abs(dx)
        val dir = if (dx > 0) 1f else -1f

        // proceed: keep moving
        if (f.proceed > 0) p.x = dir

        // turn away from eyes' gaze and screech's psst
        val gazing = t.def.id == "eyes" && t.move === t.def.specials[0]
        if (gazing && Random.nextFloat() < smart) { p.x = -dir * 0.5f; return }
        val psst = b.fighters.firstOrNull { it.def.id == "screech" && it !== f && it.move === it.def.specials[0] && it.mTarget === f }
        if (psst != null && Random.nextFloat() < smart * 0.8f) {
            p.x = if (psst.x > f.x) 0.4f else -0.4f
            return
        }

        // shield when something's swinging at us up close
        if (t.move != null && adx < 200f && abs(dy) < 120f && Random.nextFloat() < smart * 0.25f) {
            press(p, hold = 14) { it.shd = true }
            return
        }

        if (f.fear >= 100f && adx < 600f && Random.nextFloat() < 0.05f + smart * 0.1f) {
            press(p, 2) { it.sup = true }
            return
        }

        val reach = f.w * 0.9f + 40f
        if (adx < reach && abs(dy) < f.h) {
            // close: hit it
            val r = Random.nextFloat()
            press(p, 3) {
                it.atk = true
                when {
                    dy < -60f -> it.y = -1f
                    // full tilt from neutral in one frame reads as a flick, so this is a smash
                    r < 0.25f + smart * 0.2f && f.grounded -> it.x = dir
                    r < 0.7f -> it.x = dir * 0.5f
                }
            }
            wait = (20 - level * 2 + Random.nextInt(8)).coerceAtLeast(2)
            return
        }

        // mid range: specials sometimes
        if (Random.nextFloat() < 0.02f + smart * 0.03f) {
            val pick = Random.nextInt(4)
            press(p, 2) {
                it.spc = true
                when (pick) {
                    1 -> it.x = dir
                    3 -> it.y = 1f
                    2 -> if (!f.grounded) it.y = -1f
                }
            }
            if (pick == 1 || pick == 0) f.facing = dir.toInt()
            wait = 15
            return
        }

        // approach
        p.x = dir * if (adx > 300f) 1f else 0.7f
        if (dy < -120f && f.grounded && Random.nextFloat() < 0.08f) p.jmp = true
        if (dy < -150f && !f.grounded && f.vy > 0f && f.jumps > 0 && Random.nextFloat() < 0.1f) p.jmp = true
        if (dy > 100f && f.ground?.solid == false && Random.nextFloat() < 0.1f) p.y = 1f
        if (Random.nextFloat() < 0.005f) p.jmp = true
    }

    private fun press(p: Pad, hold: Int, fill: (Pad) -> Unit) {
        held.clear()
        fill(held)
        p.copyFrom(held)
        this.hold = hold
    }

    private fun drift(f: Fighter, b: Battle, p: Pad) {
        val t = target ?: return
        if (abs(t.x - f.x) > 250f) p.x = if (t.x > f.x) 0.6f else -0.6f
    }

    private fun pickTarget(f: Fighter, b: Battle): Fighter? {
        val cur = target
        if (cur != null && !cur.dead && cur.stocks > 0 && Random.nextFloat() > 0.01f) return cur
        target = b.fighters.filter { it !== f && !it.dead && it.stocks > 0 }
            .minByOrNull { abs(it.x - f.x) + if (it.human) -150f else 0f }
        return target
    }

    private fun offstage(f: Fighter, b: Battle) = f.x < b.stage.left - 10f || f.x > b.stage.right + 10f || f.y > b.stage.main.y + 30f

    private fun recover(f: Fighter, b: Battle, p: Pad) {
        if (f.hitstun > 0) { p.x = if (f.x < 0) 1f else -1f; return }
        val toward = if (f.x < 0) 1f else -1f
        p.x = toward
        val below = f.y > b.stage.main.y - 40f
        if (f.vy > 0f && f.jumps > 0 && (below || Random.nextFloat() < 0.15f)) { p.jmp = true; return }
        if (f.jumps == 0 && !f.helpless && f.vy > -2f && f.move == null) {
            p.spc = true; p.y = -1f; p.x = toward * 0.5f
        }
    }

    /** training dummy: just walks around a bit and shields sometimes */
    private fun idleTrainer(f: Fighter, b: Battle, p: Pad) {
        if (offstage(f, b)) { recover(f, b, p); return }
        if (b.t % 240 < 40) p.x = if ((b.t / 240) % 2 == 0) 0.5f else -0.5f
    }
}
