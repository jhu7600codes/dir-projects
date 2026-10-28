package dev.jhu.crossi.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import dev.jhu.crossi.game.BossTwist
import dev.jhu.crossi.game.GameState
import dev.jhu.crossi.game.Status

/**
 * The board, drawn on one canvas (cheap even at 15x15). Grids 11x11 and up can be
 * pinch-zoomed and panned.
 */
@Composable
fun CrosswordGrid(
    state: GameState,
    highlightCrossing: Boolean,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val cellColor = if (dark) colors.surfaceContainerHighest else Color.White
    val blockColor = if (dark) Color.Black else Color(0xFF1B1B1F)
    val lineColor = if (dark) Color.Black else Color(0xFF1B1B1F)
    val letterColor = colors.onSurface
    val wordColor = colors.primaryContainer
    val crossColor = colors.tertiaryContainer.copy(alpha = 0.6f)
    val cursorColor = colors.primary
    val onCursor = colors.onPrimary
    val errorColor = colors.error
    val revealColor = colors.tertiary
    val pencilColor = colors.onSurfaceVariant.copy(alpha = 0.7f)
    val fogColor = colors.surfaceVariant
    val pickupColor = colors.secondary

    val measurer = rememberTextMeasurer()
    val lock = rememberVectorPainter(Icons.Filled.Lock)
    val star = rememberVectorPainter(Icons.Filled.Star)

    val n = state.size
    val geo = state.geometry
    val active = state.activeEntry
    val activeCells = geo.entryCells[active].toSet()
    val crossingCells = if (highlightCrossing) {
        geo.entriesThrough(state.cursor).filter { it != active }.flatMap { geo.entryCells[it].toList() }.toSet()
    } else emptySet()
    val pickupCells = state.rules.pickups.keys.mapNotNull { geo.entryCells.getOrNull(it)?.firstOrNull() }.toSet()
    val showNumbers = !state.rules.has(BossTwist.SCRAMBLED_CLUES) || state.status != Status.PLAYING
    val playing = state.status == Status.PLAYING

    val zoomable = n >= 11
    var scale by remember(n) { mutableFloatStateOf(1f) }
    var offset by remember(n) { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(Size.Zero) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 3f)
        val maxX = boxSize.width * (scale - 1) / 2
        val maxY = boxSize.height * (scale - 1) / 2
        offset = Offset(
            (offset.x + pan.x).coerceIn(-maxX, maxX),
            (offset.y + pan.y).coerceIn(-maxY, maxY),
        )
    }

    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clipToBounds()
            .then(if (zoomable) Modifier.transformable(transform) else Modifier),
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .pointerInput(state.size) {
                    detectTapGestures { pos ->
                        val cell = size.width / n.toFloat()
                        val col = (pos.x / cell).toInt().coerceIn(0, n - 1)
                        val row = (pos.y / cell).toInt().coerceIn(0, n - 1)
                        onTap(row * n + col)
                    }
                },
        ) {
            boxSize = size
            val cs = size.width / n
            val letterStyle = TextStyle(fontSize = (cs * 0.55f).toSp(), fontWeight = FontWeight.Bold)
            val numberStyle = TextStyle(fontSize = (cs * 0.24f).toSp(), color = letterColor.copy(alpha = 0.8f))

            for (cell in 0 until n * n) {
                val r = cell / n
                val c = cell % n
                val topLeft = Offset(c * cs, r * cs)
                if (state.isBlock(cell)) {
                    drawRect(blockColor, topLeft, Size(cs, cs))
                    continue
                }
                if (cell !in state.visible) {
                    drawRect(fogColor, topLeft, Size(cs, cs))
                    continue
                }
                val bg = when {
                    playing && cell == state.cursor -> cursorColor
                    playing && cell in activeCells -> wordColor
                    playing && cell in crossingCells -> crossColor
                    else -> cellColor
                }
                drawRect(bg, topLeft, Size(cs, cs))
                if (cell in state.missing && playing) {
                    drawRect(errorColor.copy(alpha = 0.6f), topLeft + Offset(2f, 2f), Size(cs - 4f, cs - 4f), style = Stroke(2f))
                }

                if (showNumbers && geo.numbers[cell] > 0) {
                    drawText(measurer, geo.numbers[cell].toString(), topLeft + Offset(cs * 0.06f, cs * 0.02f), numberStyle)
                }

                val letter = state.letterAt(cell)
                if (cell in state.locked) {
                    val s = cs * 0.45f
                    translate(topLeft.x + (cs - s) / 2, topLeft.y + (cs - s) / 2) {
                        with(lock) { draw(Size(s, s), colorFilter = ColorFilter.tint(letterColor.copy(alpha = 0.6f))) }
                    }
                } else if (letter != null) {
                    val color = when {
                        cell in state.wrong -> errorColor
                        playing && cell == state.cursor -> onCursor
                        cell in state.revealed -> revealColor
                        cell in state.pencil -> pencilColor
                        else -> letterColor
                    }
                    val layout = measurer.measure(letter.toString(), letterStyle.copy(color = color))
                    drawText(
                        layout,
                        topLeft = topLeft + Offset((cs - layout.size.width) / 2, cs * 0.18f + (cs * 0.82f - layout.size.height) / 2),
                    )
                }
                if (cell in state.wrong) {
                    // red slash, like the nyt check marks
                    drawLine(errorColor, topLeft + Offset(cs * 0.15f, cs * 0.85f), topLeft + Offset(cs * 0.85f, cs * 0.15f), strokeWidth = 2f)
                }
                if (cell in state.revealed) {
                    val path = Path().apply {
                        moveTo(topLeft.x + cs * 0.7f, topLeft.y)
                        lineTo(topLeft.x + cs, topLeft.y)
                        lineTo(topLeft.x + cs, topLeft.y + cs * 0.3f)
                        close()
                    }
                    drawPath(path, revealColor)
                }
                if (cell in pickupCells && playing) {
                    val s = cs * 0.3f
                    translate(topLeft.x + cs - s - 1f, topLeft.y + cs - s - 1f) {
                        with(star) { draw(Size(s, s), colorFilter = ColorFilter.tint(pickupColor)) }
                    }
                }
            }
            for (i in 0..n) {
                val p = i * cs
                drawLine(lineColor, Offset(p, 0f), Offset(p, size.height), strokeWidth = 1.5f)
                drawLine(lineColor, Offset(0f, p), Offset(size.width, p), strokeWidth = 1.5f)
            }
        }
    }
}
