package app.venderian.ui.practice

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.venderian.VenderianApp
import app.venderian.convert.Sounds
import app.venderian.data.CardEntity
import app.venderian.srs.Grade
import app.venderian.ui.ScreenTitle
import app.venderian.ui.VText
import app.venderian.ui.appViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** what one side of a card shows */
sealed interface Face {
    data class Script(val text: String) : Face
    data class Words(val big: String, val small: String? = null) : Face
}

data class Flashcard(val card: CardEntity, val front: Face, val back: Face, val prompt: String)

data class PracticeState(
    val loading: Boolean = true,
    val deck: List<Flashcard> = emptyList(),
    val position: Int = 0,
    val flipped: Boolean = false,
    val reviewed: Int = 0,
    val missed: Int = 0,
) {
    val current get() = deck.getOrNull(position)
}

class PracticeViewModel(private val app: VenderianApp) : ViewModel() {
    val state = MutableStateFlow(PracticeState())

    init {
        load()
    }

    fun load() = viewModelScope.launch {
        state.value = PracticeState(loading = true)
        val deck = app.repository.practiceDeck().mapNotNull(::flashcard)
        state.value = PracticeState(loading = false, deck = deck)
    }

    /**
     * cards go both ways: glyph -> sound and sound -> glyph, word -> meaning and
     * meaning -> word. the direction flips with each successful review.
     */
    private fun flashcard(card: CardEntity): Flashcard? {
        val forward = card.reps % 2 == 0
        return when (card.kind) {
            CardEntity.GLYPH -> {
                val l = app.alphabet.letter(card.key)
                val glyph = Face.Script(l.glyph.toString())
                val sound = Face.Words(l.roman, "/${l.ipa}/ · ${l.hint}")
                if (forward) Flashcard(card, glyph, sound, "what sound is this?")
                else Flashcard(card, Face.Words(l.roman), Face.Script(l.glyph.toString()), "how is this sound written?")
            }
            CardEntity.WORD -> {
                val w = app.repository.word(card.key) ?: return null
                val script = Face.Script(Sounds.scriptOf(w.word))
                if (forward) Flashcard(card, script, Face.Words(w.meaning, "${w.word} · ${w.pos}"), "what does this mean?")
                else Flashcard(card, Face.Words(w.meaning, w.pos), script, "how do you write this?")
            }
            else -> null
        }
    }

    fun flip() {
        state.value = state.value.copy(flipped = true)
    }

    fun grade(grade: Grade) {
        val s = state.value
        val card = s.current ?: return
        viewModelScope.launch {
            val updated = app.repository.review(card.card, grade)
            // a missed card comes back at the end of this session
            val deck = if (grade == Grade.AGAIN) s.deck + card.copy(card = updated) else s.deck
            state.value = s.copy(
                deck = deck, position = s.position + 1, flipped = false,
                reviewed = s.reviewed + 1, missed = s.missed + if (grade == Grade.AGAIN) 1 else 0,
            )
        }
    }
}

@Composable
fun PracticeScreen(onOpenDictionary: () -> Unit) {
    val vm = appViewModel { PracticeViewModel(it) }
    val s by vm.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Practice", "flashcards that come back right before you'd forget them", onOpenDictionary)
        when {
            s.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            s.current == null -> Finished(s, onMore = vm::load)
            else -> {
                LinearProgressIndicator(
                    progress = { s.position.toFloat() / s.deck.size },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                )
                AnimatedContent(
                    targetState = s.position,
                    transitionSpec = { (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut()) },
                    label = "card",
                    modifier = Modifier.weight(1f),
                ) { position ->
                    val card = s.deck.getOrNull(position)
                    if (card != null) CardView(card, flipped = s.flipped && position == s.position, onFlip = vm::flip)
                }
                Grades(s.flipped, onGrade = vm::grade, onFlip = vm::flip)
            }
        }
    }
}

@Composable
private fun CardView(card: Flashcard, flipped: Boolean, onFlip: () -> Unit) {
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, tween(450), label = "flip")
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(card.prompt, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Card(
            onClick = onFlip,
            modifier = Modifier.fillMaxWidth().weight(1f).graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(Modifier.fillMaxSize().padding(16.dp), Alignment.Center) {
                if (rotation <= 90f) FaceView(card.front)
                else Box(Modifier.graphicsLayer { rotationY = 180f }) { FaceView(card.back) }
            }
        }
        Text(
            if (flipped) "how well did you know it?" else "tap the card to flip it",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 12.dp).alpha(0.7f),
        )
    }
}

@Composable
private fun FaceView(face: Face) {
    when (face) {
        is Face.Script -> VText(face.text, size = if (face.text.length <= 1) 120.sp else 64.sp, textAlign = TextAlign.Center)
        is Face.Words -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(face.big, fontSize = if (face.big.length < 6) 64.sp else 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            face.small?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun Grades(flipped: Boolean, onGrade: (Grade) -> Unit, onFlip: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!flipped) {
            Button(onClick = onFlip, modifier = Modifier.weight(1f)) { Text("show answer") }
        } else {
            val c = MaterialTheme.colorScheme
            FilledTonalButton(
                onClick = { onGrade(Grade.AGAIN) }, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = c.errorContainer, contentColor = c.onErrorContainer),
            ) { Text("again") }
            FilledTonalButton(onClick = { onGrade(Grade.HARD) }, modifier = Modifier.weight(1f)) { Text("hard") }
            Button(onClick = { onGrade(Grade.GOOD) }, modifier = Modifier.weight(1f)) { Text("good") }
            FilledTonalButton(onClick = { onGrade(Grade.EASY) }, modifier = Modifier.weight(1f)) { Text("easy") }
        }
    }
}

@Composable
private fun Finished(s: PracticeState, onMore: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (s.reviewed == 0) {
            Text("nothing due right now", style = MaterialTheme.typography.headlineSmall)
            Text("finish script lessons to add letters, and course lessons to add words", textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("done for now!", style = MaterialTheme.typography.headlineSmall)
            Text("${s.reviewed} reviews, ${s.reviewed - s.missed} right the first time",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onMore) { Text("check again") }
    }
}
