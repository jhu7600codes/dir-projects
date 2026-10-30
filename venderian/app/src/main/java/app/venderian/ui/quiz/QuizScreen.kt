package app.venderian.ui.quiz

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.venderian.VenderianApp
import app.venderian.convert.Sounds
import app.venderian.data.Repository
import app.venderian.ui.ScreenTitle
import app.venderian.ui.VText
import app.venderian.ui.appViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface Question {
    val prompt: String

    /** pick one of [options]; [script] says the options are venderian text */
    data class Choice(
        override val prompt: String,
        val shown: String,
        val shownIsScript: Boolean,
        val options: List<String>,
        val answer: Int,
        val script: Boolean,
    ) : Question

    /** type the romanization of [shown] */
    data class Typed(override val prompt: String, val shown: String, val shownIsScript: Boolean, val answer: String) : Question
}

data class QuizState(
    val questions: List<Question> = emptyList(),
    val index: Int = 0,
    /** null until the current question is answered */
    val lastCorrect: Boolean? = null,
    val picked: Int? = null,
    val score: Int = 0,
) {
    val current get() = questions.getOrNull(index)
    val finished get() = questions.isNotEmpty() && index >= questions.size
}

class QuizViewModel(private val app: VenderianApp) : ViewModel() {
    val state = MutableStateFlow(QuizState())

    init {
        start()
    }

    /**
     * only asks about what you've learned: letters from finished script lessons, words from
     * finished course lessons, and reading practice with words made of letters you know.
     */
    fun start() = viewModelScope.launch {
        val done = app.repository.lessonsDone.first()
        val alphabet = app.alphabet
        // letters from finished lessons, or the first lesson if none are finished yet
        val letters = alphabet.lessons.filter { it.index in done }.flatMap { it.letters }
            .ifEmpty { alphabet.lessons.first().letters }
        val letterIds = letters.map { it.id }.toSet()
        val learned = app.repository.learnedWords()
        // words you can read with the letters you know (no meanings: that's for the course)
        val readable = app.repository.cardWords.filter { w ->
            w.word.length in 2..6 && Sounds.parseRoman(w.word).all { it in letterIds }
        }

        fun <T> pick(right: T, pool: List<T>): Pair<List<T>, Int> {
            val opts = (pool.distinct().filter { it != right }.shuffled().take(3) + right).shuffled()
            return opts to opts.indexOf(right)
        }
        val letterPool = (if (letters.size >= 4) letters else alphabet.letters)

        val kinds = buildList {
            add(0); add(1)
            if (readable.isNotEmpty()) add(2)
            if (learned.size >= 4) { add(3); add(4) }
        }
        val qs = List(10) { n ->
            when (kinds[n % kinds.size]) {
                0 -> letters.random().let { l ->
                    val (opts, i) = pick(l.roman, letterPool.map { it.roman })
                    Question.Choice("what sound is this?", l.glyph.toString(), true, opts, i, script = false)
                }
                1 -> letters.random().let { l ->
                    val (opts, i) = pick(l.glyph.toString(), letterPool.map { it.glyph.toString() })
                    Question.Choice("which letter says “${l.roman}”?", l.roman, false, opts, i, script = true)
                }
                2 -> readable.random().let { w ->
                    Question.Typed("read it: type what this says", Sounds.scriptOf(w.word), true, w.word)
                }
                3 -> learned.random().let { w ->
                    val (opts, i) = pick(w.meaning, learned.map { it.meaning })
                    Question.Choice("what does this mean?", Sounds.scriptOf(w.word), true, opts, i, script = false)
                }
                else -> learned.random().let { w ->
                    val (opts, i) = pick(Sounds.scriptOf(w.word), learned.map { Sounds.scriptOf(it.word) })
                    Question.Choice("which one is “${w.meaning}”?", w.meaning, false, opts, i, script = true)
                }
            }
        }
        state.value = QuizState(questions = qs)
    }

    fun choose(i: Int) {
        val s = state.value
        val q = s.current as? Question.Choice ?: return
        if (s.lastCorrect != null) return
        answered(i == q.answer, picked = i)
    }

    fun type(text: String) {
        val s = state.value
        val q = s.current as? Question.Typed ?: return
        if (s.lastCorrect != null) return
        answered(normalize(text) == normalize(q.answer), picked = null)
    }

    private fun answered(correct: Boolean, picked: Int?) {
        val s = state.value
        state.value = s.copy(lastCorrect = correct, picked = picked, score = s.score + if (correct) 1 else 0)
        if (correct) viewModelScope.launch { app.repository.addXp(Repository.XP_QUIZ_RIGHT) }
    }

    fun next() {
        val s = state.value
        state.value = s.copy(index = s.index + 1, lastCorrect = null, picked = null)
    }

    companion object {
        /** forgiving about case, spaces, hyphens and how ë was typed (ë, e:, or ё) */
        fun normalize(s: String) = s.trim().lowercase().replace("e:", "ë").replace('ё', 'ë').replace("-", "").replace(" ", "")
    }
}

@Composable
fun QuizScreen() {
    val vm = appViewModel { QuizViewModel(it) }
    val s by vm.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Quiz", "10 questions, ${Repository.XP_QUIZ_RIGHT} xp for each right answer")
        if (s.finished) {
            Results(s, onAgain = vm::start)
            return@Column
        }
        val q = s.current ?: return@Column
        LinearProgressIndicator(
            progress = { s.index.toFloat() / s.questions.size },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )
        AnimatedContent(
            s.index,
            transitionSpec = { (fadeIn(tween(250)) + scaleIn(initialScale = 0.92f)) togetherWith fadeOut(tween(150)) },
            label = "question",
            modifier = Modifier.weight(1f),
        ) { index ->
            val question = s.questions.getOrNull(index) ?: return@AnimatedContent
            Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(question.prompt, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                when (question) {
                    is Question.Choice -> {
                        Shown(question.shown, question.shownIsScript, s.lastCorrect)
                        Spacer(Modifier.height(24.dp))
                        question.options.forEachIndexed { i, opt -> OptionButton(opt, question.script, i, question.answer, s.picked, s.lastCorrect != null) { vm.choose(i) } }
                    }
                    is Question.Typed -> {
                        Shown(question.shown, question.shownIsScript, s.lastCorrect)
                        Spacer(Modifier.height(24.dp))
                        TypeAnswer(question, s.lastCorrect, onSubmit = vm::type)
                    }
                }
            }
        }
        if (s.lastCorrect != null) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (s.lastCorrect == true) "right! +${Repository.XP_QUIZ_RIGHT} xp" else "not quite",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (s.lastCorrect == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = vm::next) { Text(if (s.index == s.questions.size - 1) "see results" else "next") }
            }
        }
    }
}

/** the thing being asked about. it gives a little shake when the answer was wrong, and a hop when right */
@Composable
private fun Shown(text: String, script: Boolean, correct: Boolean?) {
    val offset = remember(text) { Animatable(0f) }
    LaunchedEffect(correct) {
        when (correct) {
            false -> for (x in listOf(18f, -14f, 10f, -6f, 0f)) offset.animateTo(x, tween(55))
            true -> {
                offset.animateTo(-1f, tween(1))
                offset.animateTo(0f, spring(Spring.DampingRatioHighBouncy))
            }
            null -> offset.snapTo(0f)
        }
    }
    Card(
        Modifier.fillMaxWidth().graphicsLayer {
            translationX = if (correct == false) offset.value * density else 0f
            val hop = if (correct == true) 1f + 0.06f * -offset.value else 1f
            scaleX = hop
            scaleY = hop
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (script) VText(text, size = if (text.length <= 1) 96.sp else 56.sp, textAlign = TextAlign.Center)
            else Text(text, fontSize = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun OptionButton(text: String, script: Boolean, i: Int, answer: Int, picked: Int?, revealed: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val colors = when {
        revealed && i == answer -> ButtonDefaults.outlinedButtonColors(containerColor = c.primaryContainer, contentColor = c.onPrimaryContainer)
        revealed && i == picked -> ButtonDefaults.outlinedButtonColors(containerColor = c.errorContainer, contentColor = c.onErrorContainer)
        else -> ButtonDefaults.outlinedButtonColors()
    }
    OutlinedButton(onClick = onClick, colors = colors, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(64.dp)) {
        if (script) VText(text, size = 36.sp) else Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun TypeAnswer(q: Question.Typed, correct: Boolean?, onSubmit: (String) -> Unit) {
    var value by remember(q) { mutableStateOf(TextFieldValue("")) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value, { value = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = correct == null,
            label = { Text("answer") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onDone = { onSubmit(value.text) }),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // ë isn't on most keyboards
            AssistChip(onClick = {
                val t = value.text.substring(0, value.selection.start) + "ë" + value.text.substring(value.selection.end)
                value = TextFieldValue(t, TextRange(value.selection.start + 1))
            }, label = { Text("ë") }, enabled = correct == null)
            Spacer(Modifier.weight(1f))
            Button(onClick = { onSubmit(value.text) }, enabled = correct == null && value.text.isNotBlank()) { Text("check") }
        }
        if (correct == false) {
            Text("it's “${q.answer}”", style = MaterialTheme.typography.titleMedium)
            VText(Sounds.scriptOf(q.answer), size = 40.sp)
        }
    }
}

@Composable
private fun Results(s: QuizState, onAgain: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("${s.score} / ${s.questions.size}", fontSize = 72.sp, fontWeight = FontWeight.Bold)
        Text(
            when {
                s.score == s.questions.size -> "perfect!"
                s.score >= s.questions.size * 0.7 -> "really good"
                else -> "keep practicing, you've got this"
            },
            style = MaterialTheme.typography.titleLarge,
        )
        Text("+${s.score * Repository.XP_QUIZ_RIGHT} xp", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAgain) { Text("another quiz") }
    }
}
