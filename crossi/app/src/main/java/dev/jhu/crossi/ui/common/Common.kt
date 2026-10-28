package dev.jhu.crossi.ui.common

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.jhu.crossi.CrossiApp
import dev.jhu.crossi.R
import dev.jhu.crossi.di.AppContainer
import dev.jhu.crossi.game.BossTwist
import dev.jhu.crossi.game.Difficulty
import dev.jhu.crossi.game.PowerUpType

/** viewModel() with access to the app container. */
@Composable
inline fun <reified VM : ViewModel> crossiViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = (LocalContext.current.applicationContext as CrossiApp).container
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossiTopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
            }
        },
        actions = { actions() },
    )
}

val PowerUpType.icon: ImageVector
    get() = when (this) {
        PowerUpType.REVEAL_LETTER -> Icons.Filled.TextFields
        PowerUpType.REVEAL_WORD -> Icons.Filled.AutoFixHigh
        PowerUpType.EXTRA_CHECK -> Icons.Filled.DoneAll
        PowerUpType.SKIP_CLUE -> Icons.Filled.SwapHoriz
    }

@get:StringRes
val PowerUpType.label: Int
    get() = when (this) {
        PowerUpType.REVEAL_LETTER -> R.string.power_reveal_letter
        PowerUpType.REVEAL_WORD -> R.string.power_reveal_word
        PowerUpType.EXTRA_CHECK -> R.string.power_extra_check
        PowerUpType.SKIP_CLUE -> R.string.power_skip_clue
    }

@get:StringRes
val PowerUpType.description: Int
    get() = when (this) {
        PowerUpType.REVEAL_LETTER -> R.string.power_reveal_letter_desc
        PowerUpType.REVEAL_WORD -> R.string.power_reveal_word_desc
        PowerUpType.EXTRA_CHECK -> R.string.power_extra_check_desc
        PowerUpType.SKIP_CLUE -> R.string.power_skip_clue_desc
    }

val BossTwist.icon: ImageVector
    get() = when (this) {
        BossTwist.TIMER -> Icons.Filled.Timer
        BossTwist.LOCKED_CELLS -> Icons.Filled.Lock
        BossTwist.SCRAMBLED_CLUES -> Icons.Filled.Shuffle
        BossTwist.LIMITED_MISTAKES -> Icons.Filled.Favorite
        BossTwist.FOG -> Icons.Filled.Cloud
        BossTwist.TRICK_FEST -> Icons.Filled.Celebration
        BossTwist.SCRAMBLED_KEYBOARD -> Icons.Filled.Keyboard
        BossTwist.ONE_SHOT -> Icons.Filled.LooksOne
    }

@get:StringRes
val BossTwist.label: Int
    get() = when (this) {
        BossTwist.TIMER -> R.string.twist_timer
        BossTwist.LOCKED_CELLS -> R.string.twist_locked
        BossTwist.SCRAMBLED_CLUES -> R.string.twist_scrambled_clues
        BossTwist.LIMITED_MISTAKES -> R.string.twist_limited_mistakes
        BossTwist.FOG -> R.string.twist_fog
        BossTwist.TRICK_FEST -> R.string.twist_trick_fest
        BossTwist.SCRAMBLED_KEYBOARD -> R.string.twist_scrambled_keyboard
        BossTwist.ONE_SHOT -> R.string.twist_one_shot
    }

@get:StringRes
val BossTwist.description: Int
    get() = when (this) {
        BossTwist.TIMER -> R.string.twist_timer_desc
        BossTwist.LOCKED_CELLS -> R.string.twist_locked_desc
        BossTwist.SCRAMBLED_CLUES -> R.string.twist_scrambled_clues_desc
        BossTwist.LIMITED_MISTAKES -> R.string.twist_limited_mistakes_desc
        BossTwist.FOG -> R.string.twist_fog_desc
        BossTwist.TRICK_FEST -> R.string.twist_trick_fest_desc
        BossTwist.SCRAMBLED_KEYBOARD -> R.string.twist_scrambled_keyboard_desc
        BossTwist.ONE_SHOT -> R.string.twist_one_shot_desc
    }

@get:StringRes
val Difficulty.label: Int
    get() = when (this) {
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.NORMAL -> R.string.difficulty_normal
        Difficulty.HARD -> R.string.difficulty_hard
    }

fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
