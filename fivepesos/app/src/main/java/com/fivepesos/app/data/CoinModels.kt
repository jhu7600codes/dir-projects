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
