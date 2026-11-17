package com.patternbox.core

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
    operator fun times(k: Float) = Vec(x * k, y * k)
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
    const val HS = 9f    // hat dot spacing
    const val R = 36f    // fighter collision radius
    const val REST_SWING = -0.9f

    fun body(p: List<Int>) = p.map { Vec((dotX(it) - 1) * BS, (dotY(it) - 1) * BS) }
    fun armour(p: List<Int>) = p.map { Vec((dotX(it) - 1) * AS, (dotY(it) - 1) * AS) }

    /** hat sits on the head, its bottom row touching the top of the eyes */
    fun hat(body: List<Int>, hat: List<Int>): List<Vec> {
        if (hat.isEmpty()) return emptyList()
        val h = head(body)
        val maxY = hat.maxOf { dotY(it) }
        return hat.map { Vec(h.x + (dotX(it) - 1) * HS * 1.4f, h.y - 12 - (maxY - dotY(it)) * HS) }
    }

    class Weapon(val pts: List<Vec>, val base: Float, val reach: Float)

    /** grip = first dot. base = angle toward the furthest point, so the tip follows the swing */
    fun weapon(p: List<Int>, scale: Float = WS): Weapon {
        val ox = dotX(p[0])
        val oy = dotY(p[0])
        val pts = p.map { Vec((dotX(it) - ox) * scale, (dotY(it) - oy) * scale) }
        var far = pts.getOrElse(1) { Vec(1f, 0f) }
        var fd = 0f
        for (q in pts) {
            val d = hypot(q.x, q.y)
            if (d > fd) { fd = d; far = q }
        }
        return Weapon(pts, atan2(far.y, far.x), fd)
    }

    /** weapon points in fighter-local (unflipped) space for a given swing angle and forward thrust */
    fun weaponLocal(body: List<Int>, weapon: List<Int>, swing: Float, thrust: Float = 0f): List<Vec> {
        val hand = body(body).last()
        val g = weapon(weapon)
        val sw = swing - g.base
        val push = Vec(cos(swing), sin(swing)) * thrust
        return g.pts.map { hand + push + it.rot(sw) }
    }

    /** projectile shape: the weapon pattern centred on its middle */
    fun projectile(p: List<Int>, scale: Float = 14f): List<Vec> {
        val cx = p.map { dotX(it) }.average().toFloat()
        val cy = p.map { dotY(it) }.average().toFloat()
        return p.map { Vec((dotX(it) - cx) * scale, (dotY(it) - cy) * scale) }
    }

    /** where the googly eyes go: the highest body dot (rightmost on ties) */
    fun head(p: List<Int>): Vec {
        val b = body(p)
        var top = b[0]
        for (q in b) if (q.y < top.y || (q.y == top.y && q.x > top.x)) top = q
        return top
    }
}

class Fighter(
    val id: Int,
    val loadout: Loadout,
    val name: String,
    var team: Int,
    hpMult: Float = 1f,
) {
    val stats = loadout.stats
    val maxHp = stats.hp * hpMult
    var x = 0f; var y = 0f; var vx = 0f; var vy = 0f
    var prevY = 0f
    var ang = 0f; var av = 0f
    var hp = maxHp
    var face = 1f
    var swingT = -1f
    var hitSet = mutableSetOf<Any>()
    var fired = false
    var cd = 0f
    var stun = 0f
    val plates = BooleanArray(stats.plates) { true }
    var dead = false
    var ground = false
    var look = 0f
    var aiJumpT = 0f
    var aiThinkT = 0f
    var held = false
    var bot = true
    val input = Input()
    var target: Fighter? = null

    // statuses
    var burnT = 0f
    var freezeT = 0f
    var rageT = 0f
    var bubbleT = 0f
    var abilityCd = 0f
    var dashT = 0f
    var slamming = false
    var jumpsLeft = 0
    var jumpHeld = false
    var weaponGone = 0f      // glass shattered, comes back after this many seconds
    var weaponThrown = false // boomerang is out
    var spikeCd = 0f
    var sayCd = 0f

    // record keeping for the results screen
    var dmgDealt = 0f
    var kos = 0

    class Input { var left = false; var right = false; var jump = false }

    val hasWeapon get() = weaponGone <= 0 && !weaponThrown
    val reach: Float
        get() = when (loadout.weaponType) {
            WeaponType.BOW, WeaponType.BOMB -> 380f
            WeaponType.BOOMERANG -> 300f
            WeaponType.SPEAR -> Geo.weapon(loadout.weapon).reach + 70f
            else -> Geo.weapon(loadout.weapon).reach + 30f
        }
    val speed get() = stats.speed * (if (freezeT > 0) .45f else 1f) * (if (rageT > 0) 1.4f else 1f)
    val swingTime get() = stats.swing * (if (freezeT > 0) 1.6f else 1f) * (if (rageT > 0) .75f else 1f)
    val swingProgress get() = if (swingT < 0) -1f else swingT / swingTime
    val hot get() = !loadout.weaponType.ranged && swingProgress in 0.3f..0.75f && hasWeapon
}

class Debris(
    var x: Float, var y: Float, val a: Vec, val b: Vec,
    val kind: Kind, val owner: Int, var vx: Float, var vy: Float,
) {
    enum class Kind { BODY, WEAPON, ARMOUR, HAT, WOOD }
    var ang = 0f
    var av = (Random.nextFloat() - .5f) * .4f
    var life = 6f
}

class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, val kind: Kind, var life: Float) {
    enum class Kind { SPARK, FIRE, ICE, ZAP, SMOKE, HEAL, MAGIC }
    val maxLife = life
}

class FloatText(var x: Float, var y: Float, val text: String, val tone: Tone, var life: Float = 1.1f) {
    enum class Tone { NORMAL, CRIT, METAL, GOOD, FIRE, ICE, ZAP, SPEECH }
}

class Boom(val x: Float, val y: Float, val radius: Float) { var t = 0f }

class Projectile(
    val owner: Fighter,
    val kind: WeaponType,
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    val shape: List<Vec>,
) {
    var ang = atan2(vy, vx)
    var av = 0f
    var life = 0f
    var returning = false
    var stuck = false
    val hit = mutableSetOf<Any>()
}

class Prop(val kind: Kind, var x: Float, var y: Float, val w: Float, val h: Float) {
    enum class Kind(val dynamic: Boolean) { PLATFORM(false), CRATE(true), TNT(true), PAD(false), SPIKES(false) }
    var vx = 0f; var vy = 0f
    var hp = if (kind == Kind.TNT) 1f else 30f
    var dead = false
    var flash = 0f
    var ang = 0f
    var av = 0f
}

enum class Gravity(val label: String, val g: Float) {
    EARTH("earth", .55f), MOON("moon", .15f), OFF("off", 0f), JUPITER("jupiter", 1.3f)
}

enum class GameMap(val label: String, val blurb: String) {
    FLAT("flat", "nothing but floor"),
    PLATFORMS("platforms", "three floating ledges"),
    SPIKE_PIT("spike pit", "spikes in the middle, ledges above"),
    TRAMPOLINE("trampoline", "bounce pads everywhere"),
    ICE("ice rink", "you can't stop sliding"),
    CRATES("warehouse", "crates and a couple of tnt barrels"),
}

enum class Tool(val label: String) {
    GRAB("grab"), BOOM("boom"), POKE("poke"), FIRE("fire"), FREEZE("freeze"), ZAP("zap"),
    CRATE("crate"), TNT("tnt"), PAD("pad"), SPIKES("spikes"), PLATFORM("ledge"),
    SPAWN("spawn"), ERASE("erase"), HEAL("heal")
}

sealed class Event {
    data class Hit(val dmg: Int, val crit: Boolean) : Event()
    object Clang : Event()
    data class Boom(val big: Boolean) : Event()
    object Ko : Event()
    object Shoot : Event()
    object Jump : Event()
    object Bounce : Event()
    object Break : Event()
    object Ability : Event()
    object Win : Event()
}

/** one fighting arena. units are roughly css pixels; step() is one 1/60s tick */
class Arena(var width: Float = 900f, var height: Float = 500f) {
    val fighters = mutableListOf<Fighter>()
    val debris = mutableListOf<Debris>()
    val particles = mutableListOf<Particle>()
    val texts = mutableListOf<FloatText>()
    val booms = mutableListOf<Boom>()
    val projectiles = mutableListOf<Projectile>()
    val props = mutableListOf<Prop>()
    val events = mutableListOf<Event>()
    var shake = 0f
    var gravity = Gravity.EARTH
    var map = GameMap.FLAT; private set
    /** sandbox: nobody ever wins */
    var endless = false
    var winnerTeam: Int? = null; private set
    var winT = 0f; private set
    var time = 0f; private set
    var friction = 1f
    var showDamage = true
    var nextId = 0

    private var grabbed: Fighter? = null
    private var grabbedProp: Prop? = null
    private var gx = 0f; private var gy = 0f; private var gvx = 0f; private var gvy = 0f

    val floorY get() = height - 28f
    val living get() = fighters.filter { !it.dead }

    val winnerName: String?
        get() {
            val t = winnerTeam ?: return null
            if (t < 0) return ""
            val team = fighters.filter { it.team == t }
            return if (team.size == 1) team[0].name else team.joinToString(" + ") { it.name }
        }

    fun reset(map: GameMap) {
        fighters.clear(); debris.clear(); particles.clear(); texts.clear(); booms.clear()
        projectiles.clear(); props.clear(); events.clear()
        winnerTeam = null; winT = 0f; grabbed = null; grabbedProp = null; time = 0f; nextId = 0
        this.map = map
        friction = if (map == GameMap.ICE) .08f else 1f
        val w = width
        val fl = floorY
        when (map) {
            GameMap.FLAT, GameMap.ICE -> {}
            GameMap.PLATFORMS -> {
                props += Prop(Prop.Kind.PLATFORM, w * .2f, fl - 120, 140f, 12f)
                props += Prop(Prop.Kind.PLATFORM, w * .8f, fl - 120, 140f, 12f)
                props += Prop(Prop.Kind.PLATFORM, w * .5f, fl - 230, 160f, 12f)
            }
            GameMap.SPIKE_PIT -> {
                props += Prop(Prop.Kind.SPIKES, w * .5f, fl - 7, 170f, 14f)
                props += Prop(Prop.Kind.PLATFORM, w * .5f, fl - 150, 130f, 12f)
            }
            GameMap.TRAMPOLINE -> {
                for (k in listOf(.12f, .5f, .88f)) props += Prop(Prop.Kind.PAD, w * k, fl - 6, 70f, 12f)
                props += Prop(Prop.Kind.PLATFORM, w * .31f, fl - 200, 110f, 12f)
                props += Prop(Prop.Kind.PLATFORM, w * .69f, fl - 200, 110f, 12f)
            }
            GameMap.CRATES -> {
                for (k in 0..2) props += Prop(Prop.Kind.CRATE, w * .5f, fl - 17 - k * 34f, 34f, 34f)
                props += Prop(Prop.Kind.CRATE, w * .5f - 34, fl - 17, 34f, 34f)
                props += Prop(Prop.Kind.TNT, w * .5f + 34, fl - 17, 34f, 34f)
                props += Prop(Prop.Kind.TNT, w * .1f, fl - 17, 34f, 34f)
                props += Prop(Prop.Kind.CRATE, w * .9f, fl - 17, 34f, 34f)
            }
        }
    }

    fun add(l: Loadout, name: String, team: Int, x: Float? = null, hpMult: Float = 1f): Fighter {
        val f = Fighter(nextId++, l, name, team, hpMult)
        val n = fighters.size
        f.x = x ?: (width * (.15f + .7f * ((n * 0.618f) % 1f)))
        f.y = floorY - Geo.R - 60f - Random.nextFloat() * 40
        f.prevY = f.y
        f.face = if (f.x < width / 2) 1f else -1f
        f.jumpsLeft = maxJumps(f)
        fighters += f
        return f
    }

    /** classic 1v1 start */
    fun start(a: Loadout, aName: String, b: Loadout, bName: String, map: GameMap = GameMap.FLAT) {
        reset(map)
        add(a, aName, 0, width * .25f)
        add(b, bName, 1, width * .75f)
    }

    private fun maxJumps(f: Fighter) = if (f.loadout.ability == Ability.DOUBLE_JUMP) 2 else 1

    fun say(x: Float, y: Float, t: String, tone: FloatText.Tone = FloatText.Tone.NORMAL) {
        if (!showDamage && (tone == FloatText.Tone.NORMAL || tone == FloatText.Tone.CRIT) && t.startsWith("-")) return
        texts += FloatText(x, y, t, tone)
    }

    private fun speak(f: Fighter, lines: List<String>, chance: Float) {
        if (f.sayCd > 0 || Random.nextFloat() > chance) return
        f.sayCd = 2.5f
        texts += FloatText(f.x, f.y - 90, lines.random(), FloatText.Tone.SPEECH, 1.6f)
    }

    private fun puff(x: Float, y: Float, kind: Particle.Kind, n: Int, spread: Float = 3f, life: Float = .6f) {
        repeat(n) {
            particles += Particle(x, y, (Random.nextFloat() - .5f) * spread * 2, (Random.nextFloat() - .7f) * spread * 2, kind, life * (.6f + Random.nextFloat() * .6f))
        }
    }

    fun swing(f: Fighter) {
        if (f.dead || f.swingT >= 0 || f.cd > 0 || f.held || winnerTeam != null || f.stun > 0) return
        if (f.loadout.weaponType == WeaponType.BOOMERANG && f.weaponThrown) return
        f.swingT = 0f; f.hitSet.clear(); f.fired = false
    }

    fun swingAngle(f: Fighter): Float {
        val wt = f.loadout.weaponType
        if (f.swingT < 0) return when (wt) {
            WeaponType.SPEAR -> -.35f + sin(time * 2.5f + f.id) * .05f
            WeaponType.BOW -> -.15f
            else -> Geo.REST_SWING + sin(time * 2.5f + f.id) * 0.08f
        }
        val t = f.swingProgress
        return when (wt) {
            WeaponType.SPEAR -> -.1f
            WeaponType.BOW -> -.15f - (if (t < .5f) t * .3f else (1 - t) * .3f)
            WeaponType.BOOMERANG, WeaponType.BOMB -> when {
                t < .4f -> -.9f - (t / .4f) * 1.4f
                t < .6f -> -2.3f + ((t - .4f) / .2f) * 2.6f
                else -> .3f - ((t - .6f) / .4f) * 1.2f
            }
            else -> when {
                t < .3f -> -.9f - (t / .3f) * 1f
                t < .7f -> -1.9f + ((t - .3f) / .4f) * 3.4f
                else -> 1.5f - ((t - .7f) / .3f) * 2.4f
            }
        }
    }

    fun thrust(f: Fighter): Float {
        if (f.loadout.weaponType != WeaponType.SPEAR || f.swingT < 0) return 0f
        val t = f.swingProgress
        return when {
            t < .25f -> -12f * (t / .25f)
            t < .55f -> -12f + 62f * ((t - .25f) / .3f)
            else -> 50f * (1 - (t - .55f) / .45f)
        }
    }

    /** weapon points in fighter-local space, null when there's nothing in the hand */
    fun weaponLocal(f: Fighter): List<Vec>? {
        if (!f.hasWeapon) return null
        return Geo.weaponLocal(f.loadout.body, f.loadout.weapon, swingAngle(f), thrust(f))
    }

    fun toWorld(f: Fighter, p: Vec) = Vec(f.x, f.y) + Vec(p.x * f.face, p.y).rot(f.ang)

    fun weaponWorld(f: Fighter): List<Vec> {
        val local = weaponLocal(f) ?: listOf(Geo.body(f.loadout.body).last(), Geo.body(f.loadout.body).last() + Vec(10f, 0f))
        return local.map { toWorld(f, it) }
    }

    fun handWorld(f: Fighter) = toWorld(f, Geo.body(f.loadout.body).last())

    private fun enemiesOf(f: Fighter) = fighters.filter { !it.dead && it.team != f.team }

    // ---------- damage ----------
    fun damage(
        t: Fighter, amount: Float, from: Fighter?, fromX: Float, kb: Float, label: String,
        mat: WeaponMat = WeaponMat.STEEL, melee: Boolean = false,
    ) {
        if (t.dead || winnerTeam != null && !endless) return
        if (t.bubbleT > 0) {
            say(t.x, t.y - 60, "blocked", FloatText.Tone.METAL)
            puff(t.x, t.y, Particle.Kind.MAGIC, 6)
            return
        }
        val alive = t.plates.count { it }
        val frac = if (t.plates.isNotEmpty()) alive.toFloat() / t.plates.size else 0f
        val block = t.stats.def * frac
        val rage = if (from != null && from.rageT > 0) 1.6f else 1f
        var dmg = max(1, (amount * rage * (1 - block) * (.85f + Random.nextFloat() * .3f)).toInt())
        val crit = Random.nextFloat() < .08f
        if (crit) dmg *= 2
        t.hp -= dmg
        from?.let { it.dmgDealt += dmg }
        val dir = sign(t.x - fromX).let { if (it == 0f) 1f else it }
        t.vx += dir * kb; t.vy -= kb * .6f; t.av += dir * kb * .03f; t.stun = max(t.stun, .35f)
        say(t.x, t.y - 50, (if (crit) "crit " else "") + "-$dmg" + (if (label.isNotEmpty()) " $label" else ""),
            if (crit) FloatText.Tone.CRIT else FloatText.Tone.NORMAL)
        shake = min(14f, shake + dmg * .3f)
        events += Event.Hit(dmg, crit)
        puff(t.x, t.y, Particle.Kind.SPARK, 5 + dmg / 4, 4f, .35f)

        when (mat) {
            WeaponMat.FIRE -> { if (t.burnT <= 0) say(t.x, t.y - 74, "on fire", FloatText.Tone.FIRE); t.burnT = 3f }
            WeaponMat.ICE -> { if (t.freezeT <= 0) say(t.x, t.y - 74, "frozen", FloatText.Tone.ICE); t.freezeT = 2.2f; puff(t.x, t.y, Particle.Kind.ICE, 8) }
            WeaponMat.SHOCK -> { t.stun = max(t.stun, .9f); t.swingT = -1f; say(t.x, t.y - 74, "zapped", FloatText.Tone.ZAP); puff(t.x, t.y, Particle.Kind.ZAP, 8, 5f) }
            else -> {}
        }

        // armour that hits back
        if (melee && from != null && !from.dead) {
            when (t.loadout.armourMat) {
                ArmourMat.SPIKY -> if (alive > 0) {
                    from.hp -= 3; say(from.x, from.y - 50, "-3 spikes", FloatText.Tone.METAL)
                    if (from.hp <= 0) ko(from, -dir, t)
                }
                ArmourMat.RUBBER -> if (alive > 0) { from.vx -= dir * 9; from.vy -= 4; events += Event.Bounce }
                ArmourMat.ICE -> if (alive > 0) { from.freezeT = max(from.freezeT, 1.2f); puff(from.x, from.y, Particle.Kind.ICE, 5) }
                else -> {}
            }
        }

        if (alive > 0 && Random.nextFloat() < .55f + dmg / 60f) {
            val idx = t.plates.indices.filter { t.plates[it] }.random()
            t.plates[idx] = false
            val ap = Geo.armour(t.loadout.armour)
            debris += Debris(t.x, t.y, Vec(ap[idx].x * t.face, ap[idx].y), Vec(ap[idx + 1].x * t.face, ap[idx + 1].y),
                Debris.Kind.ARMOUR, t.id, dir * (3 + Random.nextFloat() * 4), -5 - Random.nextFloat() * 4)
            say(t.x, t.y - 72, t.loadout.armourName + " cracked", FloatText.Tone.METAL)
            events += Event.Break
        }
        if (t.hp <= 0) ko(t, dir, from)
        else if (!t.bot || Random.nextFloat() < .5f) speak(t, Names.HURT, .12f)
    }

    private fun ko(t: Fighter, dir: Float, by: Fighter?) {
        if (t.dead) return
        t.hp = 0f; t.dead = true; t.swingT = -1f
        if (grabbed === t) { grabbed = null; t.held = false }
        val b = Geo.body(t.loadout.body)
        for (i in 1 until b.size) {
            debris += Debris(t.x, t.y, Vec(b[i - 1].x * t.face, b[i - 1].y), Vec(b[i].x * t.face, b[i].y),
                Debris.Kind.BODY, t.id, dir * (2 + Random.nextFloat() * 6) + (Random.nextFloat() - .5f) * 4, -6 - Random.nextFloat() * 6)
        }
        if (t.hasWeapon) {
            val g = Geo.weapon(t.loadout.weapon)
            for (i in 1 until g.pts.size) {
                debris += Debris(t.x, t.y - 20, g.pts[i - 1], g.pts[i], Debris.Kind.WEAPON, t.id,
                    -dir * 4 + (Random.nextFloat() - .5f) * 6, -9f)
            }
        }
        val hat = Geo.hat(t.loadout.body, t.loadout.hat)
        for (i in 1 until hat.size) {
            debris += Debris(t.x, t.y, Vec(hat[i - 1].x * t.face, hat[i - 1].y), Vec(hat[i].x * t.face, hat[i].y),
                Debris.Kind.HAT, t.id, (Random.nextFloat() - .5f) * 4, -13f)
        }
        t.plates.fill(false)
        say(t.x, t.y - 80, Names.pick(Names.KO, t.loadout.body), FloatText.Tone.CRIT)
        puff(t.x, t.y, Particle.Kind.SMOKE, 14, 3f, 1f)
        shake = 18f
        events += Event.Ko
        if (by != null && by !== t) { by.kos++; speak(by, Names.TAUNT, .7f) }
        checkWinner()
    }

    private fun checkWinner() {
        if (endless || winnerTeam != null) return
        val teams = living.map { it.team }.toSet()
        if (teams.size <= 1) {
            winnerTeam = teams.firstOrNull() ?: -1
            winT = 0f
            events += Event.Win
        }
    }

    /** sandbox / survival can add fighters after a win was decided */
    fun clearWinner() { winnerTeam = null; winT = 0f }

    fun explode(x: Float, y: Float, radius: Float, dmg: Float, owner: Fighter?, label: String) {
        booms += Boom(x, y, radius)
        for (f in fighters) {
            if (f.dead) continue
            val d = hypot(f.x - x, f.y - y)
            if (d < radius) {
                val k = 1 - d / radius
                val n = if (d == 0f) 1f else d
                f.vx += (f.x - x) / n * 18 * k
                f.vy += ((f.y - y) / n * 18 - 6) * k
                f.av += (Random.nextFloat() - .5f) * .5f * k
                val self = if (f === owner) .5f else 1f
                if (k > .1f) damage(f, dmg * k * self, if (f === owner) null else owner, x, 0f, label)
            }
        }
        for (p in props) {
            if (p.dead || !p.kind.dynamic) continue
            val d = hypot(p.x - x, p.y - y)
            if (d < radius) {
                val k = 1 - d / radius
                p.vx += (p.x - x) / max(d, 1f) * 16 * k; p.vy -= 10 * k; p.av += (Random.nextFloat() - .5f) * .4f
                hurtProp(p, 40 * k)
            }
        }
        for (d in debris) {
            val dd = hypot(d.x - x, d.y - y)
            if (dd < radius + 30) { d.vx += (d.x - x) / max(dd, 1f) * 12; d.vy -= 10 }
        }
        puff(x, y, Particle.Kind.FIRE, 16, 6f, .5f)
        puff(x, y, Particle.Kind.SMOKE, 10, 3f, 1.2f)
        shake = 16f
        events += Event.Boom(radius > 120)
    }

    private fun hurtProp(p: Prop, amount: Float) {
        if (p.dead || !p.kind.dynamic) return
        p.hp -= amount; p.flash = .15f
        if (p.hp <= 0) breakProp(p)
    }

    private fun breakProp(p: Prop) {
        if (p.dead) return
        p.dead = true
        if (p.kind == Prop.Kind.TNT) {
            explode(p.x, p.y, 150f, 30f, null, "tnt")
        } else {
            val s = p.w / 2
            val corners = listOf(Vec(-s, -s), Vec(s, -s), Vec(s, s), Vec(-s, s))
            for (i in corners.indices) {
                debris += Debris(p.x, p.y, corners[i], corners[(i + 1) % 4], Debris.Kind.WOOD, -1,
                    (Random.nextFloat() - .5f) * 8, -4 - Random.nextFloat() * 5)
            }
            debris += Debris(p.x, p.y, corners[0], corners[2], Debris.Kind.WOOD, -1, (Random.nextFloat() - .5f) * 6, -6f)
            events += Event.Break
        }
    }

    // ---------- abilities ----------
    fun useAbility(f: Fighter) {
        val ab = f.loadout.ability
        if (f.dead || f.held || f.abilityCd > 0 || ab.cooldown == 0f || winnerTeam != null || f.stun > 0) return
        f.abilityCd = ab.cooldown
        events += Event.Ability
        when (ab) {
            Ability.DASH -> {
                f.vx = f.face * 19; f.vy = min(f.vy, -2f); f.dashT = .3f; f.hitSet.clear()
                say(f.x, f.y - 60, "dash", FloatText.Tone.ZAP)
            }
            Ability.SLAM -> {
                f.slamming = true
                if (f.ground) { f.vy = -13f; f.ground = false } else f.vy = 20f
            }
            Ability.HEAL -> {
                f.hp = min(f.maxHp, f.hp + f.maxHp * .3f)
                say(f.x, f.y - 60, "+heal", FloatText.Tone.GOOD)
                puff(f.x, f.y, Particle.Kind.HEAL, 14, 2f, 1f)
            }
            Ability.TELEPORT -> {
                val t = f.target ?: enemiesOf(f).minByOrNull { abs(it.x - f.x) }
                puff(f.x, f.y, Particle.Kind.MAGIC, 12)
                if (t != null) {
                    f.x = (t.x - t.face * 75).coerceIn(Geo.R, width - Geo.R)
                    f.y = t.y - 10
                } else f.x = width - f.x
                f.vx = 0f; f.vy = 0f
                puff(f.x, f.y, Particle.Kind.MAGIC, 12)
                say(f.x, f.y - 60, "boop", FloatText.Tone.ZAP)
            }
            Ability.RAGE -> {
                f.rageT = 4f
                say(f.x, f.y - 60, "RAGE", FloatText.Tone.FIRE)
                puff(f.x, f.y, Particle.Kind.FIRE, 12)
            }
            Ability.BUBBLE -> {
                f.bubbleT = 1.6f
                puff(f.x, f.y, Particle.Kind.MAGIC, 10)
            }
            Ability.DOUBLE_JUMP, Ability.NONE -> {}
        }
    }

    // ---------- sandbox tools ----------
    fun fighterAt(x: Float, y: Float) = fighters.firstOrNull { !it.dead && hypot(it.x - x, it.y - y) < Geo.R + 18 }
    fun propAt(x: Float, y: Float) = props.firstOrNull {
        !it.dead && abs(it.x - x) < it.w / 2 + 10 && abs(it.y - y) < it.h / 2 + 14
    }

    var spawnLoadout: () -> Pair<Loadout, String> = { Loadout.random().let { it to it.autoName } }

    fun pointerDown(tool: Tool, x: Float, y: Float) {
        when (tool) {
            Tool.GRAB -> {
                val f = fighterAt(x, y)
                if (f != null) {
                    grabbed = f; f.held = true; f.swingT = -1f
                    gx = x; gy = y; gvx = 0f; gvy = 0f
                    speak(f, Names.GRABBED, 1f)
                } else propAt(x, y)?.takeIf { it.kind.dynamic }?.let {
                    grabbedProp = it; gx = x; gy = y; gvx = 0f; gvy = 0f
                }
            }
            Tool.BOOM -> explode(x, y, 170f, 22f, null, "boom")
            Tool.POKE -> fighterAt(x, y)?.let { damage(it, 3f, null, x, 5f, "poke") } ?: propAt(x, y)?.let { hurtProp(it, 10f) }
            Tool.FIRE -> fighterAt(x, y)?.let { it.burnT = 4f; say(it.x, it.y - 60, "on fire", FloatText.Tone.FIRE) } ?: puff(x, y, Particle.Kind.FIRE, 10)
            Tool.FREEZE -> fighterAt(x, y)?.let { it.freezeT = 4f; it.vx = 0f; say(it.x, it.y - 60, "frozen", FloatText.Tone.ICE); puff(it.x, it.y, Particle.Kind.ICE, 12) } ?: puff(x, y, Particle.Kind.ICE, 10)
            Tool.ZAP -> fighterAt(x, y)?.let { damage(it, 6f, null, x, 3f, "zap", WeaponMat.SHOCK) } ?: puff(x, y, Particle.Kind.ZAP, 10, 5f)
            Tool.HEAL -> fighterAt(x, y)?.let { it.hp = it.maxHp; it.plates.fill(true); it.burnT = 0f; it.freezeT = 0f; say(it.x, it.y - 60, "+heal", FloatText.Tone.GOOD); puff(it.x, it.y, Particle.Kind.HEAL, 12) }
            Tool.CRATE -> props += Prop(Prop.Kind.CRATE, x, y, 34f, 34f)
            Tool.TNT -> props += Prop(Prop.Kind.TNT, x, y, 34f, 34f)
            Tool.PAD -> props += Prop(Prop.Kind.PAD, x.coerceIn(35f, width - 35), floorY - 6, 70f, 12f)
            Tool.SPIKES -> props += Prop(Prop.Kind.SPIKES, x.coerceIn(50f, width - 50), floorY - 7, 100f, 14f)
            Tool.PLATFORM -> props += Prop(Prop.Kind.PLATFORM, x, y.coerceAtMost(floorY - 40), 120f, 12f)
            Tool.SPAWN -> {
                val (l, n) = spawnLoadout()
                val f = add(l, n, (fighters.maxOfOrNull { it.team } ?: 0) + 1, x.coerceIn(Geo.R, width - Geo.R))
                f.y = y.coerceIn(Geo.R, floorY - Geo.R)
                puff(f.x, f.y, Particle.Kind.MAGIC, 12)
                clearWinner()
            }
            Tool.ERASE -> {
                val f = fighters.firstOrNull { hypot(it.x - x, it.y - y) < Geo.R + 18 }
                if (f != null && fighters.size > 1) { fighters.remove(f); puff(f.x, f.y, Particle.Kind.SMOKE, 10) }
                else propAt(x, y)?.let { props.remove(it); puff(it.x, it.y, Particle.Kind.SMOKE, 8) }
            }
        }
    }

    fun pointerMove(x: Float, y: Float) {
        if (grabbed == null && grabbedProp == null) return
        gvx = x - gx; gvy = y - gy; gx = x; gy = y
    }

    fun pointerUp() {
        grabbed?.let { f ->
            f.held = false
            f.vx = (gvx * 1.1f).coerceIn(-45f, 45f)
            f.vy = (gvy * 1.1f).coerceIn(-45f, 45f)
            f.av = gvx * .02f
        }
        grabbedProp?.let { p ->
            p.vx = (gvx * 1.1f).coerceIn(-40f, 40f); p.vy = (gvy * 1.1f).coerceIn(-40f, 40f); p.av = gvx * .01f
        }
        grabbed = null; grabbedProp = null
    }

    fun chaos() {
        for (f in fighters) {
            f.vx += (Random.nextFloat() - .5f) * 30
            f.vy -= 8 + Random.nextFloat() * 12
            f.av += (Random.nextFloat() - .5f) * .8f
        }
        for (p in props) if (p.kind.dynamic) { p.vx += (Random.nextFloat() - .5f) * 20; p.vy -= 10 }
        say(width / 2, 60f, "chaos", FloatText.Tone.CRIT)
    }

    fun healAll() {
        for (f in fighters) if (!f.dead) {
            f.hp = f.maxHp; f.plates.fill(true); f.burnT = 0f; f.freezeT = 0f
            say(f.x, f.y - 60, "+heal", FloatText.Tone.GOOD)
        }
    }

    // ---------- sim ----------
    fun step(dt: Float = 1f / 60f) {
        time += dt
        val g = gravity.g
        val fl = floorY
        for (f in fighters.toList()) stepFighter(f, dt, g, fl)
        separate()
        stepProjectiles(dt, g, fl)
        stepProps(g, fl)

        for (d in debris) {
            d.vy += g; d.x += d.vx; d.y += d.vy; d.ang += d.av; d.life -= dt
            if (d.y > fl - 4) { d.y = fl - 4; d.vy *= -.35f; d.vx *= .8f; d.av *= .7f }
            if (d.x < 4 || d.x > width - 4) { d.vx *= -.6f; d.x = d.x.coerceIn(4f, width - 4) }
        }
        debris.removeAll { it.life <= 0 }
        if (debris.size > 260) debris.subList(0, debris.size - 260).clear()
        for (p in particles) {
            p.x += p.vx; p.y += p.vy; p.life -= dt
            when (p.kind) {
                Particle.Kind.FIRE, Particle.Kind.SMOKE, Particle.Kind.HEAL -> p.vy -= .08f
                else -> p.vy += g * .3f
            }
            p.vx *= .97f
        }
        particles.removeAll { it.life <= 0 }
        if (particles.size > 400) particles.subList(0, particles.size - 400).clear()
        for (t in texts) { t.y -= if (t.tone == FloatText.Tone.SPEECH) .4f else 1.2f; t.life -= dt }
        texts.removeAll { it.life <= 0 }
        for (bm in booms) bm.t += dt
        booms.removeAll { it.t >= .5f }
        props.removeAll { it.dead }
        if (winnerTeam != null) winT += dt
        shake *= .88f
    }

    private fun pickTarget(f: Fighter): Fighter? {
        val cur = f.target
        if (cur != null && !cur.dead && cur.team != f.team && f.aiThinkT > 0) return cur
        f.aiThinkT = 1.5f + Random.nextFloat()
        return enemiesOf(f).minByOrNull { hypot(it.x - f.x, it.y - f.y) * (if (it.held) 3 else 1) }
    }

    private fun stepFighter(f: Fighter, dt: Float, g: Float, fl: Float) {
        f.cd = max(0f, f.cd - dt); f.stun = max(0f, f.stun - dt); f.sayCd -= dt
        f.abilityCd = max(0f, f.abilityCd - dt); f.aiThinkT -= dt; f.spikeCd -= dt
        f.rageT -= dt; f.bubbleT -= dt; f.freezeT -= dt; f.dashT -= dt
        if (f.weaponGone > 0) f.weaponGone -= dt
        if (f.dead) {
            stepBody(f, 0f, g, fl)
            return
        }
        if (f.burnT > 0) {
            f.burnT -= dt
            f.hp -= 4 * dt
            if (Random.nextFloat() < .4f) particles += Particle(f.x + (Random.nextFloat() - .5f) * 40, f.y + (Random.nextFloat() - .5f) * 40, 0f, -1f, Particle.Kind.FIRE, .5f)
            if (f.hp <= 0) { ko(f, 1f, null); return }
        }
        val o = pickTarget(f)
        f.target = o
        if (!f.held && o != null) f.face = if (o.x >= f.x) 1f else -1f
        f.look = if (o != null) atan2(o.y - f.y, (o.x - f.x) * f.face) else 0f

        if (f.held) {
            f.x += (gx - f.x) * .5f; f.y += (gy - f.y) * .5f
            f.vx = 0f; f.vy = 0f
            f.ang += sin(time * 11f) * .04f
            f.prevY = f.y
            return
        }

        var mv = 0f
        var jump = false
        if (f.stun <= 0 && (winnerTeam == null || endless)) {
            if (f.bot) {
                if (o != null) {
                    val dx = o.x - f.x
                    val adx = abs(dx)
                    val ranged = f.loadout.weaponType.ranged
                    val scared = f.hp < f.maxHp * .25f && Random.nextFloat() < .5f
                    mv = when {
                        scared && adx < 200 -> -sign(dx)
                        ranged && adx < 160 -> -sign(dx)
                        adx > f.reach + 10 -> sign(dx)
                        !ranged && adx < f.reach * .5f -> -sign(dx) * .6f
                        else -> 0f
                    }
                    val inRange = adx < f.reach + 40 && (ranged || abs(o.y - f.y) < 90)
                    if (inRange && Random.nextFloat() < .08f) swing(f)
                    val ab = f.loadout.ability
                    if (f.abilityCd <= 0 && Random.nextFloat() < .02f) {
                        val use = when (ab) {
                            Ability.DASH -> adx in 60f..260f
                            Ability.SLAM -> adx < 140
                            Ability.HEAL -> f.hp < f.maxHp * .5f
                            Ability.TELEPORT -> adx > 220
                            Ability.RAGE -> adx < 220
                            Ability.BUBBLE -> o.swingT >= 0 && adx < 160
                            else -> false
                        }
                        if (use) useAbility(f)
                    }
                    f.aiJumpT -= dt
                    if (f.aiJumpT <= 0) {
                        f.aiJumpT = .6f + Random.nextFloat() * 1.6f
                        if (Random.nextFloat() < .45f || o.y < f.y - 50) jump = true
                    }
                    // don't walk into spikes on purpose
                    if (props.any { it.kind == Prop.Kind.SPIKES && abs(it.x - (f.x + mv * 40)) < it.w / 2 + 20 } && f.ground) jump = true
                }
            } else {
                if (f.input.left) mv -= 1
                if (f.input.right) mv += 1
                // humans get a fresh press per jump so double jump works
                if (f.input.jump && !f.jumpHeld) jump = true
                f.jumpHeld = f.input.jump
            }
        }
        val accel = (if (f.ground) .9f else .35f) * (if (f.ground) friction else 1f)
        if (mv != 0f && f.dashT <= 0) f.vx += (mv * f.speed - f.vx) * accel * .4f
        if (jump && (f.ground || f.jumpsLeft > 0)) {
            if (!f.ground) f.jumpsLeft-- else f.jumpsLeft = maxJumps(f) - 1
            f.vy = -f.stats.jump * (if (g == 0f) .4f else 1f) * (if (gravity == Gravity.JUPITER) 1.3f else 1f)
            f.ground = false
            events += Event.Jump
            if (!f.ground && f.jumpsLeft >= 0 && f.loadout.ability == Ability.DOUBLE_JUMP) puff(f.x, f.y + 30, Particle.Kind.SMOKE, 4, 2f, .4f)
        }

        stepBody(f, mv, g, fl)

        // dash contact damage
        if (f.dashT > 0) {
            for (e in enemiesOf(f)) if (e !in f.hitSet && hypot(e.x - f.x, e.y - f.y) < Geo.R * 2) {
                f.hitSet += e
                damage(e, 6f + f.stats.dmg * .5f, f, f.x, 9f, "dash")
            }
        }

        // swing / shoot
        if (f.swingT >= 0) {
            f.swingT += dt
            val t = f.swingProgress
            val wt = f.loadout.weaponType
            if (wt.ranged) {
                if (!f.fired && t >= .5f) { f.fired = true; fire(f, o) }
            } else if (t > .3f && t < .75f && (wt != WeaponType.SPEAR || t > .3f)) meleeHits(f)
            if (t >= 1) { f.swingT = -1f; f.cd = .15f }
        }
    }

    private fun meleeHits(f: Fighter) {
        val ws = weaponWorld(f)
        val fist = !f.hasWeapon
        val dmg = if (fist) 3f else f.stats.dmg.toFloat()
        val mat = if (fist) WeaponMat.STEEL else f.loadout.weaponMat
        for (e in enemiesOf(f)) {
            if (e in f.hitSet || e.held) continue
            for (i in 1 until ws.size) {
                if (segDist(e.x, e.y, ws[i - 1], ws[i]) < Geo.R) {
                    f.hitSet += e
                    damage(e, dmg, f, f.x, (4 + dmg * .25f) * f.stats.kb, if (f.loadout.weaponType == WeaponType.HAMMER) "bonk" else "", mat, melee = true)
                    if (f.loadout.weaponType == WeaponType.HAMMER) shake = min(18f, shake + 6)
                    if (!fist && mat == WeaponMat.GLASS && Random.nextFloat() < .18f) shatter(f)
                    break
                }
            }
        }
        for (p in props) {
            if (p in f.hitSet || p.dead || !p.kind.dynamic) continue
            for (i in 1 until ws.size) if (segDist(p.x, p.y, ws[i - 1], ws[i]) < p.w / 2 + 4) {
                f.hitSet += p
                p.vx += f.face * 6 * f.stats.kb; p.vy -= 4
                hurtProp(p, dmg)
                break
            }
        }
        // clash with other swinging weapons
        if (fist) return
        for (o in enemiesOf(f)) {
            if (o.swingT < 0 || !o.hot || o in f.hitSet) continue
            val ow = weaponWorld(o)
            for (i in 1 until ws.size) for (j in 1 until ow.size) {
                if (segDist(ws[i].x, ws[i].y, ow[j - 1], ow[j]) < 6) {
                    f.hitSet += o; o.hitSet += f
                    f.vx -= f.face * 7; o.vx -= o.face * 7; f.vy -= 3; o.vy -= 3
                    say((f.x + o.x) / 2, min(f.y, o.y) - 60, "clang", FloatText.Tone.CRIT)
                    puff(ws[i].x, ws[i].y, Particle.Kind.SPARK, 12, 5f, .3f)
                    shake = 8f; events += Event.Clang
                    return
                }
            }
        }
    }

    private fun shatter(f: Fighter) {
        f.weaponGone = 4f
        val ws = weaponWorld(f)
        for (i in 1 until ws.size) {
            debris += Debris(ws[i - 1].x, ws[i - 1].y, Vec(0f, 0f), Vec(ws[i].x - ws[i - 1].x, ws[i].y - ws[i - 1].y),
                Debris.Kind.WEAPON, f.id, (Random.nextFloat() - .5f) * 8, -5f)
        }
        say(f.x, f.y - 74, "weapon shattered", FloatText.Tone.METAL)
        events += Event.Break
    }

    private fun fire(f: Fighter, target: Fighter?) {
        val hand = handWorld(f)
        val wt = f.loadout.weaponType
        val dx = (target?.x ?: (f.x + f.face * 300)) - hand.x
        val dy = (target?.y ?: f.y) - hand.y
        val g = max(gravity.g, .01f)
        val shape = Geo.projectile(f.loadout.weapon, if (wt == WeaponType.BOMB) 10f else 13f)
        when (wt) {
            WeaponType.BOW -> {
                val speed = 15f
                val tt = max(8f, abs(dx) / speed)
                val vy = ((dy - .5f * g * .5f * tt * tt) / tt).coerceIn(-14f, 10f)
                projectiles += Projectile(f, wt, hand.x, hand.y, sign(dx).let { if (it == 0f) f.face else it } * speed, vy, shape)
            }
            WeaponType.BOMB -> {
                val speed = 8f
                val tt = max(12f, abs(dx) / speed)
                val vy = ((dy - .5f * g * tt * tt) / tt).coerceIn(-18f, 6f)
                projectiles += Projectile(f, wt, hand.x, hand.y, dx / tt, vy, shape).also { it.av = .25f * f.face }
            }
            WeaponType.BOOMERANG -> {
                f.weaponThrown = true
                projectiles += Projectile(f, wt, hand.x, hand.y, f.face * 14f, (dy / 30f).coerceIn(-3f, 3f), shape).also { it.av = .45f }
            }
            else -> {}
        }
        events += Event.Shoot
    }

    private fun stepProjectiles(dt: Float, g: Float, fl: Float) {
        val it = projectiles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life += dt
            val owner = p.owner
            when (p.kind) {
                WeaponType.BOW -> {
                    if (!p.stuck) {
                        p.vy += g * .5f; p.x += p.vx; p.y += p.vy; p.ang = atan2(p.vy, p.vx)
                        if (p.y > fl - 3) { p.y = fl - 3; p.stuck = true; p.life = max(p.life, 3f) }
                        if (p.x < 0 || p.x > width) { p.stuck = true; p.x = p.x.coerceIn(0f, width); p.life = max(p.life, 3f) }
                        projectileHits(p, if (owner.dead) 6f else owner.stats.dmg.toFloat(), true)
                    }
                    if (p.life > 5f) it.remove()
                }
                WeaponType.BOMB -> {
                    p.vy += g; p.x += p.vx; p.y += p.vy; p.ang += p.av
                    if (p.y > fl - 8) { p.y = fl - 8; p.vy *= -.45f; p.vx *= .7f; p.av *= .8f }
                    if (p.x < 8 || p.x > width - 8) { p.vx *= -.6f; p.x = p.x.coerceIn(8f, width - 8) }
                    if (Random.nextFloat() < .5f) particles += Particle(p.x, p.y - 8, 0f, -1f, Particle.Kind.FIRE, .3f)
                    val touching = fighters.any { !it.dead && it.team != owner.team && hypot(it.x - p.x, it.y - p.y) < Geo.R }
                    if (p.life > 1.4f || (touching && p.life > .25f)) {
                        explode(p.x, p.y, 120f, max(10f, owner.stats.dmg * 1.4f), owner, "bomb")
                        if (owner.loadout.weaponMat == WeaponMat.FIRE || owner.loadout.weaponMat == WeaponMat.ICE || owner.loadout.weaponMat == WeaponMat.SHOCK) {
                            for (f in fighters) if (!f.dead && hypot(f.x - p.x, f.y - p.y) < 120 && f !== owner) {
                                when (owner.loadout.weaponMat) {
                                    WeaponMat.FIRE -> f.burnT = 3f
                                    WeaponMat.ICE -> f.freezeT = 2f
                                    WeaponMat.SHOCK -> f.stun = .9f
                                    else -> {}
                                }
                            }
                        }
                        it.remove()
                    }
                }
                WeaponType.BOOMERANG -> {
                    p.ang += p.av
                    if (!p.returning) {
                        p.vx *= .965f; p.x += p.vx; p.y += p.vy; p.vy *= .95f
                        if (abs(p.vx) < 2.5f || p.life > 1.4f || p.x < 10 || p.x > width - 10) { p.returning = true; p.hit.clear() }
                    } else {
                        val hx = owner.x - p.x
                        val hy = owner.y - p.y
                        val d = max(hypot(hx, hy), 1f)
                        val sp = min(16f, 6f + p.life * 4)
                        p.vx += (hx / d * sp - p.vx) * .15f; p.vy += (hy / d * sp - p.vy) * .15f
                        p.x += p.vx; p.y += p.vy
                        if (d < 30 || owner.dead || p.life > 5f) {
                            owner.weaponThrown = false
                            it.remove()
                            continue
                        }
                    }
                    projectileHits(p, owner.stats.dmg.toFloat(), false)
                }
                else -> it.remove()
            }
        }
    }

    private fun projectileHits(p: Projectile, dmg: Float, removeOnHit: Boolean) {
        val owner = p.owner
        for (e in fighters) {
            if (e.dead || e.team == owner.team || e in p.hit || e.held) continue
            if (hypot(e.x - p.x, e.y - p.y) < Geo.R + 6) {
                p.hit += e
                damage(e, dmg, owner, p.x - p.vx, (4 + dmg * .2f) * owner.stats.kb, "", owner.loadout.weaponMat)
                if (removeOnHit) { p.stuck = true; p.life = 99f; return }
            }
        }
        for (pr in props) {
            if (pr.dead || pr in p.hit) continue
            if (abs(pr.x - p.x) < pr.w / 2 && abs(pr.y - p.y) < pr.h / 2) {
                if (pr.kind.dynamic) {
                    p.hit += pr; hurtProp(pr, dmg); pr.vx += sign(p.vx) * 4
                    if (removeOnHit) { p.stuck = true; p.life = 99f; return }
                } else if (pr.kind == Prop.Kind.PLATFORM && removeOnHit) {
                    p.stuck = true; p.life = max(p.life, 3f); return
                }
            }
        }
    }

    /** physics shared by living and dead fighters */
    private fun stepBody(f: Fighter, mv: Float, g: Float, fl: Float) {
        f.prevY = f.y
        if (f.held) return
        f.vy += g
        if (g == 0f) { f.vx *= .995f; f.vy *= .995f }
        if (f.slamming && !f.ground) f.vy += g * 1.5f
        f.x += f.vx; f.y += f.vy
        f.ang += f.av; f.av *= .96f
        if (!f.dead) { f.av += -f.ang * .02f; f.ang *= if (f.ground) .85f else .99f }

        fun impact(speed: Float, label: String) {
            if (speed > 15 && !f.dead && f.bubbleT <= 0 && !f.slamming) damage(f, (speed - 15) * 2.2f, null, f.x, 0f, label)
        }
        val wasGround = f.ground
        f.ground = false
        val r = Geo.R
        // platforms (one way, from above)
        if (f.vy >= 0) for (p in props) {
            if (p.kind != Prop.Kind.PLATFORM || p.dead) continue
            val top = p.y - p.h / 2
            if (abs(f.x - p.x) < p.w / 2 + 6 && f.prevY + r <= top + 2 && f.y + r >= top) {
                f.y = top - r
                landed(f, f.vy)
                f.vy = 0f
                f.ground = true
            }
        }
        if (f.y > fl - r) {
            impact(f.vy, "splat")
            landed(f, f.vy)
            f.y = fl - r
            f.vy = if (f.dead) -f.vy * .3f else if (abs(f.vy) > 6) -f.vy * .25f else 0f
            f.ground = true
        }
        if (f.ground) {
            val fr = if (friction < 1f) friction else 1f
            f.vx *= if (f.dead) .9f else if (mv != 0f) 1f else 1 - .2f * fr
            if (f.dead) f.av += f.vx * .01f
            f.jumpsLeft = maxJumps(f) - 1
            if (!wasGround) f.jumpsLeft = maxJumps(f) - 1
            // pads and spikes
            for (p in props) {
                if (p.dead || abs(f.x - p.x) > p.w / 2 + 10) continue
                if (p.kind == Prop.Kind.PAD && abs((f.y + r) - (p.y - p.h / 2)) < 16) {
                    f.vy = -19f; f.ground = false; events += Event.Bounce
                    say(p.x, p.y - 30, "boing", FloatText.Tone.GOOD)
                }
                if (p.kind == Prop.Kind.SPIKES && !f.dead && f.spikeCd <= 0 && abs((f.y + r) - fl) < 4) {
                    f.spikeCd = .5f
                    damage(f, 9f, null, p.x + (if (f.x < p.x) 30 else -30), 6f, "spikes")
                    f.vy = -9f
                }
            }
        }
        if (f.y < r) { impact(-f.vy, "bonk"); f.y = r; f.vy = -f.vy * .4f }
        if (f.x < r) { impact(-f.vx, "wall"); f.x = r; f.vx = -f.vx * .5f; f.av += .1f }
        if (f.x > width - r) { impact(f.vx, "wall"); f.x = width - r; f.vx = -f.vx * .5f; f.av -= .1f }
    }

    private fun landed(f: Fighter, vy: Float) {
        if (f.slamming && !f.dead) {
            f.slamming = false
            booms += Boom(f.x, f.y + Geo.R, 140f)
            puff(f.x, f.y + Geo.R, Particle.Kind.SMOKE, 14, 5f, .6f)
            shake = 14f
            events += Event.Boom(false)
            for (e in enemiesOf(f)) {
                val d = hypot(e.x - f.x, e.y - f.y)
                if (d < 140) damage(e, 10f + f.stats.dmg * .6f + max(0f, vy - 10), f, f.x, 11f * (1 - d / 140) + 3, "slam")
            }
            for (p in props) if (p.kind.dynamic && hypot(p.x - f.x, p.y - f.y) < 140) { p.vy -= 9; hurtProp(p, 15f) }
        }
    }

    private fun separate() {
        val alive = fighters.filter { !it.held }
        for (i in alive.indices) for (j in i + 1 until alive.size) {
            val a = alive[i]
            val b = alive[j]
            if (a.dead || b.dead) continue
            push(a, b)
        }
    }

    private fun push(a: Fighter, b: Fighter) {
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

    private fun stepProps(g: Float, fl: Float) {
        for (p in props) {
            if (p.flash > 0) p.flash -= 1f / 60f
            if (!p.kind.dynamic || p.dead) continue
            if (p === grabbedProp) {
                p.x += (gx - p.x) * .5f; p.y += (gy - p.y) * .5f; p.vx = 0f; p.vy = 0f
                continue
            }
            p.vy += g
            p.x += p.vx; p.y += p.vy
            p.ang += p.av; p.av *= .95f
            val hs = p.h / 2
            var landed = false
            if (p.y > fl - hs) {
                if (p.vy > 18) hurtProp(p, (p.vy - 18) * 3)
                p.y = fl - hs; p.vy = if (abs(p.vy) > 4) -p.vy * .25f else 0f; p.vx *= .8f; landed = true
            }
            for (o in props) {
                if (o === p || o.dead) continue
                if (o.kind == Prop.Kind.PLATFORM || o.kind.dynamic) {
                    val top = o.y - o.h / 2
                    if (p.vy >= 0 && abs(p.x - o.x) < (p.w + o.w) / 2 - 6 && p.y + hs >= top && p.y + hs <= top + 14 + p.vy) {
                        p.y = top - hs; p.vy = 0f; p.vx *= .85f; landed = true
                    } else if (o.kind.dynamic && abs(p.y - o.y) < (p.h + o.h) / 2 - 4 && abs(p.x - o.x) < (p.w + o.w) / 2) {
                        val push = ((p.w + o.w) / 2 - abs(p.x - o.x)) / 2
                        val s = sign(p.x - o.x).let { if (it == 0f) 1f else it }
                        p.x += s * push; o.x -= s * push
                    }
                }
            }
            if (landed) { p.ang *= .7f; p.av *= .5f }
            if (p.x < p.w / 2) { p.x = p.w / 2; p.vx = -p.vx * .4f }
            if (p.x > width - p.w / 2) { p.x = width - p.w / 2; p.vx = -p.vx * .4f }
            if (p.y < hs) { p.y = hs; p.vy = 0f }
            // fighters push props around, fast props hurt fighters
            for (f in fighters) {
                if (f.dead || f.held) continue
                val dx = f.x - p.x
                val dy = f.y - p.y
                val d = max(hypot(dx, dy), 1f)
                val ov = Geo.R + p.w / 2 - d
                if (ov > 0) {
                    val speed = hypot(p.vx, p.vy)
                    if (speed > 12 && p !in f.hitSet) {
                        damage(f, speed * .8f, null, p.x, 4f, "crate")
                        p.vx *= .3f; p.vy *= .3f
                    }
                    val nx = dx / d
                    val ny = dy / d
                    // fighters standing on crates
                    if (ny < -.6f && f.vy >= 0) {
                        f.y = p.y - p.h / 2 - Geo.R + 2; f.vy = 0f; f.ground = true
                        f.jumpsLeft = maxJumps(f) - 1
                    } else {
                        f.x += nx * ov * .6f; p.x -= nx * ov * .4f
                        p.vx -= nx * .6f
                    }
                }
            }
        }
    }

    /** keep everything inside if the canvas changed size; x scales, y stays relative to the floor */
    fun resize(w: Float, h: Float) {
        if (w == width && h == height) return
        val kx = w / width
        val oldFloor = floorY
        width = w; height = h
        val dy = floorY - oldFloor
        for (f in fighters) {
            f.x = (f.x * kx).coerceIn(Geo.R, w - Geo.R)
            f.y = (f.y + dy).coerceIn(Geo.R, floorY - Geo.R); f.prevY = f.y
        }
        for (p in props) { p.x *= kx; p.y += dy }
        for (d in debris) { d.x *= kx; d.y += dy }
        for (p in projectiles) { p.x *= kx; p.y += dy }
    }

    companion object {
        fun segDist(px: Float, py: Float, a: Vec, b: Vec): Float {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val l = (dx * dx + dy * dy).let { if (it == 0f) 1f else it }
            val t = (((px - a.x) * dx + (py - a.y) * dy) / l).coerceIn(0f, 1f)
            return hypot(px - a.x - dx * t, py - a.y - dy * t)
        }

    }
}
