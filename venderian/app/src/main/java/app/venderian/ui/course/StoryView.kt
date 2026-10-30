@file:OptIn(ExperimentalLayoutApi::class)

package app.venderian.ui.course

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.venderian.course.Exercise
import app.venderian.course.StoryPart
import app.venderian.ui.SpeakButton
import app.venderian.ui.app

/**
 * a story, duolingo style: the conversation appears one line at a time (read out loud, every
 * word tappable), and questions about it pop up in between. [onDone] gets whether every
 * question was right the first time.
 */
@Composable
internal fun StoryView(story: Exercise.Story, locked: Boolean, onDone: (Boolean) -> Unit) {
    val app = app()
    val parts = story.parts
    var step by remember(story) { mutableIntStateOf(0) }
    var mistakes by remember(story) { mutableIntStateOf(0) }
    // per question: the options tried so far, and whether it's been answered
    val tried = remember(story) { mutableStateListOf<Pair<Int, Int>>() }
    val solved = remember(story) { mutableStateListOf<Int>() }
    // first speaker on the left, everyone else on the right
    val left = remember(story) { parts.firstOrNull { !it.isQuestion }?.who }
    val list = rememberLazyListState()
    val current = parts.getOrNull(step)

    LaunchedEffect(step) {
        current?.takeIf { !it.isQuestion }?.let { app.speakVenderian(it.v) }
        list.animateScrollToItem(step + 1)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        LazyColumn(Modifier.weight(1f), state = list, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(story.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 12.dp))
            }
            itemsIndexed(parts.take(step + 1)) { i, part ->
                AnimatedVisibility(true, enter = fadeIn() + slideInVertically { it / 3 }) {
                    if (part.isQuestion) {
                        Question(part, tried.filter { it.first == i }.map { it.second }, i in solved, enabled = !locked && i == step) { pick ->
                            if (pick == part.answer) solved += i
                            else {
                                if (tried.none { it.first == i }) mistakes++
                                tried += i to pick
                            }
                        }
                    } else {
                        Line(part, onLeft = part.who == left)
                    }
                }
            }
        }
        val waiting = current?.isQuestion == true && step !in solved
        Button(
            onClick = { if (step >= parts.lastIndex) onDone(mistakes == 0) else step++ },
            enabled = !locked && !waiting,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(52.dp),
        ) { Text(if (step >= parts.lastIndex) "finish the story" else "continue") }
    }
}

@Composable
private fun Line(part: StoryPart, onLeft: Boolean) {
    val lookup = app().lookup
    var english by remember(part) { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (onLeft) Arrangement.Start else Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (onLeft) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(part.who.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
                    SpeakButton(part.v, Modifier.padding(start = 6.dp))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    part.v.split(' ').filter { it.isNotBlank() }.forEach { HintedWord(it, lookup.analyze(it), fontSize = 28) }
                }
                Text(
                    if (english) part.e else "tap for english",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { english = !english }.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun Question(part: StoryPart, wrong: List<Int>, solved: Boolean, enabled: Boolean, onPick: (Int) -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(part.q, style = MaterialTheme.typography.titleMedium)
        part.options.forEachIndexed { i, option ->
            val right = solved && i == part.answer
            val miss = i in wrong
            OutlinedButton(
                onClick = { onPick(i) },
                enabled = enabled && !solved && !miss,
                border = BorderStroke(if (right || miss) 2.dp else 1.dp, if (right) c.primary else if (miss) c.error else c.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (right) c.primaryContainer else if (miss) c.errorContainer else c.surface,
                    disabledContainerColor = if (right) c.primaryContainer else if (miss) c.errorContainer else c.surface,
                    disabledContentColor = c.onSurface,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(option) }
        }
    }
}
