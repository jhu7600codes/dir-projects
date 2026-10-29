@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.venderian.ui.keyboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.venderian.convert.Converter
import app.venderian.convert.Sounds
import app.venderian.ui.ScreenTitle
import app.venderian.ui.VText
import app.venderian.ui.app
import kotlinx.coroutines.delay

@Composable
fun KeyboardScreen() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Keyboard", "write venderian, or type english and watch it turn into venderian")
        PrimaryTabRow(selectedTabIndex = tab) {
            Tab(tab == 0, { tab = 0 }, text = { Text("venderian keys") })
            Tab(tab == 1, { tab = 1 }, text = { Text("from english") })
        }
        AnimatedContent(tab, label = "tab", modifier = Modifier.weight(1f)) {
            if (it == 0) GlyphKeyboard() else EnglishInput()
        }
    }
}

/** the big preview of venderian text, with its romanization and copy buttons */
@Composable
private fun Preview(script: String, roman: String, placeholder: String, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    Card(
        modifier.fillMaxWidth().padding(16.dp).animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (script.isEmpty()) {
                Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.heightIn(min = 64.dp))
            } else {
                VText(script, size = 40.sp)
                Spacer(Modifier.height(6.dp))
                Text(roman, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { clipboard.setText(AnnotatedString(roman)) }, enabled = script.isNotEmpty()) { Text("copy roman") }
                FilledTonalButton(onClick = { clipboard.setText(AnnotatedString(script)) }, enabled = script.isNotEmpty()) {
                    Icon(Icons.Outlined.ContentCopy, null, Modifier.padding(end = 6.dp))
                    Text("copy")
                }
            }
        }
    }
}

@Composable
private fun GlyphKeyboard() {
    var text by rememberSaveable { mutableStateOf("") }
    var digits by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    fun type(s: String) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        text += s
    }

    Column(Modifier.fillMaxSize()) {
        Preview(text, Sounds.scriptToRoman(text), "tap the keys below", Modifier.weight(1f, fill = false))
        Spacer(Modifier.weight(1f))
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.fillMaxWidth().padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val rows = if (digits) listOf(
                    (1..5).map { "$it" }, (6..9).map { "$it" } + "0", listOf(".", ",", "?", "!"),
                ) else listOf(
                    Sounds.vowels,
                    listOf("p", "b", "f", "v", "m", "w"),
                    listOf("t", "d", "th", "s", "z", "n"),
                    listOf("k", "g", "kh", "ng", "sh", "ch"),
                    listOf("r", "l", "y", "h", ".", "?"),
                )
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { key ->
                            val glyph = if (key in Sounds.all) Sounds.glyph(key).toString() else key
                            Key(Modifier.weight(1f), onClick = { type(glyph) }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    VText(glyph, size = 26.sp)
                                    Text(key, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Key(Modifier.weight(1.3f), onClick = { digits = !digits }) { Text(if (digits) "abc" else "123") }
                    Key(Modifier.weight(3f), onClick = { if (text.isNotEmpty() && !text.endsWith(" ")) type(" ") }) { Text("word gap") }
                    Key(Modifier.weight(1.3f), onClick = { text = text.dropLast(1) }, onLongClick = { text = "" }) {
                        Icon(Icons.AutoMirrored.Outlined.Backspace, "delete")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Key(modifier: Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        modifier
            .height(58.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun EnglishInput() {
    val app = app()
    var input by rememberSaveable { mutableStateOf("") }
    var translate by rememberSaveable { mutableStateOf(false) }
    val converter by produceState<Converter?>(null) { value = app.converter.await() }
    val result by produceState<Converter.Result?>(null, input, translate, converter) {
        val c = converter ?: return@produceState
        delay(80) // let fast typing settle
        value = c.convert(input, if (translate) Converter.Mode.TRANSLATE else Converter.Mode.SOUND)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            SegmentedButton(!translate, { translate = false }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("spell by sound") }
            SegmentedButton(translate, { translate = true }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("translate") }
        }
        Text(
            if (translate) "words from the dictionary become venderian words, the rest are spelled by sound"
            else "every word is spelled the way it sounds",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        OutlinedTextField(
            input, { input = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            label = { Text("english") },
            minLines = 2,
        )
        if (converter == null) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        val r = result
        Preview(r?.script.orEmpty(), r?.roman.orEmpty(), "the venderian shows up here as you type")
        // which words came from where, so translate mode isn't a black box
        if (r != null && r.pieces.isNotEmpty()) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                r.pieces.filter { it.source != Converter.Source.PUNCTUATION }.forEach { p ->
                    Row {
                        Text(p.english, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(p.roman, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            when (p.source) {
                                Converter.Source.LEXICON -> "dictionary"
                                Converter.Source.CMUDICT -> "by sound"
                                Converter.Source.RULES -> "guessed"
                                else -> ""
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
