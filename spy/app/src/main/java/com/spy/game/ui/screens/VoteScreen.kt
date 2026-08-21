package com.spy.game.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spy.game.R
import com.spy.game.data.Player
import com.spy.game.ui.components.MascotImage
import com.spy.game.ui.theme.SpyOnSurfaceMuted
import com.spy.game.ui.theme.SpyOutline
import com.spy.game.ui.theme.SpySurface
import com.spy.game.ui.theme.SpySurfaceVariant

@Composable
fun VoteScreen(
    voter: Player,
    candidates: List<Player>,
    voterNumber: Int,
    totalVoters: Int,
    onVote: (targetId: Int?) -> Unit,
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(220.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            MascotImage(painter = painterResource(R.drawable.mascot_voter), size = 88.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                "ГОЛОСУЮЩИЙ $voterNumber ИЗ $totalVoters",
                style = MaterialTheme.typography.labelLarge,
                color = SpyOnSurfaceMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                voter.name,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Кто, по-вашему, шпион?",
                style = MaterialTheme.typography.bodyMedium,
                color = SpyOnSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.width(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(candidates, key = { it.id }) { candidate ->
                VoteTargetTile(name = candidate.name, onClick = { onVote(candidate.id) })
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SkipTile(onClick = { onVote(null) })
            }
        }
    }
}

@Composable
private fun VoteTargetTile(name: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(18.dp))
            .background(SpySurface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SkipTile(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SpySurfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Block, contentDescription = null, tint = SpyOutline)
        Spacer(Modifier.width(8.dp))
        Text(
            "Пропустить",
            style = MaterialTheme.typography.titleMedium,
            color = SpyOnSurfaceMuted,
        )
    }
}
