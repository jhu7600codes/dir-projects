@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package app.venderian.ui.course

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.venderian.VenderianApp
import app.venderian.convert.Sounds
import app.venderian.course.CourseLesson
import app.venderian.course.Exercise
import app.venderian.course.ExerciseMaker
import app.venderian.course.Kind
import app.venderian.data.Repository
import app.venderian.ui.VText
import app.venderian.ui.appViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class Feedback(val correct: Boolean, val answer: String?)

data class PlayerState(
    val lesson: CourseLesson,
    val queue: List<Exercise>,
    val index: Int = 0,
    val hearts: Int = 3,
    val mistakes: Int = 0,
    val feedback: Feedback? = null,
    val showTip: Boolean = false,
    val finished: Boolean = false,
    val failed: Boolean = false,
) {
    val current get() = queue.getOrNull(index)
    val progress get() = index.toFloat() / queue.size.coerceAtLeast(1)
}

class PlayerViewModel(private val app: VenderianApp, private val id: String) : ViewModel() {
    private val lesson = app.course.lesson(id)
    private val maker = ExerciseMaker(app.course, app.lexicon.entries)
    val state = MutableStateFlow(fresh())

    private fun fresh() = PlayerState(lesson, maker.make(lesson), showTip = lesson.tip.isNotEmpty())

    fun startOver() {
        state.value = fresh().copy(showTip = false)
    }

    fun closeTip() {
        state.value = state.value.copy(showTip = false)
    }

    fun answer(correct: Boolean, rightAnswer: String?) {
        val s = state.value
        if (s.feedback != null) return
        val hearts = if (!correct && lesson.hasHearts) s.hearts - 1 else s.hearts
        // in normal lessons a missed exercise comes back at the end
        val queue = if (!correct && !lesson.hasHearts) s.queue + s.queue[s.index] else s.queue
        state.value = s.copy(
            feedback = Feedback(correct, rightAnswer), hearts = hearts, queue = queue,
            mistakes = s.mistakes + if (correct) 0 else 1,
        )
    }

    /** to the next exercise (or the end) */
    fun next() {
        val s = state.value
        when {
            s.hearts <= 0 -> state.value = s.copy(failed = true, feedback = null)
            s.index + 1 >= s.queue.size -> {
                state.value = s.copy(finished = true, feedback = null)
                viewModelScope.launch { app.repository.completeCourseLesson(lesson, s.mistakes) }
            }
            else -> state.value = s.copy(index = s.index + 1, feedback = null)
        }
    }
}

@Composable
fun LessonPlayer(id: String, onClose: () -> Unit) {
    val vm = appViewModel { PlayerViewModel(it, id) }
    val s by vm.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "close") }
            val progress by animateFloatAsState(if (s.finished) 1f else s.progress, label = "progress")
            LinearProgressIndicator({ progress }, Modifier.weight(1f).height(10.dp))
            if (s.lesson.hasHearts) {
                Row(Modifier.padding(start = 12.dp)) {
                    repeat(3) { i ->
                        Icon(if (i < s.hearts) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null,
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Spacer(Modifier.size(8.dp))
        }
        when {
            s.finished -> Finished(s, onClose)
            s.failed -> Failed(onRetry = vm::startOver, onClose = onClose)
            s.showTip -> Tip(s.lesson, onStart = vm::closeTip)
            else -> Box(Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = s.index,
                    transitionSpec = { (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut()) },
                    label = "exercise",
                ) { index ->
                    val exercise = s.queue.getOrNull(index)
                    if (exercise != null) {
                        // keyed by index: an exercise that comes back again starts fresh
                        androidx.compose.runtime.key(index) {
                            ExerciseView(exercise, locked = s.feedback != null, onAnswer = vm::answer, onNext = vm::next)
                        }
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    s.feedback != null,
                    enter = slideInVertically { it }, exit = slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    s.feedback?.let { FeedbackBar(it, onNext = vm::next) }
                }
            }
        }
    }
}

@Composable
private fun Tip(lesson: CourseLesson, onStart: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(lesson.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(20.dp)) {
                Text("grammar tip", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Text(lesson.tip, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("start") }
    }
}

@Composable
private fun FeedbackBar(f: Feedback, onNext: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(color = if (f.correct) c.primaryContainer else c.errorContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(if (f.correct) "nice!" else "not quite", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (!f.correct && f.answer != null) {
                Text("answer:", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
                Text(f.answer, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onNext, modifier = Modifier.fillMaxWidth(),
                colors = if (f.correct) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(containerColor = c.error, contentColor = c.onError),
            ) { Text("continue") }
        }
    }
}

@Composable
private fun Finished(s: PlayerState, onClose: () -> Unit) {
    val xp = when (s.lesson.kind) {
        Kind.CHALLENGE -> Repository.XP_CHALLENGE
        Kind.GOLD -> Repository.XP_GOLD
        else -> Repository.XP_COURSE_LESSON
    }
    var shown by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { shown = true }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AnimatedVisibility(shown, enter = scaleIn() + fadeIn()) {
            Text(
                when (s.lesson.kind) {
                    Kind.GOLD -> "gold!"
                    Kind.CHALLENGE -> "locked in!"
                    else -> "lesson done!"
                },
                style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
                color = if (s.lesson.kind == Kind.GOLD) Color(0xFFFFC94D) else MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("+$xp xp", style = MaterialTheme.typography.headlineSmall)
        Text(if (s.mistakes == 0) "no mistakes" else "${s.mistakes} mistake${if (s.mistakes == 1) "" else "s"}",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("continue") }
    }
}

@Composable
private fun Failed(onRetry: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("out of hearts", style = MaterialTheme.typography.headlineMedium)
        Text("challenges need you to really know it. go again!", textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("try again") }
        TextButton(onClick = onClose) { Text("leave") }
    }
}

// ---- exercises ----

/** venderian text in script, with a tap to show the roman letters */
@Composable
private fun Script(roman: String, size: Int = 40) {
    var showRoman by remember(roman) { mutableStateOf(false) }
    Column(Modifier.clickable { showRoman = !showRoman }) {
        VText(Sounds.scriptOfText(roman), size = size.sp)
        Text(if (showRoman) roman else "tap to see the letters", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Frame(title: String, locked: Boolean, canCheck: Boolean, onCheck: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { content() }
        Button(
            onClick = onCheck, enabled = canCheck && !locked,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(52.dp),
        ) { Text("check") }
    }
}

@Composable
internal fun ExerciseView(e: Exercise, locked: Boolean, onAnswer: (Boolean, String?) -> Unit, onNext: () -> Unit) {
    when (e) {
        is Exercise.NewWord -> Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("new word", style = MaterialTheme.typography.titleLarge)
            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                VText(Sounds.scriptOf(e.word.word), size = 72.sp)
                Text(e.word.word, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text(e.word.meaning, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text(e.word.pos, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(52.dp)) { Text("got it") }
        }

        is Exercise.PickMeaning -> {
            var picked by remember { mutableStateOf<Int?>(null) }
            Frame("what does this mean?", locked, picked != null, { onAnswer(picked == e.answer, e.word.meaning) }) {
                Script(e.word.word, 56)
                Spacer(Modifier.height(20.dp))
                e.options.forEachIndexed { i, o -> Option(o, script = false, selected = picked == i, enabled = !locked) { picked = i } }
            }
        }

        is Exercise.PickWord -> {
            var picked by remember { mutableStateOf<Int?>(null) }
            Frame("which one is “${e.word.meaning}”?", locked, picked != null, { onAnswer(picked == e.answer, e.word.word) }) {
                e.options.forEachIndexed { i, o -> Option(o, script = true, selected = picked == i, enabled = !locked) { picked = i } }
            }
        }

        is Exercise.Match -> Match(e, onDone = { onAnswer(true, null) })

        is Exercise.BuildEnglish -> Build(
            "write this in english", e.tiles, e.answer, locked, script = false,
            prompt = { Script(e.venderian) }, rightAnswer = e.english, onAnswer = onAnswer,
        )

        is Exercise.BuildVenderian -> Build(
            "write this in venderian", e.tiles, e.answer, locked, script = true,
            prompt = { Text(e.english, style = MaterialTheme.typography.headlineSmall) }, rightAnswer = e.venderian, onAnswer = onAnswer,
        )

        is Exercise.TypeRoman -> {
            var value by remember { mutableStateOf(TextFieldValue("")) }
            val check = { onAnswer(ExerciseMaker.sameText(value.text, e.venderian), e.venderian) }
            Frame("read it: type what this says", locked, value.text.isNotBlank(), check) {
                VText(Sounds.scriptOfText(e.venderian), size = 48.sp)
                if (e.meaning != null) Text("(${e.meaning})", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value, { value = it }, Modifier.fillMaxWidth(), enabled = !locked, singleLine = true,
                    label = { Text("in roman letters") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, autoCorrectEnabled = false),
                    keyboardActions = KeyboardActions(onDone = { if (value.text.isNotBlank()) check() }),
                )
                AssistChip(onClick = {
                    val t = value.text.substring(0, value.selection.start) + "ë" + value.text.substring(value.selection.end)
                    value = TextFieldValue(t, TextRange(value.selection.start + 1))
                }, label = { Text("ë") }, enabled = !locked)
            }
        }
    }
}

@Composable
private fun Option(text: String, script: Boolean, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick, enabled = enabled,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) c.primary else c.outlineVariant),
        colors = if (selected) ButtonDefaults.outlinedButtonColors(containerColor = c.primaryContainer) else ButtonDefaults.outlinedButtonColors(),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).heightIn(min = 60.dp),
    ) {
        if (script) VText(Sounds.scriptOf(text), size = 30.sp) else Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/** build a sentence from word tiles */
@Composable
private fun Build(
    title: String, tiles: List<String>, answer: List<String>, locked: Boolean, script: Boolean,
    prompt: @Composable () -> Unit, rightAnswer: String, onAnswer: (Boolean, String?) -> Unit,
) {
    // indexes into tiles, so two of the same word work
    val chosen = remember { mutableStateListOf<Int>() }
    Frame(title, locked, chosen.isNotEmpty(), { onAnswer(chosen.map { tiles[it] } == answer, rightAnswer) }) {
        prompt()
        Spacer(Modifier.height(20.dp))
        FlowRow(
            Modifier.fillMaxWidth().heightIn(min = 72.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            chosen.toList().forEach { i -> Tile(tiles[i], script, enabled = !locked) { chosen.remove(i) } }
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            tiles.indices.forEach { i ->
                val used = i in chosen
                Tile(tiles[i], script, enabled = !locked && !used, faded = used) { chosen.add(i) }
            }
        }
    }
}

@Composable
private fun Tile(word: String, script: Boolean, enabled: Boolean, faded: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick, enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = if (faded) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val alpha = if (faded) 0.2f else 1f
            if (script) {
                VText(Sounds.scriptOf(word), size = 24.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
                Text(word, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
            } else {
                Text(word, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
            }
        }
    }
}

/** tap a venderian word, then its meaning */
@Composable
private fun Match(e: Exercise.Match, onDone: () -> Unit) {
    val left = remember(e) { e.pairs.map { it.first }.shuffled() }
    val right = remember(e) { e.pairs.map { it.second }.shuffled() }
    val matched = remember(e) { mutableStateListOf<String>() }
    var picked by remember(e) { mutableStateOf<String?>(null) }
    var wrong by remember(e) { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text("match the pairs", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                left.forEach { w ->
                    MatchCell(w in matched, picked == w, wrong == w, { if (w !in matched) { picked = w; wrong = null } }) {
                        VText(Sounds.scriptOf(w), size = 26.sp)
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                right.forEach { m ->
                    val done = e.pairs.any { it.second == m && it.first in matched }
                    MatchCell(done, false, wrong == m, {
                        val p = picked ?: return@MatchCell
                        if (e.pairs.any { it.first == p && it.second == m }) {
                            matched += p
                            picked = null
                            if (matched.size == e.pairs.size) onDone()
                        } else wrong = m
                    }) { Text(m, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center) }
                }
            }
        }
    }
}

@Composable
private fun MatchCell(done: Boolean, picked: Boolean, wrong: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(
        onClick = onClick, enabled = !done,
        shape = MaterialTheme.shapes.medium,
        color = when {
            done -> c.surfaceContainer
            wrong -> c.errorContainer
            picked -> c.primaryContainer
            else -> c.surfaceContainerHighest
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
    ) {
        Box(Modifier.padding(8.dp).fillMaxWidth().then(if (done) Modifier.padding(0.dp) else Modifier), contentAlignment = Alignment.Center) {
            Box(Modifier.then(if (done) Modifier.padding(0.dp) else Modifier)) { if (!done) content() else Text("✓", color = c.primary) }
        }
    }
}
