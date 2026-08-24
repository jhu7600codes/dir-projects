package com.spy.game.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spy.game.data.Player
import com.spy.game.ui.components.SpyCard
import com.spy.game.ui.theme.SpyOnSurfaceMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The brief "thrown out" beat between VOTE and RESULT: the eliminated
 * player's name card slides, tilts, and fades away, then [onFinished] is
 * called to move on to the result screen. If nobody was eliminated (a tie
 * or an all-skip vote), there's nothing to throw out, so this just shows a
 * short neutral beat instead of the animation.
 */
@Composable
fun EliminationScreen(eliminatedPlayer: Player?, onFinished: () -> Unit) {
    val offsetY = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }

    LaunchedEffect(eliminatedPlayer?.id) {
        if (eliminatedPlayer == null) {
            delay(900)
            onFinished()
            return@LaunchedEffect
        }
        delay(500)
        launch { offsetY.animateTo(1400f, tween(700, easing = FastOutLinearInEasing)) }
        launch { rotation.animateTo(35f, tween(700)) }
        launch { fade.animateTo(0f, tween(500, delayMillis = 250)) }
        delay(750)
        onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "ГОЛОСОВАНИЕ ЗАВЕРШЕНО",
            style = MaterialTheme.typography.labelLarge,
            color = SpyOnSurfaceMuted,
        )

        Spacer(Modifier.height(48.dp))

        if (eliminatedPlayer != null) {
            SpyCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = offsetY.value
                        rotationZ = rotation.value
                    }
                    .alpha(fade.value),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        eliminatedPlayer.name,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "отправляется домой...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SpyOnSurfaceMuted,
                    )
                }
            }
        } else {
            SpyCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Голоса разделились — никто не покидает игру",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
