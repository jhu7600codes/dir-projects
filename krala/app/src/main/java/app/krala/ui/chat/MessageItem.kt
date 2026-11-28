package app.krala.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.krala.R
import app.krala.core.Markdown
import app.krala.data.db.MessageStatus
import app.krala.data.db.Role
import app.krala.ui.components.MarkdownText
import app.krala.ui.components.TypingIndicator
import app.krala.ui.theme.LocalHaptics
import coil3.compose.AsyncImage
import java.io.File

@Composable
fun MessageItem(
    m: UiMessage,
    attachmentFile: (String) -> File,
    isLastAssistant: Boolean,
    busy: Boolean,
    onEdit: () -> Unit,
    onRegenerate: () -> Unit,
    onShare: (String) -> Unit,
) {
    if (m.entity.role == Role.USER) UserMessage(m, attachmentFile, busy, onEdit, onShare)
    else AssistantMessage(m, isLastAssistant, busy, onRegenerate, onShare)
}

@Composable
private fun UserMessage(m: UiMessage, file: (String) -> File, busy: Boolean, onEdit: () -> Unit, onShare: (String) -> Unit) {
    val clipboard = LocalClipboardManager.current
    val h = LocalHaptics.current
    var menu by remember { mutableStateOf(false) }
    var viewer by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth().padding(start = 48.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), horizontalAlignment = Alignment.End) {
        if (m.images.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), reverseLayout = true, modifier = Modifier.padding(bottom = 6.dp)) {
                items(m.images) { name ->
                    AsyncImage(
                        model = file(name), contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(132.dp).clip(MaterialTheme.shapes.large).combinedClickable(onClick = { viewer = name }),
                    )
                }
            }
        }
        if (m.text.isNotBlank()) Box {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp),
                modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { h.long(); menu = true }),
            ) {
                Text(m.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
            }
            DropdownMenu(menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.copy)) }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) }, onClick = {
                    clipboard.setText(AnnotatedString(m.text)); h.confirm(); menu = false
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.edit)) }, enabled = !busy, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = {
                    menu = false; onEdit()
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.share)) }, leadingIcon = { Icon(Icons.Rounded.Share, null) }, onClick = {
                    menu = false; onShare(m.text)
                })
            }
        }
    }
    viewer?.let { name ->
        Dialog(onDismissRequest = { viewer = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().combinedClickable(onClick = { viewer = null }), contentAlignment = Alignment.Center) {
                AsyncImage(model = file(name), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun AssistantMessage(m: UiMessage, isLast: Boolean, busy: Boolean, onRegenerate: () -> Unit, onShare: (String) -> Unit) {
    val clipboard = LocalClipboardManager.current
    val h = LocalHaptics.current
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 2.dp).animateContentSize()) {
        when {
            m.streaming && !m.started -> TypingIndicator()
            m.text.isNotBlank() -> SelectionContainer { MarkdownText(m.text) }
        }
        if (m.entity.status == MessageStatus.ERROR && !m.streaming) {
            Surface(color = cs.errorContainer, contentColor = cs.onErrorContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.padding(top = 6.dp)) {
                Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ErrorOutline, null, Modifier.size(18.dp))
                    Text(
                        m.entity.error ?: stringResource(R.string.reply_failed),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                    )
                    if (isLast) TextButton(onClick = { h.click(); onRegenerate() }, enabled = !busy) { Text(stringResource(R.string.retry)) }
                }
            }
        }
        AnimatedVisibility(!m.streaming && m.text.isNotBlank(), enter = fadeIn()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                SmallAction(Icons.Rounded.ContentCopy, stringResource(R.string.copy)) {
                    clipboard.setText(AnnotatedString(Markdown.toPlain(m.text).ifBlank { m.text })); h.confirm()
                }
                if (isLast) SmallAction(Icons.Rounded.Refresh, stringResource(R.string.regenerate), enabled = !busy) { h.click(); onRegenerate() }
                SmallAction(Icons.Rounded.Share, stringResource(R.string.share)) { onShare(m.text) }
                if (m.entity.status == MessageStatus.STOPPED) {
                    Text(stringResource(R.string.stopped), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
                }
                Spacer(Modifier.weight(1f))
                val label = listOfNotNull(m.entity.providerName, m.entity.model).joinToString(" · ")
                if (label.isNotBlank()) Text(label, style = MaterialTheme.typography.labelSmall, color = cs.outline, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun SmallAction(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
        Icon(icon, desc, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
