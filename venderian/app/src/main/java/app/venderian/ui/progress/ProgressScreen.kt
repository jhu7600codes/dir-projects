package app.venderian.ui.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.venderian.data.Progress
import app.venderian.data.Repository
import app.venderian.ui.ScreenTitle
import app.venderian.ui.app
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ProgressScreen() {
    val app = app()
    val p by app.repository.progress.collectAsState(initial = Progress())
    val due by app.repository.dueCount().collectAsState(initial = 0)
    val totalLetters = app.alphabet.letters.size

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenTitle("Progress")
        // streak
        Card(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Whatshot, null, Modifier.width(48.dp).height(48.dp),
                    tint = if (p.todayXp > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f))
                Spacer(Modifier.width(16.dp))
                Column {
                    val streak by animateIntAsState(p.streak, tween(600), label = "streak")
                    Text("$streak day${if (streak == 1) "" else "s"}", fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (p.todayXp > 0) "streak, you practiced today" else "streak, do anything today to keep it",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text("longest: ${p.longestStreak}", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        // xp + level
        Card(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(20.dp)) {
                val xp by animateIntAsState(p.totalXp, tween(800), label = "xp")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$xp xp", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("level ${p.level}", style = MaterialTheme.typography.titleMedium)
                }
                val level by animateFloatAsState(p.levelProgress, tween(800), label = "level")
                LinearProgressIndicator({ level }, Modifier.fillMaxWidth().padding(vertical = 8.dp).height(8.dp))
                Text("${Progress.xpFor(p.level + 1) - p.totalXp} xp to level ${p.level + 1} · ${p.todayXp} today",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Week(p.week)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("letters", "${p.lettersLearned} / $totalLetters", Modifier.weight(1f))
            Stat("words", "${p.wordsSeen}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("mastered", "${p.mastered}", Modifier.weight(1f))
            Stat("due now", "$due", Modifier.weight(1f))
        }
        Text(
            "xp: lesson ${Repository.XP_LESSON}, flashcard ${Repository.XP_REVIEW}, quiz answer ${Repository.XP_QUIZ_RIGHT}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/** xp bars for the last 7 days */
@Composable
private fun Week(week: List<Int>) {
    val max = (week.maxOrNull() ?: 0).coerceAtLeast(1)
    val today = LocalDate.now()
    Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        week.forEachIndexed { i, xp ->
            val f by animateFloatAsState(xp / max.toFloat(), tween(700, delayMillis = i * 60), label = "bar")
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier.fillMaxWidth().fillMaxHeight(f.coerceAtLeast(0.03f))
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (xp > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
                    )
                }
                Text(
                    today.minusDays((6 - i).toLong()).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(16.dp)) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
