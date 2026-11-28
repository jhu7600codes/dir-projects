package app.krala.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos

/**
 * Some providers (gemini especially) stream in big lumps. This types the text out evenly: it
 * catches up with what has arrived over roughly a third of a second, never slower than 50
 * characters a second. Text that was already complete when it first appeared shows at once.
 */
@Composable
fun rememberSmoothText(key: Any, target: String, streaming: Boolean): String {
    val t by rememberUpdatedState(target)
    var shown by remember(key) { mutableIntStateOf(if (streaming) 0 else Int.MAX_VALUE) }
    LaunchedEffect(key) {
        var last = 0L
        var carry = 0f
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceIn(0f, 0.1f)
                last = now
                val len = t.length
                if (shown < len) {
                    val behind = len - shown
                    carry += maxOf(50f, behind / 0.35f) * dt
                    val step = carry.toInt()
                    if (step > 0) {
                        carry -= step
                        shown = minOf(len, shown + step)
                    }
                } else {
                    carry = 0f
                    if (shown > len) shown = len
                }
            }
        }
    }
    return if (shown >= t.length) t else t.substring(0, shown)
}
