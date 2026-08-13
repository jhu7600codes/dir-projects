package com.vaultgame.app.data

import android.graphics.Color
import com.vaultgame.core.economy.SkinCatalog

/**
 * :core only knows skins as opaque ids (see [SkinCatalog]) -- how each one actually looks is
 * purely a rendering concern, so it lives here rather than leaking Android's [Color] type into
 * :core.
 */
data class SkinVisual(
    val beanieColor: Int,
    val jacketColor: Int,
    val jacketAccentColor: Int,
    val shoeColor: Int,
)

object SkinVisuals {
    private val visuals: Map<String, SkinVisual> = mapOf(
        SkinCatalog.DEFAULT_SKIN_ID to SkinVisual(
            beanieColor = Color.parseColor("#E8544B"),
            jacketColor = Color.parseColor("#2B2A4A"),
            jacketAccentColor = Color.parseColor("#FFC145"),
            shoeColor = Color.parseColor("#FBF6EE"),
        ),
        "skin_red_beanie" to SkinVisual(
            beanieColor = Color.parseColor("#C41E3A"),
            jacketColor = Color.parseColor("#161A2E"),
            jacketAccentColor = Color.parseColor("#E8544B"),
            shoeColor = Color.parseColor("#FBF6EE"),
        ),
        "skin_windbreaker" to SkinVisual(
            beanieColor = Color.parseColor("#4CC9A0"),
            jacketColor = Color.parseColor("#FFC145"),
            jacketAccentColor = Color.parseColor("#FF6F3C"),
            shoeColor = Color.parseColor("#161A2E"),
        ),
        "skin_neon_runner" to SkinVisual(
            beanieColor = Color.parseColor("#39FF88"),
            jacketColor = Color.parseColor("#161A2E"),
            jacketAccentColor = Color.parseColor("#39FF88"),
            shoeColor = Color.parseColor("#39FF88"),
        ),
        "skin_neon_legend" to SkinVisual(
            beanieColor = Color.parseColor("#FF3DF2"),
            jacketColor = Color.parseColor("#161A2E"),
            jacketAccentColor = Color.parseColor("#FF3DF2"),
            shoeColor = Color.parseColor("#8CE0FF"),
        ),
        "skin_ghost" to SkinVisual(
            beanieColor = Color.parseColor("#DDE6FF"),
            jacketColor = Color.parseColor("#8895C4"),
            jacketAccentColor = Color.parseColor("#FFFFFF"),
            shoeColor = Color.parseColor("#DDE6FF"),
        ),
        "skin_gold_jacket" to SkinVisual(
            beanieColor = Color.parseColor("#161A2E"),
            jacketColor = Color.parseColor("#FFC145"),
            jacketAccentColor = Color.parseColor("#FFD65A"),
            shoeColor = Color.parseColor("#161A2E"),
        ),
        "skin_platinum" to SkinVisual(
            beanieColor = Color.parseColor("#E5E8EF"),
            jacketColor = Color.parseColor("#B8BFD6"),
            jacketAccentColor = Color.parseColor("#FFFFFF"),
            shoeColor = Color.parseColor("#E5E8EF"),
        ),
        "skin_shadow" to SkinVisual(
            beanieColor = Color.parseColor("#0E0C1A"),
            jacketColor = Color.parseColor("#1C1B33"),
            jacketAccentColor = Color.parseColor("#7A6BC9"),
            shoeColor = Color.parseColor("#0E0C1A"),
        ),
        "skin_jetpack_kid" to SkinVisual(
            beanieColor = Color.parseColor("#5FA8D3"),
            jacketColor = Color.parseColor("#161A2E"),
            jacketAccentColor = Color.parseColor("#5FA8D3"),
            shoeColor = Color.parseColor("#FFC145"),
        ),
    )

    private val default = visuals.getValue(SkinCatalog.DEFAULT_SKIN_ID)

    fun forId(skinId: String): SkinVisual = visuals[skinId] ?: default
}
