package com.patternbox.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import com.patternbox.core.addDot
import com.patternbox.core.dotX
import com.patternbox.core.dotY

/** a 3x3 lock screen. drawing a pattern shorter than minDots is ignored, like a wrong unlock */
@Composable
fun PatternPad(
    pattern: List<Int>,
    minDots: Int,
    lineColor: Color,
    onPattern: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val temp = remember { mutableStateListOf<Int>() }
    var drawing by remember { mutableStateOf(false) }
    var cursor by remember { mutableStateOf(Offset.Zero) }
    val haptic = LocalHapticFeedback.current
    val onPat by rememberUpdatedState(onPattern)
    val dotColor = MaterialTheme.colorScheme.outline
    val activeDot = MaterialTheme.colorScheme.onSurface
    val bad = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .pointerInput(minDots) {
                fun dotPos(i: Int) = Offset(size.width * (dotX(i) * 2 + 1) / 6f, size.height * (dotY(i) * 2 + 1) / 6f)
                fun hit(p: Offset): Int {
                    for (i in 0..8) if ((dotPos(i) - p).getDistance() < size.width * .13f) return i
                    return -1
                }
                fun touch(p: Offset) {
                    cursor = p
                    val h = hit(p)
                    if (h >= 0 && h !in temp) {
                        val l = temp.toMutableList().apply { addDot(h) }
                        temp.clear(); temp.addAll(l)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    temp.clear(); drawing = true
                    touch(down.position)
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                        c.consume()
                        if (!c.pressed) break
                        touch(c.position)
                    }
                    drawing = false
                    if (temp.size >= minDots) onPat(temp.toList())
                    else if (temp.isNotEmpty()) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
    ) {
        val cell = size.width / 3f
        fun pos(i: Int) = Offset(cell * (dotX(i) + .5f), cell * (dotY(i) + .5f))
        val pat: List<Int> = if (drawing) temp else pattern
        val color = if (!drawing && pat.size < minDots) bad else lineColor
        if (pat.isNotEmpty()) {
            val path = Path().apply {
                moveTo(pos(pat[0]).x, pos(pat[0]).y)
                for (i in 1 until pat.size) lineTo(pos(pat[i]).x, pos(pat[i]).y)
                if (drawing) lineTo(cursor.x, cursor.y)
            }
            drawPath(path, color.copy(alpha = .9f), style = Stroke(cell * .12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        for (i in 0..8) {
            val on = i in pat
            drawCircle(if (on) activeDot else dotColor, if (on) cell * .11f else cell * .07f, pos(i))
            if (on && pat[0] == i) drawCircle(activeDot, cell * .2f, pos(i), style = Stroke(cell * .03f))
        }
    }
}
