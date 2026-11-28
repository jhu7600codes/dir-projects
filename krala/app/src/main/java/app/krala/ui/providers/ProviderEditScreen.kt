package app.krala.ui.providers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.krala.R
import app.krala.ui.settings.BackTopBar
import app.krala.ui.theme.LocalHaptics
import kotlinx.coroutines.launch

@Composable
fun ProviderEditScreen(vm: ProviderEditViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val h = LocalHaptics.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showKey by remember { mutableStateOf(false) }
    var newModel by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    val preset = vm.preset

    Scaffold(
        topBar = {
            BackTopBar(if (s.id == null) stringResource(R.string.add_provider) else s.name.ifBlank { stringResource(R.string.provider) }, onBack, scroll) {
                if (s.id != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.delete)) }
                TextButton(onClick = { scope.launch { if (vm.save()) { h.confirm(); onBack() } } }, enabled = s.valid) { Text(stringResource(R.string.save)) }
            }
        },
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection).imePadding(),
    ) { pad ->
        if (!s.loaded) return@Scaffold
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(s.name, { v -> vm.update { it.copy(name = v) } }, label = { Text(stringResource(R.string.name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                s.baseUrl, { v -> vm.update { it.copy(baseUrl = v.trim()) } },
                label = { Text(stringResource(R.string.base_url)) }, singleLine = true,
                supportingText = { Text(stringResource(R.string.base_url_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            if (s.needsAccountId) {
                OutlinedTextField(s.accountId, { v -> vm.update { it.copy(accountId = v.trim()) } }, label = { Text(stringResource(R.string.account_id)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(
                s.apiKey, { v -> vm.update { it.copy(apiKey = v) } },
                label = { Text(stringResource(R.string.api_key)) }, singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { IconButton(onClick = { showKey = !showKey }) { Icon(if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null) } },
                supportingText = {
                    Text(
                        listOfNotNull(
                            preset?.keyHint?.takeIf { it.isNotBlank() },
                            if (preset?.needsKey == false) stringResource(R.string.no_key_needed) else null,
                            stringResource(R.string.key_stays_on_device),
                        ).joinToString(" · ")
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )

            // models
            Text(stringResource(R.string.models), style = MaterialTheme.typography.titleSmall, color = cs.primary, modifier = Modifier.padding(top = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = { h.click(); vm.fetchModels() }, enabled = s.valid && !s.fetching) {
                    if (s.fetching) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Rounded.CloudDownload, null)
                    Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.fetch_models))
                }
                Spacer(Modifier.width(8.dp))
                if (s.models.isNotEmpty()) TextButton(onClick = { vm.update { it.copy(models = emptyList(), defaultModel = null) } }) { Text(stringResource(R.string.clear)) }
            }
            s.fetchError?.let { Text(it, color = cs.error, style = MaterialTheme.typography.bodySmall) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                s.models.forEach { m ->
                    InputChip(
                        selected = m == s.defaultModel,
                        onClick = { vm.update { it.copy(defaultModel = m) } },
                        label = { Text(m, maxLines = 1) },
                        leadingIcon = if (m == s.defaultModel) ({ Icon(Icons.Rounded.Star, null, Modifier.size(16.dp)) }) else null,
                        trailingIcon = {
                            Icon(Icons.Rounded.Close, stringResource(R.string.remove), Modifier.size(16.dp).clickableNoRipple {
                                vm.update { st -> st.copy(models = st.models - m, defaultModel = if (st.defaultModel == m) (st.models - m).firstOrNull() else st.defaultModel) }
                            })
                        },
                    )
                }
            }
            if (s.models.isNotEmpty()) Text(stringResource(R.string.default_model_hint), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            OutlinedTextField(
                newModel, { newModel = it }, label = { Text(stringResource(R.string.add_model)) }, singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        val m = newModel.trim()
                        if (m.isNotEmpty()) vm.update { it.copy(models = (it.models + m).distinct(), defaultModel = it.defaultModel ?: m) }
                        newModel = ""
                    }, enabled = newModel.isNotBlank()) { Icon(Icons.Rounded.Add, null) }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                s.contextSize, { v -> vm.update { it.copy(contextSize = v.filter(Char::isDigit).take(8)) } },
                label = { Text(stringResource(R.string.context_size)) }, singleLine = true,
                supportingText = { Text(stringResource(R.string.context_size_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.vision), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.vision_hint), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
                Switch(s.vision, onCheckedChange = { v -> vm.update { it.copy(vision = v) } })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.enabled), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(s.enabled, onCheckedChange = { v -> vm.update { it.copy(enabled = v) } })
            }

            // headers
            Text(stringResource(R.string.custom_headers), style = MaterialTheme.typography.titleSmall, color = cs.primary, modifier = Modifier.padding(top = 8.dp))
            s.headers.forEachIndexed { i, row ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(row.key, { v -> vm.update { it.copy(headers = it.headers.toMutableList().also { l -> l[i] = row.copy(key = v) }) } },
                        label = { Text(stringResource(R.string.header)) }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(row.value, { v -> vm.update { it.copy(headers = it.headers.toMutableList().also { l -> l[i] = row.copy(value = v) }) } },
                        label = { Text(stringResource(R.string.value)) }, singleLine = true, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.update { it.copy(headers = it.headers.toMutableList().also { l -> l.removeAt(i) }) } }) { Icon(Icons.Rounded.Close, stringResource(R.string.remove)) }
                }
            }
            TextButton(onClick = { vm.update { it.copy(headers = it.headers + HeaderRow()) } }) {
                Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.add_header))
            }

            // test
            Button(onClick = { h.click(); vm.test() }, enabled = s.valid && !s.testing, modifier = Modifier.fillMaxWidth()) {
                if (s.testing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = cs.onPrimary) else Icon(Icons.Rounded.NetworkCheck, null)
                Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.test_connection))
            }
            AnimatedVisibility(s.test != null) {
                s.test?.let { t ->
                    Surface(
                        color = if (t.ok) cs.secondaryContainer else cs.errorContainer,
                        contentColor = if (t.ok) cs.onSecondaryContainer else cs.onErrorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (t.ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline, null)
                                Text(
                                    stringResource(if (t.ok) R.string.test_ok else R.string.test_failed),
                                    style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp).weight(1f),
                                )
                                Text(stringResource(R.string.latency_ms, t.latencyMs), style = MaterialTheme.typography.labelLarge)
                            }
                            Text(
                                (t.httpCode?.let { "http $it · " } ?: "") + t.detail,
                                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp),
                            )
                            if (t.ok && t.models.isNotEmpty() && t.models != s.models) {
                                TextButton(onClick = { vm.update { it.copy(models = t.models, defaultModel = it.defaultModel?.takeIf { d -> d in t.models } ?: t.models.first()) } }) {
                                    Text(stringResource(R.string.use_these_models, t.models.size))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(48.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_provider_q)) },
            text = { Text(stringResource(R.string.delete_provider_body, s.name)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; scope.launch { vm.delete(); h.long(); onBack() } }, colors = ButtonDefaults.textButtonColors(contentColor = cs.error)) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.clickable(interactionSource = src, indication = null, onClick = onClick)
}
