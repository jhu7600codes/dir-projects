package com.jhu.smashrooms

import kotlin.math.abs
import kotlin.math.sin

/** a floor. solid ones have sides and an underside, thin ones you can drop through and jump up into. */
class Plat(var x1: Float, var x2: Float, var y: Float, val solid: Boolean, val thick: Float = 0f) {
    var dx = 0f
    var dy = 0f
    /** moving platforms: swing around (ox, oy) */
    var swingX = 0f
    var swingY = 0f
    var period = 0f
    private val ox1 = x1
    private val ox2 = x2
    private val oy = y

    fun tick(t: Int) {
        if (period == 0f) return
        val a = t / period * 6.2832f
        val nx = ox1 + sin(a) * swingX
        val ny = oy + sin(a * 2f) * swingY
        dx = nx - x1
        dy = ny - y
        x2 = ox2 + (nx - ox1)
        x1 = nx
        y = ny
    }
}

class StageDef(
    val name: String,
    val bg: String,
    val music: String,
    val tint: Int,
    val dark: Boolean = false,
    val build: () -> List<Plat>,
)

class Stage(val def: StageDef) {
    val plats = def.build()
    val main = plats.first { it.solid }
    val left get() = main.x1
    val right get() = main.x2
    val blastL = main.x1 - 820f
    val blastR = main.x2 + 820f
    val blastT = -1150f
    val blastB = 720f
    var t = 0

    fun tick() {
        t++
        plats.forEach { it.tick(t) }
    }

    fun collide(f: Fighter, b: Battle, drop: Boolean) {
        val wasGround = f.ground
        if (f.grounded && wasGround != null) {
            f.x += wasGround.dx
            f.y += wasGround.dy
        }
        val prevY = f.y
        f.x += f.vx
        f.y += f.vy
        var landed: Plat? = null
        val hw = f.w * 0.3f
        for (p in plats) {
            if (f.x + hw < p.x1 || f.x - hw > p.x2) continue
            if (f.vy >= 0f && prevY <= p.y + 2f + abs(p.dy) && f.y >= p.y) {
                if (!p.solid && drop) continue
                landed = p
                f.y = p.y
                if (f.vy > 0f && !(f.hitstun > 0 && f.vy > 10f)) f.vy = 0f
            }
        }
        // sides and underside of the main block
        for (p in plats) {
            if (!p.solid) continue
            val top = p.y
            val bot = p.y + p.thick
            if (f.y > top + 2f && f.y - f.h < bot && f.x + f.w * 0.35f > p.x1 && f.x - f.w * 0.35f < p.x2) {
                if (prevY - f.h >= bot - 2f && f.vy < 0f) {
                    f.y = bot + f.h
                    f.vy = 0f
                } else {
                    val toL = f.x + f.w * 0.35f - p.x1
                    val toR = p.x2 - (f.x - f.w * 0.35f)
                    if (toL < toR) f.x = p.x1 - f.w * 0.35f else f.x = p.x2 + f.w * 0.35f
                    if (f.hitstun > 0) f.vx = -f.vx * 0.5f else f.vx = 0f
                }
            }
        }
        if (landed != null) {
            f.ground = landed
            if (f.vy >= 0f) f.land(b) else f.grounded = false
            if (f.hitstun > 0 && f.vy < 0f) { f.grounded = false; f.ground = null }
        } else {
            f.grounded = false
            f.ground = null
        }
    }

    /** standing spot for respawns / the start */
    fun spawn(i: Int, n: Int): Pair<Float, Float> {
        val span = (right - left) * 0.7f
        val x = left + (right - left) * 0.15f + if (n <= 1) span / 2 else span * i / (n - 1)
        return x to main.y
    }

    companion object {
        val ALL = listOf(
            StageDef("the lobby", "s_lobby", "exit_ambience", 0xff2a1a12.toInt()) {
                listOf(
                    Plat(-560f, 560f, 0f, true, 300f),
                    Plat(-380f, -140f, -190f, false),
                    Plat(140f, 380f, -190f, false),
                )
            },
            StageDef("the offices", "floor_offices", "a120_far", 0xff1d1b17.toInt()) {
                listOf(
                    Plat(-600f, 600f, 0f, true, 260f),
                    Plat(-420f, -170f, -180f, false),
                    Plat(170f, 420f, -180f, false),
                    Plat(-125f, 125f, -350f, false),
                )
            },
            StageDef("the wires", "floor_wires", "a60_far", 0xff101418.toInt()) {
                listOf(
                    Plat(-520f, 520f, 0f, true, 240f),
                    Plat(-300f, -60f, -200f, false).apply { swingX = 160f; period = 360f },
                    Plat(60f, 300f, -320f, false).apply { swingX = -160f; period = 360f },
                )
            },
            StageDef("locker room", "s_locker", "rush_far", 0xff15161a.toInt()) {
                listOf(Plat(-680f, 680f, 0f, true, 280f))
            },
            StageDef("the tall room", "s_tall", "ambush_far", 0xff1a1414.toInt()) {
                listOf(
                    Plat(-470f, 470f, 0f, true, 300f),
                    Plat(-330f, -120f, -170f, false),
                    Plat(120f, 330f, -170f, false),
                    Plat(-330f, -120f, -360f, false),
                    Plat(120f, 330f, -360f, false),
                    Plat(-100f, 100f, -530f, false),
                )
            },
            StageDef("the city", "s_city", "eyes_ambience", 0xff12141c.toInt()) {
                listOf(
                    Plat(-720f, 720f, 0f, true, 260f),
                    Plat(-200f, 200f, -230f, false).apply { swingX = 360f; swingY = 40f; period = 540f },
                )
            },
            StageDef("the dark", "s_dark", "a60_rumble", 0xff050506.toInt(), dark = true) {
                listOf(
                    Plat(-540f, 540f, 0f, true, 260f),
                    Plat(-360f, -120f, -200f, false),
                    Plat(120f, 360f, -200f, false),
                )
            },
            StageDef("a-200's room", "s_a200", "a200_voice", 0xff141b2c.toInt()) {
                listOf(
                    Plat(-460f, 460f, 0f, true, 240f),
                    Plat(-140f, 140f, -210f, false).apply { swingY = 60f; period = 300f },
                )
            },
            StageDef("the wire end", "s_wires", "a60b_far", 0xff140f0e.toInt()) {
                listOf(
                    Plat(-500f, 500f, 0f, true, 260f),
                    Plat(-650f, -420f, -150f, false),
                    Plat(420f, 650f, -150f, false),
                    Plat(-120f, 120f, -260f, false),
                )
            },
        )
    }
}
