package dev.jhu.crossi.ui.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jhu.crossi.R
import dev.jhu.crossi.game.Keyboards
import dev.jhu.crossi.game.Language

/** In-app keyboard: the system one would cover the grid and autocorrect into it. */
@Composable
fun GameKeyboard(
    language: Language,
    scrambleSeed: Long,
    haptics: Boolean,
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onToggleDirection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = Keyboards.rows(language, scrambleSeed)
    val haptic = LocalHapticFeedback.current
    val buzz = { if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    val keyText = if (language == Language.RU) 15.sp else 18.sp

    Column(modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEachIndexed { index, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (index == rows.lastIndex) {
                    Key(weight = 1.5f, onClick = { buzz(); onToggleDirection() }) {
                        Icon(Icons.Filled.SwapVert, stringResource(R.string.toggle_direction))
                    }
                }
                for (ch in row) {
                    Key(onClick = { buzz(); onKey(ch) }) {
                        Text(ch.toString(), fontSize = keyText, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (index == rows.lastIndex) {
                    Key(weight = 1.5f, onClick = { buzz(); onBackspace() }) {
                        Icon(Icons.AutoMirrored.Filled.Backspace, stringResource(R.string.backspace))
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.Key(weight: Float = 1f, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.weight(weight).height(48.dp),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Box(Modifier.fillMaxSize().clickable(onClick = onClick), contentAlignment = Alignment.Center) { content() }
    }
}
