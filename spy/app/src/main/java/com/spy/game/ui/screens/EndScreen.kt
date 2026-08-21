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
import com.spy.game.data.Player
import com.spy.game.data.WordEntry
import com.spy.game.data.Winner
import com.spy.game.ui.components.MascotImage
import com.spy.game.ui.components.SpyCard
import com.spy.game.ui.components.SpyPrimaryButton
import com.spy.game.ui.theme.SpyGreen
import com.spy.game.ui.theme.SpyOnSurfaceMuted
import com.spy.game.ui.theme.SpyRed

@Composable
fun EndScreen(
    winner: Winner,
    wordEntry: WordEntry?,
    spy: Player?,
    onNewGame: () -> Unit,
) {
    val accent = if (winner == Winner.SPY) SpyRed else SpyGreen
    val mascot = if (winner == Winner.SPY) R.drawable.mascot_spiey else R.drawable.mascot_voter

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))

        SpyCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MascotImage(painter = painterResource(mascot), size = 112.dp)
                Spacer(Modifier.height(24.dp))
                Text(
                    if (winner == Winner.SPY) "ШПИОН ПОБЕДИЛ" else "МИРНЫЕ ЖИТЕЛИ ПОБЕДИЛИ",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = accent,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(28.dp))

                if (spy != null) {
                    Text(
                        "ШПИОНОМ БЫЛ",
                        style = MaterialTheme.typography.labelLarge,
                        color = SpyOnSurfaceMuted,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        spy.name,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                }

                Text(
                    "СЕКРЕТНОЕ СЛОВО",
                    style = MaterialTheme.typography.labelLarge,
                    color = SpyOnSurfaceMuted,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    wordEntry?.word ?: "",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        SpyPrimaryButton(text = "Новая игра", onClick = onNewGame)
    }
}
