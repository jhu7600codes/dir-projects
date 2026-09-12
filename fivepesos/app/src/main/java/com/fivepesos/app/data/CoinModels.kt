package com.fivepesos.app.data

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.fivepesos.app.R

/** Which face of the coin is currently showing. */
enum class Face { HEADS, TAILS }

/** Where the flip/spin state machine currently is. */
enum class FlipPhase { IDLE, FLIPPING, RESULT }

/** Which "Your Own Coin" slot a picked/downloaded image should fill. */
enum class ImageTarget { HEADS, TAILS }

/**
 * How a skin's two faces are actually drawn. [Photo] renders bundled bitmap
 * assets (the real 5 Pesos coin); [Engraved] is a small procedural coin --
 * gradient disc, rim rings, a symbol and a caption -- used for the bundled
 * "themed" skins so they don't need bespoke art; [Custom] defers to
 * whatever images the user picked in Settings ("Your Own Coin").
 */
sealed interface CoinArt {
    data class Photo(
        @DrawableRes val headsRes: Int,
        @DrawableRes val tailsRes: Int,
    ) : CoinArt

    data class Engraved(
        val rimColor: Color,
        val faceColorLight: Color,
        val faceColorDark: Color,
        val headsSymbol: String,
        val headsCaption: String,
        val tailsSymbol: String,
        val tailsCaption: String,
    ) : CoinArt

    data object Custom : CoinArt
}

data class CoinSkin(
    val id: String,
    val displayName: String,
    val art: CoinArt,
)

/**
 * One manufacturer's take on a CR2032 -- the "CR2032" skin isn't a single
 * fixed photo, it's this whole list, picked from a sub-picker under it in
 * Settings once it's selected. [tailsRes] is shared (`coin_cr2032_blank_tails`)
 * for every brand except KTS: a real, brand-specific photo of the blank
 * negative face wasn't available for the others, so they use one plain
 * "blank metal" render instead of pretending to be a specific photo --
 * true to how featureless these backs actually are in real life.
 */
data class CoinBrand(
    val id: String,
    val displayName: String,
    @DrawableRes val headsRes: Int,
    @DrawableRes val tailsRes: Int,
)

val Cr2032Brands: List<CoinBrand> = listOf(
    CoinBrand(
        id = "kts",
        displayName = "KTS",
        headsRes = R.drawable.coin_cr2032_kts_heads,
        tailsRes = R.drawable.coin_cr2032_kts_tails,
    ),
    CoinBrand(
        id = "panasonic",
        displayName = "Panasonic",
        headsRes = R.drawable.coin_cr2032_panasonic_heads,
        tailsRes = R.drawable.coin_cr2032_blank_tails,
    ),
    CoinBrand(
        id = "duracell",
        displayName = "Duracell",
        headsRes = R.drawable.coin_cr2032_duracell_heads,
        tailsRes = R.drawable.coin_cr2032_blank_tails,
    ),
    CoinBrand(
        id = "maxell",
        displayName = "Maxell",
        headsRes = R.drawable.coin_cr2032_maxell_heads,
        tailsRes = R.drawable.coin_cr2032_blank_tails,
    ),
    CoinBrand(
        id = "toshiba",
        displayName = "Toshiba",
        headsRes = R.drawable.coin_cr2032_toshiba_heads,
        tailsRes = R.drawable.coin_cr2032_blank_tails,
    ),
)

val BuiltInSkins: List<CoinSkin> = listOf(
    CoinSkin(
        id = "pesos5",
        displayName = "5 Pesos",
        art = CoinArt.Photo(
            headsRes = R.drawable.coin_5pesos_heads,
            tailsRes = R.drawable.coin_5pesos_tails,
        ),
    ),
    CoinSkin(
        id = "ruble2014",
        displayName = "2014 Ruble",
        art = CoinArt.Photo(
            headsRes = R.drawable.coin_ruble2014_heads,
            tailsRes = R.drawable.coin_ruble2014_tails,
        ),
    ),
    CoinSkin(
        // .art here is only the default/placeholder (the first brand) --
        // CoinViewModel overwrites it each state update to whichever
        // CoinBrand is currently selected. See CoinViewModel.kt.
        id = "cr2032",
        displayName = "CR2032",
        art = CoinArt.Photo(
            headsRes = Cr2032Brands.first().headsRes,
            tailsRes = Cr2032Brands.first().tailsRes,
        ),
    ),
    CoinSkin(
        id = "goldstar",
        displayName = "Gold Star",
        art = CoinArt.Engraved(
            rimColor = Color(0xFF9C6B00),
            faceColorLight = Color(0xFFFFEBAE),
            faceColorDark = Color(0xFFE3B23C),
            headsSymbol = "★",
            headsCaption = "LUCKY ONE",
            tailsSymbol = "☆",
            tailsCaption = "TRY AGAIN",
        ),
    ),
    CoinSkin(
        id = "custom",
        displayName = "Your Own Coin",
        art = CoinArt.Custom,
    ),
)
