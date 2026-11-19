package com.lot.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlin.random.Random

val Bg = Color(0xFF1B1530)
val Card = Color(0xFF2A2148)
val CardHi = Color(0xFF362B5E)
val Dim = Color(0xFFB3A9D6)

private val LotColors = darkColorScheme(
    primary = Gold,
    onPrimary = Ink,
    secondary = Pink,
    onSecondary = Ink,
    tertiary = Mint,
    background = Bg,
    onBackground = Color.White,
    surface = Card,
    onSurface = Color.White,
    surfaceVariant = CardHi,
    onSurfaceVariant = Dim,
)

@Composable
fun LotTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LotColors, content = content)
}

fun fmt(n: Int): String = "%,d".format(n)

@Composable
fun CoinPill(coins: Int, modifier: Modifier = Modifier) {
    val shown by animateIntAsState(coins, tween(600), label = "coins")
    Row(
        modifier
            .background(Ink, RoundedCornerShape(50))
            .border(2.dp, Gold, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(18.dp)) { drawCoin(center, size.minDimension / 2) }
        Spacer(Modifier.width(6.dp))
        Text(fmt(shown), color = Gold, fontWeight = FontWeight.Black, fontSize = 16.sp)
    }
}

@Composable
fun LotTopBar(title: String, coins: Int?, onBack: () -> Unit, onHelp: (() -> Unit)? = null) {
    val fx = LocalFx.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { fx.play(Sfx.CLICK); onBack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "back", tint = Color.White)
        }
        Text(title, fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.White, modifier = Modifier.weight(1f))
        if (onHelp != null) {
            IconButton(onClick = { fx.play(Sfx.CLICK); onHelp() }) {
                Icon(Icons.Filled.Info, "how to play", tint = Dim)
            }
        }
        if (coins != null) CoinPill(coins, Modifier.padding(end = 8.dp))
    }
}

/** a fat cartoony button that squishes when pressed */
@Composable
fun ChunkyButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Gold,
    textColor: Color = Ink,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    sfx: Sfx? = Sfx.CLICK,
    onClick: () -> Unit,
) {
    val fx = LocalFx.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.93f else 1f, spring(0.4f, 800f), label = "press")
    val bg = if (enabled) color else Color(0xFF4A4363)
    Box(
        modifier
            .scale(sc)
            .height(height)
            .background(Ink, RoundedCornerShape(16.dp))
            .padding(bottom = 4.dp)
            .background(bg, RoundedCornerShape(16.dp))
            .border(2.5.dp, Ink, RoundedCornerShape(16.dp))
            .clickable(src, null, enabled = enabled) {
                sfx?.let { fx.play(it) }
                fx.buzz(Buzz.LIGHT)
                onClick()
            }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (enabled) textColor else Dim,
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, color: Color = Card, content: @Composable () -> Unit) {
    Box(
        modifier
            .background(Ink, RoundedCornerShape(20.dp))
            .padding(bottom = 4.dp)
            .background(color, RoundedCornerShape(20.dp))
            .border(2.5.dp, Ink, RoundedCornerShape(20.dp))
    ) { content() }
}

@Composable
fun LotLogo(modifier: Modifier = Modifier, big: Boolean = true) {
    val inf = rememberInfiniteTransition(label = "logo")
    val wob by inf.animateFloat(-1f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "wob")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "spin")
    val fs = if (big) 96.sp else 48.sp
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(if (big) 260.dp else 130.dp, if (big) 150.dp else 75.dp)) {
            // sunburst
            rotate(spin, center) {
                for (i in 0 until 12) {
                    rotate(i * 30f, center) {
                        val p = androidx.compose.ui.graphics.Path().apply {
                            moveTo(center.x, center.y)
                            lineTo(center.x + size.width, center.y - size.width * 0.12f)
                            lineTo(center.x + size.width, center.y + size.width * 0.12f)
                            close()
                        }
                        drawPath(p, Gold.copy(alpha = 0.07f))
                    }
                }
            }
            // ticket behind the letters
            rotate(-8f + wob * 2f, center) {
                val tw = size.width * 0.86f
                val th = size.height * 0.62f
                val tl = Offset(center.x - tw / 2, center.y - th / 2)
                drawRoundRect(Ink, tl + Offset(6f, 8f), androidx.compose.ui.geometry.Size(tw, th), androidx.compose.ui.geometry.CornerRadius(24f))
                drawRoundRect(Pink, tl, androidx.compose.ui.geometry.Size(tw, th), androidx.compose.ui.geometry.CornerRadius(24f))
                drawCircle(Bg, th * 0.16f, Offset(tl.x, center.y))
                drawCircle(Bg, th * 0.16f, Offset(tl.x + tw, center.y))
                for (i in 0 until 10) {
                    drawCircle(Color.White.copy(alpha = 0.5f), 4f, Offset(tl.x + tw * (0.08f + i * 0.093f), tl.y + 14f))
                    drawCircle(Color.White.copy(alpha = 0.5f), 4f, Offset(tl.x + tw * (0.08f + i * 0.093f), tl.y + th - 14f))
                }
            }
        }
        Text(
            "lot",
            modifier = Modifier.scale(1f + wob * 0.03f),
            style = TextStyle(
                fontSize = fs,
                fontWeight = FontWeight.Black,
                color = Gold,
                shadow = Shadow(Ink, Offset(6f, 8f), 0f),
                letterSpacing = (-2).sp,
            ),
        )
    }
}

@Composable
fun TutorialDialog(title: String, pages: List<String>, onDone: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    Dialog(onDismissRequest = {}) {
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("how to play: $title", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Gold)
                Spacer(Modifier.height(14.dp))
                Text(pages[page], fontSize = 17.sp, color = Color.White, textAlign = TextAlign.Center, lineHeight = 24.sp)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pages.indices.forEach {
                        Box(
                            Modifier
                                .size(if (it == page) 12.dp else 8.dp)
                                .background(if (it == page) Gold else Dim, RoundedCornerShape(50))
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                val last = page == pages.lastIndex
                ChunkyButton(if (last) "let's go" else "next", Modifier.fillMaxWidth()) {
                    if (last) onDone() else page++
                }
            }
        }
    }
}

@Composable
fun BadgePopup(badge: BadgeDef?, onShown: () -> Unit) {
    val fx = LocalFx.current
    var shown by remember { mutableStateOf<BadgeDef?>(null) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(badge) {
        if (badge != null) {
            shown = badge
            visible = true
            fx.play(Sfx.BADGE)
            fx.buzz(Buzz.SUCCESS)
            delay(2600)
            visible = false
            delay(350)
            onShown()
        }
    }
    AnimatedVisibility(
        visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        val b = shown ?: return@AnimatedVisibility
        Panel(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            color = CardHi,
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(48.dp)) { drawMedal(center + Offset(0f, size.height * 0.15f), size.minDimension * 0.32f, modeColor(b.mode), false) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("badge unlocked!", color = Gold, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text(b.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
                    Text(b.desc, color = Dim, fontSize = 13.sp)
                }
            }
        }
    }
}

fun modeColor(mode: String) = when (mode) {
    "player" -> Gold
    "host" -> Pink
    "stealer" -> Mint
    "claim" -> Color(0xFFFF8C42)
    "bingo" -> Sky
    "dumpster" -> Color(0xFF9CCC65)
    "derby" -> Color(0xFFFF8C42)
    "slots" -> Color(0xFFE53935)
    "inspector" -> Color(0xFF26C6DA)
    "rain" -> Color(0xFF7986CB)
    "hilo" -> Color(0xFFFFB74D)
    "campaign" -> Color(0xFFFFD54F)
    "claw" -> Color(0xFFF06292)
    "flight" -> Color(0xFF4FC3F7)
    "tower" -> Color(0xFFAED581)
    "creator" -> Color(0xFFBA68C8)
    "ball" -> Color(0xFF5C6BC0)
    "locker" -> Color(0xFF4DD0E1)
    else -> Grape
}

/** confetti burst, restarts every time [key] changes (and key > 0) */
@Composable
fun Confetti(key: Int, modifier: Modifier = Modifier) {
    if (key <= 0) return
    val prog = remember(key) { Animatable(0f) }
    val bits = remember(key) {
        List(70) {
            floatArrayOf(
                Random.nextFloat(), Random.nextFloat() * -0.3f, (Random.nextFloat() - 0.5f) * 0.8f,
                0.6f + Random.nextFloat() * 0.9f, Random.nextFloat() * 360f, Random.nextInt(ballColors.size).toFloat(),
            )
        }
    }
    LaunchedEffect(key) { prog.animateTo(1f, tween(2200, easing = LinearEasing)) }
    if (prog.value >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        val t = prog.value
        bits.forEach { b ->
            val x = (b[0] + b[2] * t) * size.width
            val y = (b[1] + b[3] * t * 1.4f + 0.6f * t * t) * size.height
            rotate(b[4] + t * 720f * b[3], Offset(x, y)) {
                drawRect(
                    ballColors[b[5].toInt()].copy(alpha = (1f - t).coerceIn(0f, 1f)),
                    Offset(x - 8f, y - 4f), androidx.compose.ui.geometry.Size(16f, 8f),
                )
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier, color: Color = Color.White) {
    Panel(modifier, color = CardHi) {
        Column(Modifier.padding(10.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 20.sp, maxLines = 1)
            Text(label, color = Dim, fontSize = 12.sp, maxLines = 1)
        }
    }
}

/** what [coins] would be "worth" in a currency. purely for laughs, nothing is ever paid out. */
fun coinValue(coins: Int, code: String): String {
    val c = currencyOf(code)
    return "%,.${c.decimals}f %s".format(coins * c.perCoin, c.code)
}
