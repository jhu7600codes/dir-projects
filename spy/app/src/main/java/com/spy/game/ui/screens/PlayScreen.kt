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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spy.game.data.Player
import com.spy.game.ui.components.SpyBigRedButton
import com.spy.game.ui.components.SpyCard
import com.spy.game.ui.components.SpySecondaryButton
import com.spy.game.ui.theme.SpyOnSurfaceMuted
import com.spy.game.ui.theme.SpyRed
import com.spy.game.ui.theme.SpySurfaceVariant

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayScreen(
    activePlayers: List<Player>,
    hintRoundNumber: Int,
    discussionStarted: Boolean,
    timerSeconds: Int,
    onAdvanceHintRound: () -> Unit,
    onStartDiscussion: () -> Unit,
    onCallMeetingEarly: () -> Unit,
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

        if (!discussionStarted) {
            HintRoundSection(
                hintRoundNumber = hintRoundNumber,
                onAdvanceHintRound = onAdvanceHintRound,
                onStartDiscussion = onStartDiscussion,
            )
        } else {
            DiscussionSection(
                timerSeconds = timerSeconds,
                onCallMeetingEarly = onCallMeetingEarly,
            )
        }
    }
}

@Composable
private fun HintRoundSection(
    hintRoundNumber: Int,
    onAdvanceHintRound: () -> Unit,
    onStartDiscussion: () -> Unit,
) {
    Text("ПОДСКАЗКИ", style = MaterialTheme.typography.labelLarge, color = SpyOnSurfaceMuted)
    Spacer(Modifier.height(12.dp))

    SpyCard(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Раунд $hintRoundNumber",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Подсказка — $hintRoundNumber ${wordFormForCount(hintRoundNumber)}",
                style = MaterialTheme.typography.bodyLarge,
                color = SpyOnSurfaceMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "По очереди назовите подсказку из этого числа слов. " +
                    "Если никто не догадался, кто шпион, — следующий раунд длиннее.",
                style = MaterialTheme.typography.bodyMedium,
                color = SpyOnSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }
    }

    Spacer(Modifier.height(20.dp))
    SpySecondaryButton(text = "Никто не догадался", onClick = onAdvanceHintRound)

    Spacer(Modifier.height(28.dp))
    SpyBigRedButton(text = "Я знаю,\nкто шпион!", onClick = onStartDiscussion)
    Spacer(Modifier.height(12.dp))
    Text(
        "Нажмите, если кто-то готов обвинить игрока — начнётся 3-минутное обсуждение",
        style = MaterialTheme.typography.bodyMedium,
        color = SpyOnSurfaceMuted,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun DiscussionSection(timerSeconds: Int, onCallMeetingEarly: () -> Unit) {
    Text("ОБСУЖДЕНИЕ", style = MaterialTheme.typography.labelLarge, color = SpyOnSurfaceMuted)
    Spacer(Modifier.height(12.dp))

    SpyCard(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                formatTimer(timerSeconds),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Обсудите и решите, кто шпион",
                style = MaterialTheme.typography.bodyMedium,
                color = SpyOnSurfaceMuted,
            )
        }
    }

    Spacer(Modifier.height(24.dp))
    SpySecondaryButton(text = "Голосовать досрочно", onClick = onCallMeetingEarly)
}

/** Nominative plural agreement for "слово" after a count -- 1 слово, 2-4 слова, 5+/11-14 слов. */
private fun wordFormForCount(n: Int): String = when {
    n % 100 in 11..14 -> "слов"
    n % 10 == 1 -> "слово"
    n % 10 in 2..4 -> "слова"
    else -> "слов"
}

private fun formatTimer(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
