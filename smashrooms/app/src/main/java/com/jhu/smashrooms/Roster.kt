package com.jhu.smashrooms

/** how the sprite gets animated on top of the shared squash/lean/spin */
enum class Look { STILL, BOB, JITTER, FACES, FLOAT, PULSE }

class Def(
    val id: String,
    val name: String,
    val blurb: String,
    val sprite: String,
    val look: Look,
    val color: Int,
    val w: Float,
    val h: Float,
    val weight: Float,
    val speed: Float,
    val air: Float,
    val airAcc: Float,
    val jump: Float,
    val jumps: Int,
    val grav: Float,
    val fall: Float,
    reach: Float,
    pow: Float,
    spd: Float,
    armored: Boolean = false,
    kit: Moves.Kit,
) {
    val normals = Normals.build(reach, pow, spd, armored)
    val specials = listOf(kit.n, kit.s, kit.u, kit.d)
    val sup = kit.x

    fun spriteFor(f: Fighter?, frame: Long): String = when {
        // a-60 shows its current face but keeps glitching through the others
        id == "a60" -> "a60_face_${(if (frame % 37 < 4) (frame / 2 % 3).toInt() else f?.face ?: (frame / 30 % 3).toInt()) + 1}"
        id == "a90" && f != null && f.move === specials[0] -> "a90_stop"
        id == "a90b" && f != null && f.move === specials[1] -> "a90b_attack"
        else -> sprite
    }
}

object Roster {
    val ALL = listOf(
        Def(
            "rush", "rush", "fast, loud, goes straight through you", "rush_face", Look.JITTER, 0xffe8e8e8.toInt(),
            w = 110f, h = 120f, weight = 85f, speed = 9.5f, air = 8f, airAcc = 0.9f, jump = 19f, jumps = 2, grav = 0.95f, fall = 16f,
            reach = 1f, pow = 0.95f, spd = 0.85f, kit = Moves.rush(),
        ),
        Def(
            "ambush", "ambush", "rush, but it comes back. again. and again.", "ambush_face", Look.JITTER, 0xff7dffb0.toInt(),
            w = 110f, h = 125f, weight = 95f, speed = 8.5f, air = 7.5f, airAcc = 0.85f, jump = 19f, jumps = 2, grav = 0.95f, fall = 16f,
            reach = 1.05f, pow = 1f, spd = 0.9f, kit = Moves.ambush(),
        ),
        Def(
            "eyes", "eyes", "floaty. don't look at it.", "eyes_face", Look.PULSE, 0xffb57bff.toInt(),
            w = 120f, h = 115f, weight = 80f, speed = 6.5f, air = 7f, airAcc = 0.7f, jump = 16f, jumps = 4, grav = 0.55f, fall = 9f,
            reach = 1.1f, pow = 0.9f, spd = 1f, kit = Moves.eyes(),
        ),
        Def(
            "screech", "screech", "tiny, sneaky, bites", "screech_face", Look.BOB, 0xff6b7bff.toInt(),
            w = 95f, h = 90f, weight = 70f, speed = 9f, air = 8.5f, airAcc = 1f, jump = 18f, jumps = 3, grav = 0.85f, fall = 15f,
            reach = 0.85f, pow = 0.85f, spd = 0.8f, kit = Moves.screech(),
        ),
        Def(
            "figure", "figure", "huge, slow, blind. still wins", "figure_body", Look.STILL, 0xffff8a3d.toInt(),
            w = 120f, h = 200f, weight = 140f, speed = 5.5f, air = 5.5f, airAcc = 0.55f, jump = 20f, jumps = 2, grav = 1.05f, fall = 18f,
            reach = 1.35f, pow = 1.3f, spd = 1.2f, armored = true, kit = Moves.figure(),
        ),
        Def(
            "seek", "seek", "runs you down. hands everywhere", "seek_body", Look.STILL, 0xff3a3a3a.toInt(),
            w = 95f, h = 175f, weight = 105f, speed = 8f, air = 7f, airAcc = 0.8f, jump = 20f, jumps = 2, grav = 1f, fall = 17f,
            reach = 1.1f, pow = 1.05f, spd = 0.95f, kit = Moves.seek(),
        ),
        Def(
            "a60", "a-60", "three faces, all angry", "a60_face_1", Look.FACES, 0xffff3b3b.toInt(),
            w = 125f, h = 125f, weight = 95f, speed = 9f, air = 8f, airAcc = 0.9f, jump = 19f, jumps = 2, grav = 0.95f, fall = 16f,
            reach = 1.05f, pow = 1.05f, spd = 0.9f, kit = Moves.a60(),
        ),
        Def(
            "a60b", "a-60b", "has a theme song. it's not nice", "a60b_face", Look.JITTER, 0xffff2a2a.toInt(),
            w = 125f, h = 125f, weight = 100f, speed = 7.5f, air = 7f, airAcc = 0.8f, jump = 19f, jumps = 2, grav = 1f, fall = 16f,
            reach = 1.1f, pow = 1.1f, spd = 1f, kit = Moves.a60b(),
        ),
        Def(
            "a90", "a-90", "STOP. don't move.", "a90_face", Look.JITTER, 0xffff4040.toInt(),
            w = 105f, h = 115f, weight = 90f, speed = 7.5f, air = 7f, airAcc = 0.8f, jump = 18f, jumps = 2, grav = 0.95f, fall = 16f,
            reach = 0.95f, pow = 1f, spd = 0.95f, kit = Moves.a90(),
        ),
        Def(
            "a90b", "a-90b", "halt. proceed. halt. proceed.", "a90b_face", Look.JITTER, 0xffe0e0e0.toInt(),
            w = 105f, h = 130f, weight = 100f, speed = 7.5f, air = 7f, airAcc = 0.8f, jump = 18f, jumps = 2, grav = 1f, fall = 16f,
            reach = 1.1f, pow = 1.1f, spd = 1f, kit = Moves.a90b(),
        ),
        Def(
            "a120", "a-120", "a slow ghost that hides", "a120_face", Look.FLOAT, 0xffc8d4ff.toInt(),
            w = 115f, h = 115f, weight = 85f, speed = 6.5f, air = 7.5f, airAcc = 0.75f, jump = 17f, jumps = 3, grav = 0.6f, fall = 10f,
            reach = 1f, pow = 1f, spd = 1f, kit = Moves.a120(),
        ),
        Def(
            "a200", "a-200", "the happy scribble :)", "a200_face", Look.BOB, 0xffffffff.toInt(),
            w = 120f, h = 110f, weight = 90f, speed = 8f, air = 8f, airAcc = 0.9f, jump = 21f, jumps = 2, grav = 1.05f, fall = 17f,
            reach = 1f, pow = 1f, spd = 0.9f, kit = Moves.a200(),
        ),
        Def(
            "a150", "a-150", "stars and a smile", "a150_face", Look.BOB, 0xffd8b25a.toInt(),
            w = 110f, h = 120f, weight = 90f, speed = 8f, air = 7.5f, airAcc = 0.85f, jump = 19f, jumps = 2, grav = 0.95f, fall = 16f,
            reach = 1f, pow = 0.95f, spd = 0.9f, kit = Moves.a150(),
        ),
        Def(
            "a220", "a-220", "marches. glitches. breaks your controls", "a220", Look.JITTER, 0xff38d9ff.toInt(),
            w = 120f, h = 125f, weight = 115f, speed = 6.5f, air = 6.5f, airAcc = 0.7f, jump = 19f, jumps = 2, grav = 1f, fall = 17f,
            reach = 1.15f, pow = 1.15f, spd = 1.05f, kit = Moves.a220(),
        ),
        Def(
            "doorsman", "doorsman", "the guy from doors. brought his whole inventory", "doorsman", Look.STILL, 0xffc0392b.toInt(),
            w = 95f, h = 170f, weight = 100f, speed = 8.6f, air = 7.8f, airAcc = 0.9f, jump = 20f, jumps = 2, grav = 1f, fall = 16.5f,
            reach = 1.15f, pow = 1.1f, spd = 0.88f, kit = Moves.doorsman(),
        ),
    )
}
