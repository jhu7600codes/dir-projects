package app.venderian.ui.course

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.venderian.course.Course
import app.venderian.course.CourseLesson
import app.venderian.course.Kind
import app.venderian.course.Module
import app.venderian.course.Section
import kotlin.math.sin

private sealed interface Row {
    data class SectionHeader(val section: Section) : Row
    data class ModuleHeader(val module: Module) : Row
    data class Node(val lesson: CourseLesson, val number: Int, val index: Int) : Row
}

/**
 * the course as a winding path, duolingo style: sections, modules, and a node per lesson.
 * lessons unlock one after another; the whole thing waits for the script lessons.
 */
@Composable
fun CourseMap(course: Course, done: Set<String>, scriptDone: Boolean, scriptProgress: Float, onOpen: (String) -> Unit) {
    val rows = buildList {
        var index = 0
        for (section in course.sections) {
            add(Row.SectionHeader(section))
            for (module in section.modules) {
                add(Row.ModuleHeader(module))
                var number = 0
                for (lesson in module.lessons) {
                    if (lesson.kind == Kind.LESSON) number++
                    add(Row.Node(lesson, number + if (module.half) 5 else 0, index++))
                }
            }
        }
    }
    val current = course.lessons.indexOfFirst { it.id !in done }.let { if (it < 0) course.lessons.size else it }
    val state = rememberLazyListState()
    // start scrolled to where you are
    LaunchedEffect(current) {
        val at = rows.indexOfFirst { it is Row.Node && it.index == current }
        if (at > 2) state.scrollToItem(at - 2)
    }

    LazyColumn(state = state, contentPadding = PaddingValues(bottom = 32.dp)) {
        if (!scriptDone) {
            item {
                Card(
                    Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("learn the script first", style = MaterialTheme.typography.titleMedium)
                        Text("everything in the course is written in venderian, so the course opens once all 5 script lessons are done.",
                            style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator({ scriptProgress }, Modifier.fillMaxWidth())
                    }
                }
            }
        }
        items(rows, key = {
            when (it) {
                is Row.SectionHeader -> "s${it.section.id}"
                is Row.ModuleHeader -> "m${it.module.id}"
                is Row.Node -> it.lesson.id
            }
        }) { row ->
            when (row) {
                is Row.SectionHeader -> SectionCard(row.section)
                is Row.ModuleHeader -> Text(
                    "module ${row.module.id} · ${row.module.title}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp),
                )
                is Row.Node -> {
                    val isDone = row.lesson.id in done
                    val unlocked = scriptDone && row.index <= current
                    Node(row.lesson, row.number, row.index, isDone, unlocked, row.index == current && scriptDone) {
                        if (unlocked) onOpen(row.lesson.id)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(section: Section) {
    Card(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("section ${section.id}", style = MaterialTheme.typography.labelLarge)
            Text(section.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(section.subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Node(lesson: CourseLesson, number: Int, index: Int, done: Boolean, unlocked: Boolean, current: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        1f, 1.08f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse",
    )
    val (bg, fg) = when {
        lesson.kind == Kind.GOLD && done -> Color(0xFFFFC94D) to Color(0xFF3D2E00)
        done -> c.primary to c.onPrimary
        current -> c.tertiary to c.onTertiary
        unlocked -> c.secondaryContainer to c.onSecondaryContainer
        else -> c.surfaceContainerHighest to c.onSurfaceVariant
    }
    val size = if (lesson.kind == Kind.LESSON) 68.dp else 80.dp
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Column(
            // wind left and right like a path
            Modifier.offset(x = (sin(index * 0.9) * 70).dp).width(160.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(size).scale(if (current) pulse else 1f).clip(CircleShape).background(bg).clickable(enabled = unlocked, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    !unlocked -> Icon(Icons.Filled.Lock, "locked", tint = fg)
                    lesson.kind == Kind.GOLD -> Icon(Icons.Filled.EmojiEvents, "gold lesson", tint = fg, modifier = Modifier.size(36.dp))
                    lesson.kind == Kind.CHALLENGE -> Icon(Icons.Filled.Bolt, "challenge", tint = fg, modifier = Modifier.size(36.dp))
                    done -> Icon(Icons.Filled.Check, "done", tint = fg, modifier = Modifier.size(32.dp))
                    else -> Text("$number", color = fg, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                lesson.title,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                color = if (unlocked) c.onSurface else c.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
