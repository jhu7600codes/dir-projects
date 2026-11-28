package app.krala.ui.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.krala.BuildConfig
import app.krala.R
import app.krala.data.provider.Provider
import app.krala.data.settings.Personality
import app.krala.data.settings.ThemeMode
import app.krala.ui.components.*
import app.krala.ui.theme.LocalHaptics
import java.time.LocalDate

@Composable
fun BackTopBar(title: String, onBack: () -> Unit, scroll: TopAppBarScrollBehavior? = null, actions: @Composable RowScope.() -> Unit = {}) {
    LargeTopAppBar(
        title = { Text(title) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) } },
        actions = actions,
        scrollBehavior = scroll,
        colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
    onPersonality: () -> Unit,
    onProviders: () -> Unit,
    onUsage: () -> Unit,
    onMemory: () -> Unit,
    onAccount: () -> Unit,
    onVoice: () -> Unit = {},
) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val providers by vm.providers.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val h = LocalHaptics.current
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbar = remember { SnackbarHostState() }
    var pickMemoryModel by remember { mutableStateOf(false) }
    var pickEmbeddings by remember { mutableStateOf(false) }
    var pickLanguage by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var webDialog by remember { mutableStateOf(false) }
    var proxyDialog by remember { mutableStateOf(false) }

    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(vm::backupTo) }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::restoreFrom) }

    val backupDone = stringResource(R.string.backup_done)
    val restoreDone = stringResource(R.string.restore_done)
    val chatsDeleted = stringResource(R.string.all_chats_deleted)
    LaunchedEffect(Unit) {
        vm.messages.collect { m ->
            when (m) {
                is SettingsMessage.BackupDone -> snackbar.showSnackbar(backupDone.format(m.s.chats, m.s.memory))
                is SettingsMessage.RestoreDone -> snackbar.showSnackbar(restoreDone.format(m.s.chats, m.s.messages, m.s.memory))
                is SettingsMessage.Failed -> snackbar.showSnackbar(m.error)
                SettingsMessage.ChatsDeleted -> snackbar.showSnackbar(chatsDeleted)
            }
        }
    }

    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS ||
            BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.settings), onBack, scroll) },
        snackbarHost = { SnackbarHost(snackbar) },
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
    ) { pad ->
        LazyColumn(contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 32.dp)) {
            item { SectionHeader(stringResource(R.string.appearance)) }
            item {
                SettingsCard {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(10.dp))
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            val modes = ThemeMode.entries
                            modes.forEachIndexed { i, m ->
                                SegmentedButton(
                                    selected = s.themeMode == m,
                                    onClick = { h.tick(); vm.setTheme(m) },
                                    shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                                    label = {
                                        Text(
                                            stringResource(
                                                when (m) {
                                                    ThemeMode.SYSTEM -> R.string.theme_system
                                                    ThemeMode.LIGHT -> R.string.theme_light
                                                    ThemeMode.DARK -> R.string.theme_dark
                                                    ThemeMode.AMOLED -> R.string.theme_amoled
                                                }
                                            ),
                                            maxLines = 1,
                                        )
                                    },
                                )
                            }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= 31) {
                        SwitchRow(stringResource(R.string.dynamic_color), stringResource(R.string.dynamic_color_hint), Icons.Rounded.Palette, s.dynamicColor) { vm.setDynamic(it) }
                    }
                    SwitchRow(stringResource(R.string.haptics), null, Icons.Rounded.Vibration, s.haptics) { vm.setHaptics(it) }
                    SwitchRow(stringResource(R.string.send_on_enter), null, Icons.AutoMirrored.Rounded.KeyboardReturn, s.sendOnEnter) { vm.setSendOnEnter(it) }
                    SettingRow(
                        stringResource(R.string.language),
                        when (s.language) { "en" -> "english"; "ru" -> "русский"; else -> stringResource(R.string.language_system) },
                        Icons.Rounded.Translate,
                        onClick = { pickLanguage = true },
                    )
                }
            }

            item { SectionHeader(stringResource(R.string.krala)) }
            item {
                SettingsCard {
                    SettingRow(
                        stringResource(R.string.personality),
                        if (s.systemPrompt == null) stringResource(R.string.personality_default) else stringResource(R.string.personality_custom),
                        Icons.Rounded.TheaterComedy, onClick = onPersonality,
                    )
                    SettingRow(stringResource(R.string.providers), stringResource(R.string.providers_count, providers.size), Icons.Rounded.Hub, onClick = onProviders)
                    SettingRow(stringResource(R.string.usage), stringResource(R.string.usage_hint), Icons.Rounded.BarChart, onClick = onUsage)
                    SettingRow(stringResource(R.string.web_search), webLabel(s.webProvider), Icons.Rounded.TravelExplore, onClick = { webDialog = true })
                    SettingRow(stringResource(R.string.voice), stringResource(R.string.voice_hint), Icons.Rounded.RecordVoiceOver, onClick = onVoice)
                    SwitchRow(stringResource(R.string.notify_replies), stringResource(R.string.notify_replies_hint), Icons.Rounded.Notifications, s.notifyReplies) { vm.setNotifyReplies(it) }
                    var randomMenu by remember { mutableStateOf(false) }
                    Box {
                        SettingRow(
                            stringResource(R.string.random_messages),
                            stringResource(when (s.randomMessages) { "sometimes" -> R.string.random_sometimes; "off" -> R.string.random_off; else -> R.string.random_rare }),
                            Icons.Rounded.MarkChatUnread, onClick = { randomMenu = true },
                        )
                        DropdownMenu(randomMenu, onDismissRequest = { randomMenu = false }) {
                            listOf("rare" to R.string.random_rare, "sometimes" to R.string.random_sometimes, "off" to R.string.random_off).forEach { (id, label) ->
                                DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { randomMenu = false; vm.setRandomMessages(id, context) })
                            }
                        }
                    }
                    SettingRow(
                        stringResource(R.string.gemini_proxy),
                        stringResource(when (s.geminiProxy) { "always" -> R.string.gemini_proxy_always; "off" -> R.string.gemini_proxy_off; else -> R.string.gemini_proxy_auto }),
                        Icons.Rounded.VpnLock, onClick = { proxyDialog = true; vm.checkRoute() },
                    )
                    var thinkMenu by remember { mutableStateOf(false) }
                    Box {
                        SettingRow(
                            stringResource(R.string.gemini_thinking),
                            stringResource(when (s.geminiThinking) { "deep" -> R.string.thinking_deep; "balanced" -> R.string.thinking_balanced; else -> R.string.thinking_fast }),
                            Icons.Rounded.Psychology, onClick = { thinkMenu = true },
                        )
                        DropdownMenu(thinkMenu, onDismissRequest = { thinkMenu = false }) {
                            listOf("fast" to R.string.thinking_fast, "balanced" to R.string.thinking_balanced, "deep" to R.string.thinking_deep).forEach { (id, label) ->
                                DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { thinkMenu = false; vm.setGeminiThinking(id) })
                            }
                        }
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.memory)) }
            item {
                SettingsCard {
                    SettingRow(stringResource(R.string.memory_entries), stringResource(R.string.memory_entries_hint), Icons.Rounded.Psychology, onClick = onMemory)
                    SettingRow(
                        stringResource(R.string.memory_writer),
                        modelLabel(providers, s.memoryProviderId, s.memoryModel) ?: stringResource(R.string.memory_writer_auto),
                        Icons.Rounded.EditNote,
                        onClick = { pickMemoryModel = true },
                    )
                    SwitchRow(stringResource(R.string.auto_recall), stringResource(R.string.auto_recall_hint), Icons.Rounded.History, s.autoRecall) { vm.setAutoRecall(it) }
                    SettingRow(
                        stringResource(R.string.embeddings),
                        if (s.embeddingsEnabled) modelLabel(providers, s.embeddingProviderId, s.embeddingModel) else stringResource(R.string.embeddings_off),
                        Icons.Rounded.Hub,
                        onClick = { pickEmbeddings = true },
                    )
                }
            }

            item { SectionHeader(stringResource(R.string.account_and_security)) }
            item {
                SettingsCard {
                    SettingRow(stringResource(R.string.account_sync), stringResource(if (s.syncEnabled) R.string.sync_on else R.string.sync_off), Icons.Rounded.CloudSync, onClick = onAccount)
                    SwitchRow(
                        stringResource(R.string.biometric_lock),
                        stringResource(if (biometricAvailable) R.string.biometric_lock_hint else R.string.biometric_unavailable),
                        Icons.Rounded.Fingerprint, s.biometricLock, enabled = biometricAvailable || s.biometricLock,
                    ) { vm.setBiometric(it) }
                }
            }

            item { SectionHeader(stringResource(R.string.data)) }
            item {
                SettingsCard {
                    SettingRow(stringResource(R.string.backup), stringResource(R.string.backup_hint), Icons.Rounded.Backup, onClick = { backup.launch("krala-backup-${LocalDate.now()}.json") })
                    SettingRow(stringResource(R.string.restore), stringResource(R.string.restore_hint), Icons.Rounded.Restore, onClick = { restore.launch(arrayOf("application/json", "*/*")) })
                    SettingRow(stringResource(R.string.delete_all_chats), null, Icons.Rounded.DeleteSweep, onClick = { confirmDeleteAll = true })
                }
            }

            item { SectionHeader(stringResource(R.string.about)) }
            item {
                SettingsCard {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        BreathingLogo(44.dp)
                        Column(Modifier.padding(start = 14.dp)) {
                            Text("krala " + BuildConfig.VERSION_NAME, style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.about_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (pickLanguage) {
        AlertDialog(
            onDismissRequest = { pickLanguage = false },
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column {
                    listOf("" to stringResource(R.string.language_system), "en" to "english", "ru" to "русский").forEach { (tag, label) ->
                        ListItem(
                            headlineContent = { Text(label) },
                            leadingContent = { RadioButton(s.language == tag, onClick = null) },
                            modifier = Modifier.clickableWithHaptic { pickLanguage = false; vm.setLanguage(tag) },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickLanguage = false }) { Text(stringResource(R.string.close)) } },
        )
    }
    if (pickMemoryModel) {
        ProviderModelDialog(
            title = stringResource(R.string.memory_writer),
            body = stringResource(R.string.memory_writer_body),
            providers = providers,
            selectedProvider = s.memoryProviderId,
            selectedModel = s.memoryModel,
            allowAuto = true,
            onPick = { p, m -> vm.setMemoryModel(p, m) },
            onDismiss = { pickMemoryModel = false },
        )
    }
    if (pickEmbeddings) {
        EmbeddingsDialog(
            providers = providers,
            enabled = s.embeddingsEnabled,
            providerId = s.embeddingProviderId,
            model = s.embeddingModel,
            onSave = { e, p, m -> vm.setEmbeddings(e, p, m) },
            onDismiss = { pickEmbeddings = false },
        )
    }
    if (proxyDialog) {
        val status by vm.routeStatus.collectAsStateWithLifecycle()
        AlertDialog(
            onDismissRequest = { proxyDialog = false },
            title = { Text(stringResource(R.string.gemini_proxy)) },
            text = {
                Column {
                    Text(stringResource(R.string.gemini_proxy_body), style = MaterialTheme.typography.bodyMedium)
                    listOf("auto" to R.string.gemini_proxy_auto, "always" to R.string.gemini_proxy_always, "off" to R.string.gemini_proxy_off).forEach { (id, label) ->
                        ListItem(
                            headlineContent = { Text(stringResource(label)) },
                            leadingContent = { RadioButton(s.geminiProxy == id, onClick = null) },
                            modifier = Modifier.clickableWithHaptic { vm.setGeminiProxy(id) },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        )
                    }
                    status?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
                }
            },
            confirmButton = { TextButton(onClick = { proxyDialog = false }) { Text(stringResource(R.string.close)) } },
            dismissButton = { TextButton(onClick = { vm.checkRoute() }) { Text(stringResource(R.string.refresh)) } },
        )
    }
    if (webDialog) {
        WebSearchDialog(s.webProvider, vm.webKey(), s.webSearxUrl, s.webPages, onSave = vm::setWebSearch, onDismiss = { webDialog = false })
    }
    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            icon = { Icon(Icons.Rounded.DeleteSweep, null) },
            title = { Text(stringResource(R.string.delete_all_chats_q)) },
            text = { Text(stringResource(R.string.delete_all_chats_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDeleteAll = false; vm.deleteAllChats() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

fun modelLabel(providers: List<Provider>, providerId: String?, model: String?): String? {
    val p = providers.firstOrNull { it.id == providerId } ?: return null
    return p.name + " · " + (model ?: p.firstModel ?: "")
}

@Composable
fun ProviderModelDialog(
    title: String,
    body: String?,
    providers: List<Provider>,
    selectedProvider: String?,
    selectedModel: String?,
    allowAuto: Boolean,
    onPick: (String?, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                body?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp)) }
                if (allowAuto) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.model_auto)) },
                        supportingContent = { Text(stringResource(R.string.model_auto_hint)) },
                        leadingContent = { RadioButton(selectedProvider == null, onClick = null) },
                        modifier = Modifier.clickableWithHaptic { onPick(null, null); onDismiss() },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    )
                }
                providers.filter { it.entity.enabled }.forEach { p ->
                    Text(p.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 12.dp))
                    p.models.ifEmpty { listOfNotNull(p.firstModel) }.forEach { m ->
                        ListItem(
                            headlineContent = { Text(m, maxLines = 1) },
                            leadingContent = { RadioButton(selectedProvider == p.id && (selectedModel ?: p.firstModel) == m, onClick = null) },
                            modifier = Modifier.clickableWithHaptic { onPick(p.id, m); onDismiss() },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun EmbeddingsDialog(
    providers: List<Provider>,
    enabled: Boolean,
    providerId: String?,
    model: String?,
    onSave: (Boolean, String?, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var on by remember { mutableStateOf(enabled) }
    var pid by remember { mutableStateOf(providerId ?: providers.firstOrNull()?.id) }
    var m by remember { mutableStateOf(model ?: "") }
    var expanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.embeddings)) },
        text = {
            Column {
                Text(stringResource(R.string.embeddings_body), style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(stringResource(R.string.enabled), modifier = Modifier.weight(1f))
                    Switch(on, onCheckedChange = { on = it })
                }
                ExposedDropdownMenuBox(expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = providers.firstOrNull { it.id == pid }?.name ?: "",
                        onValueChange = {}, readOnly = true, enabled = on,
                        label = { Text(stringResource(R.string.provider)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                        providers.forEach { p ->
                            DropdownMenuItem(text = { Text(p.name) }, onClick = { pid = p.id; expanded = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = m, onValueChange = { m = it }, enabled = on, singleLine = true,
                    label = { Text(stringResource(R.string.embedding_model)) },
                    placeholder = { Text("text-embedding-004") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(on && m.isNotBlank() && pid != null, pid, m.trim().ifBlank { null }); onDismiss() }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun PersonalityScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val h = LocalHaptics.current
    var prompt by remember(s.settingsUpdatedAt) { mutableStateOf(s.effectiveSystemPrompt) }
    var instructions by remember(s.settingsUpdatedAt) { mutableStateOf(s.customInstructions) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val changed = prompt != s.effectiveSystemPrompt || instructions != s.customInstructions
    Scaffold(
        topBar = {
            BackTopBar(stringResource(R.string.personality), onBack, scroll) {
                TextButton(onClick = { h.confirm(); vm.savePersonality(prompt, instructions); onBack() }, enabled = changed) { Text(stringResource(R.string.save)) }
            }
        },
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection).imePadding(),
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(stringResource(R.string.system_prompt), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.system_prompt_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
            OutlinedTextField(prompt, { prompt = it }, minLines = 8, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { prompt = Personality.DEFAULT_SYSTEM_PROMPT }, enabled = prompt != Personality.DEFAULT_SYSTEM_PROMPT) {
                Icon(Icons.Rounded.RestartAlt, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.reset_default))
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.custom_instructions), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.custom_instructions_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
            OutlinedTextField(instructions, { instructions = it }, minLines = 5, modifier = Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.custom_instructions_placeholder)) })
            Spacer(Modifier.height(48.dp))
        }
    }
}


fun webLabel(p: String) = when (p) { "brave" -> "brave search"; "tavily" -> "tavily"; "searxng" -> "searxng"; else -> "duckduckgo" }

@Composable
private fun WebSearchDialog(
    provider: String,
    key: String,
    searx: String,
    pages: Int,
    onSave: (String, String, String, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var p by remember { mutableStateOf(provider) }
    var k by remember { mutableStateOf(key) }
    var u by remember { mutableStateOf(searx) }
    var n by remember { mutableStateOf(pages.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.web_search)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.web_search_body), style = MaterialTheme.typography.bodyMedium)
                listOf("duckduckgo", "brave", "tavily", "searxng").forEach { id ->
                    ListItem(
                        headlineContent = { Text(webLabel(id)) },
                        supportingContent = { Text(stringResource(if (id == "duckduckgo") R.string.no_key_needed else if (id == "searxng") R.string.web_searx_hint else R.string.web_key_hint)) },
                        leadingContent = { RadioButton(p == id, onClick = null) },
                        modifier = Modifier.clickableWithHaptic { p = id },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    )
                }
                if (p == "brave" || p == "tavily") OutlinedTextField(
                    k, { k = it }, singleLine = true, label = { Text(stringResource(R.string.api_key)) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (p == "searxng") OutlinedTextField(u, { u = it }, singleLine = true, label = { Text(stringResource(R.string.base_url)) }, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.web_pages, n.toInt()), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                Slider(n, { n = it }, valueRange = 0f..5f, steps = 4)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(p, k, u, n.toInt()); onDismiss() }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
