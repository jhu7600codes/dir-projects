package com.spy.game.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spy.game.R
import com.spy.game.data.Player
import com.spy.game.data.WordEntry
import com.spy.game.ui.components.MascotImage
import com.spy.game.ui.components.SpyCard
import com.spy.game.ui.components.SpyPrimaryButton
import com.spy.game.ui.theme.SpyOnSurfaceMuted
import com.spy.game.ui.theme.SpyRed

@Composable
fun RevealScreen(
    player: Player,
    wordEntry: WordEntry?,
    playerNumber: Int,
    totalPlayers: Int,
    onNext: () -> Unit,
) {
    // Fresh "face down" state every time we move to a new player.
    var revealed by remember(player.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "ИГРОК $playerNumber ИЗ $totalPlayers",
            style = MaterialTheme.typography.labelLarge,
            color = SpyOnSurfaceMuted,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            player.name,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = revealed,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
                label = "reveal-card",
            ) { isRevealed ->
                if (!isRevealed) {
                    FaceDownCard(onTap = { revealed = true })
                } else {
                    FaceUpCard(player = player, wordEntry = wordEntry)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        if (revealed) {
            SpyPrimaryButton(
                text = if (playerNumber < totalPlayers) "Скрыть и передать дальше" else "Скрыть и начать игру",
                onClick = onNext,
            )
        } else {
            Text(
                "Убедитесь, что телефон видите только вы",
                style = MaterialTheme.typography.bodyMedium,
                color = SpyOnSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FaceDownCard(onTap: () -> Unit) {
    SpyCard(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.72f)
            .clickable(onClick = onTap),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MascotImage(painter = painterResource(R.drawable.mascot_spiey), size = 96.dp)
            Spacer(Modifier.height(20.dp))
            Text(
                "Нажмите, чтобы открыть карту",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Карта откроет вашу роль",
                style = MaterialTheme.typography.bodyMedium,
                color = SpyOnSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FaceUpCard(player: Player, wordEntry: WordEntry?) {
    SpyCard(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.72f),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MascotImage(painter = painterResource(R.drawable.mascot_spiey), size = 96.dp)
            Spacer(Modifier.height(20.dp))
            if (player.isSpy) {
                Text(
                    "ВЫ — ШПИОН",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SpyRed,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Вы не знаете секретное слово. Слушайте остальных и постарайтесь не выдать себя.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SpyOnSurfaceMuted,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    wordEntry?.category?.uppercase() ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    color = SpyRed,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    wordEntry?.word ?: "",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Один из игроков — шпион и не знает это слово. Не называйте его прямо!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpyOnSurfaceMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
