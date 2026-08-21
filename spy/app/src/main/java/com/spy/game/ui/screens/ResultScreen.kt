package com.spy.game.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spy.game.R
import com.spy.game.data.VoteOutcome
import com.spy.game.ui.components.MascotImage
import com.spy.game.ui.components.SpyCard
import com.spy.game.ui.components.SpyPrimaryButton
import com.spy.game.ui.theme.SpyOnSurfaceMuted
import com.spy.game.ui.theme.SpyRed

@Composable
fun ResultScreen(outcome: VoteOutcome, onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "РЕЗУЛЬТАТ ГОЛОСОВАНИЯ",
            style = MaterialTheme.typography.labelLarge,
            color = SpyOnSurfaceMuted,
        )

        Spacer(Modifier.height(24.dp))

        SpyCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MascotImage(painter = painterResource(R.drawable.mascot_voter), size = 104.dp)
                Spacer(Modifier.height(24.dp))
                Text(
                    resultHeadline(outcome),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = if (outcome.wasSpy) SpyRed else MaterialTheme.colorScheme.onSurface,
                )
                val subline = resultSubline(outcome)
                if (subline.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        subline,
                        style = MaterialTheme.typography.bodyLarge,
                        color = SpyOnSurfaceMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        SpyPrimaryButton(text = "Далее", onClick = onContinue)
    }
}

/** Big line of the result card -- who was eliminated, and whether it was the spy. */
private fun resultHeadline(outcome: VoteOutcome): String {
    val player = outcome.eliminatedPlayer ?: return "Никто не был исключён"
    val verdict = if (outcome.wasSpy) "был Шпионом!" else "не был Шпионом!"
    return "${player.name} $verdict"
}

/**
 * Smaller "Осталось N" line -- omitted for 3-player games, where the round
 * decides the game outright and the count is redundant.
 */
private fun resultSubline(outcome: VoteOutcome): String {
    return if (outcome.totalPlayerCount <= 3) "" else "Осталось ${outcome.remainingActiveCount}"
}
