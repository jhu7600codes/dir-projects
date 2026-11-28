package app.krala.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

enum class StarMode { IDLE, LISTENING, THINKING, TALKING }

/** the krala star from the logo svg, centred on (0,0) in its 380 unit space */
private val starPath = Path().apply {
    moveTo(0f, -150f)
    quadraticTo(35f, -35f, 150f, 0f)
    quadraticTo(35f, 35f, 0f, 150f)
    quadraticTo(-35f, 35f, -150f, 0f)
    quadraticTo(-35f, -35f, 0f, -150f)
    close()
}
private val starBrush = Brush.linearGradient(
    0f to Color(0xFFB3121F), 0.5f to Color(0xFF5A0912), 1f to Color(0xFF000000),
    start = Offset(-130f, -130f), end = Offset(130f, 130f),
)
private val ghostBrush = Brush.linearGradient(
    0f to Color(0xFFE0303D), 1f to Color(0xFF5A0912),
    start = Offset(-130f, -130f), end = Offset(130f, 130f),
)

/**
 * The living logo. Every parameter eases toward its target each frame, so switching between
 * modes never jumps:
 * - idle: slow breathing
 * - listening: swells with the mic level, glow pulses
 * - thinking: spins up smoothly, leaves fading afterimages and a trail of tiny orbiting stars
 * - talking: slow side to side tilt with a little bob, squash and motion ghosts; [energy]
 *   (0..1) makes it livelier for excited tones
 */
@Composable
fun KralaStar(mode: StarMode, size: Dp, modifier: Modifier = Modifier, level: Float = 0f, energy: Float = 0.5f) {
    val m by rememberUpdatedState(mode)
    val lvl by rememberUpdatedState(level)
    val en by rememberUpdatedState(energy)
    val glowColor = MaterialTheme.colorScheme.primary

    var angle by remember { mutableFloatStateOf(0f) }      // spin, degrees
    var tilt by remember { mutableFloatStateOf(0f) }       // talking sway, degrees
    var scale by remember { mutableFloatStateOf(1f) }
    var bob by remember { mutableFloatStateOf(0f) }        // vertical offset, star units
    var glow by remember { mutableFloatStateOf(0f) }
    var spin by remember { mutableFloatStateOf(0f) }       // 0..1 how much it's spinning
    var trail by remember { mutableStateOf(listOf<Pair<Float, Float>>()) } // (angle, tilt) history

    LaunchedEffect(Unit) {
        var last = 0L
        var speed = 0f       // deg / s
        var t = 0.0          // seconds
        var swayPhase = 0.0
        var sinceSample = 0f
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceIn(0f, 0.05f)
                last = now
                t += dt
                fun ease(cur: Float, target: Float, rate: Float) = cur + (target - cur) * (1f - exp(-rate * dt))

                val thinking = m == StarMode.THINKING
                val talking = m == StarMode.TALKING
                val listening = m == StarMode.LISTENING

                // spin: ease the speed, not the angle, so it winds up and down like a real object
                speed = ease(speed, if (thinking) 300f else 0f, if (thinking) 2.2f else 3.0f)
                angle += speed * dt
                if (!thinking && speed < 40f) {
                    // settle on the nearest resting pose (the star looks the same every 90°)
                    val rest = (angle / 90f).roundToInt() * 90f
                    angle = ease(angle, rest, 4f)
                }
                angle %= 360f
                spin = (speed / 300f).coerceIn(0f, 1f)

                // talking sway: slow tilt both ways, faster and wider with energy
                val swaySpeed = 2 * PI / (2.6 - 1.1 * en)
                swayPhase += dt * swaySpeed * (if (talking) 1.0 else 0.4)
                val swayTarget = if (talking) (10f + 8f * en) * sin(swayPhase).toFloat() else 0f
                tilt = ease(tilt, swayTarget, 5f)
                val bobTarget = if (talking) (6f + 6f * en) * sin(swayPhase * 2).toFloat() else 0f
                bob = ease(bob, bobTarget, 6f)

                val breathe = 1f + 0.035f * sin(t * 2 * PI / 3.2).toFloat()
                val scaleTarget = when {
                    listening -> 1f + 0.18f * lvl
                    talking -> 1.02f + 0.03f * abs(sin(swayPhase * 2)).toFloat() * (0.5f + en)
                    thinking -> 0.92f
                    else -> breathe
                }
                scale = ease(scale, scaleTarget, if (listening) 14f else 6f)
                glow = ease(glow, when { listening -> 0.35f + 0.65f * lvl; thinking -> 0.55f; talking -> 0.45f; else -> 0.12f }, 5f)

                sinceSample += dt
                if (sinceSample > 0.028f) {
                    sinceSample = 0f
                    trail = (listOf(angle to tilt) + trail).take(9)
                }
            }
        }
    }

    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / 380f
        val c = center
        // glow
        drawCircle(
            Brush.radialGradient(
                listOf(glowColor.copy(alpha = 0.32f * glow), Color.Transparent),
                center = c, radius = this.size.minDimension * 0.5f,
            ),
            radius = this.size.minDimension * 0.5f,
            center = c,
        )
        // afterimages: spin ghosts while thinking, motion ghosts while swaying
        val motion = (abs(tilt) / 18f).coerceIn(0f, 1f) * 0.6f
        val ghostAmount = maxOf(spin, motion)
        if (ghostAmount > 0.02f) {
            trail.drop(1).forEachIndexed { i, (a, tl) ->
                val k = 1f - (i + 1) / trail.size.toFloat()
                drawStar(c, unit * scale * (1f - 0.04f * (i + 1)), a + tl, bob, ghostBrush, alpha = 0.22f * k * ghostAmount)
            }
        }
        // the little star trail orbiting behind the spin
        if (spin > 0.02f) {
            val r = this.size.minDimension * 0.42f
            for (i in 0 until 7) {
                val a = Math.toRadians((angle - 90f - i * 16f).toDouble())
                val p = Offset(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat())
                val s = unit * (0.11f - i * 0.011f)
                drawStar(p, s, angle * 2 + i * 30f, 0f, ghostBrush, alpha = spin * (1f - i / 7f) * 0.9f)
            }
        }
        drawStar(c, unit * scale, angle + tilt, bob, starBrush, alpha = 1f)
    }
}

private fun DrawScope.drawStar(center: Offset, unit: Float, degrees: Float, bob: Float, brush: Brush, alpha: Float) {
    translate(center.x, center.y + bob * unit) {
        rotate(degrees, pivot = Offset.Zero) {
            scale(unit, unit, pivot = Offset.Zero) {
                drawPath(starPath, brush, alpha = alpha.coerceIn(0f, 1f))
            }
        }
    }
}
