package app.krala.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.krala.core.Markdown
import app.krala.core.Markdown.Block
import app.krala.core.Markdown.Span
import app.krala.ui.theme.LocalHaptics
import app.krala.ui.theme.MonoStyle
import kotlinx.coroutines.delay

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    val blocks = remember(text) { Markdown.parse(text) }
    val style = MaterialTheme.typography.bodyLarge.copy(color = color)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { b -> BlockView(b, style) }
    }
}

@Composable
private fun BlockView(b: Block, base: TextStyle) {
    val cs = MaterialTheme.colorScheme
    when (b) {
        is Block.Heading -> {
            val st = when (b.level) {
                1 -> MaterialTheme.typography.headlineSmall
                2 -> MaterialTheme.typography.titleLarge
                3 -> MaterialTheme.typography.titleMedium
                else -> MaterialTheme.typography.titleSmall
            }
            Text(inline(b.text), style = st.copy(color = base.color, fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(top = 4.dp))
        }
        is Block.Paragraph -> Text(inline(b.text), style = base)
        is Block.Code -> CodeBlock(b.language, b.code)
        is Block.ListItem -> Row(Modifier.padding(start = (b.depth * 16).dp)) {
            when {
                b.checked != null -> Icon(
                    if (b.checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank, null,
                    Modifier.padding(top = 2.dp, end = 6.dp).size(18.dp), tint = cs.primary,
                )
                b.ordered -> Text("${b.number}.", style = base.copy(color = cs.primary, fontWeight = FontWeight.Medium), modifier = Modifier.widthIn(min = 22.dp).padding(end = 4.dp))
                else -> Text(if (b.depth % 2 == 0) "•" else "◦", style = base.copy(color = cs.primary), modifier = Modifier.width(18.dp))
            }
            Text(inline(b.text), style = base)
        }
        is Block.Quote -> Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(cs.primary.copy(alpha = 0.6f), RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Markdown.parse(b.text).forEach { BlockView(it, base.copy(color = cs.onSurfaceVariant)) }
            }
        }
        is Block.Table -> TableView(b, base)
        Block.Rule -> HorizontalDivider(Modifier.padding(vertical = 4.dp), color = cs.outlineVariant)
    }
}

@Composable
fun inline(text: String): AnnotatedString {
    val cs = MaterialTheme.colorScheme
    val codeStyle = SpanStyle(fontFamily = FontFamily.Monospace, background = cs.surfaceContainerHighest, color = cs.tertiary, fontSize = 14.sp)
    val linkStyles = TextLinkStyles(SpanStyle(color = cs.primary, textDecoration = TextDecoration.Underline))
    val spans = remember(text) { Markdown.inline(text) }
    return remember(spans, cs) {
        buildAnnotatedString {
            fun emit(list: List<Span>) {
                list.forEach { sp ->
                    when (sp) {
                        is Span.Text -> append(sp.text)
                        is Span.InlineCode -> withStyle(codeStyle) { append(" "); append(sp.code); append(" ") }
                        is Span.Styled -> withStyle(
                            SpanStyle(
                                fontWeight = if (sp.bold) FontWeight.SemiBold else null,
                                fontStyle = if (sp.italic) FontStyle.Italic else null,
                                textDecoration = if (sp.strike) TextDecoration.LineThrough else null,
                            )
                        ) { emit(sp.children) }
                        is Span.Link -> {
                            val url = sp.url
                            if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("mailto:")) {
                                withLink(LinkAnnotation.Url(url, linkStyles)) { emit(sp.children) }
                            } else emit(sp.children)
                        }
                    }
                }
            }
            emit(spans)
        }
    }
}

@Composable
fun CodeBlock(language: String, code: String) {
    val cs = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHaptics.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1800); copied = false } }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().border(1.dp, cs.outlineVariant.copy(alpha = 0.5f), MaterialTheme.shapes.medium),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(cs.surfaceContainerHighest).padding(start = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    language.ifBlank { "code" }.lowercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString(code))
                    haptics.confirm()
                    copied = true
                }) {
                    AnimatedContent(copied, label = "copy") { done ->
                        Icon(
                            if (done) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                            contentDescription = "copy code",
                            modifier = Modifier.size(18.dp),
                            tint = if (done) cs.primary else cs.onSurfaceVariant,
                        )
                    }
                }
            }
            Box(Modifier.horizontalScroll(rememberScrollState()).padding(14.dp)) {
                Text(code, style = MonoStyle.copy(color = cs.onSurface), softWrap = false)
            }
        }
    }
}

@Composable
private fun TableView(t: Block.Table, base: TextStyle) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = MaterialTheme.shapes.small, color = cs.surfaceContainerLow, modifier = Modifier.border(1.dp, cs.outlineVariant, MaterialTheme.shapes.small)) {
        Box(Modifier.horizontalScroll(rememberScrollState())) {
            Column {
                Row(Modifier.background(cs.surfaceContainerHigh)) {
                    t.header.forEach { h ->
                        Text(inline(h), style = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp), modifier = Modifier.width(150.dp).padding(10.dp))
                    }
                }
                t.rows.forEach { r ->
                    HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.6f))
                    Row {
                        r.forEach { c ->
                            Text(inline(c), style = base.copy(fontSize = 14.sp), modifier = Modifier.width(150.dp).padding(10.dp))
                        }
                    }
                }
            }
        }
    }
}
