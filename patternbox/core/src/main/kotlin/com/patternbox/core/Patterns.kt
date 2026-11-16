package com.patternbox.core

import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/** a dot on the 3x3 grid, 0..8 left-to-right, top-to-bottom */
fun dotX(d: Int) = d % 3
fun dotY(d: Int) = d / 3

/** android lock rules: skipping over an unused middle dot grabs it too */
fun MutableList<Int>.addDot(d: Int) {
    if (d !in 0..8 || contains(d)) return
    if (isNotEmpty()) {
        val a = last()
        val sx = dotX(a) + dotX(d)
        val sy = dotY(a) + dotY(d)
        if (sx % 2 == 0 && sy % 2 == 0) {
            val m = (sy / 2) * 3 + sx / 2
            if (!contains(m)) add(m)
        }
    }
    add(d)
}

fun patternOf(vararg dots: Int): List<Int> = mutableListOf<Int>().apply { dots.forEach { addDot(it) } }

fun length(p: List<Int>): Float {
    var l = 0f
    for (i in 1 until p.size) {
        l += hypot((dotX(p[i]) - dotX(p[i - 1])).toFloat(), (dotY(p[i]) - dotY(p[i - 1])).toFloat())
    }
    return l
}

fun turns(p: List<Int>): Int {
    var t = 0
    var prev: Int? = null
    for (i in 1 until p.size) {
        val ang = (atan2(
            (dotY(p[i]) - dotY(p[i - 1])).toFloat(),
            (dotX(p[i]) - dotX(p[i - 1])).toFloat()
        ) * 100).roundToInt()
        if (prev != null && ang != prev) t++
        prev = ang
    }
    return t
}

fun hash(p: List<Int>): Int {
    var h = 7L
    for (d in p) h = (h * 31 + d + 1) and 0xffffffffL
    return (h and 0x7fffffff).toInt()
}

fun randomPattern(minDots: Int, maxDots: Int, rnd: Random = Random.Default): List<Int> {
    val n = rnd.nextInt(minDots, maxDots + 1)
    val p = mutableListOf<Int>()
    var guard = 0
    while (p.size < n && guard++ < 200) p.addDot(rnd.nextInt(9))
    return p
}

enum class Part(val minDots: Int, val maxRandom: Int) {
    BODY(4, 9), WEAPON(2, 6), ARMOUR(2, 6)
}

data class Loadout(val body: List<Int>, val weapon: List<Int>, val armour: List<Int>) {
    operator fun get(part: Part) = when (part) {
        Part.BODY -> body
        Part.WEAPON -> weapon
        Part.ARMOUR -> armour
    }

    fun with(part: Part, p: List<Int>) = when (part) {
        Part.BODY -> copy(body = p)
        Part.WEAPON -> copy(weapon = p)
        Part.ARMOUR -> copy(armour = p)
    }

    val stats: Stats get() = Stats.of(this)
    val autoName: String get() = Names.pick(Names.ADJ, weapon + body) + " " + Names.pick(Names.BODY, body)
    val weaponName: String get() = Names.pick(Names.WEAPON, weapon)
    val armourName: String get() = Names.pick(Names.ARMOUR, armour)

    /** share code, dots numbered 1..9 like a pin: "14789-159-456" */
    fun encode(): String = listOf(body, weapon, armour).joinToString("-") { p -> p.joinToString("") { (it + 1).toString() } }

    companion object {
        fun decode(code: String): Loadout? {
            val parts = code.trim().split("-")
            if (parts.size != 3) return null
            val ps = parts.map { s ->
                val p = mutableListOf<Int>()
                for (c in s) if (c in '1'..'9') p.addDot(c - '1')
                p.toList()
            }
            val l = Loadout(ps[0], ps[1], ps[2])
            return if (Part.values().all { l[it].size >= it.minDots }) l else null
        }

        fun random(rnd: Random = Random.Default) = Loadout(
            randomPattern(4, 9, rnd), randomPattern(2, 6, rnd), randomPattern(2, 6, rnd)
        )

        val DEFAULT_P1 = Loadout(patternOf(0, 3, 6, 7, 8), patternOf(0, 4, 8), patternOf(3, 4, 5))
        val DEFAULT_P2 = Loadout(patternOf(2, 4, 6, 7, 8, 5), patternOf(6, 4, 2, 1), patternOf(0, 1, 2, 5))
    }
}

data class Stats(
    val hp: Int,
    val speed: Float,
    val jump: Float,
    val dmg: Int,
    val swing: Float,
    val def: Float,
    val plates: Int,
) {
    companion object {
        fun of(l: Loadout): Stats {
            val b = l.body
            val w = l.weapon
            val a = l.armour
            val bl = length(b)
            val wl = length(w)
            return Stats(
                hp = (50 + b.size * 9 + bl * 4).roundToInt(),
                speed = min(5.2f, 2.4f + turns(b) * 0.3f) * (1 - a.size * 0.035f),
                jump = 9f + (9 - b.size) * 0.35f - a.size * 0.12f,
                dmg = (4 + wl * 2.4f + w.size * 0.8f).roundToInt(),
                swing = 0.32f + wl * 0.055f,
                def = if (a.size >= 2) min(0.65f, 0.1f + a.size * 0.055f) else 0f,
                plates = max(0, a.size - 1),
            )
        }
    }
}

object Names {
    val BODY = listOf("gerald", "dmitri", "swipey", "lockjaw", "bob", "zig", "patternina", "gleb", "pin-boi", "kevin", "vasya", "the unlocker", "sergei", "doug", "nyusha", "squiggle")
    val ADJ = listOf("angry", "sleepy", "wobbly", "crunchy", "illegal", "shiny", "sus", "feral", "polite", "overclocked", "soggy", "based")
    val WEAPON = listOf("spork", "bonk stick", "noodle blade", "yeet lance", "zigzagger", "pointy boi", "unlock axe", "swipe saber", "pin pike", "lightning fork", "coat hanger", "pool noodle")
    val ARMOUR = listOf("tin foil", "screen protector", "cardboard plate", "rubber case", "fridge door", "spaghetti mail", "bubble wrap", "pizza box", "dish rack")
    val KO = listOf("unlocked.", "wrong pattern.", "try again in 30 seconds", "bricked", "factory reset")
    val GRABBED = listOf("hey!", "put me down", "wheee", "bro", "nooo")

    fun pick(list: List<String>, p: List<Int>) = list[hash(p) % list.size]
}
