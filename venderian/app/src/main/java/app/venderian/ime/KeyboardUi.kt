package app.venderian.ime

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.venderian.convert.Sounds
import app.venderian.ui.VText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val KEY_HEIGHT = 50.dp

@Composable
fun KeyboardUi(ime: VenderianIme) {
    val s = ime.state
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp)) {
            Strip(ime)
            AnimatedContent(
                targetState = if (s.numbersOnly) Layout.Symbols else s.layout,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "layout",
            ) { layout ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    when (layout) {
                        Layout.Qwerty -> Qwerty(ime)
                        Layout.Venderian -> Glyphs(ime)
                        Layout.Symbols -> Symbols(ime)
                    }
                }
            }
        }
    }
}

/**
 * the strip above the keys: live venderian for what you've typed, the translate button,
 * and the two switches (translate vs sound, script vs roman output)
 */
@Composable
private fun Strip(ime: VenderianIme) {
    val s = ime.state
    val tail = ime.englishTail()
    val preview = if (s.layout == Layout.Venderian) {
        // typing venderian already: show how it reads
        Sounds.scriptToRoman(s.before.takeLast(80)).trim()
    } else if (ime.converter != null) ime.convert(tail.takeLast(400)) else ""
    val previewIsScript = s.layout != Layout.Venderian && s.outputScript

    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
        FilterChip(
            selected = s.translate,
            onClick = ime::toggleTranslate,
            label = { Text(if (s.translate) "translate" else "by sound") },
        )
        Spacer(Modifier.width(6.dp))
        val scroll = rememberScrollState()
        LaunchedEffect(preview) { scroll.scrollTo(scroll.maxValue) }
        Box(Modifier.weight(1f).horizontalScroll(scroll), contentAlignment = Alignment.CenterStart) {
            when {
                preview.isEmpty() -> Text(
                    if (s.layout == Layout.Venderian) "venderian keys" else "type english, see venderian",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium,
                )
                previewIsScript -> VText(preview, size = 22.sp)
                else -> Text(preview, maxLines = 1, overflow = TextOverflow.Clip, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.width(6.dp))
        FilterChip(
            selected = s.outputScript,
            onClick = ime::toggleOutput,
            label = {
                if (s.outputScript) VText(Sounds.scriptOf("vë"), size = 16.sp) else Text("abc")
            },
        )
        if (s.layout != Layout.Venderian) {
            Spacer(Modifier.width(6.dp))
            val pop by animateFloatAsState(if (tail.isNotBlank() || s.canUndo) 1f else 0.85f, label = "fab")
            SmallFloatingActionButton(
                onClick = { if (s.canUndo) ime.undoTranslate() else ime.translateNow() },
                modifier = Modifier.scale(pop),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(if (s.canUndo) Icons.AutoMirrored.Outlined.Undo else Icons.Outlined.Translate,
                    if (s.canUndo) "undo translation" else "translate to venderian")
            }
        }
    }
}

private val QWERTY = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")

@Composable
private fun Qwerty(ime: VenderianIme) {
    val s = ime.state
    val upper = s.shift != Shift.Off
    KeyRow { QWERTY[0].forEach { c -> CharKey(if (upper) c.uppercase() else "$c") { ime.type("$c") } } }
    KeyRow(Modifier.padding(horizontal = 16.dp)) { QWERTY[1].forEach { c -> CharKey(if (upper) c.uppercase() else "$c") { ime.type("$c") } } }
    KeyRow {
        FnKey(1.5f, onClick = ime::cycleShift, highlighted = s.shift != Shift.Off) {
            Icon(if (s.shift == Shift.Locked) Icons.Filled.KeyboardCapslock else Icons.Outlined.ArrowUpward, "shift")
        }
        QWERTY[2].forEach { c -> CharKey(if (upper) c.uppercase() else "$c") { ime.type("$c") } }
        BackspaceKey(ime)
    }
    KeyRow {
        FnKey(1.5f, onClick = { ime.setLayout(Layout.Symbols) }) { Text("?123") }
        // the layout switch shows a venderian letter
        FnKey(1f, onClick = { ime.setLayout(Layout.Venderian) }, onLongClick = ime::nextKeyboard) {
            VText(Sounds.glyph("v").toString(), size = 20.sp)
        }
        CharKey(",") { ime.type(",") }
        SpaceKey("english", ime)
        CharKey(".") { ime.type(".") }
        EnterKey(ime)
    }
}

private val GLYPH_ROWS = listOf(
    Sounds.vowels,
    listOf("p", "b", "f", "v", "m", "w"),
    listOf("t", "d", "th", "s", "z", "n"),
    listOf("k", "g", "kh", "ng", "sh", "ch"),
)

@Composable
private fun Glyphs(ime: VenderianIme) {
    GLYPH_ROWS.forEach { row -> KeyRow { row.forEach { GlyphKey(it, ime) } } }
    KeyRow {
        listOf("r", "l", "y", "h").forEach { GlyphKey(it, ime) }
        CharKey("?") { ime.type("?") }
        BackspaceKey(ime)
    }
    KeyRow {
        FnKey(1.5f, onClick = { ime.setLayout(Layout.Symbols) }) { Text("?123") }
        FnKey(1f, onClick = { ime.setLayout(Layout.Qwerty) }, onLongClick = ime::nextKeyboard) { Text("abc") }
        CharKey(",") { ime.type(",") }
        SpaceKey("word gap", ime)
        CharKey(".") { ime.type(".") }
        EnterKey(ime)
    }
}

@Composable
private fun RowScope.GlyphKey(sound: String, ime: VenderianIme) {
    Key(1f, onClick = { ime.typeSound(sound) }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            VText(Sounds.glyph(sound).toString(), size = 22.sp)
            Text(sound, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Symbols(ime: VenderianIme) {
    val s = ime.state
    KeyRow { "1234567890".forEach { c -> CharKey("$c") { ime.type("$c") } } }
    KeyRow { "@#$&_-+()/".forEach { c -> CharKey("$c") { ime.type("$c") } } }
    KeyRow {
        "*\"':;!?=".forEach { c -> CharKey("$c") { ime.type("$c") } }
        BackspaceKey(ime)
    }
    KeyRow {
        if (!s.numbersOnly) FnKey(1.5f, onClick = { ime.setLayout(Layout.Qwerty) }) { Text("abc") }
        CharKey(",") { ime.type(",") }
        SpaceKey("", ime)
        CharKey(".") { ime.type(".") }
        EnterKey(ime)
    }
}

// ---- keys ----

@Composable
private fun KeyRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth().height(KEY_HEIGHT), horizontalArrangement = Arrangement.spacedBy(5.dp), content = content)
}

@Composable
private fun RowScope.Key(
    weight: Float,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    repeat: Boolean = false,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "press")
    Box(
        Modifier
            .weight(weight)
            .height(KEY_HEIGHT)
            .scale(scale)
            .clip(MaterialTheme.shapes.small)
            .background(if (pressed) MaterialTheme.colorScheme.secondaryContainer else color)
            .pointerInput(onClick, onLongClick, repeat) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        // backspace keeps deleting while held
                        val repeater = if (repeat) scope.launch {
                            delay(400)
                            while (true) {
                                onClick()
                                delay(50)
                            }
                        } else null
                        tryAwaitRelease()
                        repeater?.cancel()
                        pressed = false
                    },
                    onTap = { onClick() },
                    onLongPress = onLongClick?.let { { _ -> it() } },
                )
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun RowScope.CharKey(label: String, onClick: () -> Unit) =
    Key(1f, onClick) { Text(label, fontSize = 20.sp) }

@Composable
private fun RowScope.FnKey(
    weight: Float,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    content: @Composable () -> Unit,
) = Key(
    weight, onClick, onLongClick,
    color = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
    content = content,
)

@Composable
private fun RowScope.BackspaceKey(ime: VenderianIme) =
    Key(1.5f, onClick = ime::backspace, color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f), repeat = true) {
        Icon(Icons.AutoMirrored.Outlined.Backspace, "delete")
    }

@Composable
private fun RowScope.SpaceKey(label: String, ime: VenderianIme) =
    Key(4f, onClick = { ime.type(" ") }, onLongClick = ime::nextKeyboard) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

@Composable
private fun RowScope.EnterKey(ime: VenderianIme) {
    val icon: ImageVector = when (ime.state.enterLabel) {
        app.venderian.ime.EnterKey.Search -> Icons.Outlined.Search
        app.venderian.ime.EnterKey.Send -> Icons.AutoMirrored.Filled.Send
        app.venderian.ime.EnterKey.Go, app.venderian.ime.EnterKey.Next -> Icons.AutoMirrored.Outlined.ArrowForward
        app.venderian.ime.EnterKey.Done -> Icons.Filled.Check
        app.venderian.ime.EnterKey.Newline -> Icons.AutoMirrored.Filled.KeyboardReturn
    }
    Key(1.5f, onClick = ime::enter, color = MaterialTheme.colorScheme.primary) {
        Icon(icon, "enter", tint = MaterialTheme.colorScheme.onPrimary)
    }
}
