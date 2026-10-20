package com.jhu.smashrooms

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** every entity's specials (neutral, side, up, down) and their fear super. no two work the same. */
object Moves {
    class Kit(val n: Move, val s: Move, val u: Move, val d: Move, val x: Move)

    // ---------- helpers ----------

    private fun foes(f: Fighter, b: Battle) = b.fighters.filter { it !== f && !it.dead && it.stocks > 0 }

    private fun shot(
        f: Fighter, b: Battle, dx: Float, dy: Float, vx: Float, vy: Float, r: Float, life: Int,
        dmg: Float, base: Float, growth: Float, angle: Float, sprite: String? = null,
    ): Proj {
        val p = Proj(f, f.x + dx * f.facing, f.cy + dy, vx * f.facing, vy, r, life, dmg, base, growth, angle)
        p.sprite = sprite
        p.color = f.def.color
        return b.shoot(p)
    }

    private fun warp(f: Fighter, b: Battle, x: Float, y: Float) {
        b.ghost(f.def.sprite, f.x, f.cy, f.h, 14, f.def.color)
        b.burst(f.x, f.cy, f.def.color, 8)
        f.x = x.coerceIn(b.stage.blastL + 120f, b.stage.blastR - 120f)
        f.y = y.coerceIn(b.stage.blastT + 200f, b.stage.blastB - 100f)
        f.vx = 0f; f.vy = 0f
        f.grounded = false
        b.ring(f.x, f.cy, 90f, f.def.color)
    }

    private fun behind(t: Fighter) = t.x - t.facing * (t.w * 0.5f + 50f)

    /** a big sprite sweeping across the stage. used by the "chase" supers */
    private fun pass(f: Fighter, b: Battle, sprite: String, y: Float, dir: Int, speed: Float, dmg: Float, size: Float) {
        val sx = if (dir > 0) b.stage.blastL + 100f else b.stage.blastR - 100f
        val p = Proj(f, sx, y, speed * dir, 0f, size * 0.4f, ((b.stage.blastR - b.stage.blastL) / speed).toInt() + 10, dmg, 60f, 90f, 40f)
        p.sprite = sprite
        p.size = size
        p.pierce = true
        p.color = f.def.color
        p.onTick = { pp, bb -> if (pp.t % 3 == 0) bb.burst(pp.x - pp.vx, pp.y, f.def.color, 1, 3f) }
        b.shoot(p)
    }

    private fun warn(b: Battle, y: Float) {
        b.fx.add(Fx(Fx.BEAM, b.stage.blastL, y, 0f, 0f, 24, 0x66ff2020, 90f).also { it.a0 = b.stage.blastR - b.stage.blastL })
    }

    /** a-90's stop sign punishment, called from the curse check in Fighter */
    fun a90Jumpscare(a: Fighter, v: Fighter, b: Battle) {
        v.shielding = false
        v.invuln = 0
        v.intangible = false
        b.scare(v, "a90_jumpscare", 36)
        b.ghost("a90_jumpscare", v.x, v.cy - 20f, v.h * 1.8f, 30)
        Assets.play("a90_jumpscare", 1f)
        b.flash(0xaa000000.toInt(), 8)
        val dir = if (v.x >= a.x) 1 else -1
        b.apply(a, v, 22f, 70f, 90f, 50f, dir, a.x, a.cy, false, null)
        b.text(v.x, v.y - v.h - 30f, "you moved", 0xffff3030.toInt())
    }

    /** a-60b's counter, called when something hits it during the stance */
    fun counterHit(v: Fighter, a: Fighter, b: Battle) {
        Assets.play("a60b_near", 1f)
        b.flash(0x88a3161c.toInt(), 8)
        warp(v, b, behind(a), a.y)
        v.faceToward(a)
        v.invuln = 20
        v.endMove()
        val dir = if (a.x >= v.x) 1 else -1
        b.apply(v, a, 15f, 60f, 85f, 40f, dir, v.x, v.cy, false, null)
        b.text(v.x, v.y - v.h - 20f, "countered", 0xffff6b6b.toInt())
    }

    // ---------- rush ----------

    fun rush() = Kit(
        n = Move("light flicker", 34, cooldown = 60, desc = "lights flicker around you. anyone close gets stunned.") { f, b, t ->
            if (t == 6) {
                Assets.play("shakelight", 1f, 0.8f)
                b.flash(0x66000000, 6)
                b.ring(f.x, f.cy, 170f, 0xfffff6c8.toInt())
                b.hit(f, 0f, 0f, 340f, 240f, 4f, 0f, 0f, 0f) { it.stun = 50; it.flash = 20 }
            }
        },
        s = Move("rush pass", 42, drift = false, cooldown = 45, desc = "rushes straight through the room. doesn't stop for anyone.") { f, b, t ->
            f.noGrav = true
            if (t == 0) Assets.play("rush_far", 0.8f, 1.3f)
            if (t in 6..30) {
                f.vx = f.facing * 24f; f.vy = 0f; f.trail = 3
                b.hit(f, f.w * 0.2f, 0f, f.w * 1.2f, f.h, 9f, 45f, 75f, 30f)
            }
            if (t > 30) f.vx *= 0.8f
        },
        u = Move("ceiling rush", 34, helpless = true, drift = false, desc = "screams upward, hitting a bunch of times.") { f, b, t ->
            f.noGrav = t < 24
            if (t in 4..23) {
                f.vy = -19f
                f.vx = f.facing * 5f + f.pad.x * 4f
                f.trail = 3
                if (t % 6 == 4) f.group = b.nextGroup()
                b.hit(f, 0f, -20f, f.w * 1.2f, f.h, 3f, 25f, 20f, 80f)
            }
            if (t == 24) { f.group = b.nextGroup(); b.hit(f, 0f, -30f, f.w * 1.4f, f.h, 6f, 45f, 90f, 75f) }
        },
        d = Move("ground rumble", 44, drift = false, desc = "slams down. landing sends shockwaves both ways.") { f, b, t ->
            if (!f.grounded && f.m0 == 0f) {
                f.noGrav = true
                f.vy = 24f; f.vx *= 0.8f
                b.hit(f, 0f, f.h * 0.4f, f.w, 80f, 10f, 25f, 70f, -60f)
                if (t == 43) f.moveT = 40
            }
            if (f.grounded && f.m0 == 0f) {
                f.m0 = 1f
                b.shake = 10f
                Assets.play("figure_step", 0.7f, 1.3f)
                for (dir in intArrayOf(-1, 1)) {
                    val p = Proj(f, f.x, f.y - 24f, dir * 14f, 0f, 26f, 30, 7f, 35f, 55f, 60f)
                    p.color = f.def.color
                    p.shape = 2
                    p.onTick = { pp, bb -> if (pp.t % 2 == 0) bb.dust(pp.x, pp.y + 24f, 1) }
                    b.shoot(p)
                }
            }
        },
        x = Move("the rush", 150, drift = false, desc = "rush tears across the whole room three times.") { f, b, t ->
            f.noGrav = true; f.intangible = true; f.invis = 4; f.vx = 0f; f.vy = 0f
            if (t == 0) { Assets.play("rush_far", 1f); b.darkT = 150 }
            val ys = floatArrayOf(-140f, -330f, -70f)
            for (k in 0..2) {
                if (t == k * 45) warn(b, ys[k])
                if (t == k * 45 + 20) pass(f, b, "rush_face", ys[k], if (k % 2 == 0) 1 else -1, 44f, 18f, 280f)
            }
            if (t == 149) f.intangible = false
        },
    )

    // ---------- ambush ----------

    fun ambush() = Kit(
        n = Move("rebound shriek", 36, cooldown = 50, desc = "a scream that flies out and comes back. hits twice.") { f, b, t ->
            if (t == 8) {
                Assets.play("ambush_far", 0.5f, 1.5f)
                val dir = f.facing
                val p = shot(f, b, f.w * 0.5f, -10f, 17f, 0f, 30f, 72, 6f, 30f, 50f, 40f)
                p.shape = 2
                p.pierce = true
                p.rehit = 36
                p.onTick = { pp, _ -> pp.vx -= dir * 0.5f }
            }
        },
        s = Move("ambush", 64, drift = false, cooldown = 60, desc = "dashes, rebounds, dashes again. three passes.") { f, b, t ->
            f.noGrav = true
            f.vy = 0f
            if (t == 0) Assets.play("ambush_far", 0.8f, 1.2f)
            if (t == 21 || t == 41) { f.facing = -f.facing; f.group = b.nextGroup(); b.ring(f.x, f.cy, 60f, f.def.color) }
            if (t in 4..58 && t % 20 in 1..18) {
                f.vx = f.facing * 21f; f.trail = 3
                b.hit(f, 0f, 0f, f.w * 1.1f, f.h, 6f, 35f, 55f, 35f)
            } else f.vx *= 0.6f
        },
        u = Move("rebound rise", 42, helpless = true, drift = false, desc = "flies up, bounces, flies up again.") { f, b, t ->
            f.noGrav = t < 34
            f.vx = f.facing * 3f + f.pad.x * 4f
            when (t) {
                in 3..17 -> { f.vy = -20f; b.hit(f, 0f, -10f, f.w * 1.2f, f.h, 7f, 30f, 60f, 80f) }
                in 18..21 -> f.vy = 6f
                22 -> f.group = b.nextGroup()
                in 23..33 -> { f.vy = -15f; b.hit(f, 0f, -10f, f.w * 1.2f, f.h, 6f, 45f, 80f, 80f) }
            }
        },
        d = Move("lurk", 54, cooldown = 120, desc = "vanishes, then strikes from behind whoever's closest.") { f, b, t ->
            if (t < 20) { f.intangible = true; f.invis = 3 }
            if (t == 20) {
                f.intangible = false
                f.nearestFoe(b)?.let { tg -> warp(f, b, behind(tg), tg.y - 4f); f.faceToward(tg) }
                Assets.play("ambush_far", 0.9f, 1.4f)
            }
            if (t in 22..26) b.hit(f, f.w * 0.4f, 0f, f.w * 1.3f, f.h, 11f, 40f, 85f, 45f)
        },
        x = Move("rebound storm", 200, drift = false, desc = "ambush rebounds across the room six times.") { f, b, t ->
            f.noGrav = true; f.intangible = true; f.invis = 4; f.vx = 0f; f.vy = 0f
            if (t == 0) Assets.play("ambush_far", 1f)
            if (t % 30 == 0 && t < 180) {
                val y = -60f - Random.nextFloat() * 300f
                warn(b, y)
                b.later(18) { pass(f, b, "ambush_face", y, if ((t / 30) % 2 == 0) 1 else -1, 46f, 13f, 240f) }
            }
            if (t == 199) f.intangible = false
        },
    )

    // ---------- eyes ----------

    fun eyes() = Kit(
        n = Move("gaze", 100, cooldown = 240, desc = "don't look at it. anyone facing eyes takes damage.") { f, b, t ->
            if (t == 0) Assets.play("eyes_ambience", 0.9f)
            if (t % 12 == 0) b.ring(f.x, f.cy, 120f + (t % 36) * 6f, 0xffb57bff.toInt())
            if (t > 10 && t % 8 == 0) for (o in foes(f, b)) {
                if (abs(o.x - f.x) < 800f && o.facing == (if (f.x > o.x) 1 else -1)) {
                    b.statusHit(f, o, 1.5f)
                    if (t % 24 == 0) Assets.play("eyes_hurt", 0.6f)
                }
            }
        },
        s = Move("eye orb", 34, cooldown = 40, desc = "a slow eye that drifts toward the nearest player.") { f, b, t ->
            if (t == 10) {
                val p = shot(f, b, f.w * 0.5f, -10f, 8f, 0f, 26f, 150, 7f, 30f, 60f, 45f, "eyes_face")
                p.homing = 0.04f
                p.size = 70f
                p.spinV = 3f
            }
        },
        u = Move("ascend", 60, helpless = true, desc = "floats up slowly, eyes spinning around it.") { f, b, t ->
            f.noGrav = true
            f.vy = -11f
            f.vx = f.pad.x * 6f
            if (t % 8 == 0) f.group = b.nextGroup()
            b.hit(f, 0f, 0f, f.w * 1.4f, f.h * 1.2f, 2f, 20f, 15f, 80f, radial = true)
            if (t % 6 == 0) b.burst(f.x, f.cy, 0xffb57bff.toInt(), 2)
        },
        d = Move("eye swarm", 30, cooldown = 300, desc = "leaves a cluster of eyes on the floor. it hurts to touch.") { f, b, t ->
            if (t == 8) {
                val p = Proj(f, f.x + f.facing * 60f, f.y - 36f, 0f, 0f, 40f, 330, 3f, 15f, 25f, 70f)
                p.sprite = "eyes_face"
                p.size = 90f
                p.pierce = true
                p.rehit = 30
                p.color = f.def.color
                p.onTick = { pp, _ -> pp.alpha = 150 + (60 * sin(pp.t * 0.15f)).toInt() }
                b.shoot(p)
                Assets.play("eyes_hurt", 0.5f, 0.7f)
            }
        },
        x = Move("room of eyes", 180, desc = "the room fills with eyes. look at any of them and it hurts.") { f, b, t ->
            if (t == 0) { Assets.play("eyes_ambience", 1f); b.darkT = 180 }
            if (t % 10 == 0) {
                val cx = f.x + (Random.nextFloat() - 0.5f) * 1200f
                val cy = -100f - Random.nextFloat() * 500f
                b.ghost("eyes_face", cx, cy, 90f + Random.nextFloat() * 80f, 50)
            }
            if (t % 6 == 0) for (o in foes(f, b)) if (o.facing == (if (f.x > o.x) 1 else -1)) b.statusHit(f, o, 2f)
            if (t == 175) for (o in foes(f, b)) {
                Assets.play("eyes_hurt", 1f)
                b.apply(f, o, 10f, 60f, 70f, 60f, if (o.x > f.x) 1 else -1, f.x, f.cy, true, null)
            }
        },
    )

    // ---------- screech ----------

    fun screech() = Kit(
        n = Move("psst", 70, cooldown = 150, desc = "psst. if they don't turn to look at you in time, you bite.") { f, b, t ->
            if (t == 0) {
                Assets.play("screech_psst", 1f)
                f.mTarget = f.nearestFoe(b)
            }
            if (t < 40) f.invis = 3
            if (t == 40) {
                val tg = f.mTarget ?: return@Move
                if (tg.dead) return@Move
                val looking = tg.facing == (if (f.x > tg.x) 1 else -1)
                if (looking) {
                    Assets.play("screech_caught", 1f)
                    b.text(f.x, f.y - f.h - 20f, "caught", 0xff7dff8a.toInt())
                    f.endMove()
                    f.stun = 60
                } else {
                    warp(f, b, behind(tg), tg.y)
                    f.faceToward(tg)
                    Assets.play("screech_bite", 1f)
                    b.apply(f, tg, 12f, 45f, 80f, 40f, f.facing, f.x, f.cy, false, null)
                }
            }
        },
        s = Move("lunge bite", 34, drift = false, desc = "a fast lunge with its whole mouth.") { f, b, t ->
            if (t in 6..16) {
                f.vx = f.facing * 18f
                if (!f.grounded) { f.noGrav = true; f.vy = -1f }
                f.trail = 2
                b.hit(f, f.w * 0.5f, 0f, f.w, f.h * 0.8f, 9f, 35f, 70f, 40f) { Assets.play("screech_bite", 0.9f) }
            }
            if (t > 16) { f.noGrav = false; f.vx *= 0.8f }
        },
        u = Move("tendril climb", 36, helpless = true, drift = false, desc = "pulls itself up with its tendrils.") { f, b, t ->
            if (t in 2..20) {
                f.noGrav = true
                f.vy = -19f
                f.vx = f.facing * 3f + f.pad.x * 5f
                if (t % 4 == 0) b.slash(f, 0f, -20f, 70f, -180f, 0f, 6)
                b.hit(f, 0f, -20f, f.w * 1.4f, f.h, 8f, 35f, 70f, 85f)
            }
        },
        d = Move("darkness", 24, cooldown = 300, desc = "hides in the dark. your next hit does 1.5x.") { f, b, t ->
            if (t == 4) {
                Assets.play("screech_psst", 0.7f, 0.7f)
                f.invis = 200
                f.bonus = 1.5f
                b.text(f.x, f.y - f.h - 20f, "x1.5", 0xffd8b25a.toInt())
            }
        },
        x = Move("feeding frenzy", 170, desc = "five bites on whoever it finds, then a big one.") { f, b, t ->
            f.noGrav = true
            if (t == 0) Assets.play("screech_psst", 1f, 0.8f)
            if (t % 28 == 14) {
                val list = foes(f, b)
                if (list.isNotEmpty()) {
                    val tg = list[Random.nextInt(list.size)]
                    warp(f, b, behind(tg), tg.y)
                    f.faceToward(tg)
                    Assets.play("screech_bite", 1f)
                    val big = t > 140
                    tg.hitstun = 0
                    b.apply(f, tg, if (big) 15f else 7f, if (big) 70f else 10f, if (big) 90f else 10f, 45f, f.facing, f.x, f.cy, false, null)
                }
            } else f.invis = 3
        },
    )

    // ---------- figure ----------

    fun figure() = Kit(
        n = Move("growl", 54, cooldown = 90, desc = "a roar that shoves everyone away.") { f, b, t ->
            if (t == 8) { Assets.play("figure_growl", 1f); b.shake = 14f }
            if (t in 10..16 && t % 3 == 1) b.ring(f.x, f.cy, 140f + t * 14f, 0xffff8a3d.toInt())
            if (t in 10..14) b.hit(f, 0f, -20f, 460f, 300f, 5f, 75f, 40f, 30f, radial = true)
        },
        s = Move("grab and slam", 58, drift = false, cooldown = 30, desc = "grabs whoever's in front and slams them behind.") { f, b, t ->
            if (t in 4..14) {
                f.vx = f.facing * 11f
                if (f.mTarget == null) b.hit(f, f.w * 0.55f, -10f, f.w * 0.9f, f.h * 0.8f, 2f, 0f, 0f, 0f) { v -> f.mTarget = v }
            }
            if (t > 14 && f.mTarget == null) f.vx *= 0.8f
            val tg = f.mTarget ?: return@Move
            if (t in 15..33) {
                tg.x = f.x + f.facing * (f.w * 0.55f)
                tg.y = f.y - 30f - (t - 15) * 3f
                tg.vx = 0f; tg.vy = 0f; tg.stun = 6; tg.hitstun = 0
            }
            if (t == 34) {
                f.facing = -f.facing
                tg.x = f.x + f.facing * f.w * 0.5f
                tg.y = f.y
                Assets.play("figure_kill", 1f)
                b.shake = 20f
                b.dust(tg.x, f.y, 10)
                b.apply(f, tg, 18f, 60f, 95f, 70f, f.facing, f.x, f.cy, false, null)
            }
        },
        u = Move("heavy leap", 70, helpless = true, desc = "a huge jump. lands with a shockwave.") { f, b, t ->
            if (t == 2) { f.vy = -27f; f.vx = f.facing * 6f; f.grounded = false }
            if (t in 2..18) b.hit(f, 0f, -f.h * 0.4f, f.w, f.h * 0.6f, 10f, 40f, 75f, 80f)
            if (t > 6 && f.grounded && f.m0 == 0f) {
                f.m0 = 1f
                f.group = b.nextGroup()
                Assets.play("figure_step", 1f, 0.8f)
                b.shake = 16f
                b.dust(f.x, f.y, 12)
                b.hit(f, 0f, f.h * 0.3f, 360f, 120f, 8f, 40f, 70f, 70f, radial = true)
                f.moveT = 60
            }
        },
        d = Move("stomp", 46, cooldown = 120, desc = "stomps so hard everyone standing on the floor trips.") { f, b, t ->
            if (t == 12) {
                Assets.play("figure_step", 1f, 0.7f)
                b.shake = 22f
                for (k in -6..6) b.dust(f.x + k * 70f, f.y, 2)
                b.hit(f, 0f, f.h * 0.35f, f.w * 2f, 80f, 12f, 45f, 70f, 60f, radial = true)
                if (f.grounded) for (o in foes(f, b)) if (o.grounded && abs(o.x - f.x) < 900f && abs(o.y - f.y) < 30f) {
                    o.stun = max(o.stun, 40); o.endMove(); b.statusHit(f, o, 4f); o.vy = -6f; o.grounded = false
                }
            }
        },
        x = Move("rampage", 220, drift = false, desc = "charges back and forth across the room, can't be stopped.") { f, b, t ->
            if (t == 0) { f.armor = 220; Assets.play("figure_growl", 1f); b.shake = 20f }
            if (t < 12) return@Move
            f.vx = f.facing * 14f
            if ((f.facing > 0 && f.x > b.stage.right - 70f) || (f.facing < 0 && f.x < b.stage.left + 70f)) {
                f.facing = -f.facing
                f.group = b.nextGroup()
            }
            if (t % 25 == 0) { Assets.play("figure_step", 0.8f); b.shake = 10f; f.group = b.nextGroup() }
            f.trail = 2
            b.hit(f, f.w * 0.3f, 0f, f.w * 1.2f, f.h, 13f, 55f, 85f, 40f)
        },
    )

    // ---------- seek ----------

    fun seek() = Kit(
        n = Move("seek eyes", 30, cooldown = 220, desc = "opens three eyes on the walls. they shoot at people.") { f, b, t ->
            if (t == 6) {
                Assets.play("seek_eyes", 1f)
                for (k in -1..1) {
                    val p = Proj(f, f.x + k * 260f, f.y - 380f - abs(k) * 40f, 0f, 0f, 30f, 220, 0f)
                    p.sprite = "seek_eye"
                    p.size = 80f
                    p.pierce = true
                    p.reflectable = false
                    p.color = f.def.color
                    p.onTick = { pp, bb ->
                        if (pp.t % 55 == 20 + (k + 1) * 10) pp.owner.nearestFoe(bb)?.let { tg ->
                            val a = atan2(tg.cy - pp.y, tg.x - pp.x)
                            val s = Proj(pp.owner, pp.x, pp.y, cos(a) * 13f, sin(a) * 13f, 12f, 90, 5f, 25f, 40f, 45f)
                            s.shape = 3
                            s.color = 0xff000000.toInt()
                            bb.shoot(s)
                        }
                    }
                    b.shoot(p)
                }
            }
        },
        s = Move("chase", 70, drift = false, cooldown = 60, desc = "a sprint that keeps hitting anyone in the way.") { f, b, t ->
            if (t == 0) Assets.play("seek_steps", 1f, 1.3f)
            if (t in 4..62) {
                f.vx = f.facing * 15f * f.speed
                if (t % 14 == 4) f.group = b.nextGroup()
                if (t % 5 == 0) b.dust(f.x, f.y, 1)
                b.hit(f, f.w * 0.4f, 0f, f.w, f.h, 7f, 35f, 60f, 35f)
            } else f.friction(0.8f)
        },
        u = Move("ink rise", 40, helpless = true, drift = false, desc = "rides a geyser of ink up.") { f, b, t ->
            if (t in 3..20) {
                f.noGrav = true
                f.vy = -21f
                f.vx = f.pad.x * 6f
                b.burst(f.x, f.y, 0xff000000.toInt(), 2, 4f)
                b.hit(f, 0f, 0f, f.w * 1.3f, f.h, 9f, 40f, 70f, 80f)
            }
        },
        d = Move("hands", 54, cooldown = 120, desc = "black hands burst out of the floor in front of you.") { f, b, t ->
            for (k in 0..2) if (t == 10 + k * 8) {
                val hx = f.x + f.facing * (130f + k * 120f)
                val gy = b.stage.plats.filter { hx in it.x1..it.x2 && it.y >= f.y - 10f }.minOfOrNull { it.y } ?: f.y
                b.fx.add(Fx(Fx.HAND, hx, gy, 0f, 0f, 26, 0xff000000.toInt(), 150f))
                b.hitAt(f, hx, gy - 75f, 70f, 150f, 8f, 40f, 70f, 85f, b.nextGroup(), f.facing) { Assets.play("seek_kill", 0.8f) }
            }
        },
        x = Move("the chase", 210, drift = false, desc = "seek screams and chases. hands come up everywhere.") { f, b, t ->
            if (t == 0) { f.armor = 210; Assets.play("seek_scream", 1f); b.flash(0x66000000, 20) }
            if (t < 16) return@Move
            f.vx = f.facing * 16f
            if ((f.facing > 0 && f.x > b.stage.right - 70f) || (f.facing < 0 && f.x < b.stage.left + 70f)) f.facing = -f.facing
            if (t % 16 == 0) {
                f.group = b.nextGroup()
                val hx = f.x + f.facing * 260f
                b.fx.add(Fx(Fx.HAND, hx, b.stage.main.y, 0f, 0f, 26, 0xff000000.toInt(), 180f))
                b.hitAt(f, hx, b.stage.main.y - 90f, 80f, 180f, 9f, 45f, 80f, 85f, b.nextGroup(), f.facing) { Assets.play("seek_kill", 0.8f) }
            }
            if (t % 20 == 0) Assets.play("seek_steps", 0.6f, 1.5f)
            b.hit(f, f.w * 0.3f, 0f, f.w * 1.1f, f.h, 12f, 50f, 80f, 40f)
        },
    )

    // ---------- a-60 ----------

    private val A60_FACES = arrayOf("power", "speed", "armor")

    fun a60() = Kit(
        n = Move("face swap", 22, cooldown = 30, desc = "swaps faces. power hits harder, speed moves faster, armor takes less.") { f, b, t ->
            if (t == 4) {
                f.face = (f.face + 1) % 3
                f.power = if (f.face == 0) 1.25f else 1f
                f.speed = if (f.face == 1) 1.3f else 1f
                f.guard = if (f.face == 2) 1.45f else 1f
                f.powerT = 0; f.speedT = 0; f.guardT = 0
                Assets.play("glitch", 0.6f, 1.6f)
                b.ring(f.x, f.cy, 100f, f.def.color)
                b.text(f.x, f.y - f.h - 20f, A60_FACES[f.face], 0xffff6b6b.toInt())
            }
        },
        s = Move("rumble rush", 48, drift = false, cooldown = 50, desc = "a screaming dash that shakes the whole screen.") { f, b, t ->
            f.noGrav = true
            if (t == 2) Assets.play("a60_near", 0.9f, 1.4f)
            if (t in 8..34) {
                f.vx = f.facing * 27f * f.speed; f.vy = 0f; f.trail = 3
                b.shake = max(b.shake, 5f)
                b.hit(f, f.w * 0.3f, 0f, f.w * 1.2f, f.h, 11f, 50f, 80f, 32f)
            } else f.vx *= 0.75f
        },
        u = Move("teleport", 30, helpless = true, drift = false, desc = "blinks up (or wherever you point).") { f, b, t ->
            f.noGrav = t < 20
            if (t < 8) { f.vx *= 0.7f; f.vy *= 0.5f }
            if (t == 8) {
                val px = f.pad.x
                val py = min(f.pad.y, -0.4f)
                Assets.play("glitch", 0.7f, 1.8f)
                warp(f, b, f.x + px * 280f, f.y + py * 320f)
                f.group = b.nextGroup()
            }
            if (t in 9..12) b.hit(f, 0f, 0f, f.w * 1.6f, f.h * 1.2f, 8f, 40f, 60f, 70f, radial = true)
        },
        d = Move("rumble", 30, cooldown = 200, desc = "the floor rumbles... then everyone standing on it gets hit.") { f, b, t ->
            if (t == 0) {
                Assets.play("a60_rumble", 1f)
                b.text(f.x, f.y - f.h - 20f, "!!", 0xffff3030.toInt())
                repeat(5) { k -> b.later(k * 9) { b.shake = max(b.shake, 4f + k * 2f) } }
                b.later(48) {
                    b.shake = 24f
                    b.flash(0x55a3161c, 6)
                    for (o in foes(f, b)) if (o.grounded) b.apply(f, o, 10f, 45f, 70f, 80f, if (o.x > f.x) 1 else -1, o.x, o.y, false, null)
                    for (k in -8..8) b.dust(k * 90f, b.stage.main.y, 2)
                }
            }
        },
        x = Move("a-60 chase", 200, drift = false, desc = "a-60 goes through the room four times, from both sides.") { f, b, t ->
            f.noGrav = true; f.intangible = true; f.invis = 4; f.vx = 0f; f.vy = 0f
            if (t == 0) { Assets.play("a60_far", 1f); b.darkT = 200 }
            for (k in 0..3) if (t == k * 42) {
                val y = -60f - Random.nextFloat() * 320f
                warn(b, y)
                b.later(22) { pass(f, b, "a60_face_${k % 3 + 1}", y, if (k % 2 == 0) 1 else -1, 50f, 16f, 300f) }
            }
            if (t == 199) f.intangible = false
        },
    )

    // ---------- a-60b ----------

    fun a60b() = Kit(
        n = Move("theme", 120, cooldown = 300, desc = "its theme plays and everything near you burns. you slow down.") { f, b, t ->
            if (t == 0) { Assets.play("a60b_theme", 0.9f); f.speed = 0.6f; f.speedT = 120 }
            if (t % 10 == 0) {
                f.group = b.nextGroup()
                b.ring(f.x, f.cy, 150f, 0xffff2a2a.toInt(), 12)
                b.hit(f, 0f, 0f, 300f, 280f, 2f, 15f, 10f, 60f, radial = true)
            }
        },
        s = Move("prime dash", 48, drift = false, desc = "dashes, then blows up at the end.") { f, b, t ->
            f.noGrav = t < 28
            if (t in 4..24) { f.vx = f.facing * 20f; f.vy = 0f; f.trail = 2 }
            if (t == 26) {
                f.vx = 0f
                f.group = b.nextGroup()
                Assets.play("a60b_near", 0.9f, 1.3f)
                b.ring(f.x, f.cy, 150f, 0xffff2a2a.toInt())
                b.burst(f.x, f.cy, 0xffff2a2a.toInt(), 16, 14f)
                b.shake = 12f
                b.hit(f, 0f, 0f, 280f, 260f, 13f, 50f, 85f, 45f, radial = true)
            }
        },
        u = Move("ascend", 42, helpless = true, drift = false, desc = "rockets up and explodes at the top.") { f, b, t ->
            f.noGrav = t < 22
            if (t in 2..18) { f.vy = -22f; f.vx = f.pad.x * 5f; f.trail = 2 }
            if (t == 19) {
                f.vy = -4f
                f.group = b.nextGroup()
                Assets.play("a60b_near", 0.7f, 1.6f)
                b.ring(f.x, f.cy, 120f, 0xffff2a2a.toInt())
                b.hit(f, 0f, 0f, 240f, 240f, 9f, 45f, 70f, 70f, radial = true)
            }
        },
        d = Move("counter", 42, cooldown = 60, desc = "a stance. get hit during it and you strike back from behind.") { f, b, t ->
            if (t == 0) { f.counter = 30; Assets.play("a60b_far", 0.6f, 1.6f) }
            if (f.counter > 0 && t % 4 == 0) b.ring(f.x, f.cy, f.h * 0.7f, 0xffff2a2a.toInt(), 5)
        },
        x = Move("overload", 120, desc = "warning circles under everyone. then they explode, three times.") { f, b, t ->
            if (t == 0) { Assets.play("a60b_theme", 1f); b.flash(0x66a3161c, 20) }
            if (t == 0 || t == 40 || t == 75) {
                for (o in foes(f, b)) {
                    val ox = o.x; val oy = o.cy
                    b.ring(ox, oy, 150f, 0xffff2a2a.toInt(), 30)
                    b.later(30) {
                        Assets.play("a60b_near", 0.8f)
                        b.burst(ox, oy, 0xffff2a2a.toInt(), 20, 16f)
                        b.ring(ox, oy, 180f, 0xffffffff.toInt())
                        b.shake = 16f
                        b.hitAt(f, ox, oy, 280f, 280f, 14f, 55f, 85f, 60f, b.nextGroup(), f.facing, radial = true)
                    }
                }
            }
        },
    )

    // ---------- a-90 ----------

    fun a90() = Kit(
        n = Move("stop", 36, cooldown = 120, desc = "throws its stop sign. whoever it hits must not move. at all.") { f, b, t ->
            if (t == 10) {
                Assets.play("a90_spawn", 0.7f, 1.4f)
                val p = shot(f, b, f.w * 0.5f, -10f, 15f, 0f, 30f, 60, 3f, 0f, 0f, 0f, "a90_stop")
                p.size = 70f
                p.onHit = { v ->
                    v.curse = 90; v.curseGrace = 16; v.curseBy = f
                    b.text(v.x, v.y - v.h - 50f, "STOP", 0xffff3030.toInt())
                    Assets.play("a90_knock", 0.8f)
                }
            }
        },
        s = Move("knock", 40, cooldown = 90, desc = "knocks, then appears in front of the closest player and hits.") { f, b, t ->
            if (t == 0) { Assets.play("a90_knock", 1f); f.mTarget = f.nearestFoe(b) }
            if (t in 1..13) f.invis = 3
            if (t == 14) {
                val tg = f.mTarget
                if (tg != null && !tg.dead) {
                    val side = if (f.x < tg.x) -1 else 1
                    warp(f, b, tg.x + side * (tg.w * 0.5f + 50f), tg.y)
                    f.faceToward(tg)
                }
            }
            if (t in 16..19) b.hit(f, f.w * 0.4f, 0f, f.w * 1.2f, f.h, 10f, 40f, 80f, 40f)
        },
        u = Move("glitch up", 30, helpless = true, drift = false, desc = "glitches upward.") { f, b, t ->
            f.noGrav = t < 14
            if (t < 6) { f.vx *= 0.6f; f.vy = 0f }
            if (t == 6) {
                Assets.play("a90_spawn", 0.6f, 1.6f)
                b.glitchT = 6
                warp(f, b, f.x + f.pad.x * 160f, f.y - 290f)
                f.group = b.nextGroup()
            }
            if (t in 7..10) b.hit(f, 0f, 0f, f.w * 1.5f, f.h * 1.2f, 7f, 40f, 60f, 75f, radial = true)
        },
        d = Move("despawn", 46, cooldown = 100, desc = "disappears, then comes back somewhere else hitting.") { f, b, t ->
            if (t == 0) Assets.play("a90_despawn", 1f)
            if (t < 30) { f.intangible = true; f.invis = 3; f.noGrav = true; f.vx = 0f; f.vy = 0f }
            if (t == 30) {
                f.intangible = false
                val dx = if (abs(f.pad.x) > 0.3f) f.pad.x * 300f else f.facing * 250f
                warp(f, b, f.x + dx, f.y)
                b.glitchT = 6
                f.group = b.nextGroup()
            }
            if (t in 31..34) b.hit(f, 0f, 0f, f.w * 1.8f, f.h * 1.2f, 9f, 40f, 70f, 60f, radial = true)
        },
        x = Move("everybody stop", 60, desc = "stop signs on everyone. move and you get jumpscared.") { f, b, t ->
            if (t == 0) {
                Assets.play("a90_spawn", 1f)
                b.flash(0x99a3161c.toInt(), 16)
                for (o in foes(f, b)) {
                    o.curse = 150; o.curseGrace = 24; o.curseBy = f
                    b.ghost("a90_stop", o.x, o.cy, 200f, 30)
                }
            }
        },
    )

    // ---------- a-90b ----------

    fun a90b() = Kit(
        n = Move("halt", 30, cooldown = 260, desc = "halt: don't move. then proceed: don't stand still. or else.") { f, b, t ->
            if (t == 6) {
                Assets.play("a90b_halt", 1f)
                for (o in foes(f, b)) if (abs(o.x - f.x) < 1000f) { o.halt = 70; o.haltBy = f; o.proceed = 0 }
            }
        },
        s = Move("attack", 46, drift = false, desc = "a big lunging slash.") { f, b, t ->
            if (t == 0) Assets.play("a90b_attack", 1f)
            if (t in 8..20) {
                f.vx = f.facing * 16f
                if (t == 8) b.ghost("a90b_attack", f.x + f.facing * 80f, f.cy, f.h * 1.3f, 16)
                b.hit(f, f.w * 0.6f, 0f, f.w * 1.5f, f.h * 1.2f, 13f, 45f, 90f, 38f)
            } else f.vx *= 0.8f
        },
        u = Move("spawn rise", 38, helpless = true, drift = false, desc = "rises up out of nowhere.") { f, b, t ->
            if (t == 4) Assets.play("a90b_spawn", 1f)
            if (t in 4..16) {
                f.noGrav = true
                f.vy = -23f
                f.vx = f.pad.x * 5f
                b.hit(f, 0f, -10f, f.w * 1.2f, f.h, 8f, 40f, 65f, 85f)
            }
        },
        d = Move("proceed", 18, cooldown = 360, desc = "gives itself the green light. faster and stronger for a while.") { f, b, t ->
            if (t == 4) {
                Assets.play("a90b_proceed", 1f)
                f.speed = 1.45f; f.speedT = 300
                f.power = 1.15f; f.powerT = 300
                b.ghost("a90b_proceed", f.x, f.y - f.h - 50f, 90f, 40)
            }
        },
        x = Move("red light green light", 30, desc = "halt and proceed on everyone, for much longer.") { f, b, t ->
            if (t == 0) {
                Assets.play("a90b_halt", 1f)
                b.flash(0x88a3161c.toInt(), 14)
                for (o in foes(f, b)) { o.halt = 150; o.haltBy = f; o.proceed = 0 }
            }
        },
    )

    // ---------- a-120 ----------

    fun a120() = Kit(
        n = Move("near", 36, cooldown = 100, desc = "a slow face that follows people around, then pops.") { f, b, t ->
            if (t == 10) {
                Assets.play("a120_near", 0.8f)
                val p = shot(f, b, f.w * 0.5f, -10f, 6f, 0f, 30f, 240, 11f, 40f, 70f, 45f, "a120_face")
                p.size = 90f
                p.alpha = 190
                p.homing = 0.06f
                p.radial = true
            }
        },
        s = Move("phase walk", 46, drift = false, desc = "walks through everything, then hits on the way out.") { f, b, t ->
            if (t < 30) { f.intangible = true; f.invis = 3; f.vx = f.facing * 13f }
            if (t == 30) { f.intangible = false; f.vx *= 0.3f }
            if (t in 30..33) b.hit(f, f.w * 0.5f, 0f, f.w * 1.2f, f.h, 10f, 40f, 75f, 40f)
        },
        u = Move("drift", 72, helpless = true, drift = false, desc = "floats up slowly. you can steer.") { f, b, t ->
            f.noGrav = true
            f.vy = -9f
            f.vx = f.pad.x * 7f
            if (t % 12 == 0) { f.group = b.nextGroup(); b.hit(f, 0f, 0f, f.w * 1.3f, f.h * 1.1f, 3f, 25f, 20f, 80f, radial = true) }
        },
        d = Move("hide", 20, cooldown = 400, desc = "goes almost invisible. first hit out of hiding does 1.5x.") { f, b, t ->
            if (t == 4) {
                Assets.play("locker", 0.8f)
                f.invis = 260
                f.bonus = 1.5f
            }
        },
        x = Move("lights out", 200, desc = "the room goes dark. it keeps hurting until the lights come back.") { f, b, t ->
            if (t == 0) { Assets.play("a120_far", 1f); b.darkT = 200; b.darkBy = f }
            if (t % 25 == 12) for (o in foes(f, b)) { b.statusHit(f, o, 3f); Assets.play("shakelight", 0.4f, 0.6f) }
            if (t == 190) for (o in foes(f, b)) {
                b.ghost("a120_face", o.x, o.cy, 200f, 20)
                b.apply(f, o, 10f, 50f, 80f, 60f, if (o.x > f.x) 1 else -1, f.x, f.cy, true, null)
            }
        },
    )

    // ---------- a-200 ----------

    fun a200() = Kit(
        n = Move("scribble", 30, cooldown = 45, desc = "throws a bouncy scribble.") { f, b, t ->
            if (t == 8) {
                Assets.play("a200_spawn", 0.4f, 1.6f)
                val p = shot(f, b, f.w * 0.5f, -20f, 10f, -7f, 26f, 200, 7f, 30f, 55f, 50f, "a200_face")
                p.size = 64f
                p.grav = 0.6f
                p.bounce = 3
                p.spinV = 12f * f.facing
            }
        },
        s = Move("voice", 52, cooldown = 90, desc = "talks at people. it hurts. a lot of small hits then a push.") { f, b, t ->
            if (t == 4) Assets.play("a200_voice", 0.9f)
            if (t in 8..40 && t % 8 == 0) {
                f.group = b.nextGroup()
                b.ring(f.x + f.facing * (80f + (t - 8) * 6f), f.cy - 10f, 50f + (t - 8) * 2f, 0xffffffff.toInt(), 10)
                b.hit(f, f.w * 0.5f + 140f, -10f, 300f, 170f, 3f, 15f, 15f, 30f)
            }
            if (t == 44) { f.group = b.nextGroup(); b.hit(f, f.w * 0.5f + 140f, -10f, 300f, 170f, 6f, 50f, 70f, 35f) }
        },
        u = Move("spring", 34, helpless = true, desc = "boing.") { f, b, t ->
            if (t == 2) { f.vy = -30f; f.grounded = false; f.squash = -0.5f; Assets.play("a200_spawn", 0.3f, 2f) }
            if (t in 2..14) b.hit(f, 0f, 0f, f.w * 1.3f, f.h * 1.2f, 9f, 40f, 70f, 80f, radial = true)
        },
        d = Move("doodle trap", 26, cooldown = 200, desc = "draws a scribble on the floor. step on it and you're stuck.") { f, b, t ->
            if (t == 8) {
                val p = Proj(f, f.x + f.facing * 70f, f.y - 22f, 0f, 0f, 30f, 420, 4f, 0f, 0f, 0f)
                p.sprite = "a200_face"
                p.size = 60f
                p.alpha = 160
                p.color = f.def.color
                p.onHit = { v -> v.stun = 70; v.flash = 30; b.text(v.x, v.y - v.h - 20f, "stuck", 0xffffffff.toInt()) }
                b.shoot(p)
            }
        },
        x = Move("happy room", 150, desc = "you're in its room now. scribbles fall from everywhere.") { f, b, t ->
            if (t == 0) { Assets.play("a200_far", 1f); b.roomFlash = "a200_room"; b.roomFlashT = 40 }
            if (t > 20 && t % 7 == 0) {
                val list = foes(f, b)
                val tx = if (list.isNotEmpty() && Random.nextBoolean()) list[Random.nextInt(list.size)].x else b.stage.left + Random.nextFloat() * (b.stage.right - b.stage.left)
                val p = Proj(f, tx + (Random.nextFloat() - 0.5f) * 120f, -760f, 0f, 12f, 26f, 160, 8f, 35f, 60f, 60f)
                p.sprite = "a200_face"; p.size = 64f; p.grav = 0.3f; p.bounce = 1; p.spinV = 9f; p.color = f.def.color
                b.shoot(p)
            }
        },
    )

    // ---------- a-150 ----------

    fun a150() = Kit(
        n = Move("star shot", 34, cooldown = 40, desc = "throws two spinning stars.") { f, b, t ->
            if (t == 10) for (vy in floatArrayOf(-2.5f, 2.5f)) {
                val p = shot(f, b, f.w * 0.5f, -20f, 15f, vy, 18f, 70, 5f, 25f, 45f, 40f)
                p.shape = 1; p.spinV = 18f; p.color = 0xffd8b25a.toInt()
            }
        },
        s = Move("smile roll", 52, drift = false, desc = "rolls into people over and over.") { f, b, t ->
            if (t in 4..40) {
                f.vx = f.facing * 14f
                f.spin += 24f * f.facing
                if (t % 12 == 4) f.group = b.nextGroup()
                b.hit(f, 0f, 0f, f.w * 1.1f, f.h, 5f, 25f, 45f, 45f)
            }
            if (t == 41) { f.group = b.nextGroup(); b.hit(f, f.w * 0.3f, 0f, f.w * 1.3f, f.h, 7f, 45f, 75f, 40f) }
            if (t > 41) f.vx *= 0.8f
        },
        u = Move("star spin", 40, helpless = true, drift = false, desc = "spins up in a ring of stars.") { f, b, t ->
            if (t in 2..22) {
                f.noGrav = true
                f.vy = -18f
                f.vx = f.pad.x * 5f
                f.spin += 30f
                if (t % 6 == 2) { f.group = b.nextGroup(); b.burst(f.x, f.cy, 0xffd8b25a.toInt(), 3) }
                b.hit(f, 0f, 0f, f.w * 1.4f, f.h * 1.2f, if (t == 22) 6f else 3f, if (t == 22) 45f else 20f, if (t == 22) 75f else 15f, 85f, radial = t != 22)
            }
        },
        d = Move("falling star", 30, cooldown = 150, desc = "calls a star down on whoever's closest.") { f, b, t ->
            if (t == 6) {
                val tg = f.nearestFoe(b) ?: return@Move
                val tx = tg.x
                b.ring(tx, tg.y - 10f, 70f, 0xffd8b25a.toInt(), 40)
                b.later(36) {
                    Assets.play("gold", 0.6f, 0.7f)
                    val p = Proj(f, tx, b.stage.blastT + 300f, 0f, 24f, 40f, 90, 14f, 50f, 80f, 60f)
                    p.shape = 1; p.size = 110f; p.spinV = 10f; p.color = 0xffd8b25a.toInt(); p.radial = true
                    b.shoot(p)
                }
            }
        },
        x = Move("starfall", 150, desc = "it rains stars.") { f, b, t ->
            if (t == 0) { Assets.play("gold", 1f, 0.6f); b.flash(0x44d8b25a, 20) }
            if (t > 10 && t % 6 == 0) {
                val x = b.stage.left - 100f + Random.nextFloat() * (b.stage.right - b.stage.left + 200f)
                val p = Proj(f, x, b.stage.blastT + 300f, (Random.nextFloat() - 0.5f) * 4f, 20f, 26f, 100, 9f, 40f, 60f, 60f)
                p.shape = 1; p.size = 70f; p.spinV = 14f; p.color = 0xffd8b25a.toInt(); p.radial = true
                b.shoot(p)
            }
        },
    )

    // ---------- a-220 ----------

    fun a220() = Kit(
        n = Move("static beam", 52, cooldown = 80, desc = "charges up, then a long glitchy beam.") { f, b, t ->
            if (t == 4) Assets.play("glitch", 0.7f, 1.2f)
            if (t in 4..16 && t % 3 == 0) b.burst(f.x + f.facing * 40f, f.cy - 10f, f.def.color, 2, 4f)
            if (t == 18) b.beam(f.x + f.facing * f.w * 0.4f, f.cy - 10f, 720f, f.facing, 40f, f.def.color, 10)
            if (t in 18..22) b.hit(f, f.w * 0.4f + 360f, -10f, 720f, 46f, 11f, 40f, 70f, 20f)
        },
        s = Move("march", 64, drift = false, desc = "three heavy marching steps. you can't flinch it.") { f, b, t ->
            if (t == 0) f.armor = 60
            for (k in 0..2) {
                val s = 10 + k * 16
                if (t == s) { f.group = b.nextGroup(); Assets.play("figure_step", 0.5f, 1.5f); b.dust(f.x, f.y, 4) }
                if (t in s..s + 5) { f.vx = f.facing * 10f; b.hit(f, f.w * 0.5f, 0f, f.w, f.h, 6f, 30f, 55f, 40f) }
            }
            if (t % 16 > 6) f.vx *= 0.6f
        },
        u = Move("glitch jump", 40, helpless = true, drift = false, desc = "three quick glitch-hops upward.") { f, b, t ->
            f.noGrav = t < 32
            f.vx *= 0.8f; f.vy *= 0.5f
            if (t == 4 || t == 14 || t == 24) {
                Assets.play("glitch", 0.4f, 1.9f)
                warp(f, b, f.x + f.pad.x * 70f, f.y - 115f)
                f.group = b.nextGroup()
            }
            if (t in 5..7 || t in 15..17 || t in 25..27) b.hit(f, 0f, 0f, f.w * 1.4f, f.h * 1.1f, 4f, 35f, 40f, 75f, radial = true)
        },
        d = Move("corrupt", 32, cooldown = 300, desc = "everyone nearby gets their left and right swapped.") { f, b, t ->
            if (t == 8) {
                Assets.play("glitch", 1f)
                b.glitchT = 20
                b.ring(f.x, f.cy, 260f, f.def.color)
                for (o in foes(f, b)) if (abs(o.x - f.x) < 520f && abs(o.y - f.y) < 400f) {
                    o.reverse = 200
                    b.text(o.x, o.y - o.h - 20f, "corrupted", f.def.color)
                }
            }
        },
        x = Move("system crash", 120, desc = "the whole game glitches. everyone's controls flip and pillars fall.") { f, b, t ->
            if (t == 0) {
                Assets.play("glitch", 1f, 0.6f)
                b.glitchT = 120
                for (o in foes(f, b)) { o.reverse = 300; o.stun = 30 }
            }
            if (t == 40 || t == 80) for (o in foes(f, b)) {
                b.fx.add(Fx(Fx.BEAM, o.x - 50f, b.stage.blastT, 0f, 0f, 14, f.def.color, 100f).also { it.a1 = 1f; it.a0 = b.stage.blastB - b.stage.blastT })
                b.hitAt(f, o.x, o.cy, 110f, 900f, 12f, 50f, 75f, 80f, b.nextGroup(), f.facing)
            }
        },
    )

    // ---------- doorsman ----------

    fun doorsman() = Kit(
        n = Move("crucifix", 56, cooldown = 260, desc = "holds up the crucifix. anyone in front gets locked in place, then thrown.") { f, b, t ->
            if (t == 6) {
                b.ghost("doors_crucifix", f.x + f.facing * 70f, f.cy - 20f, 110f, 40)
                b.ring(f.x + f.facing * 160f, f.cy, 180f, 0xff9fd8ff.toInt(), 24)
                b.flash(0x339fd8ff, 10)
                Assets.play("gold", 0.8f, 0.7f)
                val caught = foes(f, b).filter { o ->
                    val dx = (o.x - f.x) * f.facing
                    dx in -20f..340f && abs(o.cy - f.cy) < 200f && o.invuln == 0 && !o.intangible
                }
                for (o in caught) {
                    o.endMove(); o.hitstun = 0; o.vx = 0f; o.vy = 0f
                    o.stun = 50; o.flash = 50
                    b.statusHit(f, o, 6f)
                    b.text(o.x, o.y - o.h - 30f, "chained", 0xff9fd8ff.toInt())
                }
                b.later(44) {
                    for (o in caught) if (!o.dead) {
                        b.burst(o.x, o.cy, 0xff9fd8ff.toInt(), 14, 12f)
                        b.apply(f, o, 10f, 55f, 85f, 75f, if (o.x > f.x) 1 else -1, f.x, f.cy, false, null)
                    }
                }
            }
        },
        s = Move("flashlight", 40, cooldown = 50, desc = "a long beam of light. hurts and blinds whoever's in it.") { f, b, t ->
            if (t == 4) b.ghost("doors_flashlight", f.x + f.facing * 55f, f.cy - 10f, 60f, 20)
            if (t == 8) {
                Assets.play("flashlight_click", 1f)
                b.beam(f.x + f.facing * f.w * 0.5f, f.cy - 10f, 540f, f.facing, 110f, 0x88fff6c8.toInt(), 14)
                b.hit(f, f.w * 0.5f + 270f, -10f, 540f, 120f, 7f, 30f, 55f, 30f) { v -> v.stun = 30 }
            }
        },
        u = Move("skeleton key", 46, helpless = true, drift = false, desc = "unlocks a door way above and steps through it, slamming whoever's there.") { f, b, t ->
            f.noGrav = t < 22
            if (t == 0) { b.ghost("doors_skeleton_key", f.x, f.cy - 20f, 80f, 20); Assets.play("locker", 0.5f, 1.6f) }
            if (t < 10) { f.vx *= 0.6f; f.vy = 0f }
            if (t == 10) {
                Assets.play("doors_door_open", 1f)
                warp(f, b, f.x + f.pad.x * 200f, f.y - 360f)
                f.group = b.nextGroup()
            }
            if (t in 11..15) b.hit(f, 0f, 0f, f.w * 2f, f.h * 1.2f, 11f, 50f, 70f, 80f, radial = true)
        },
        d = Move("lighter", 36, cooldown = 160, desc = "drops fire on the floor in front of you. it keeps burning.") { f, b, t ->
            if (t == 6) b.ghost("doors_lighter", f.x + f.facing * 50f, f.y - 40f, 60f, 20)
            if (t == 10 || t == 16) {
                Assets.play("shakelight", 0.6f, 0.6f)
                val dx = if (t == 10) 90f else 190f
                val p = Proj(f, f.x + f.facing * dx, f.y - 30f, 0f, 0f, 36f, 160, 2.5f, 12f, 12f, 80f)
                p.color = 0xffff8a1f.toInt()
                p.pierce = true
                p.rehit = 14
                p.reflectable = false
                p.onTick = { pp, bb -> if (pp.t % 3 == 0) bb.fx.add(Fx(Fx.DOT, pp.x + (Random.nextFloat() - 0.5f) * 50f, pp.y + 20f, 0f, -3f - Random.nextFloat() * 3f, 22, if (Random.nextBoolean()) 0xffff8a1f.toInt() else 0xffffd23f.toInt(), 9f)) }
                b.shoot(p)
            }
        },
        x = Move("guiding light", 110, desc = "heals you, then blue light comes down on everyone else.") { f, b, t ->
            if (t == 0) {
                Assets.play("vitamins", 1f, 0.8f)
                f.dmg = max(0f, f.dmg - 35f)
                f.invuln = 60
                b.flash(0x553a8bff, 20)
                b.text(f.x, f.y - f.h - 30f, "guiding light", 0xff7fb8ff.toInt())
            }
            if (t == 20) for (o in foes(f, b)) b.ring(o.x, o.cy, 120f, 0xff7fb8ff.toInt(), 30)
            if (t == 50) for (o in foes(f, b)) {
                Assets.play("gold", 1f, 1.2f)
                b.fx.add(Fx(Fx.BEAM, o.x - 60f, b.stage.blastT, 0f, 0f, 18, 0xcc7fb8ff.toInt(), 120f).also { it.a1 = 1f; it.a0 = o.y - b.stage.blastT })
                b.apply(f, o, 20f, 70f, 95f, 80f, if (o.x > f.x) 1 else -1, o.x, o.y - 300f, false, null)
            }
        },
    )
}
