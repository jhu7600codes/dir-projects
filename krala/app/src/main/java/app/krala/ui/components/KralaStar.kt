package app.krala.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
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
 * The living logo. Every value eases toward its target each frame, so modes blend, never jump.
 * - idle: a barely visible breath
 * - listening: a calm spin while circling a point, like it's hanging on to something
 * - thinking: spins in place first, then drifts onto that circle, spinning in waves (fast,
 *   slower, fast again) and leaving fading afterimages along its path
 * - talking: a tiny nod with soft twinkles near the tips; [energy] (0..1) livens it a little
 * No glow or background, just the star.
 */
@Composable
fun KralaStar(mode: StarMode, size: Dp, modifier: Modifier = Modifier, level: Float = 0f, energy: Float = 0.5f) {
    val m by rememberUpdatedState(mode)
    val lvl by rememberUpdatedState(level)
    val en by rememberUpdatedState(energy)

    var angle by remember { mutableFloatStateOf(0f) }   // self spin, degrees
    var tilt by remember { mutableFloatStateOf(0f) }    // talking nod, degrees
    var scale by remember { mutableFloatStateOf(1f) }
    var ox by remember { mutableFloatStateOf(0f) }      // orbit offset, star units
    var oy by remember { mutableFloatStateOf(0f) }
    var ghosts by remember { mutableFloatStateOf(0f) }  // 0..1 afterimage strength
    var twinkle by remember { mutableFloatStateOf(0f) }
    var twinklePhase by remember { mutableFloatStateOf(0f) }
    var trail by remember { mutableStateOf(listOf<FloatArray>()) } // angle, tilt, ox, oy

    LaunchedEffect(Unit) {
        var last = 0L
        var speed = 0f          // self spin, deg/s
        var orbitAngle = 0.0    // radians
        var orbitSpeed = 0f     // rad/s
        var radius = 0f         // star units
        var t = 0.0
        var thinkingFor = 0.0
        var nodPhase = 0.0
        var since = 0f
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceIn(0f, 0.05f)
                last = now
                t += dt
                fun ease(cur: Float, target: Float, rate: Float) = cur + (target - cur) * (1f - exp(-rate * dt))

                val thinking = m == StarMode.THINKING
                val talking = m == StarMode.TALKING
                val listening = m == StarMode.LISTENING
                thinkingFor = if (thinking) thinkingFor + dt else 0.0

                // thinking spins in waves: fast, eases off, picks up again
                val wave = (0.5 + 0.5 * sin(t * 2 * PI / 2.2)).toFloat()
                val spinTarget = when {
                    thinking -> 70f + 330f * wave * wave
                    listening -> 55f
                    else -> 0f
                }
                speed = ease(speed, spinTarget, 2.4f)
                angle += speed * dt
                if (!thinking && !listening && speed < 30f) {
                    // settle on the nearest resting pose (the star looks the same every 90°)
                    angle = ease(angle, (angle / 90f).roundToInt() * 90f, 3f)
                }
                angle %= 360f

                // circling a point: listening right away, thinking after a moment in place
                val radiusTarget = when {
                    listening -> 16f
                    thinking && thinkingFor > 0.9 -> 22f
                    else -> 0f
                }
                radius = ease(radius, radiusTarget, 1.8f)
                orbitSpeed = ease(orbitSpeed, when { thinking -> 1.2f + 2.4f * wave; listening -> 1.1f; else -> 0f }, 2f)
                orbitAngle += orbitSpeed * dt
                ox = radius * cos(orbitAngle).toFloat()
                oy = radius * sin(orbitAngle).toFloat()

                // talking: a nod you barely notice
                nodPhase += dt * 2 * PI / (2.8 - 0.8 * en)
                tilt = ease(tilt, if (talking) (2f + 1.5f * en) * sin(nodPhase).toFloat() else 0f, 4f)
                twinkle = ease(twinkle, if (talking) 0.35f + 0.4f * en else 0f, 3f)

                val scaleTarget = when {
                    listening -> 1f + 0.05f * lvl
                    thinking -> 0.94f
                    talking -> 1f + 0.012f * sin(nodPhase * 2).toFloat()
                    else -> 1f + 0.008f * sin(t * 2 * PI / 4.0).toFloat()
                }
                scale = ease(scale, scaleTarget, if (listening) 10f else 4f)
                ghosts = ease(ghosts, when { thinking -> 0.25f + 0.75f * (speed / 400f).coerceIn(0f, 1f); listening -> 0.35f; talking -> 0.12f; else -> 0f }, 3f)

                since += dt
                if (since > 0.03f) {
                    since = 0f
                    trail = (listOf(floatArrayOf(angle, tilt, ox, oy)) + trail).take(10)
                }
                twinklePhase = t.toFloat()
            }
        }
    }

    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / 380f
        // the star stays inside the canvas while it circles
        val fit = 0.86f
        val c = center
        if (ghosts > 0.02f) {
            trail.drop(1).forEachIndexed { i, g ->
                val k = 1f - (i + 1) / trail.size.toFloat()
                drawStar(
                    Offset(c.x + g[2] * unit, c.y + g[3] * unit), unit * fit * scale * (1f - 0.035f * (i + 1)),
                    g[0] + g[1], 0f, ghostBrush, alpha = 0.2f * k * ghosts,
                )
            }
        }
        val pos = Offset(c.x + ox * unit, c.y + oy * unit)
        if (twinkle > 0.02f) {
            // three tiny sparkles near the tips, fading in and out out of step
            for (i in 0 until 3) {
                val a = Math.toRadians((tilt + 35.0 + i * 120.0))
                val r = 118f * unit * fit
                val p = Offset(pos.x + (r * cos(a)).toFloat(), pos.y + (r * sin(a)).toFloat())
                val pulse = (0.5f + 0.5f * sin(twinklePhase * 3.1f + i * 2.1f))
                drawStar(p, unit * 0.07f * (0.7f + 0.3f * pulse), 45f * i + twinklePhase * 40f, 0f, ghostBrush, alpha = twinkle * pulse * 0.8f)
            }
        }
        drawStar(pos, unit * fit * scale, angle + tilt, 0f, starBrush, alpha = 1f)
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
