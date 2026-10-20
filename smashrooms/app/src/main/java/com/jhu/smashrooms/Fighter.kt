package com.jhu.smashrooms

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/** a timed action. tick runs every frame from t=0 until t=frames-1 (or until the move cancels itself). */
class Move(
    val name: String,
    val frames: Int,
    /** can't act again until landing (up specials) */
    val helpless: Boolean = false,
    /** ends the moment you land (aerials, air specials) */
    val landCancel: Boolean = false,
    /** keep normal air drift during the move */
    val drift: Boolean = true,
    val cooldown: Int = 0,
    val desc: String = "",
    val tick: (f: Fighter, b: Battle, t: Int) -> Unit,
)

class Fighter(val def: Def, val slot: Int, val human: Boolean, var stocks: Int) {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var facing = 1
    var grounded = false
    var ground: Plat? = null
    var jumps = 0
    var dmg = 0f
    var fear = 0f

    var move: Move? = null
    var moveT = 0
    var group = 0
    var helpless = false
    var noGrav = false
    var hitstun = 0
    var hitlag = 0
    var tumble = false
    var invuln = 0
    var intangible = false
    var shield = 100f
    var shielding = false
    var shieldBreak = 0
    var landLag = 0
    var dodge = 0
    var dead = false
    var respawn = 0
    var lastHitBy: Fighter? = null
    var kos = 0
    var falls = 0
    var place = 0

    // status effects from specials
    var stun = 0
    var curse = 0
    var curseGrace = 0
    var curseBy: Fighter? = null
    var halt = 0
    var proceed = 0
    var haltBy: Fighter? = null
    var reverse = 0
    var invis = 0
    var armor = 0
    var counter = 0
    var powerT = 0
    var power = 1f
    var speedT = 0
    var speed = 1f
    var guardT = 0
    var guard = 1f
    var bonus = 1f
    /** scratch space for whatever the current move needs */
    var m0 = 0f
    var m1 = 0f
    var m2 = 0f
    var mTarget: Fighter? = null
    var face = 0
    val cool = IntArray(5)
    var pad = Pad()

    // purely visual
    var flash = 0
    var squash = 0f
    var spin = 0f
    var walkT = 0f
    var lean = 0f
    var trail = 0

    val w get() = def.w
    val h get() = def.h
    val cy get() = y - h / 2f
    val alive get() = !dead && stocks > 0
    val busy get() = move != null || hitstun > 0 || stun > 0 || hitlag > 0 || landLag > 0 || dodge > 0 || shieldBreak > 0

    fun start(m: Move, b: Battle, slotIdx: Int = -1) {
        if (slotIdx >= 0) {
            if (cool[slotIdx] > 0) return
            cool[slotIdx] = m.cooldown
        }
        move = m
        moveT = 0
        noGrav = false
        m0 = 0f; m1 = 0f; m2 = 0f; mTarget = null
        group = b.nextGroup()
        if (m.helpless) helpless = true
    }

    fun endMove() {
        move = null
        noGrav = false
        intangible = false
    }

    fun update(p: Pad, b: Battle) {
        pad = p
        if (dead) return
        if (hitlag > 0) { hitlag--; return }
        if (flash > 0) flash--
        if (trail > 0) trail--
        for (i in cool.indices) if (cool[i] > 0) cool[i]--
        if (invuln > 0) invuln--
        if (invis > 0) invis--
        if (armor > 0) armor--
        if (counter > 0) counter--
        if (reverse > 0) { reverse--; p.x = -p.x }
        if (powerT > 0 && --powerT == 0) power = 1f
        if (speedT > 0 && --speedT == 0) speed = 1f
        if (guardT > 0 && --guardT == 0) guard = 1f
        squash *= 0.85f
        lean *= 0.8f
        statusChecks(p, b)

        if (!shielding && shield < 100f) shield = min(100f, shield + 0.25f)
        noGrav = noGrav && move != null

        when {
            hitstun > 0 -> {
                hitstun--
                // a little di so getting launched isn't totally out of your hands
                vx += p.x * 0.12f
                vx -= sign(vx) * min(abs(vx), 0.35f)
                spin += vx * 1.5f
                if (hitstun == 0) tumble = false
            }
            stun > 0 || shieldBreak > 0 -> {
                if (stun > 0) stun--
                if (shieldBreak > 0) shieldBreak--
                friction(0.7f)
            }
            landLag > 0 -> { landLag--; friction(0.8f) }
            dodge > 0 -> {
                dodge--
                if (grounded) friction(0.6f)
                if (dodge == 0) intangible = false
            }
            move != null -> {
                val m = move!!
                if (m.drift && !grounded) airDrift(p)
                if (grounded && m.drift) friction(0.85f)
                m.tick(this, b, moveT)
                moveT++
                if (move === m && moveT >= m.frames) endMove()
            }
            else -> control(p, b)
        }

        // gravity
        if (!noGrav && !grounded) {
            val fall = if (hitstun > 0) 0.9f else def.grav
            vy += fall
            val cap = if (p.y > 0.7f && hitstun == 0 && vy > 0) def.fall * 1.45f else def.fall
            if (hitstun == 0 && vy > cap) vy = cap
        }
        b.stage.collide(this, b, p.y > 0.6f && hitstun == 0)
        spin *= if (hitstun > 0) 1f else 0.8f
        if (hitstun == 0) spin = 0f
    }

    private fun statusChecks(p: Pad, b: Battle) {
        if (curse > 0) {
            curse--
            if (curseGrace > 0) curseGrace--
            else if (p.anything() && hitstun == 0) {
                curse = 0
                curseBy?.let { Moves.a90Jumpscare(it, this, b) }
            }
        }
        if (halt > 0) {
            halt--
            if ((abs(p.x) > 0.35f || p.jmp || abs(vx) > 3f) && halt % 8 == 0) b.statusHit(haltBy, this, 3f)
            if (halt == 0) { proceed = 60; Assets.play("a90b_proceed", 0.6f) }
        } else if (proceed > 0) {
            proceed--
            if (abs(p.x) < 0.3f && abs(vx) < 2f && grounded && proceed % 10 == 0) b.statusHit(haltBy, this, 3f)
        }
    }

    fun friction(k: Float) {
        vx *= k
        if (abs(vx) < 0.2f) vx = 0f
    }

    private fun airDrift(p: Pad) {
        val top = def.air * speed
        if (abs(p.x) > 0.2f) {
            vx += p.x * def.airAcc
            if (abs(vx) > top) vx = sign(vx) * max(top, abs(vx) - 0.6f)
        } else vx *= 0.97f
    }

    private fun control(p: Pad, b: Battle) {
        // shield / dodges
        if (p.shd && grounded) {
            if (abs(p.x) > 0.7f && p.shdP || (p.flickX != 0 && shielding)) {
                shielding = false
                dodge = 22
                intangible = true
                vx = sign(p.x) * 14f
                return
            }
            shielding = true
            shield -= 0.35f
            if (shield <= 0f) {
                shielding = false
                shieldBreak = 160
                vy = -16f
                grounded = false
                b.text(x, y - h - 20, "shield broke", 0xffff6b6b.toInt())
                Assets.play("glitch", 0.7f)
            }
            friction(0.6f)
            if (p.jmpP) { shielding = false; jump() }
            return
        }
        shielding = false
        if (p.shdP && !grounded) {
            dodge = 26
            intangible = true
            vx = p.x * 10f; vy = p.y * 10f
            return
        }

        if (p.supP && fear >= 100f) {
            fear = 0f
            b.superFlash(this)
            start(def.sup, b)
            return
        }
        if (p.spcP) {
            val i = dirIndex(p)
            if (abs(p.x) > 0.3f && i == 1) facing = sign(p.x).toInt()
            val m = def.specials[i]
            if (cool[i] == 0 && !(helpless && i == 2)) { start(m, b, i); return }
        }
        if (p.atkP) {
            if (abs(p.x) > 0.3f && grounded) facing = sign(p.x).toInt()
            start(Normals.pick(this, p), b)
            return
        }

        if (p.jmpP && (grounded || jumps > 0)) { jump(); return }

        if (grounded) {
            val top = def.speed * speed
            if (abs(p.x) > 0.2f) {
                facing = sign(p.x).toInt()
                val target = p.x * top
                vx += (target - vx) * 0.35f
                walkT += abs(vx) * 0.06f
            } else friction(0.75f)
        } else {
            airDrift(p)
            if (p.flickY > 0 && vy > -2f) vy = def.fall * 1.45f
        }
    }

    fun jump() {
        if (grounded) {
            vy = -def.jump
            jumps = def.jumps - 1
            grounded = false
            squash = -0.25f
        } else if (jumps > 0) {
            vy = -def.jump * 0.92f
            jumps--
            spin = 360f * facing
            squash = -0.2f
        }
    }

    fun land(b: Battle) {
        if (!grounded) {
            jumps = def.jumps - 1
            helpless = false
            squash = 0.25f
            val m = move
            if (m != null && m.landCancel) {
                endMove()
                landLag = 6
            }
            if (hitstun > 0 && vy > 10f) {
                // bounce off the floor when you get spiked into it
                vy = -vy * 0.45f
                b.dust(x, y, 6)
                return
            }
        }
        grounded = true
    }

    fun dirIndex(p: Pad): Int = when {
        p.y < -0.5f && abs(p.y) > abs(p.x) -> 2
        p.y > 0.5f && abs(p.y) > abs(p.x) -> 3
        abs(p.x) > 0.3f -> 1
        else -> 0
    }

    fun nearestFoe(b: Battle): Fighter? =
        b.fighters.filter { it !== this && it.alive && !it.dead }.minByOrNull { abs(it.x - x) + abs(it.y - y) * 0.7f }

    fun faceToward(o: Fighter?) {
        if (o != null && abs(o.x - x) > 2f) facing = if (o.x > x) 1 else -1
    }

    fun reset(px: Float, py: Float) {
        x = px; y = py; vx = 0f; vy = 0f
        dmg = 0f
        move = null; hitstun = 0; hitlag = 0; stun = 0; curse = 0; halt = 0; proceed = 0; reverse = 0
        invis = 0; armor = 0; counter = 0; landLag = 0; dodge = 0; shieldBreak = 0
        tumble = false; helpless = false; intangible = false; shield = 100f
        jumps = def.jumps - 1
        grounded = false
        dead = false
    }
}
