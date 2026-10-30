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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
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

private sealed interface MapRow {
    data class SectionHeader(val section: Section) : MapRow
    data class ModuleHeader(val module: Module) : MapRow
    data class Node(val lesson: CourseLesson, val number: Int, val index: Int) : MapRow
    data class SectionPractice(val section: Section) : MapRow
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
            add(MapRow.SectionHeader(section))
            for (module in section.modules) {
                add(MapRow.ModuleHeader(module))
                var number = 0
                for (lesson in module.lessons) {
                    if (lesson.kind == Kind.LESSON) number++
                    add(MapRow.Node(lesson, number + if (module.half) 10 else 0, index++))
                }
            }
            add(MapRow.SectionPractice(section))
        }
    }
    val current = course.lessons.indexOfFirst { it.id !in done }.let { if (it < 0) course.lessons.size else it }
    val state = rememberLazyListState()
    // start scrolled to where you are
    LaunchedEffect(current) {
        // the whole course done: straight to the practice lessons at the end
        if (current >= course.lessons.size) state.scrollToItem(rows.size + if (scriptDone) 0 else 1)
        else {
            val at = rows.indexOfFirst { it is MapRow.Node && it.index == current }
            if (at > 2) state.scrollToItem(at - 2)
        }
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
                is MapRow.SectionHeader -> "s${it.section.id}"
                is MapRow.ModuleHeader -> "m${it.module.id}"
                is MapRow.Node -> it.lesson.id
                is MapRow.SectionPractice -> "ps${it.section.id}"
            }
        }) { row ->
            when (row) {
                is MapRow.SectionHeader -> SectionCard(row.section)
                is MapRow.ModuleHeader -> Text(
                    "module ${row.module.id} · ${row.module.title}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp),
                )
                is MapRow.SectionPractice -> {
                    val all = row.section.modules.flatMap { it.lessons }
                    Practice(
                        title = "section ${row.section.id} practice",
                        unlocked = all.all { it.id in done },
                        open = "mixes lessons from section ${row.section.id}, the ones you got wrong or haven't seen in a while come up more.",
                        locked = "finish section ${row.section.id} to unlock its practice lessons.",
                        onStart = { onOpen(CourseLesson.sectionPracticeId(row.section.id)) },
                    )
                }
                is MapRow.Node -> {
                    val isDone = row.lesson.id in done
                    val unlocked = scriptDone && row.index <= current
                    Node(row.lesson, row.number, row.index, isDone, unlocked, row.index == current && scriptDone) {
                        if (unlocked) onOpen(row.lesson.id)
                    }
                }
            }
        }
        item(key = "practice") {
            Practice(
                title = "practice everything",
                unlocked = current >= course.lessons.size,
                open = "each one mixes lessons from every section, so you don't forget. the ones you got wrong or haven't seen in a while come up more.",
                locked = "finish the whole course to unlock practice lessons: mixes of everything, so you don't forget.",
                onStart = { onOpen(CourseLesson.PRACTICE_ID) },
            )
        }
    }
}

/** after the whole course: endless practice lessons mixing every section */
@Composable
private fun Practice(title: String, unlocked: Boolean, open: String, locked: String, onStart: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 28.dp),
        colors = CardDefaults.cardColors(containerColor = if (unlocked) c.tertiaryContainer else c.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (unlocked) Icons.Filled.Refresh else Icons.Filled.Lock, null)
                Text("  $title", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(if (unlocked) open else locked, style = MaterialTheme.typography.bodyMedium)
            if (unlocked) {
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("start a practice lesson") }
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
    val size = if (lesson.kind == Kind.LESSON || lesson.kind == Kind.STORY) 68.dp else 80.dp
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
                    lesson.kind == Kind.STORY -> Icon(Icons.AutoMirrored.Filled.Chat, "story", tint = fg, modifier = Modifier.size(32.dp))
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
