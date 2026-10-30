package app.venderian.ui.learn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.venderian.ui.ScreenTitle
import app.venderian.ui.course.CourseMap
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import app.venderian.ui.VText
import app.venderian.ui.app

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(onOpenLesson: (Int) -> Unit, onOpenCourse: (String) -> Unit, onOpenDictionary: () -> Unit) {
    val app = app()
    val scriptDone by app.repository.scriptDone.collectAsState(initial = false)
    val scriptLessons by app.repository.lessonsDone.collectAsState(initial = emptySet())
    val courseDone by app.repository.courseDone.collectAsState(initial = emptySet())
    // the course once the script is done, the script before that
    var tab by rememberSaveable { mutableStateOf<Int?>(null) }
    val shown = tab ?: if (scriptDone) 1 else 0
    Column {
        ScreenTitle("Learn", if (shown == 0) "the venderian script, a few letters at a time" else "the language: 3 sections, 9 modules", onOpenDictionary)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            SegmentedButton(shown == 0, { tab = 0 }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("script") }
            SegmentedButton(shown == 1, { tab = 1 }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("course") }
        }
        if (shown == 0) ScriptLessons(onOpenLesson)
        else CourseMap(
            app.course, courseDone, scriptDone,
            scriptProgress = scriptLessons.size / app.alphabet.lessons.size.toFloat(),
            onOpen = onOpenCourse,
        )
    }
}

@Composable
private fun ScriptLessons(onOpenLesson: (Int) -> Unit) {
    val app = app()
    val alphabet = app.alphabet
    val done by app.repository.lessonsDone.collectAsState(initial = emptySet())
    val next = alphabet.lessons.firstOrNull { it.index !in done }?.index

    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
        item { HowItWorks() }
        items(alphabet.lessons, key = { it.index }) { lesson ->
            val isDone = lesson.index in done
            Card(
                onClick = { onOpenLesson(lesson.index) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (lesson.index == next) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${lesson.index + 1}. ${lesson.title}", style = MaterialTheme.typography.titleMedium)
                            Text(lesson.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isDone) Icon(Icons.Filled.CheckCircle, "done", tint = MaterialTheme.colorScheme.primary)
                        else if (lesson.index == next) Text("next", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.size(10.dp))
                    // the lesson's letters, each one on its own so the headlines don't join
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        lesson.letters.forEach { VText(it.glyph.toString(), size = 34.sp) }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("digits", style = MaterialTheme.typography.titleMedium)
                    Text("a slanted stem with ticks. 5 adds a foot, so 7 is the foot + 2 ticks.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.size(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        (0..9).forEach { d ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                VText("$d", size = 28.sp)
                                Text("$d", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HowItWorks() {
    var open by rememberSaveable { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.clickable { open = !open }.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("how the script works", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
            }
            AnimatedVisibility(open) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "one letter per sound, written left to right, no capitals.",
                        "every letter hangs under a headline. inside a word the headlines join into one line; a gap in the line is a space.",
                        "consonants touch the headline, vowels float under it.",
                        "a consonant is a leg (where in the mouth) plus a mark: a foot for stops like p, a bar for hissy sounds like s.",
                        "a little spark above the line means voiced: p becomes b, s becomes z.",
                        "a consonant + ë gets a bar underneath, a doubled consonant gets a second headline.",
                    ).forEach { Text("· $it", style = MaterialTheme.typography.bodyMedium) }
                    VText(app.venderian.convert.Sounds.scriptOf("vela mi"), size = 30.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
