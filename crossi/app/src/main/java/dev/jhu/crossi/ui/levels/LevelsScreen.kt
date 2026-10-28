package dev.jhu.crossi.ui.levels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dev.jhu.crossi.R
import dev.jhu.crossi.data.LanguageManager
import dev.jhu.crossi.data.LevelProgressEntity
import dev.jhu.crossi.di.AppContainer
import dev.jhu.crossi.game.GameMode
import dev.jhu.crossi.game.LevelCatalog
import dev.jhu.crossi.ui.common.CrossiTopBar
import dev.jhu.crossi.ui.common.crossiViewModel
import dev.jhu.crossi.ui.common.icon
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LevelsViewModel(c: AppContainer) : ViewModel() {
    val progress: StateFlow<Map<Int, LevelProgressEntity>> =
        c.games.levelProgress(LanguageManager.current()).stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
}

@Composable
fun LevelsScreen(onBack: () -> Unit, onPlay: (GameMode) -> Unit) {
    val vm = crossiViewModel { LevelsViewModel(it) }
    val progress by vm.progress.collectAsStateWithLifecycle()

    Scaffold(topBar = { CrossiTopBar(stringResource(R.string.levels), onBack) }) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items((1..LevelCatalog.COUNT).toList()) { n ->
                val spec = LevelCatalog.spec(n)
                val unlocked = n == 1 || progress.containsKey(n - 1)
                val stars = progress[n]?.stars ?: 0
                LevelTile(n, spec.isBoss, spec.twists.firstOrNull()?.icon, unlocked, stars) {
                    onPlay(GameMode.Level(n))
                }
            }
        }
    }
}

@Composable
private fun LevelTile(
    n: Int,
    boss: Boolean,
    bossIcon: androidx.compose.ui.graphics.vector.ImageVector?,
    unlocked: Boolean,
    stars: Int,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            enabled = unlocked,
            shape = if (boss) RoundedCornerShape(12.dp) else CircleShape,
            color = when {
                !unlocked -> colors.surfaceContainerHigh
                boss -> colors.errorContainer
                stars > 0 -> colors.primaryContainer
                else -> colors.secondaryContainer
            },
            modifier = Modifier.aspectRatio(1f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                when {
                    !unlocked -> Icon(Icons.Filled.Lock, stringResource(R.string.locked), tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    boss && bossIcon != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(bossIcon, null, modifier = Modifier.size(16.dp))
                        Text(n.toString(), fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                    else -> Text(n.toString(), fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            if (stars > 0) "★".repeat(stars) + "☆".repeat(3 - stars) else " ",
            color = colors.secondary,
            fontSize = 11.sp,
        )
    }
}
