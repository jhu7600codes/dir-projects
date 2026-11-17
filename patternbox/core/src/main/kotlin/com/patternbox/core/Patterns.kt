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
    if (maxDots <= 0) return emptyList()
    val n = rnd.nextInt(max(1, minDots), maxDots + 1)
    val p = mutableListOf<Int>()
    var guard = 0
    while (p.size < n && guard++ < 200) p.addDot(rnd.nextInt(9))
    return p
}

enum class Part(val label: String, val minDots: Int, val maxRandom: Int) {
    BODY("body", 4, 9), WEAPON("weapon", 2, 6), ARMOUR("armour", 2, 6), HAT("hat", 0, 4)
}

data class Loadout(
    val body: List<Int>,
    val weapon: List<Int>,
    val armour: List<Int>,
    val hat: List<Int> = emptyList(),
    val weaponType: WeaponType = WeaponType.BLADE,
    val weaponMat: WeaponMat = WeaponMat.STEEL,
    val armourMat: ArmourMat = ArmourMat.STEEL,
    val ability: Ability = Ability.NONE,
) {
    operator fun get(part: Part) = when (part) {
        Part.BODY -> body
        Part.WEAPON -> weapon
        Part.ARMOUR -> armour
        Part.HAT -> hat
    }

    fun with(part: Part, p: List<Int>) = when (part) {
        Part.BODY -> copy(body = p)
        Part.WEAPON -> copy(weapon = p)
        Part.ARMOUR -> copy(armour = p)
        Part.HAT -> copy(hat = p)
    }

    val stats: Stats get() = Stats.of(this)
    val autoName: String get() = Names.pick(Names.ADJ, weapon + body) + " " + Names.pick(Names.BODY, body)
    val weaponName: String
        get() = (if (weaponMat == WeaponMat.STEEL) "" else weaponMat.label + " ") + when (weaponType) {
            WeaponType.BLADE -> Names.pick(Names.WEAPON, weapon)
            WeaponType.HAMMER -> Names.pick(Names.HAMMER, weapon)
            WeaponType.SPEAR -> Names.pick(Names.SPEAR, weapon)
            WeaponType.BOW -> Names.pick(Names.BOW, weapon)
            WeaponType.BOOMERANG -> Names.pick(Names.BOOMERANG, weapon)
            WeaponType.BOMB -> Names.pick(Names.BOMB, weapon)
        }
    val armourName: String get() = (if (armourMat == ArmourMat.STEEL) "" else armourMat.label + " ") + Names.pick(Names.ARMOUR, armour)
    val hatName: String get() = if (hat.isEmpty()) "no hat" else Names.pick(Names.HAT, hat)

    /**
     * share code, dots numbered 1..9 like a pin: "body-weapon-armour-hat-xxxx".
     * the last part is weapon type, weapon material, armour material, ability.
     * old 3 part codes ("14789-159-456") still load.
     */
    fun encode(): String {
        val pats = listOf(body, weapon, armour, hat).joinToString("-") { p -> p.joinToString("") { (it + 1).toString() } }
        return pats + "-" + listOf(weaponType.ordinal, weaponMat.ordinal, armourMat.ordinal, ability.ordinal).joinToString("")
    }

    companion object {
        fun decode(code: String): Loadout? {
            val parts = code.trim().lowercase().split("-")
            if (parts.size != 3 && parts.size != 5) return null
            fun pat(s: String): List<Int> {
                val p = mutableListOf<Int>()
                for (c in s) if (c in '1'..'9') p.addDot(c - '1') else return emptyList()
                return p
            }
            var l = Loadout(pat(parts[0]), pat(parts[1]), pat(parts[2]))
            if (parts.size == 5) {
                l = l.copy(hat = pat(parts[3]))
                val f = parts[4]
                if (f.length != 4 || f.any { !it.isDigit() }) return null
                fun <T> pick(vals: Array<T>, c: Char): T? = vals.getOrNull(c - '0')
                l = l.copy(
                    weaponType = pick(WeaponType.values(), f[0]) ?: return null,
                    weaponMat = pick(WeaponMat.values(), f[1]) ?: return null,
                    armourMat = pick(ArmourMat.values(), f[2]) ?: return null,
                    ability = pick(Ability.values(), f[3]) ?: return null,
                )
            }
            return if (Part.values().all { l[it].size >= it.minDots }) l else null
        }

        fun random(rnd: Random = Random.Default) = Loadout(
            randomPattern(4, 9, rnd), randomPattern(2, 6, rnd), randomPattern(2, 6, rnd),
            if (rnd.nextFloat() < .6f) randomPattern(2, 4, rnd) else emptyList(),
            WeaponType.values().random(rnd), WeaponMat.values().random(rnd),
            ArmourMat.values().random(rnd), Ability.values().random(rnd),
        )

        val DEFAULT_P1 = Loadout(patternOf(0, 3, 6, 7, 8), patternOf(0, 4, 8), patternOf(3, 4, 5), patternOf(0, 1, 2))
        val DEFAULT_P2 = Loadout(patternOf(2, 4, 6, 7, 8, 5), patternOf(6, 4, 2, 1), patternOf(0, 1, 2, 5), weaponType = WeaponType.HAMMER, ability = Ability.DASH)
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
    val kb: Float,
) {
    companion object {
        fun of(l: Loadout): Stats {
            val b = l.body
            val w = l.weapon
            val a = l.armour
            val bl = length(b)
            val wl = length(w)
            val am = l.armourMat
            val wt = l.weaponType
            val baseDef = if (a.size >= 2) min(0.65f, 0.1f + a.size * 0.055f) else 0f
            return Stats(
                hp = (50 + b.size * 9 + bl * 4).roundToInt(),
                speed = max(1.2f, min(5.2f, 2.4f + turns(b) * 0.3f) * (1 - a.size * 0.035f) + am.speed),
                jump = 9f + (9 - b.size) * 0.35f - a.size * 0.12f + am.jump,
                dmg = max(1, ((4 + wl * 2.4f + w.size * 0.8f) * wt.dmg * l.weaponMat.dmg).roundToInt()),
                swing = (0.32f + wl * 0.055f) * wt.swing,
                def = min(0.8f, baseDef * am.def),
                plates = max(0, a.size - 1),
                kb = wt.kb * l.weaponMat.kb,
            )
        }
    }
}

object Names {
    val BODY = listOf("gerald", "dmitri", "swipey", "lockjaw", "bob", "zig", "patternina", "gleb", "pin-boi", "kevin", "vasya", "the unlocker", "sergei", "doug", "nyusha", "squiggle", "boris", "karen", "pixel", "steve", "olga", "bluetooth", "wifi", "motherboard")
    val ADJ = listOf("angry", "sleepy", "wobbly", "crunchy", "illegal", "shiny", "sus", "feral", "polite", "overclocked", "soggy", "based", "haunted", "unpaid", "laggy", "spicy", "damp", "legendary", "confused", "rooted")
    val WEAPON = listOf("spork", "bonk stick", "noodle blade", "yeet lance", "zigzagger", "pointy boi", "unlock axe", "swipe saber", "pin pike", "lightning fork", "coat hanger", "pool noodle")
    val HAMMER = listOf("bonker", "meat tenderizer", "brick on a stick", "gavel of doom", "fridge hammer", "the persuader")
    val SPEAR = listOf("long poke", "selfie stick", "antenna", "chopstick", "toothpick of justice", "usb stick")
    val BOW = listOf("pattern bow", "slingshot", "nerf thing", "rubber band", "pea shooter", "crossbow-ish")
    val BOOMERANG = listOf("comeback", "frisbee", "boomer rang", "return policy", "the ex", "bluetooth disc")
    val BOMB = listOf("pattern bomb", "spicy ball", "mystery box", "notification", "battery 1%", "airpod case")
    val ARMOUR = listOf("tin foil", "screen protector", "cardboard plate", "rubber case", "fridge door", "spaghetti mail", "bubble wrap", "pizza box", "dish rack")
    val HAT = listOf("crown", "traffic cone", "propeller cap", "tiny top hat", "antenna", "party hat", "ushanka", "halo", "lampshade")
    val KO = listOf("unlocked.", "wrong pattern.", "try again in 30 seconds", "bricked", "factory reset", "too many attempts", "battery died", "app not responding")
    val GRABBED = listOf("hey!", "put me down", "wheee", "bro", "nooo", "i can fly", "not the face")
    val TAUNT = listOf("ez", "get unlocked", "skill issue", "gg", "pattern diff", "sit down", "lag", "uninstall", "nice try", "first try")
    val HURT = listOf("ow", "rude", "hey", "my plates", "lag!", "ouch", "bruh")

    fun pick(list: List<String>, p: List<Int>) = list[hash(p) % list.size]
}

/** ready made fighters for the presets list */
object Presets {
    val all: List<Pair<String, Loadout>> = listOf(
        "the toaster" to Loadout(patternOf(0, 2, 8, 6, 0), patternOf(3, 5), patternOf(0, 1, 2), patternOf(1, 4), WeaponType.BLADE, WeaponMat.FIRE, ArmourMat.STEEL, Ability.RAGE),
        "big gleb" to Loadout(patternOf(0, 1, 2, 5, 8, 7, 6, 3, 4), patternOf(0, 4, 8, 5), patternOf(0, 4, 2, 5, 8), patternOf(0, 2), WeaponType.HAMMER, WeaponMat.STEEL, ArmourMat.GOLD, Ability.SLAM),
        "noodle" to Loadout(patternOf(0, 3, 6, 7), patternOf(0, 2, 5), patternOf(3, 4), emptyList(), WeaponType.SPEAR, WeaponMat.BOUNCY, ArmourMat.PAPER, Ability.DOUBLE_JUMP),
        "archer vasya" to Loadout(patternOf(1, 4, 7, 6, 8), patternOf(0, 4, 8), patternOf(3, 4, 5), patternOf(0, 1, 2), WeaponType.BOW, WeaponMat.ICE, ArmourMat.RUBBER, Ability.TELEPORT),
        "the ex" to Loadout(patternOf(2, 4, 6, 3, 0), patternOf(0, 4, 2), patternOf(0, 1), patternOf(6, 4, 8), WeaponType.BOOMERANG, WeaponMat.SHOCK, ArmourMat.STEEL, Ability.DASH),
        "battery 1%" to Loadout(patternOf(0, 1, 2, 5, 8, 7, 6), patternOf(4, 2), patternOf(6, 7, 8), patternOf(1, 0, 2), WeaponType.BOMB, WeaponMat.FIRE, ArmourMat.SPIKY, Ability.BUBBLE),
        "glass cannon" to Loadout(patternOf(4, 1, 0, 3, 6), patternOf(6, 4, 2, 5, 8), patternOf(0, 4), emptyList(), WeaponType.BLADE, WeaponMat.GLASS, ArmourMat.PAPER, Ability.DASH),
        "fridge" to Loadout(patternOf(0, 2, 8, 6, 3, 5), patternOf(1, 4), patternOf(0, 1, 2, 5, 8, 7, 6), emptyList(), WeaponType.HAMMER, WeaponMat.ICE, ArmourMat.STEEL, Ability.HEAL),
        "zappy" to Loadout(patternOf(0, 4, 2, 5, 8), patternOf(0, 4, 3, 7), patternOf(1, 4), patternOf(0, 4, 2), WeaponType.SPEAR, WeaponMat.SHOCK, ArmourMat.RUBBER, Ability.TELEPORT),
        "porcupine" to Loadout(patternOf(3, 4, 5, 8, 6), patternOf(0, 4, 8), patternOf(0, 4, 2, 5, 3), patternOf(1, 3, 5), WeaponType.BLADE, WeaponMat.STEEL, ArmourMat.SPIKY, Ability.BUBBLE),
        "frisbee dad" to Loadout(patternOf(1, 4, 7, 8, 5), patternOf(3, 4, 5), patternOf(0, 1, 2), patternOf(0, 1, 2), WeaponType.BOOMERANG, WeaponMat.BOUNCY, ArmourMat.STEEL, Ability.DOUBLE_JUMP),
        "rage quit" to Loadout(patternOf(6, 3, 0, 4, 8, 5, 2), patternOf(6, 4, 2), patternOf(3, 4, 5, 8), patternOf(0, 4, 2), WeaponType.HAMMER, WeaponMat.FIRE, ArmourMat.STEEL, Ability.RAGE),
        "sniper olga" to Loadout(patternOf(0, 3, 6, 7, 8, 5), patternOf(0, 4, 8, 7, 6), patternOf(1, 2), patternOf(3, 4, 5), WeaponType.BOW, WeaponMat.GLASS, ArmourMat.PAPER, Ability.TELEPORT),
        "goldie" to Loadout(patternOf(0, 1, 2, 4, 6, 7, 8), patternOf(1, 4, 7), patternOf(0, 4, 8, 5, 2), patternOf(0, 4, 2), WeaponType.BLADE, WeaponMat.STEEL, ArmourMat.GOLD, Ability.HEAL),
        "bomb squad" to Loadout(patternOf(0, 3, 4, 5, 8), patternOf(0, 1, 4), patternOf(3, 4), emptyList(), WeaponType.BOMB, WeaponMat.SHOCK, ArmourMat.RUBBER, Ability.DASH),
        "wifi" to Loadout(patternOf(6, 4, 2, 1, 0, 3), patternOf(0, 4, 8, 5), patternOf(6, 7, 8), patternOf(0, 4), WeaponType.SPEAR, WeaponMat.FIRE, ArmourMat.ICE, Ability.SLAM),
    )
}
