package com.spy.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spy.game.data.Player
import com.spy.game.ui.components.SpyCard
import com.spy.game.ui.components.SpySecondaryButton
import com.spy.game.ui.theme.SpyOnSurfaceMuted
import com.spy.game.ui.theme.SpyRed
import com.spy.game.ui.theme.SpySurfaceVariant

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayScreen(
    activePlayers: List<Player>,
    timerSeconds: Int,
    timerRunning: Boolean,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onCallMeeting: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Groups, contentDescription = null, tint = SpyRed)
            Text(
                "Активных игроков: ${activePlayers.size}",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(Modifier.height(20.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            activePlayers.forEach { player ->
                Text(
                    player.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpySurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            "ОБСУЖДЕНИЕ",
            style = MaterialTheme.typography.labelLarge,
            color = SpyOnSurfaceMuted,
        )
        Spacer(Modifier.height(12.dp))

        SpyCard(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatTimer(timerSeconds),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onResetTimer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(SpySurfaceVariant),
                    ) {
                        Icon(Icons.Filled.Replay, contentDescription = "Сбросить таймер", tint = SpyOnSurfaceMuted)
                    }
                    IconButton(
                        onClick = onToggleTimer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(SpyRed),
                    ) {
                        Icon(
                            if (timerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (timerRunning) "Пауза" else "Старт",
                            tint = androidx.compose.ui.graphics.Color.White,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        SpySecondaryButton(text = "Объявить сходку", onClick = onCallMeeting)
    }
}

private fun formatTimer(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
