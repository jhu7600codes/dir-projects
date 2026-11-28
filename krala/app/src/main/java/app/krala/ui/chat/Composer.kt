package app.krala.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.krala.R
import app.krala.core.KralaTags
import app.krala.core.RecallQuery
import app.krala.data.db.MemoryEntryEntity
import app.krala.data.memory.RecallResult
import app.krala.ui.theme.LocalHaptics
import coil3.compose.AsyncImage
import java.io.File

@Composable
fun ChatComposer(
    state: Composer,
    busy: Boolean,
    sendOnEnter: Boolean,
    incognito: Boolean,
    attachmentFile: (String) -> File,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: (String) -> Unit,
    onRemoveRecall: (KralaTags.RecallLine) -> Unit,
    onCancelEdit: () -> Unit,
    onTalk: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val h = LocalHaptics.current
    val text = state.text
    val isCommand = text.trimStart().startsWith("/recall", ignoreCase = true)
    val canSend = !isCommand && (text.isNotBlank() || state.images.isNotEmpty()) && !state.importing
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = if (incognito) cs.surfaceContainerHighest else cs.surfaceContainerHigh,
        tonalElevation = 2.dp,
    ) {
        Column(Modifier.padding(6.dp)) {
            AnimatedVisibility(state.editingId != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Edit, null, Modifier.size(16.dp), tint = cs.primary)
                    Text(stringResource(R.string.editing_message), style = MaterialTheme.typography.labelLarge, color = cs.primary, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    TextButton(onClick = onCancelEdit) { Text(stringResource(R.string.cancel)) }
                }
            }
            if (state.recall.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(horizontal = 6.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                    items(state.recall) { line ->
                        InputChip(
                            selected = true,
                            onClick = { onRemoveRecall(line) },
                            label = { Text(line.label.substringBefore(" · "), maxLines = 1) },
                            leadingIcon = { Icon(Icons.Rounded.History, null, Modifier.size(16.dp)) },
                            trailingIcon = { Icon(Icons.Rounded.Close, stringResource(R.string.remove), Modifier.size(16.dp)) },
                        )
                    }
                }
            }
            if (state.images.isNotEmpty() || state.importing) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
                    items(state.images) { name ->
                        Box {
                            AsyncImage(attachmentFile(name), null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.medium))
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(3.dp).size(22.dp).clip(CircleShape).background(cs.scrim.copy(alpha = 0.6f))
                                    .clickable { onRemoveImage(name) },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Rounded.Close, stringResource(R.string.remove), Modifier.size(14.dp), tint = cs.inverseOnSurface) }
                        }
                    }
                    if (state.importing) item {
                        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }
                    }
                }
            }
            // slash command suggestion
            AnimatedVisibility(text.startsWith("/") && !isCommand && "/recall".startsWith(text.trim().lowercase())) {
                SuggestionChip(
                    onClick = { h.tick(); onInput("/recall ") },
                    label = { Text("/recall · " + stringResource(R.string.recall_hint)) },
                    icon = { Icon(Icons.Rounded.History, null, Modifier.size(16.dp)) },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                IconButton(onClick = { h.click(); onPickImage() }, enabled = state.editingId == null) {
                    Icon(Icons.Rounded.AddPhotoAlternate, stringResource(R.string.attach_image), tint = cs.onSurfaceVariant)
                }
                Box(Modifier.weight(1f).padding(vertical = 12.dp, horizontal = 2.dp), contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) {
                        Text(
                            stringResource(if (incognito) R.string.composer_hint_incognito else R.string.composer_hint),
                            style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant.copy(alpha = 0.75f),
                        )
                    }
                    var field by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
                    if (field.text != text) field = TextFieldValue(text, TextRange(text.length))
                    BasicTextField(
                        value = field,
                        onValueChange = { field = it; if (it.text != text) onInput(it.text) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
                        cursorBrush = SolidColor(cs.primary),
                        maxLines = 7,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = if (sendOnEnter) ImeAction.Send else ImeAction.Default,
                        ),
                        keyboardActions = KeyboardActions(onSend = { if (canSend && !busy) { h.confirm(); onSend() } }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                AnimatedContent(
                    targetState = busy,
                    transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                    label = "send",
                    modifier = Modifier.padding(4.dp),
                ) { isBusy ->
                    if (isBusy) {
                        FilledTonalIconButton(onClick = { h.click(); onStop() }) { Icon(Icons.Rounded.Stop, stringResource(R.string.stop)) }
                    } else if (onTalk != null && text.isBlank() && state.images.isEmpty() && state.editingId == null) {
                        FilledTonalIconButton(onClick = { h.click(); onTalk() }) { Icon(Icons.Rounded.GraphicEq, stringResource(R.string.talk_mode)) }
                    } else {
                        FilledIconButton(
                            onClick = { h.confirm(); onSend() },
                            enabled = canSend,
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = cs.primary, contentColor = cs.onPrimary),
                        ) {
                            Icon(if (state.editingId != null) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.Send, stringResource(R.string.send))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecallPanelView(
    panel: RecallPanel,
    label: (String, Long) -> String,
    onOpenChat: (String) -> Unit,
    onGiveResult: (RecallResult) -> Unit,
    onGiveMemory: (MemoryEntryEntity) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    AnimatedVisibility(panel !is RecallPanel.Hidden, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut(), modifier = modifier) {
        Surface(
            shape = MaterialTheme.shapes.large, color = cs.surfaceContainer, tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).heightIn(max = 360.dp),
        ) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.History, null, Modifier.size(18.dp), tint = cs.primary)
                    Text(
                        when (panel) {
                            is RecallPanel.Results -> stringResource(R.string.recall_results, panel.results.size)
                            else -> stringResource(R.string.recall_latest)
                        },
                        style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).padding(start = 8.dp),
                    )
                    IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.close)) }
                }
                when (panel) {
                    is RecallPanel.Latest -> if (panel.entries.isEmpty()) {
                        Text(stringResource(R.string.recall_no_memory), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                    } else LazyColumn(contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(panel.entries, key = { it.id }) { e ->
                            RecallCard(
                                title = if (e.kind == "archive") stringResource(R.string.memory_archive) else e.chatName,
                                date = label("", e.updatedAt).removePrefix(" · "),
                                excerpt = e.notes.take(260),
                                onOpen = e.chatId?.let { id -> { onOpenChat(id) } },
                                onGive = { onGiveMemory(e) },
                            )
                        }
                    }
                    is RecallPanel.Results -> if (panel.results.isEmpty()) {
                        Text(stringResource(R.string.recall_nothing, panel.query), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                    } else LazyColumn(contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(panel.results) { r ->
                            val chatId = when (r) {
                                is RecallResult.FromChat -> r.hit.conversationId
                                is RecallResult.FromMemory -> r.hit.chatId
                            }
                            RecallCard(
                                title = (if (r is RecallResult.FromMemory) "★ " else "") + r.title.ifBlank { stringResource(R.string.untitled) },
                                date = label("", r.date).removePrefix(" · "),
                                excerpt = r.excerpt,
                                onOpen = chatId?.let { id -> { onOpenChat(id) } },
                                onGive = { onGiveResult(r) },
                            )
                        }
                    }
                    RecallPanel.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp))
                    RecallPanel.Hidden -> Unit
                }
            }
        }
    }
}

@Composable
private fun RecallCard(title: String, date: String, excerpt: String, onOpen: (() -> Unit)?, onGive: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val h = LocalHaptics.current
    ElevatedCard(
        onClick = { onOpen?.invoke() },
        enabled = onOpen != null,
        colors = CardDefaults.elevatedCardColors(containerColor = cs.surfaceContainerHigh, disabledContainerColor = cs.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                Text("  $date", style = MaterialTheme.typography.labelSmall, color = cs.outline)
            }
            Text(highlighted(excerpt), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 4, modifier = Modifier.padding(top = 4.dp, end = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (onOpen != null) TextButton(onClick = { h.click(); onOpen() }) { Text(stringResource(R.string.open)) }
                TextButton(onClick = { h.confirm(); onGive() }) {
                    Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.give_to_krala))
                }
            }
        }
    }
}

@Composable
private fun highlighted(s: String) = buildAnnotatedString {
    val primary = MaterialTheme.colorScheme.primary
    var i = 0
    while (i < s.length) {
        val start = s.indexOf(RecallQuery.HL_START, i)
        if (start < 0) { append(s.substring(i)); break }
        append(s.substring(i, start))
        val end = s.indexOf(RecallQuery.HL_END, start + 1).let { if (it < 0) s.length else it }
        withStyle(SpanStyle(color = primary, fontWeight = FontWeight.SemiBold)) { append(s.substring(start + 1, end)) }
        i = end + 1
    }
}
