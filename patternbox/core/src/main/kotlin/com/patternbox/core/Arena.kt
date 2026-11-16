package com.patternbox.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

data class Vec(val x: Float, val y: Float) {
    operator fun plus(o: Vec) = Vec(x + o.x, y + o.y)
    fun rot(a: Float): Vec {
        val c = cos(a)
        val s = sin(a)
        return Vec(x * c - y * s, x * s + y * c)
    }
}

/** shared shape math, the renderer uses the same numbers as the hit detection */
object Geo {
    const val BS = 26f   // body dot spacing
    const val WS = 22f   // weapon dot spacing
    const val AS = 31f   // armour dot spacing
    const val R = 36f    // fighter collision radius
    const val REST_SWING = -0.9f

    fun body(p: List<Int>) = p.map { Vec((dotX(it) - 1) * BS, (dotY(it) - 1) * BS) }
    fun armour(p: List<Int>) = p.map { Vec((dotX(it) - 1) * AS, (dotY(it) - 1) * AS) }

    class Weapon(val pts: List<Vec>, val base: Float, val reach: Float)

    /** grip = first dot. base = angle toward the furthest point, so the tip follows the swing */
    fun weapon(p: List<Int>): Weapon {
        val ox = dotX(p[0])
        val oy = dotY(p[0])
        val pts = p.map { Vec((dotX(it) - ox) * WS, (dotY(it) - oy) * WS) }
        var far = pts.getOrElse(1) { Vec(1f, 0f) }
        var fd = 0f
        for (q in pts) {
            val d = hypot(q.x, q.y)
            if (d > fd) { fd = d; far = q }
        }
        return Weapon(pts, atan2(far.y, far.x), fd)
    }

    /** weapon points in fighter-local (unflipped) space for a given swing angle */
    fun weaponLocal(body: List<Int>, weapon: List<Int>, swing: Float): List<Vec> {
        val b = body(body)
        val hand = b.last()
        val g = weapon(weapon)
        val sw = swing - g.base
        return g.pts.map { hand + it.rot(sw) }
    }

    /** where the googly eyes go: the highest body dot (rightmost on ties) */
    fun head(p: List<Int>): Vec {
        val b = body(p)
        var top = b[0]
        for (q in b) if (q.y < top.y || (q.y == top.y && q.x > top.x)) top = q
        return top
    }
}

class Fighter(val index: Int, val loadout: Loadout, val name: String) {
    val stats = loadout.stats
    var x = 0f; var y = 0f; var vx = 0f; var vy = 0f
    var ang = 0f; var av = 0f
    var hp = stats.hp.toFloat()
    var face = 1f
    var swingT = -1f
    var hitDone = false
    var cd = 0f
    var stun = 0f
    val plates = BooleanArray(stats.plates) { true }
    var dead = false
    var ground = false
    var look = 0f
    var aiJumpT = 0f
    var held = false
    val reach = Geo.weapon(loadout.weapon).reach + 30f

    val swingProgress get() = if (swingT < 0) -1f else swingT / stats.swing
    val hot get() = swingProgress in 0.3f..0.75f
}

class Debris(
    var x: Float, var y: Float, val a: Vec, val b: Vec,
    val kind: Kind, val owner: Int, var vx: Float, var vy: Float,
) {
    enum class Kind { BODY, WEAPON, ARMOUR }
    var ang = 0f
    var av = (Random.nextFloat() - .5f) * .4f
    var life = 6f
}

class FloatText(var x: Float, var y: Float, val text: String, val tone: Tone) {
    enum class Tone { NORMAL, CRIT, METAL, GOOD }
    var life = 1.1f
}

class Boom(val x: Float, val y: Float) { var t = 0f }

enum class Gravity(val label: String, val g: Float) {
    EARTH("earth", .55f), MOON("moon", .15f), OFF("off", 0f), JUPITER("jupiter", 1.3f)
}

enum class Tool { GRAB, BOOM, POKE }

/** one fighting arena. units are roughly css pixels; step() is one 1/60s tick */
class Arena(var width: Float = 900f, var height: Float = 500f) {
    var fighters: List<Fighter> = emptyList(); private set
    val debris = mutableListOf<Debris>()
    val texts = mutableListOf<FloatText>()
    val booms = mutableListOf<Boom>()
    var shake = 0f
    var winner: String? = null; private set
    var winT = 0f; private set
    var gravity = Gravity.EARTH
    val bot = booleanArrayOf(true, true)
    /** bumps every time something hits hard, the app turns it into haptics */
    var impacts = 0; private set
    var time = 0f; private set

    // input per player
    class Input { var left = false; var right = false; var jump = false }
    val input = arrayOf(Input(), Input())

    private var grabbed: Fighter? = null
    private var gx = 0f; private var gy = 0f; private var gvx = 0f; private var gvy = 0f

    val floorY get() = height - 28f

    fun start(a: Loadout, aName: String, b: Loadout, bName: String) {
        debris.clear(); texts.clear(); booms.clear()
        winner = null; winT = 0f; grabbed = null
        fighters = listOf(Fighter(0, a, aName), Fighter(1, b, bName)).onEach {
            it.x = if (it.index == 0) width * .25f else width * .75f
            it.y = floorY - Geo.R - 60f
            it.face = if (it.index == 0) 1f else -1f
        }
    }

    private fun say(x: Float, y: Float, t: String, tone: FloatText.Tone = FloatText.Tone.NORMAL) {
        texts += FloatText(x, y, t, tone)
    }

    fun swing(f: Fighter) {
        if (f.dead || f.swingT >= 0 || f.cd > 0 || f.held || winner != null) return
        f.swingT = 0f; f.hitDone = false
    }

    fun swingAngle(f: Fighter): Float {
        if (f.swingT < 0) return Geo.REST_SWING + sin(time * 2.5f + f.index) * 0.08f
        val t = f.swingProgress
        return when {
            t < .3f -> -.9f - (t / .3f) * 1f
            t < .7f -> -1.9f + ((t - .3f) / .4f) * 3.4f
            else -> 1.5f - ((t - .7f) / .3f) * 2.4f
        }
    }

    fun weaponWorld(f: Fighter): List<Vec> =
        Geo.weaponLocal(f.loadout.body, f.loadout.weapon, swingAngle(f)).map {
            Vec(f.x, f.y) + Vec(it.x * f.face, it.y).rot(f.ang)
        }

    fun damage(t: Fighter, amount: Float, fromX: Float, kb: Float, label: String) {
        if (t.dead) return
        val alive = t.plates.count { it }
        val frac = if (t.plates.isNotEmpty()) alive.toFloat() / t.plates.size else 0f
        val block = t.stats.def * frac
        var dmg = max(1, (amount * (1 - block) * (.85f + Random.nextFloat() * .3f)).toInt())
        val crit = Random.nextFloat() < .08f
        if (crit) dmg *= 2
        t.hp -= dmg
        val dir = sign(t.x - fromX).let { if (it == 0f) 1f else it }
        t.vx += dir * kb; t.vy -= kb * .6f; t.av += dir * kb * .03f; t.stun = .35f
        say(t.x, t.y - 50, (if (crit) "crit " else "") + "-$dmg" + (if (label.isNotEmpty()) " $label" else ""),
            if (crit) FloatText.Tone.CRIT else FloatText.Tone.NORMAL)
        shake = min(14f, shake + dmg * .3f)
        impacts++
        if (alive > 0 && Random.nextFloat() < .55f + dmg / 60f) {
            val idx = t.plates.indices.filter { t.plates[it] }.random()
            t.plates[idx] = false
            val ap = Geo.armour(t.loadout.armour)
            debris += Debris(t.x, t.y, Vec(ap[idx].x * t.face, ap[idx].y), Vec(ap[idx + 1].x * t.face, ap[idx + 1].y),
                Debris.Kind.ARMOUR, t.index, dir * (3 + Random.nextFloat() * 4), -5 - Random.nextFloat() * 4)
            say(t.x, t.y - 72, t.loadout.armourName + " cracked", FloatText.Tone.METAL)
        }
        if (t.hp <= 0) ko(t, dir)
    }

    private fun ko(t: Fighter, dir: Float) {
        t.hp = 0f; t.dead = true; t.swingT = -1f
        if (grabbed === t) { grabbed = null; t.held = false }
        val b = Geo.body(t.loadout.body)
        for (i in 1 until b.size) {
            debris += Debris(t.x, t.y, Vec(b[i - 1].x * t.face, b[i - 1].y), Vec(b[i].x * t.face, b[i].y),
                Debris.Kind.BODY, t.index, dir * (2 + Random.nextFloat() * 6) + (Random.nextFloat() - .5f) * 4, -6 - Random.nextFloat() * 6)
        }
        val g = Geo.weapon(t.loadout.weapon)
        for (i in 1 until g.pts.size) {
            debris += Debris(t.x, t.y - 20, g.pts[i - 1], g.pts[i], Debris.Kind.WEAPON, t.index,
                -dir * 4 + (Random.nextFloat() - .5f) * 6, -9f)
        }
        t.plates.fill(false)
        val other = fighters[1 - t.index]
        if (winner == null) { winner = if (other.dead) "" else other.name; winT = 0f }
        say(t.x, t.y - 80, Names.pick(Names.KO, t.loadout.body), FloatText.Tone.CRIT)
        shake = 18f
        impacts += 3
    }

    // ---- sandbox tools ----
    fun fighterAt(x: Float, y: Float) = fighters.firstOrNull { !it.dead && hypot(it.x - x, it.y - y) < Geo.R + 18 }

    fun pointerDown(tool: Tool, x: Float, y: Float) {
        when (tool) {
            Tool.GRAB -> fighterAt(x, y)?.let { f ->
                grabbed = f; f.held = true; f.swingT = -1f
                gx = x; gy = y; gvx = 0f; gvy = 0f
                say(f.x, f.y - 60, Names.GRABBED.random())
            }
            Tool.BOOM -> boom(x, y)
            Tool.POKE -> fighterAt(x, y)?.let { damage(it, 3f, x, 5f, "poke") }
        }
    }

    fun pointerMove(x: Float, y: Float) {
        if (grabbed == null) return
        gvx = x - gx; gvy = y - gy; gx = x; gy = y
    }

    fun pointerUp() {
        val f = grabbed ?: return
        f.held = false
        f.vx = (gvx * 1.1f).coerceIn(-45f, 45f)
        f.vy = (gvy * 1.1f).coerceIn(-45f, 45f)
        f.av = gvx * .02f
        grabbed = null
    }

    fun boom(x: Float, y: Float) {
        booms += Boom(x, y)
        for (f in fighters) {
            val d = hypot(f.x - x, f.y - y)
            if (d < 170) {
                val k = 1 - d / 170
                val n = if (d == 0f) 1f else d
                f.vx += (f.x - x) / n * 18 * k
                f.vy += ((f.y - y) / n * 18 - 6) * k
                f.av += (Random.nextFloat() - .5f) * .5f * k
                if (k > .15f) damage(f, 22 * k, x, 0f, "boom")
            }
        }
        for (d in debris) {
            val dd = hypot(d.x - x, d.y - y)
            if (dd < 200) { d.vx += (d.x - x) / max(dd, 1f) * 12; d.vy -= 10 }
        }
        shake = 16f
        impacts += 2
    }

    fun chaos() {
        for (f in fighters) {
            f.vx += (Random.nextFloat() - .5f) * 30
            f.vy -= 8 + Random.nextFloat() * 12
            f.av += (Random.nextFloat() - .5f) * .8f
        }
        say(width / 2, 60f, "chaos", FloatText.Tone.CRIT)
    }

    fun heal() {
        for (f in fighters) if (!f.dead) {
            f.hp = f.stats.hp.toFloat(); f.plates.fill(true)
            say(f.x, f.y - 60, "+heal", FloatText.Tone.GOOD)
        }
    }

    // ---- sim ----
    fun step(dt: Float = 1f / 60f) {
        if (fighters.size < 2) return
        time += dt
        val g = gravity.g
        val fl = floorY
        for (f in fighters) {
            val o = fighters[1 - f.index]
            f.cd = max(0f, f.cd - dt); f.stun = max(0f, f.stun - dt)
            if (!f.dead && !f.held) f.face = if (o.x >= f.x) 1f else -1f
            f.look = atan2(o.y - f.y, (o.x - f.x) * f.face)

            if (f.held) {
                f.x += (gx - f.x) * .5f; f.y += (gy - f.y) * .5f
                f.vx = 0f; f.vy = 0f
                f.ang += sin(time * 11f) * .04f
                continue
            }

            var mv = 0f
            var jump = false
            if (!f.dead && f.stun <= 0 && winner == null) {
                if (bot[f.index]) {
                    val dx = o.x - f.x
                    val adx = abs(dx)
                    val scared = f.hp < f.stats.hp * .25f && Random.nextFloat() < .5f
                    mv = when {
                        o.dead -> 0f
                        scared && adx < 200 -> -sign(dx)
                        adx > f.reach + 10 -> sign(dx)
                        adx < f.reach * .5f -> -sign(dx) * .6f
                        else -> 0f
                    }
                    if (!o.dead && adx < f.reach + 40 && abs(o.y - f.y) < 90 && Random.nextFloat() < .08f) swing(f)
                    f.aiJumpT -= dt
                    if (f.aiJumpT <= 0) {
                        f.aiJumpT = .6f + Random.nextFloat() * 1.6f
                        if (Random.nextFloat() < .45f || o.y < f.y - 50) jump = true
                    }
                } else {
                    val inp = input[f.index]
                    if (inp.left) mv -= 1
                    if (inp.right) mv += 1
                    jump = inp.jump
                }
            }
            val accel = if (f.ground) .9f else .35f
            if (mv != 0f) f.vx += (mv * f.stats.speed - f.vx) * accel * .4f
            if (jump && f.ground) {
                f.vy = -f.stats.jump * (if (g == 0f) .4f else 1f) * (if (gravity == Gravity.JUPITER) 1.3f else 1f)
                f.ground = false
            }

            f.vy += g
            if (g == 0f) { f.vx *= .995f; f.vy *= .995f }
            f.x += f.vx; f.y += f.vy
            f.ang += f.av; f.av *= .96f
            if (!f.dead) { f.av += -f.ang * .02f; f.ang *= if (f.ground) .85f else .99f }

            fun impact(speed: Float, label: String) {
                if (speed > 15 && !f.dead) damage(f, (speed - 15) * 2.2f, f.x, 0f, label)
            }
            f.ground = false
            val r = Geo.R
            if (f.y > fl - r) {
                impact(f.vy, "splat")
                f.y = fl - r
                f.vy = if (f.dead) -f.vy * .3f else if (abs(f.vy) > 6) -f.vy * .25f else 0f
                f.ground = true
                f.vx *= if (f.dead) .9f else if (mv != 0f) 1f else .8f
                if (f.dead) f.av += f.vx * .01f
            }
            if (f.y < r) { impact(-f.vy, "bonk"); f.y = r; f.vy = -f.vy * .4f }
            if (f.x < r) { impact(-f.vx, "wall"); f.x = r; f.vx = -f.vx * .5f; f.av += .1f }
            if (f.x > width - r) { impact(f.vx, "wall"); f.x = width - r; f.vx = -f.vx * .5f; f.av -= .1f }

            if (f.swingT >= 0) {
                f.swingT += dt
                val t = f.swingProgress
                if (!f.hitDone && t > .3f && t < .75f && !o.dead && !o.held) {
                    val ws = weaponWorld(f)
                    for (i in 1 until ws.size) {
                        if (segDist(o.x, o.y, ws[i - 1], ws[i]) < r) {
                            f.hitDone = true
                            damage(o, f.stats.dmg.toFloat(), f.x, 4 + f.stats.dmg * .25f, "")
                            break
                        }
                    }
                    if (!f.hitDone && o.swingT >= 0) clash@ for (i in 1 until ws.size) {
                        val ow = weaponWorld(o)
                        for (j in 1 until ow.size) {
                            if (segDist(ws[i].x, ws[i].y, ow[j - 1], ow[j]) < 6) {
                                f.hitDone = true; o.hitDone = true
                                f.vx -= f.face * 7; o.vx -= o.face * 7; f.vy -= 3; o.vy -= 3
                                say((f.x + o.x) / 2, min(f.y, o.y) - 60, "clang", FloatText.Tone.CRIT)
                                shake = 8f; impacts++
                                break@clash
                            }
                        }
                    }
                }
                if (t >= 1) { f.swingT = -1f; f.cd = .15f }
            }
        }

        val a = fighters[0]
        val b = fighters[1]
        if (!a.held && !b.held) {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val d = max(hypot(dx, dy), 1f)
            val ov = Geo.R * 2 - d
            if (ov > 0) {
                val nx = dx / d
                val ny = dy / d
                a.x -= nx * ov / 2; a.y -= ny * ov / 2; b.x += nx * ov / 2; b.y += ny * ov / 2
                val rel = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny
                if (rel < 0) {
                    a.vx += nx * rel * .6f; a.vy += ny * rel * .6f
                    b.vx -= nx * rel * .6f; b.vy -= ny * rel * .6f
                }
            }
        }

        for (d in debris) {
            d.vy += g; d.x += d.vx; d.y += d.vy; d.ang += d.av; d.life -= dt
            if (d.y > fl - 4) { d.y = fl - 4; d.vy *= -.35f; d.vx *= .8f; d.av *= .7f }
            if (d.x < 4 || d.x > width - 4) { d.vx *= -.6f; d.x = d.x.coerceIn(4f, width - 4) }
        }
        debris.removeAll { it.life <= 0 }
        for (t in texts) { t.y -= 1.2f; t.life -= dt }
        texts.removeAll { it.life <= 0 }
        for (bm in booms) bm.t += dt
        booms.removeAll { it.t >= .5f }
        if (winner != null) winT += dt
        shake *= .88f
    }

    /** keep fighters inside if the canvas changed size */
    fun resize(w: Float, h: Float) {
        width = w; height = h
        for (f in fighters) {
            f.x = f.x.coerceIn(Geo.R, w - Geo.R)
            f.y = f.y.coerceIn(Geo.R, floorY - Geo.R)
        }
    }

    companion object {
        fun segDist(px: Float, py: Float, a: Vec, b: Vec): Float {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val l = (dx * dx + dy * dy).let { if (it == 0f) 1f else it }
            val t = (((px - a.x) * dx + (py - a.y) * dy) / l).coerceIn(0f, 1f)
            return hypot(px - a.x - dx * t, py - a.y - dy * t)
        }

        @Suppress("unused") const val TAU = (PI * 2).toFloat()
    }
}
