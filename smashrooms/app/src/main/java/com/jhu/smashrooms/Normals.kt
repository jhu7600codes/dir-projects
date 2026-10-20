package com.jhu.smashrooms

import kotlin.math.abs
import kotlin.math.sign

/** jabs, tilts, smashes and aerials. same shapes for everyone, scaled by each entity's reach / power / speed. */
class NormalSet(
    val jab: Move, val ftilt: Move, val utilt: Move, val dtilt: Move,
    val fsmash: Move, val usmash: Move, val dsmash: Move,
    val nair: Move, val fair: Move, val bair: Move, val uair: Move, val dair: Move,
)

object Normals {
    fun pick(f: Fighter, p: Pad): Move {
        val n = f.def.normals
        if (f.grounded) {
            return when {
                p.flickX != 0 -> n.fsmash
                p.flickY < 0 -> n.usmash
                p.flickY > 0 -> n.dsmash
                p.y < -0.5f && abs(p.y) > abs(p.x) -> n.utilt
                p.y > 0.5f && abs(p.y) > abs(p.x) -> n.dtilt
                abs(p.x) > 0.3f -> n.ftilt
                else -> n.jab
            }
        }
        return when {
            p.y < -0.5f && abs(p.y) > abs(p.x) -> n.uair
            p.y > 0.5f && abs(p.y) > abs(p.x) -> n.dair
            abs(p.x) > 0.3f -> if (sign(p.x).toInt() == f.facing) n.fair else n.bair
            else -> n.nair
        }
    }

    /** reach scales hitbox size, pow scales damage, spd < 1 is faster */
    fun build(reach: Float, pow: Float, spd: Float, armored: Boolean): NormalSet {
        fun fr(n: Int) = (n * spd).toInt().coerceAtLeast(2)

        fun smashCharge(f: Fighter, t: Int, hold: Int): Boolean {
            if (t == hold && f.pad.atk && f.m0 < 60f) {
                f.m0 += 1f
                f.moveT--
                if (f.m0.toInt() % 6 == 0) f.flash = 3
                return true
            }
            return false
        }

        val jab = Move("jab", fr(16)) { f, b, t ->
            if (t == fr(3)) { b.slash(f, f.w * 0.4f, -f.h * 0.05f, 60f * reach, -40f, 40f); f.lean = 10f }
            if (t in fr(3)..fr(5)) b.hit(f, f.w * 0.5f * reach, -f.h * 0.05f, 70f * reach, 60f, 3f * pow, 25f, 30f, 45f)
        }
        val ftilt = Move("tilt", fr(26)) { f, b, t ->
            if (t == fr(7)) { b.slash(f, f.w * 0.3f, 0f, 80f * reach, -70f, 50f); f.lean = 16f }
            if (t in fr(7)..fr(9)) b.hit(f, f.w * 0.65f * reach, 0f, 90f * reach, 70f, 8f * pow, 30f, 85f, 35f)
        }
        val utilt = Move("up tilt", fr(24)) { f, b, t ->
            if (t == fr(6)) b.slash(f, 0f, -f.h * 0.3f, 80f * reach, -150f, -30f)
            if (t in fr(6)..fr(9)) b.hit(f, 0f, -f.h * 0.55f, f.w + 30f * reach, 80f * reach, 7f * pow, 30f, 80f, 88f)
        }
        val dtilt = Move("low sweep", fr(20)) { f, b, t ->
            if (t == fr(5)) { b.slash(f, f.w * 0.3f, f.h * 0.35f, 70f * reach, 10f, 60f); b.dust(f.x + f.facing * 50f, f.y, 3) }
            if (t in fr(5)..fr(7)) b.hit(f, f.w * 0.6f * reach, f.h * 0.4f, 100f * reach, 40f, 6f * pow, 30f, 60f, 20f)
        }
        val fsmash = Move("smash", fr(44)) { f, b, t ->
            if (armored && t == 0) f.armor = fr(44)
            if (smashCharge(f, 4, 4)) return@Move
            if (t == fr(12)) { f.vx = f.facing * 7f; f.lean = 26f }
            if (t == fr(14)) { b.slash(f, f.w * 0.3f, 0f, 110f * reach, -80f, 60f, 10); b.shake = 4f }
            if (t in fr(14)..fr(17)) b.hit(f, f.w * 0.75f * reach, 0f, 120f * reach, 90f, 15f * pow * (1f + f.m0 / 100f), 40f, 100f, 38f)
        }
        val usmash = Move("up smash", fr(42)) { f, b, t ->
            if (armored && t == 0) f.armor = fr(42)
            if (smashCharge(f, 4, 4)) return@Move
            if (t == fr(12)) { b.slash(f, 0f, -f.h * 0.4f, 110f * reach, -170f, -10f, 10); f.squash = -0.3f }
            if (t in fr(12)..fr(16)) b.hit(f, 0f, -f.h * 0.65f, f.w + 50f * reach, 110f * reach, 14f * pow * (1f + f.m0 / 100f), 40f, 100f, 90f)
        }
        val dsmash = Move("down smash", fr(40)) { f, b, t ->
            if (armored && t == 0) f.armor = fr(40)
            if (smashCharge(f, 4, 4)) return@Move
            if (t == fr(12)) {
                b.slash(f, f.w * 0.3f, f.h * 0.35f, 90f * reach, 0f, 40f, 10)
                b.slash(f, -f.w * 0.3f, f.h * 0.35f, 90f * reach, 140f, 180f, 10)
                b.dust(f.x, f.y, 8)
                f.squash = 0.3f
            }
            if (t in fr(12)..fr(14)) b.hit(f, 0f, f.h * 0.35f, f.w * 2.2f * reach, 50f, 13f * pow * (1f + f.m0 / 100f), 40f, 95f, 25f, radial = true)
        }
        val nair = Move("nair", fr(30), landCancel = true) { f, b, t ->
            if (t == fr(5)) b.slash(f, 0f, 0f, f.h * 0.6f, -180f, 180f, 12)
            if (t in fr(5)..fr(16)) { b.hit(f, 0f, 0f, f.w * 1.3f * reach, f.h * 1.1f, 8f * pow, 25f, 70f, 45f, radial = true); f.spin += 20f * f.facing }
        }
        val fair = Move("fair", fr(30), landCancel = true) { f, b, t ->
            if (t == fr(8)) { b.slash(f, f.w * 0.3f, 0f, 90f * reach, -90f, 70f); f.lean = 18f }
            if (t in fr(8)..fr(11)) b.hit(f, f.w * 0.7f * reach, 0f, 100f * reach, 80f, 10f * pow, 30f, 90f, 40f)
        }
        val bair = Move("bair", fr(30), landCancel = true) { f, b, t ->
            if (t == fr(7)) { b.slash(f, -f.w * 0.3f, 0f, 90f * reach, 110f, 250f); f.lean = -18f }
            if (t in fr(7)..fr(10)) b.hit(f, -f.w * 0.7f * reach, 0f, 100f * reach, 80f, 12f * pow, 35f, 95f, 35f, back = true)
        }
        val uair = Move("uair", fr(28), landCancel = true) { f, b, t ->
            if (t == fr(6)) b.slash(f, 0f, -f.h * 0.35f, 90f * reach, -180f, 0f)
            if (t in fr(6)..fr(10)) b.hit(f, 0f, -f.h * 0.6f, f.w * 1.2f * reach, 90f, 9f * pow, 30f, 85f, 85f)
        }
        val dair = Move("dair", fr(36), landCancel = true) { f, b, t ->
            if (t == fr(10)) { b.slash(f, 0f, f.h * 0.35f, 90f * reach, 20f, 160f); f.squash = 0.2f }
            if (t in fr(10)..fr(14)) b.hit(f, 0f, f.h * 0.6f, f.w * reach, 90f, 12f * pow, 25f, 75f, -70f)
        }
        return NormalSet(jab, ftilt, utilt, dtilt, fsmash, usmash, dsmash, nair, fair, bair, uair, dair)
    }
}
