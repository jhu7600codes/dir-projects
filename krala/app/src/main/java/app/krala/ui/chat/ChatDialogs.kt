package app.krala.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.krala.R
import app.krala.data.provider.Provider
import app.krala.ui.theme.LocalHaptics

@Composable
fun ModelPickerSheet(
    providers: List<Provider>,
    selectedProvider: String?,
    selectedModel: String?,
    onSelect: (String?, String?) -> Unit,
    onMakeDefault: (String?, String?) -> Unit,
    onManageProviders: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val h = LocalHaptics.current
    var makeDefault by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)) {
        Text(stringResource(R.string.pick_model), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp))
        Text(stringResource(R.string.pick_model_hint), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        fun pick(p: String?, m: String?) {
            h.tick()
            onSelect(p, m)
            if (makeDefault) onMakeDefault(p, m)
            onDismiss()
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                ModelRow(
                    title = stringResource(R.string.model_auto),
                    subtitle = stringResource(R.string.model_auto_hint),
                    selected = selectedProvider == null,
                    onClick = { pick(null, null) },
                )
            }
            providers.filter { it.entity.enabled }.forEach { p ->
                item(key = p.id) {
                    Text(
                        p.name, style = MaterialTheme.typography.labelLarge, color = cs.primary, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
                    )
                }
                val models = p.models.ifEmpty { listOfNotNull(p.firstModel) }
                items(models, key = { p.id + "/" + it }) { m ->
                    ModelRow(
                        title = m,
                        subtitle = null,
                        selected = selectedProvider == p.id && (selectedModel ?: p.firstModel) == m,
                        onClick = { pick(p.id, m) },
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth().clickable { makeDefault = !makeDefault }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(makeDefault, onCheckedChange = { makeDefault = it })
                    Text(stringResource(R.string.use_for_new_chats))
                }
                TextButton(onClick = { onDismiss(); onManageProviders() }, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Icon(Icons.Rounded.Tune, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.manage_providers))
                }
            }
        }
    }
}

@Composable
private fun ModelRow(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, maxLines = 1) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { RadioButton(selected, onClick = onClick) },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp),
    )
}

@Composable
fun TextEditDialog(
    title: String,
    initial: String,
    label: String? = null,
    singleLine: Boolean = true,
    confirm: String = stringResource(R.string.save),
    extraAction: Pair<String, () -> Unit>? = null,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value, onValueChange = { value = it }, singleLine = singleLine,
                label = label?.let { { Text(it) } },
                minLines = if (singleLine) 1 else 5, maxLines = if (singleLine) 1 else 12,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value); onDismiss() }) { Text(confirm) } },
        dismissButton = {
            Row {
                extraAction?.let { (t, a) -> TextButton(onClick = { a(); onDismiss() }) { Text(t) } }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

@Composable
fun DeleteChatDialog(title: String, onConfirm: (forgetMemory: Boolean) -> Unit, onDismiss: () -> Unit) {
    var forget by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.DeleteOutline, null) },
        title = { Text(stringResource(R.string.delete_chat_q)) },
        text = {
            Column {
                Text(stringResource(R.string.delete_chat_body, title))
                Row(Modifier.fillMaxWidth().clickable { forget = !forget }.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(forget, onCheckedChange = { forget = it })
                    Text(stringResource(R.string.also_forget_memory))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(forget); onDismiss() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun ExportSheet(onShare: () -> Unit, onSaveMarkdown: () -> Unit, onSaveJson: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(stringResource(R.string.export_chat), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        ListItem(
            headlineContent = { Text(stringResource(R.string.export_share)) }, leadingContent = { Icon(Icons.Rounded.Share, null) },
            modifier = Modifier.clickable { onDismiss(); onShare() },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.export_md)) }, leadingContent = { Icon(Icons.Rounded.Description, null) },
            modifier = Modifier.clickable { onDismiss(); onSaveMarkdown() },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.export_json)) }, leadingContent = { Icon(Icons.Rounded.DataObject, null) },
            modifier = Modifier.clickable { onDismiss(); onSaveJson() },
        )
        Spacer(Modifier.height(24.dp))
    }
}
