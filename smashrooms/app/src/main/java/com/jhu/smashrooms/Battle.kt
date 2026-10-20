package com.jhu.smashrooms

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class Hitbox(
    val owner: Fighter,
    var x: Float, var y: Float, val w: Float, val h: Float,
    val dmg: Float, val base: Float, val growth: Float, val angle: Float,
    val group: Int, val dir: Int,
    val radial: Boolean,
    val onHit: ((Fighter) -> Unit)?,
)

class Proj(
    val owner: Fighter,
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    val r: Float, var life: Int,
    val dmg: Float, val base: Float = 30f, val growth: Float = 60f, val angle: Float = 40f,
) {
    var sprite: String? = null
    var size = r * 2.2f
    var rot = 0f
    var spinV = 0f
    var grav = 0f
    var bounce = 0
    var homing = 0f
    var pierce = false
    var radial = false
    var color = 0xffece6dc.toInt()
    var alpha = 255
    var rehit = 0
    var group = 0
    var t = 0
    var dead = false
    var reflectable = true
    /** 0 glow ball, 1 star, 2 sound wave, 3 ink tracer */
    var shape = 0
    var onHit: ((Fighter) -> Unit)? = null
    var onTick: ((Proj, Battle) -> Unit)? = null
    var onDie: ((Proj, Battle) -> Unit)? = null
    val hitSet = HashSet<Fighter>()
}

/** short lived visuals */
class Fx(val kind: Int, var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Int, val color: Int, val size: Float) {
    val max = life
    var text: String? = null
    var a0 = 0f
    var a1 = 0f
    var owner: Fighter? = null
    var sprite: String? = null

    companion object {
        const val DOT = 0
        const val RING = 1
        const val TEXT = 2
        const val SLASH = 3
        const val LINE = 4
        const val SPRITE = 5
        const val BEAM = 6
        const val HAND = 7
    }
}

class Item(val kind: Int, var x: Float, var y: Float) {
    var vy = 0f
    var life = 900
    var landed = false

    companion object {
        const val VITAMINS = 0
        const val GOLD = 1
        const val FLASHLIGHT = 2
        const val NAMES = 3
    }
}

/** the whole fight: fighters, hits, projectiles, effects, rules */
class Battle(val settings: Settings) {
    val stage = Stage(Stage.ALL[settings.stage])
    val fighters = ArrayList<Fighter>()
    val pads = ArrayList<Pad>()
    val ais = ArrayList<Ai?>()
    val hitboxes = ArrayList<Hitbox>()
    val projs = ArrayList<Proj>()
    val fx = ArrayList<Fx>()
    val items = ArrayList<Item>()
    private val later = ArrayList<Pair<Int, () -> Unit>>()
    private val hitLog = HashSet<Long>()
    private var groupSeq = 1

    var t = 0
    var countdown = 200
    var over = 0
    var winner: Fighter? = null
    var shake = 0f
    var flashColor = 0
    var flashT = 0
    var flashMax = 1
    var darkT = 0
    var darkBy: Fighter? = null
    var glitchT = 0
    var slowmo = 0
    var jumpscare: String? = null
    var jumpscareT = 0
    var roomFlash: String? = null
    var roomFlashT = 0
    val koOrder = ArrayList<Fighter>()
    private var itemTimer = 600

    init {
        val roster = Roster.ALL
        val n = 1 + settings.cpus.size
        val p1 = Fighter(roster[settings.p1], 0, true, settings.stocks)
        fighters.add(p1)
        ais.add(null)
        settings.cpus.forEachIndexed { i, c ->
            val id = if (c < 0) Random.nextInt(roster.size) else c
            fighters.add(Fighter(roster[id], i + 1, false, if (settings.training) 99 else settings.stocks))
            ais.add(Ai(settings.level, settings.training))
        }
        fighters.forEachIndexed { i, f ->
            pads.add(Pad())
            val (x, y) = stage.spawn(i, n)
            f.reset(x, y)
            f.grounded = true
            f.facing = if (x > 0) -1 else 1
        }
    }

    val p1 get() = fighters[0]

    fun nextGroup() = groupSeq++

    fun later(frames: Int, fn: () -> Unit) {
        later.add((t + frames) to fn)
    }

    fun update(humanPad: Pad) {
        t++
        if (countdown > 0) {
            countdown--
            if (countdown == 180 || countdown == 120 || countdown == 60) Assets.play("flashlight_click", 0.9f)
            if (countdown == 0) Assets.play("door_open", 1f)
        }
        if (slowmo > 0) {
            slowmo--
            if (slowmo % 3 != 0) return
        }
        stage.tick()
        if (flashT > 0) flashT--
        if (darkT > 0) darkT--
        if (glitchT > 0) glitchT--
        if (jumpscareT > 0) jumpscareT--
        if (roomFlashT > 0) roomFlashT--
        shake *= 0.85f

        val fighting = countdown == 0 && over == 0
        for (i in fighters.indices) {
            val p = pads[i]
            p.clear()
            if (fighting) {
                if (i == 0) p.copyFrom(humanPad) else ais[i]?.think(fighters[i], this, p)
            }
            p.latch(i == 0)
        }

        if (fighting || over > 0) {
            for (i in fighters.indices) {
                val f = fighters[i]
                if (f.dead) {
                    if (f.stocks > 0 && --f.respawn <= 0) respawn(f)
                    continue
                }
                f.update(pads[i], this)
            }
        }

        hitboxes.forEach { resolve(it) }
        hitboxes.clear()
        tickProjs()
        tickItems()
        val now = later.filter { it.first <= t }
        later.removeAll(now.toSet())
        now.forEach { it.second() }
        tickFx()
        checkBlast()

        if (over > 0) {
            over++
        } else if (countdown == 0) {
            val left = fighters.filter { it.stocks > 0 }
            if (left.size <= 1 && !settings.training) {
                winner = left.firstOrNull()
                over = 1
                slowmo = 60
                Assets.play("figure_kill", 1f)
            }
        }
    }

    // ---------- hits ----------

    /** dx/dy are from the fighter's middle, dx flips with facing */
    fun hit(
        f: Fighter, dx: Float, dy: Float, w: Float, h: Float,
        dmg: Float, base: Float, growth: Float, angle: Float,
        radial: Boolean = false, back: Boolean = false, onHit: ((Fighter) -> Unit)? = null,
    ) {
        val dir = if (back) -f.facing else f.facing
        hitboxes.add(Hitbox(f, f.x + dx * f.facing, f.cy + dy, w, h, dmg, base, growth, angle, f.group, dir, radial, onHit))
    }

    /** hit at a world position (traps, shockwaves, stuff not attached to the body) */
    fun hitAt(
        f: Fighter, x: Float, y: Float, w: Float, h: Float,
        dmg: Float, base: Float, growth: Float, angle: Float, group: Int, dir: Int,
        radial: Boolean = false, onHit: ((Fighter) -> Unit)? = null,
    ) {
        hitboxes.add(Hitbox(f, x, y, w, h, dmg, base, growth, angle, group, dir, radial, onHit))
    }

    private fun overlaps(v: Fighter, x: Float, y: Float, w: Float, h: Float): Boolean {
        val vw = v.w * 0.7f
        return abs(v.x - x) < (vw + w) / 2f && abs(v.cy - y) < (v.h + h) / 2f
    }

    private fun resolve(hb: Hitbox) {
        for (v in fighters) {
            if (v === hb.owner || v.dead) continue
            if (!overlaps(v, hb.x, hb.y, hb.w, hb.h)) continue
            val key = (hb.group.toLong() shl 4) or v.slot.toLong()
            if (key in hitLog) continue
            if (apply(hb.owner, v, hb.dmg, hb.base, hb.growth, hb.angle, hb.dir, hb.x, hb.y, hb.radial, hb.onHit)) hitLog.add(key)
        }
    }

    /** true if the hit connected (even on a shield) */
    fun apply(
        a: Fighter, v: Fighter, rawDmg: Float, base: Float, growth: Float, angle: Float, dir: Int,
        sx: Float, sy: Float, radial: Boolean, onHit: ((Fighter) -> Unit)?,
    ): Boolean {
        if (v.invuln > 0 || v.intangible || v.dead) return false
        if (v.counter > 0) {
            v.counter = 0
            Moves.counterHit(v, a, this)
            return true
        }
        if (v.shielding) {
            v.shield -= rawDmg * 1.5f
            v.vx = (if (v.x > sx) 1 else -1) * min(12f, rawDmg * 0.5f)
            v.hitlag = 4; a.hitlag = 4
            Assets.play("locker", 0.5f, 1.4f)
            burst(v.x, v.cy, 0xff9fd8ff.toInt(), 5)
            return true
        }
        var dmg = rawDmg * a.power * a.bonus / v.guard
        if (a.bonus > 1f) { a.bonus = 1f; a.invis = 0 }
        if (dmg <= 0f) return false
        v.dmg = min(999f, v.dmg + dmg)
        a.fear = min(100f, a.fear + dmg * 0.9f)
        v.fear = min(100f, v.fear + dmg * 0.6f)
        v.lastHitBy = a
        v.flash = 10

        val p = v.dmg
        var kb = ((p / 10f + p * dmg / 20f) * (200f / (v.def.weight + 100f)) * 1.4f + 18f) * (growth / 100f) + base
        if (growth == 0f) kb = base

        if (v.armor > 0 && kb < 150f) {
            v.hitlag = 4; a.hitlag = 4
            burst(v.x, v.cy, 0xffffd27a.toInt(), 4)
            Assets.play("locker", 0.4f, 0.8f)
            onHit?.invoke(v)
            return true
        }

        val launch = kb * 0.19f
        if (radial) {
            val ang = atan2(v.cy - sy, v.x - sx)
            v.vx = cos(ang) * launch
            v.vy = min(sin(ang) * launch, -launch * 0.35f)
        } else {
            val r = Math.toRadians(angle.toDouble())
            v.vx = cos(r).toFloat() * launch * dir
            v.vy = -sin(r).toFloat() * launch
        }
        if (v.grounded && v.vy > 0f && v.vy < 8f) v.vy = -v.vy * 0.3f
        v.hitstun = (kb * 0.42f).toInt()
        v.tumble = kb > 80f
        if (v.vy < 0f) { v.grounded = false; v.ground = null }
        v.endMove()
        v.shielding = false
        v.landLag = 0
        v.dodge = 0
        v.stun = 0
        val lag = min(20, (dmg * 0.45f + 4f).toInt())
        v.hitlag = lag
        a.hitlag = lag
        shake = max(shake, min(26f, kb * 0.12f))
        burst(v.x, v.cy, a.def.color, (4 + dmg / 2).toInt())
        ring(v.x, v.cy, 30f + dmg * 3f, a.def.color)
        when {
            kb > 140f -> { Assets.play("figure_step", 1f, 0.8f); flash(0x55ffffff, 4) }
            dmg >= 9f -> Assets.play("locker", 0.8f, 0.9f)
            else -> Assets.play("shakelight", 0.7f, 1.2f)
        }
        onHit?.invoke(v)
        return true
    }

    /** chip damage from status effects (halt / proceed, gaze...) */
    fun statusHit(by: Fighter?, v: Fighter, dmg: Float) {
        if (v.invuln > 0 || v.dead) return
        v.dmg = min(999f, v.dmg + dmg)
        v.flash = 6
        by?.let { it.fear = min(100f, it.fear + dmg * 0.6f); v.lastHitBy = it }
        burst(v.x, v.cy, 0xffff3030.toInt(), 3)
    }

    // ---------- projectiles ----------

    fun shoot(p: Proj): Proj {
        p.group = nextGroup()
        projs.add(p)
        return p
    }

    private fun tickProjs() {
        for (p in projs.toList()) {
            p.t++
            if (p.homing > 0f) {
                p.owner.nearestFoe(this)?.let { tg ->
                    val a = atan2(tg.cy - p.y, tg.x - p.x)
                    val sp = hypot(p.vx, p.vy)
                    p.vx += (cos(a) * sp - p.vx) * p.homing
                    p.vy += (sin(a) * sp - p.vy) * p.homing
                }
            }
            p.vy += p.grav
            p.x += p.vx
            p.y += p.vy
            p.rot += p.spinV
            if (p.grav > 0f && p.y >= stage.main.y - p.r && abs(p.x) < stage.right && p.vy > 0f && p.y < stage.main.y + 40f) {
                if (p.bounce > 0) {
                    p.bounce--
                    p.y = stage.main.y - p.r
                    p.vy = -abs(p.vy) * 0.75f
                    dust(p.x, stage.main.y, 3)
                } else p.life = 0
            }
            p.onTick?.invoke(p, this)
            if (p.rehit > 0 && p.t % p.rehit == 0) p.hitSet.clear()
            for (v in fighters) {
                if (v === p.owner || v.dead || v in p.hitSet) continue
                if (abs(v.x - p.x) < v.w * 0.35f + p.r && abs(v.cy - p.y) < v.h / 2f + p.r) {
                    p.hitSet.add(v)
                    val dir = if (p.vx >= 0f) 1 else -1
                    if (apply(p.owner, v, p.dmg, p.base, p.growth, p.angle, dir, p.x, p.y, p.radial, p.onHit)) {
                        if (!p.pierce) p.life = 0
                    }
                }
            }
            if (--p.life <= 0 || p.x < stage.blastL || p.x > stage.blastR || p.y > stage.blastB || p.y < stage.blastT) {
                p.dead = true
                p.onDie?.invoke(p, this)
            }
        }
        projs.removeAll { it.dead }
    }

    // ---------- items ----------

    private fun tickItems() {
        if (!settings.items || countdown > 0 || over > 0) return
        if (--itemTimer <= 0) {
            itemTimer = 600 + Random.nextInt(600)
            if (items.size < 2) {
                val x = stage.left + 80f + Random.nextFloat() * (stage.right - stage.left - 160f)
                items.add(Item(Random.nextInt(Item.NAMES), x, -700f))
            }
        }
        for (itm in items.toList()) {
            if (!itm.landed) {
                itm.vy = min(itm.vy + 0.5f, 10f)
                itm.y += itm.vy
                for (pl in stage.plats) if (itm.x in pl.x1..pl.x2 && itm.y >= pl.y && itm.y - itm.vy <= pl.y + 2f) {
                    itm.y = pl.y; itm.landed = true
                }
                if (itm.y > stage.blastB) itm.life = 0
            }
            if (--itm.life <= 0) { items.remove(itm); continue }
            val taker = fighters.firstOrNull { f ->
                !f.dead && f.hitstun == 0 && abs(f.x - itm.x) < f.w * 0.5f + 24f && abs(f.cy - (itm.y - 20f)) < f.h / 2f + 24f
            }
            if (taker != null) {
                items.remove(itm)
                take(taker, itm)
            }
        }
    }

    private fun take(f: Fighter, it: Item) {
        when (it.kind) {
            Item.VITAMINS -> {
                f.dmg = max(0f, f.dmg - 20f)
                f.speed = 1.35f; f.speedT = 360
                Assets.play("vitamins")
                text(f.x, f.y - f.h - 20f, "vitamins", 0xff7dff8a.toInt())
            }
            Item.GOLD -> {
                f.fear = min(100f, f.fear + 50f)
                Assets.play("gold")
                text(f.x, f.y - f.h - 20f, "+50 fear", 0xffd8b25a.toInt())
            }
            else -> {
                Assets.play("flashlight_click")
                flash(0x88fff6c8.toInt(), 10)
                for (o in fighters) if (o !== f && !o.dead && abs(o.x - f.x) < 500f && abs(o.y - f.y) < 400f) {
                    o.stun = max(o.stun, 70); o.endMove(); o.flash = 20
                }
                text(f.x, f.y - f.h - 20f, "flashlight", 0xfffff6c8.toInt())
            }
        }
    }

    // ---------- kos ----------

    private fun checkBlast() {
        for (f in fighters) {
            if (f.dead) continue
            val out = f.x < stage.blastL || f.x > stage.blastR || f.y > stage.blastB || (f.y - f.h < stage.blastT && f.hitstun > 0)
            if (!out) continue
            ko(f)
        }
    }

    private fun ko(f: Fighter) {
        f.dead = true
        f.stocks--
        f.falls++
        f.respawn = 100
        f.lastHitBy?.let { if (it !== f) it.kos++ }
        if (f.stocks <= 0) koOrder.add(f)
        val ex = f.x.coerceIn(stage.blastL + 40f, stage.blastR - 40f)
        val ey = f.y.coerceIn(stage.blastT + 40f, stage.blastB - 40f)
        for (i in 0 until 24) {
            val a = Random.nextFloat() * 6.283f
            val sp = 8f + Random.nextFloat() * 22f
            fx.add(Fx(Fx.LINE, ex, ey, cos(a) * sp, sin(a) * sp, 30, f.def.color, 6f))
        }
        ring(ex, ey, 260f, 0xffffffff.toInt())
        shake = 30f
        flash(0x66ffffff, 6)
        Assets.play("death", 0.8f)
        Assets.play("glitch", 0.5f, 1.3f)
        if (settings.training && f.slot != 0) f.stocks = 99
        // last stock music
        val humansLeft = fighters.filter { it.stocks > 0 }
        if (humansLeft.size == 2 && humansLeft.all { it.stocks == 1 }) Assets.music("a60b_theme", 0.7f)
    }

    private fun respawn(f: Fighter) {
        f.reset(0f + (f.slot - 1.5f) * 60f, -420f)
        f.invuln = 150
        f.vy = 0f
        f.respawn = 0
        f.helpless = false
        Assets.play("door_open", 0.7f)
        ring(f.x, f.cy, 120f, f.def.color)
    }

    // ---------- fx ----------

    fun burst(x: Float, y: Float, color: Int, n: Int, sp: Float = 9f) {
        repeat(n) {
            val a = Random.nextFloat() * 6.283f
            val s = sp * (0.3f + Random.nextFloat())
            fx.add(Fx(Fx.DOT, x, y, cos(a) * s, sin(a) * s, 18 + Random.nextInt(12), color, 4f + Random.nextFloat() * 6f))
        }
    }

    fun dust(x: Float, y: Float, n: Int) {
        repeat(n) {
            fx.add(Fx(Fx.DOT, x, y - 4f, (Random.nextFloat() - 0.5f) * 8f, -Random.nextFloat() * 3f, 20, 0xaa9c948a.toInt(), 8f))
        }
    }

    fun ring(x: Float, y: Float, r: Float, color: Int, life: Int = 16) {
        fx.add(Fx(Fx.RING, x, y, 0f, 0f, life, color, r))
    }

    fun text(x: Float, y: Float, s: String, color: Int) {
        fx.add(Fx(Fx.TEXT, x, y, 0f, -1.2f, 60, color, 34f).also { it.text = s })
    }

    /** a swing arc around the fighter, angles in degrees with 0 = facing direction */
    fun slash(f: Fighter, dx: Float, dy: Float, r: Float, from: Float, to: Float, life: Int = 8) {
        fx.add(Fx(Fx.SLASH, dx, dy, 0f, 0f, life, f.def.color, r).also { it.a0 = from; it.a1 = to; it.owner = f })
    }

    fun beam(x: Float, y: Float, len: Float, dir: Int, thick: Float, color: Int, life: Int) {
        fx.add(Fx(Fx.BEAM, x, y, 0f, 0f, life, color, thick).also { it.a0 = len * dir })
    }

    fun ghost(sprite: String, x: Float, y: Float, size: Float, life: Int, color: Int = 0xffffffff.toInt()) {
        fx.add(Fx(Fx.SPRITE, x, y, 0f, 0f, life, color, size).also { it.sprite = sprite })
    }

    fun flash(color: Int, frames: Int) {
        flashColor = color; flashT = frames; flashMax = frames
    }

    fun superFlash(f: Fighter) {
        flash(0x99000000.toInt(), 30)
        ring(f.x, f.cy, 400f, f.def.color, 30)
        slowmo = 24
        text(f.x, f.y - f.h - 40f, f.def.sup.name, 0xffd8b25a.toInt())
        Assets.play("glitch", 1f, 0.8f)
    }

    fun scare(victim: Fighter, sprite: String, frames: Int = 40) {
        if (!victim.human) return
        jumpscare = sprite
        jumpscareT = frames
    }

    private fun tickFx() {
        for (e in fx) {
            e.life--
            e.x += e.vx
            e.y += e.vy
            if (e.kind == Fx.DOT) { e.vx *= 0.92f; e.vy = e.vy * 0.92f + 0.25f }
            if (e.kind == Fx.LINE) { e.vx *= 0.95f; e.vy *= 0.95f }
        }
        fx.removeAll { it.life <= 0 }
        if (fx.size > 600) fx.subList(0, fx.size - 600).clear()
    }
}
