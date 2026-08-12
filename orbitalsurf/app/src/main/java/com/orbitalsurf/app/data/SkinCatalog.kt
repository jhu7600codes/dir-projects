package com.orbitalsurf.app.data

import com.orbitalsurf.core.economy.Inventory

/** A skin's visual identity: no art pipeline exists for this project, so every skin is a base/trail color pair, not new geometry. */
data class SkinVisual(val skinId: String, val displayName: String, val baseColor: FloatArray, val trailColor: FloatArray)

/** One entry per skin id referenced anywhere in `:core` (shop items + achievement rewards) plus the default. */
object SkinCatalog {
    val all: List<SkinVisual> = listOf(
        SkinVisual(Inventory.DEFAULT_SKIN_ID, "Classic", rgba(0.95f, 0.95f, 0.95f), rgba(0.95f, 0.95f, 0.95f, 0.8f)),
        SkinVisual("skin_neon_ball", "Neon", rgba(0.20f, 0.90f, 0.90f), rgba(0.20f, 0.90f, 0.90f, 0.8f)),
        SkinVisual("skin_checker_ball", "Checkerboard", rgba(0.15f, 0.15f, 0.20f), rgba(0.80f, 0.80f, 0.85f, 0.8f)),
        SkinVisual("skin_lava_ball", "Lava", rgba(0.95f, 0.35f, 0.15f), rgba(1.00f, 0.60f, 0.20f, 0.8f)),
        SkinVisual("skin_bronze_roller", "Bronze Roller", rgba(0.72f, 0.45f, 0.20f), rgba(0.72f, 0.45f, 0.20f, 0.8f)),
        SkinVisual("skin_silver_roller", "Silver Roller", rgba(0.75f, 0.76f, 0.78f), rgba(0.75f, 0.76f, 0.78f, 0.8f)),
        SkinVisual("skin_gold_roller", "Gold Roller", rgba(1.00f, 0.84f, 0.30f), rgba(1.00f, 0.84f, 0.30f, 0.8f)),
        SkinVisual("skin_plates_hoarder", "Plates Hoarder", rgba(0.85f, 0.75f, 0.35f), rgba(0.85f, 0.75f, 0.35f, 0.8f)),
        SkinVisual("skin_mission_ace", "Mission Ace", rgba(0.35f, 0.65f, 0.95f), rgba(0.35f, 0.65f, 0.95f, 0.8f)),
        SkinVisual("skin_daily_grinder", "Daily Grinder", rgba(0.55f, 0.85f, 0.55f), rgba(0.55f, 0.85f, 0.55f, 0.8f)),
        SkinVisual("skin_appteka_follower", "Appteka Follower", rgba(0.85f, 0.45f, 0.85f), rgba(0.85f, 0.45f, 0.85f, 0.8f)),
        SkinVisual("skin_appteka_supporter", "Appteka Supporter", rgba(1.00f, 0.55f, 0.75f), rgba(1.00f, 0.55f, 0.75f, 0.8f)),
    )

    fun forId(skinId: String): SkinVisual = all.firstOrNull { it.skinId == skinId } ?: all.first()

    private fun rgba(r: Float, g: Float, b: Float, a: Float = 1f): FloatArray = floatArrayOf(r, g, b, a)
}
