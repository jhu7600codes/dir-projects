package app.krala.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.krala.R
import app.krala.core.Commands
import app.krala.core.PluginSpec

/** one row of the "/" suggestion list */
data class CommandHint(val name: String, val description: String, val plugin: Boolean)

@Composable
fun builtInHints(): List<CommandHint> = listOf(
    CommandHint("help", stringResource(R.string.cmd_help), false),
    CommandHint("web", stringResource(R.string.cmd_web), false),
    CommandHint("recall", stringResource(R.string.recall_hint), false),
    CommandHint("plugin", stringResource(R.string.cmd_plugin), false),
    CommandHint("plugins", stringResource(R.string.cmd_plugins), false),
    CommandHint("new", stringResource(R.string.cmd_new), false),
    CommandHint("incognito", stringResource(R.string.cmd_incognito), false),
    CommandHint("talk", stringResource(R.string.cmd_talk), false),
    CommandHint("model", stringResource(R.string.cmd_model), false),
    CommandHint("prompt", stringResource(R.string.cmd_prompt), false),
    CommandHint("rename", stringResource(R.string.cmd_rename), false),
    CommandHint("export", stringResource(R.string.cmd_export), false),
)

/** shown above the composer while typing a command name */
@Composable
fun CommandSuggestions(text: String, hints: List<CommandHint>, onPick: (String) -> Unit) {
    val t = text.trimStart()
    if (!t.startsWith("/") || t.contains(' ')) return
    val q = t.drop(1).lowercase()
    val matches = hints.filter { it.name.startsWith(q) }.take(6)
    if (matches.isEmpty() || (matches.size == 1 && matches[0].name == q)) return
    Surface(
        shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            matches.forEach { h ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick("/" + h.name + " ") }.padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (h.plugin) Icons.Rounded.Extension else Icons.Rounded.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("/" + h.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 10.dp))
                    Text(
                        h.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
        }
    }
}

/** check and edit a plugin before saving it */
@Composable
fun PluginEditDialog(initial: PluginSpec, isNew: Boolean, onSave: (PluginSpec) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var desc by remember { mutableStateOf(initial.description) }
    var instr by remember { mutableStateOf(initial.instructions) }
    var web by remember { mutableStateOf(initial.web) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Extension, null) },
        title = { Text(stringResource(if (isNew) R.string.plugin_new else R.string.plugin_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = Commands.slug(it).let { s -> if (it.endsWith("-") || it.endsWith(" ")) "$s-" else s } },
                    label = { Text(stringResource(R.string.plugin_command)) }, prefix = { Text("/") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(desc, { desc = it }, label = { Text(stringResource(R.string.plugin_when)) }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(instr, { instr = it }, label = { Text(stringResource(R.string.plugin_instructions)) }, minLines = 4, maxLines = 10, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.plugin_web), modifier = Modifier.weight(1f))
                    Switch(web, onCheckedChange = { web = it })
                }
                Text(stringResource(R.string.plugin_how), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(PluginSpec(name.trim('-'), desc, instr, web)); onDismiss() }, enabled = name.isNotBlank() && instr.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                onDelete?.let { d ->
                    TextButton(onClick = { d(); onDismiss() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.delete)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

@Composable
fun HelpSheet(hints: List<CommandHint>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(stringResource(R.string.commands), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            items(hints) { h ->
                ListItem(
                    headlineContent = { Text("/" + h.name) },
                    supportingContent = { Text(h.description) },
                    leadingContent = { Icon(if (h.plugin) Icons.Rounded.Extension else Icons.Rounded.Terminal, null) },
                    modifier = Modifier.clickable { onDismiss(); onPick("/" + h.name + " ") },
                )
            }
        }
    }
}
