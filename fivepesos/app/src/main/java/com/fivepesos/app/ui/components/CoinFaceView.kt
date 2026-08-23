package com.fivepesos.app.ui.components

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fivepesos.app.data.CoinArt
import com.fivepesos.app.data.CoinSkin
import com.fivepesos.app.data.Face

/** Renders one face of [skin] -- dispatches to a bitmap, a drawn coin, or
 * the user's own picked photo, depending on the skin's [CoinArt]. */
@Composable
fun CoinFaceView(
    skin: CoinSkin,
    face: Face,
    customHeadsUri: Uri?,
    customTailsUri: Uri?,
    modifier: Modifier = Modifier,
) {
    when (val art = skin.art) {
        is CoinArt.Photo -> {
            val res = if (face == Face.HEADS) art.headsRes else art.tailsRes
            Image(
                painter = painterResource(id = res),
                contentDescription = null,
                modifier = modifier,
            )
        }

        is CoinArt.Engraved -> EngravedCoinFace(art = art, face = face, modifier = modifier)

        CoinArt.Custom -> {
            val uri = if (face == Face.HEADS) customHeadsUri else customTailsUri
            val bitmap = rememberUriImageBitmap(uri)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = modifier.clip(CircleShape),
                )
            } else {
                CustomCoinPlaceholder(modifier)
            }
        }
    }
}

@Composable
private fun EngravedCoinFace(art: CoinArt.Engraved, face: Face, modifier: Modifier) {
    val symbol = if (face == Face.HEADS) art.headsSymbol else art.tailsSymbol
    val caption = if (face == Face.HEADS) art.headsCaption else art.tailsCaption
    val symbolFontSize = when {
        symbol.length <= 1 -> 64.sp
        symbol.length <= 2 -> 48.sp
        else -> 34.sp
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Background disc + rim, drawn to fill the same box the Text sits in.
        Canvas(modifier = Modifier.matchParentSize()) {
            val radius = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(art.faceColorLight, art.faceColorDark),
                    center = c,
                    radius = radius,
                ),
                radius = radius,
                center = c,
            )
            drawCircle(
                color = art.rimColor,
                radius = radius * 0.98f,
                center = c,
                style = Stroke(width = radius * 0.07f),
            )
            drawCircle(
                color = art.rimColor.copy(alpha = 0.5f),
                radius = radius * 0.8f,
                center = c,
                style = Stroke(width = radius * 0.015f),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = symbol, fontSize = symbolFontSize, fontWeight = FontWeight.Bold, color = art.rimColor)
            Spacer(Modifier.height(6.dp))
            Text(
                text = caption,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = art.rimColor,
                letterSpacing = 2.sp,
            )
        }
    }
}

@Composable
private fun CustomCoinPlaceholder(modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(width = 2.dp, color = Color.White.copy(alpha = 0.45f), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.AddPhotoAlternate,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.75f),
            modifier = Modifier.size(48.dp),
        )
    }
}
