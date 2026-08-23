package com.msfviewer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.msfviewer.parser.MesfColorTable
import com.msfviewer.parser.MesfLayout
import kotlin.math.ceil

// Matches the reference web renderer's checkerboard behind the image, so
// transparent units (spaces, orphaned punctuation) read as "no color"
// rather than as invisible/missing squares.
private val CheckerLight = Color(0xFF1A1D1B)
private val CheckerDark = Color(0xFF0F1210)
private const val CHECKER_SQUARES_ACROSS = 8

/**
 * Renders a computed [MesfLayout] by drawing each unit as a plain filled
 * square -- the whole "image" the spec describes. Transparent units
 * (colorNumber == null) simply aren't painted, letting the checkerboard
 * behind them show through. The canvas is scaled so the layout's
 * base-unit grid fills the available width, at whatever height that
 * implies.
 */
@Composable
fun MesfImageCanvas(layout: MesfLayout, modifier: Modifier = Modifier) {
    if (layout.widthUnits == 0 || layout.heightUnits == 0) return

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)),
    ) {
        val aspect = layout.widthUnits.toFloat() / layout.heightUnits.toFloat()
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect),
        ) {
            val basePx = size.width / layout.widthUnits
            drawCheckerboard(size.width, size.height)
            for (placed in layout.placedUnits) {
                val colorNumber = placed.unit.colorNumber ?: continue
                drawUnit(
                    color = Color(MesfColorTable.colorFor(colorNumber) or 0xFF000000.toInt()),
                    x = placed.x * basePx,
                    y = placed.y * basePx,
                    sizePx = placed.unit.size * basePx,
                )
            }
        }
    }
}

private fun DrawScope.drawCheckerboard(width: Float, height: Float) {
    val square = width / CHECKER_SQUARES_ACROSS
    if (square <= 0f) return
    val cols = ceil(width / square).toInt()
    val rows = ceil(height / square).toInt()
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val isLight = (row + col) % 2 == 0
            drawRect(
                color = if (isLight) CheckerLight else CheckerDark,
                topLeft = Offset(col * square, row * square),
                size = Size(square, square),
            )
        }
    }
}

private fun DrawScope.drawUnit(color: Color, x: Float, y: Float, sizePx: Float) {
    drawRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(sizePx, sizePx),
    )
}
