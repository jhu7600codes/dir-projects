package com.spy.game.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spy.game.ui.theme.SpyOutline
import com.spy.game.ui.theme.SpyRed
import com.spy.game.ui.theme.SpyRedDark
import com.spy.game.ui.theme.SpyRedLight
import com.spy.game.ui.theme.SpySurface
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.ui.graphics.painter.Painter

/** Primary, filled, full-width call-to-action button in the brand red. */
@Composable
fun SpyPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SpyRed,
            contentColor = Color.White,
            disabledContainerColor = SpySurface,
            disabledContentColor = SpyOutline,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

/** Secondary, outlined button for less prominent actions ("Пропустить", "Пауза", etc). */
@Composable
fun SpySecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, if (enabled) SpyRed else SpyOutline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (enabled) SpyRed else SpyOutline,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

/** Plain text link-style action, e.g. "новая игра". */
@Composable
fun SpyTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Rounded, bordered dark card -- the base surface used across every screen. */
@Composable
fun SpyCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    contentPadding: PaddingValues = PaddingValues(24.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(SpySurface)
            .border(1.dp, SpyOutline, RoundedCornerShape(cornerRadius))
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * One of the three mascot doodles, drawn contextually across the screens.
 *
 * The source art is a tall portrait doodle (roughly 1:2), not a square icon,
 * so this constrains height only and lets width follow the painter's own
 * aspect ratio instead of squashing it into a square box.
 */
@Composable
fun MascotImage(painter: Painter, modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier.height(size),
    )
}

/**
 * The big red "call the meeting" button -- a physical arcade-button
 * illusion: a darker, fixed base sits [pressDepth] below a lighter,
 * glossy face. Pressing it animates the face down onto the base (and back
 * up on release), so it visually gets pushed in rather than just changing
 * color like a normal Material button.
 */
@Composable
fun SpyBigRedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 200.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressDepth = 12.dp
    val faceOffset by animateDpAsState(
        targetValue = if (pressed) pressDepth else 0.dp,
        animationSpec = tween(durationMillis = if (pressed) 60 else 140),
        label = "big-red-button-press",
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.TopCenter,
    ) {
        // Base/shadow layer: fixed in place, always visible below the face.
        Box(
            modifier = Modifier
                .padding(top = pressDepth)
                .size(size - pressDepth)
                .clip(CircleShape)
                .background(SpyRedDark),
        )
        // Face layer: glossy gradient, slides down onto the base when pressed.
        Box(
            modifier = Modifier
                .offset(y = faceOffset)
                .size(size - pressDepth)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = if (enabled) listOf(SpyRedLight, SpyRed) else listOf(SpyOutline, SpyOutline),
                    ),
                )
                .border(2.dp, if (enabled) SpyRedDark else SpyOutline, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
    }
}
