package app.venderian.ui.learn

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.venderian.data.Letter
import app.venderian.data.Repository
import app.venderian.ui.StrokeOrderGlyph
import app.venderian.ui.VText
import app.venderian.ui.app
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(index: Int, onDone: () -> Unit) {
    val app = app()
    val lesson = app.alphabet.lessons[index]
    val pager = rememberPagerState { lesson.letters.size + 1 }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("${index + 1}. ${lesson.title}") },
            navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "back") } },
        )
        LinearProgressIndicator(
            progress = { (pager.currentPage + 1f) / pager.pageCount },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        HorizontalPager(pager, Modifier.weight(1f).testTag("pager")) { page ->
            if (page < lesson.letters.size) {
                LetterPage(lesson.letters[page], app.alphabet.strokeWidth, active = pager.currentPage == page, speak = app::speak)
            } else {
                FinishPage(lesson.letters) {
                    scope.launch {
                        app.repository.completeLesson(lesson)
                        withContext(Dispatchers.Main) { onDone() }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${minOf(pager.currentPage + 1, lesson.letters.size)} / ${lesson.letters.size}", style = MaterialTheme.typography.labelLarge)
            if (pager.currentPage < lesson.letters.size) {
                Text("swipe for the next letter →", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LetterPage(letter: Letter, strokeWidth: Float, active: Boolean, speak: (String) -> Unit) {
    var replay by remember { mutableIntStateOf(0) }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Box(contentAlignment = Alignment.BottomEnd) {
            // only animate the page that's on screen, and start over when you come back to it
            StrokeOrderGlyph(letter, strokeWidth, Modifier.height(260.dp), replay = if (active) replay else -1)
            FilledTonalIconButton(onClick = { replay++ }) { Icon(Icons.Outlined.Replay, "draw again") }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(letter.roman, fontSize = 56.sp, fontWeight = FontWeight.Bold)
            Text("/${letter.ipa}/", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp))
        }
        Text("called “${letter.name}”", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Card(
            onClick = { speak(listOfNotNull(letter.say, letter.example?.let { "as in $it" }).joinToString(", ")) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.VolumeUp, null)
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(letter.hint, style = MaterialTheme.typography.titleMedium)
                    Text("tap to hear", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun FinishPage(letters: List<Letter>, onFinish: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 1.15f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "pop")
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("nice!", style = MaterialTheme.typography.displaySmall)
        Text("you met ${letters.size} letters", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            letters.forEach {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    VText(it.glyph.toString(), size = 40.sp)
                    Text(it.roman, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("they're now in your practice deck", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { if (!pressed) { pressed = true; onFinish() } },
            modifier = Modifier.scale(scale),
        ) { Text("finish  +${Repository.XP_LESSON} xp") }
    }
}
