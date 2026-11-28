package app.krala.ui.chat

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.krala.Incoming
import app.krala.IntentRouter
import app.krala.R
import app.krala.data.chat.ChatEvent
import app.krala.data.db.ConversationEntity
import app.krala.data.db.Role
import app.krala.ui.components.BreathingLogo
import app.krala.ui.components.KralaLogo
import app.krala.ui.theme.LocalHaptics
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ChatScreen(
    vm: ChatViewModel,
    router: IntentRouter,
    attachmentFile: (String) -> File,
    onSettings: () -> Unit,
    onMemory: () -> Unit,
    onAccount: () -> Unit,
    onProviders: () -> Unit,
    onTalk: (String?) -> Unit,
) {
    val context = LocalContext.current
    val h = LocalHaptics.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme

    val conversations by vm.conversations.collectAsStateWithLifecycle()
    val conv by vm.conversation.collectAsStateWithLifecycle()
    val currentId by vm.current.collectAsStateWithLifecycle()
    val messages by vm.messages.collectAsStateWithLifecycle()
    val live by vm.live.collectAsStateWithLifecycle()
    val composer by vm.composer.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()
    val providers by vm.providers.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val search by vm.search.collectAsStateWithLifecycle()
    val searchMatches by vm.searchMatches.collectAsStateWithLifecycle()
    val recallPanel by vm.recallPanel.collectAsStateWithLifecycle()
    val incoming by router.incoming.collectAsStateWithLifecycle()

    val busy = live != null
    val incognito = conv?.incognito ?: draft.incognito
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var renameTarget by remember { mutableStateOf<ConversationEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<ConversationEntity?>(null) }
    var exportTarget by remember { mutableStateOf<ConversationEntity?>(null) }
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var showModels by remember { mutableStateOf(false) }
    var showPrompt by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }

    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(6)) { uris ->
        uris.forEach { vm.addImage(it) }
    }
    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.take(8).forEach { vm.addDocument(it) }
    }
    val unreadable = stringResource(R.string.file_unreadable)
    LaunchedEffect(Unit) { vm.notice.collect { snackbar.showSnackbar(unreadable.format(it)) } }
    val saveMd = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        val text = pendingExport
        if (uri != null && text != null) vm.writeExport(uri, text)
        pendingExport = null
    }
    val saveJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val text = pendingExport
        if (uri != null && text != null) vm.writeExport(uri, text)
        pendingExport = null
    }

    // share sheet / widget / notifications coming in
    LaunchedEffect(incoming) {
        when (val i = incoming) {
            is Incoming.Share -> { vm.startWithShare(i.text, i.images); router.consume() }
            is Incoming.OpenChat -> { vm.open(i.id); drawer.close(); router.consume() }
            Incoming.NewChat -> { vm.newChat(); router.consume() }
            null -> Unit
        }
    }

    val fellBackMsg = stringResource(R.string.fell_back)
    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                is ChatEvent.ReplyDone -> if (e.conversationId == vm.current.value) h.confirm()
                is ChatEvent.ReplyFailed -> if (e.conversationId == vm.current.value) h.reject()
                is ChatEvent.FellBack -> if (e.conversationId == vm.current.value) {
                    snackbar.currentSnackbarData?.dismiss()
                    launch { snackbar.showSnackbar(fellBackMsg.format(e.from, e.to)) }
                }
            }
        }
    }

    // keep the newest message in view while streaming, unless the user scrolled up
    val atBottom by remember { derivedStateOf { listState.firstVisibleItemIndex <= 1 } }
    LaunchedEffect(messages.size, live?.text?.length) {
        if (atBottom && messages.isNotEmpty()) listState.animateScrollToItem(0)
    }
    LaunchedEffect(currentId) { if (messages.isNotEmpty()) listState.scrollToItem(0) }

    BackHandler(enabled = recallPanel !is RecallPanel.Hidden) { vm.closeRecall() }
    BackHandler(enabled = composer.editingId != null && recallPanel is RecallPanel.Hidden) { vm.cancelEdit() }

    fun shareText(text: String) {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, null))
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ChatDrawer(
                conversations = conversations,
                currentId = currentId,
                search = search,
                searchMatches = searchMatches,
                onSearch = vm::setSearch,
                onOpen = { vm.open(it); scope.launch { drawer.close() } },
                onNewChat = { vm.newChat(); scope.launch { drawer.close() } },
                onNewIncognito = { vm.newChat(incognito = true); scope.launch { drawer.close() } },
                onRename = { renameTarget = it },
                onTogglePin = { vm.togglePin(it) },
                onExport = { exportTarget = it },
                onDelete = { deleteTarget = it },
                onSettings = { scope.launch { drawer.close() }; onSettings() },
                onMemory = { scope.launch { drawer.close() }; onMemory() },
                onAccount = { scope.launch { drawer.close() }; onAccount() },
            )
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            containerColor = cs.surface,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = if (incognito) cs.surfaceContainerHigh else cs.surface),
                    navigationIcon = {
                        IconButton(onClick = { h.click(); scope.launch { drawer.open() } }) { Icon(Icons.Rounded.Menu, stringResource(R.string.chats)) }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showModels = true }) {
                            if (incognito) Icon(Icons.Rounded.VisibilityOff, null, Modifier.size(22.dp), tint = cs.primary)
                            else app.krala.ui.components.KralaStar(
                                mode = when {
                                    live == null -> app.krala.ui.components.StarMode.IDLE
                                    live?.started == true -> app.krala.ui.components.StarMode.TALKING
                                    else -> app.krala.ui.components.StarMode.THINKING
                                },
                                size = 30.dp,
                            )
                            Column(Modifier.padding(start = 10.dp)) {
                                Text(
                                    conv?.title?.takeIf { it.isNotBlank() } ?: stringResource(if (incognito) R.string.incognito_chat else R.string.app_name),
                                    style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                                val pid = conv?.providerId ?: if (conv == null) draft.providerId else null
                                val model = conv?.model ?: if (conv == null) draft.model else null
                                val p = providers.firstOrNull { it.id == pid }
                                Text(
                                    if (p == null) stringResource(R.string.model_auto) + (providers.firstOrNull { it.entity.enabled }?.let { " · " + it.name } ?: "")
                                    else p.name + " · " + (model ?: p.firstModel ?: ""),
                                    style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(Icons.Rounded.ExpandMore, null, Modifier.size(18.dp), tint = cs.onSurfaceVariant)
                        }
                    },
                    actions = {
                        if (currentId == null) {
                            IconToggleButton(checked = draft.incognito, onCheckedChange = { h.tick(); vm.setDraftIncognito(it) }) {
                                Icon(if (draft.incognito) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, stringResource(R.string.incognito))
                            }
                        } else {
                            IconButton(onClick = { h.click(); vm.newChat() }) { Icon(Icons.Rounded.AddComment, stringResource(R.string.new_chat)) }
                        }
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.more)) }
                            DropdownMenu(menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.chat_prompt)) }, leadingIcon = { Icon(Icons.Rounded.TheaterComedy, null) }, onClick = { menu = false; showPrompt = true })
                                DropdownMenuItem(text = { Text(stringResource(R.string.pick_model)) }, leadingIcon = { Icon(Icons.Rounded.Memory, null) }, onClick = { menu = false; showModels = true })
                                conv?.let { c ->
                                    DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, null) }, onClick = { menu = false; renameTarget = c })
                                    DropdownMenuItem(text = { Text(stringResource(if (c.pinned) R.string.unpin else R.string.pin)) }, leadingIcon = { Icon(Icons.Rounded.PushPin, null) }, onClick = { menu = false; vm.togglePin(c) })
                                    DropdownMenuItem(text = { Text(stringResource(R.string.export)) }, leadingIcon = { Icon(Icons.Rounded.IosShare, null) }, onClick = { menu = false; exportTarget = c })
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete), color = cs.error) },
                                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = cs.error) }, onClick = { menu = false; deleteTarget = c },
                                    )
                                }
                            }
                        }
                    },
                )
            },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().imePadding().navigationBarsPadding()) {
                Box(Modifier.weight(1f)) {
                    Crossfade(messages.isEmpty(), label = "empty") { empty ->
                        if (empty) {
                            EmptyChat(incognito = incognito, onSuggestion = { vm.onInput(it) })
                        } else {
                            val lastAssistantId = messages.lastOrNull { it.entity.role == Role.ASSISTANT }?.entity?.id
                            val reversed = remember(messages) { messages.asReversed() }
                            LazyColumn(state = listState, reverseLayout = true, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 12.dp)) {
                                itemsIndexed(reversed, key = { _, m -> m.entity.id }) { _, m ->
                                    Box(Modifier.animateItem()) {
                                        MessageItem(
                                            m = m,
                                            attachmentFile = attachmentFile,
                                            isLastAssistant = m.entity.id == lastAssistantId && messages.last().entity.id == m.entity.id,
                                            busy = busy,
                                            onEdit = { vm.startEdit(m) },
                                            onRegenerate = { vm.regenerate() },
                                            onShare = ::shareText,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !atBottom && messages.isNotEmpty(),
                        enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                    ) {
                        SmallFloatingActionButton(onClick = { scope.launch { listState.animateScrollToItem(0) } }, containerColor = cs.surfaceContainerHighest) {
                            Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.scroll_down))
                        }
                    }
                }
                RecallPanelView(
                    panel = recallPanel,
                    label = vm::recallLabel,
                    onOpenChat = { vm.closeRecall(); vm.open(it) },
                    onGiveResult = vm::giveToKrala,
                    onGiveMemory = vm::giveMemoryToKrala,
                    onClose = vm::closeRecall,
                )
                ChatComposer(
                    state = composer,
                    busy = busy,
                    sendOnEnter = settings.sendOnEnter,
                    incognito = incognito,
                    attachmentFile = attachmentFile,
                    onInput = vm::onInput,
                    onSend = vm::send,
                    onStop = vm::stop,
                    onPickImage = { pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onRemoveImage = vm::removeImage,
                    onRemoveRecall = vm::removeRecall,
                    onCancelEdit = vm::cancelEdit,
                    onTalk = if (incognito) null else ({ onTalk(currentId) }),
                    onPickFile = { pickFiles.launch(arrayOf("*/*")) },
                    onRemoveDoc = vm::removeDocument,
                    onToggleWeb = vm::toggleWeb,
                )
            }
        }
    }

    if (showModels) {
        ModelPickerSheet(
            providers = providers,
            selectedProvider = conv?.providerId ?: if (conv == null) draft.providerId else null,
            selectedModel = conv?.model ?: if (conv == null) draft.model else null,
            onSelect = vm::selectModel,
            onMakeDefault = { p, m -> vm.setDefaultModel(p, m) },
            onManageProviders = onProviders,
            onDismiss = { showModels = false },
        )
    }
    if (showPrompt) {
        val current = conv?.systemPrompt ?: if (conv == null) draft.systemPrompt else null
        TextEditDialog(
            title = stringResource(R.string.chat_prompt),
            initial = current ?: settings.effectiveSystemPrompt,
            label = stringResource(R.string.chat_prompt_hint),
            singleLine = false,
            extraAction = stringResource(R.string.use_default) to { vm.setChatPrompt(null) },
            onConfirm = { text -> vm.setChatPrompt(if (text.trim() == settings.effectiveSystemPrompt.trim()) null else text) },
            onDismiss = { showPrompt = false },
        )
    }
    renameTarget?.let { c ->
        TextEditDialog(
            title = stringResource(R.string.rename_chat),
            initial = c.title,
            onConfirm = { vm.rename(c.id, it) },
            onDismiss = { renameTarget = null },
        )
    }
    deleteTarget?.let { c ->
        DeleteChatDialog(c.title.ifBlank { stringResource(R.string.untitled) }, onConfirm = { forget -> h.long(); vm.delete(c.id, forget) }, onDismiss = { deleteTarget = null })
    }
    exportTarget?.let { c ->
        ExportSheet(
            onShare = { scope.launch { context.startActivity(Intent.createChooser(vm.shareIntent(c.id), null)) } },
            onSaveMarkdown = { scope.launch { val (t, body) = vm.exportText(c.id, json = false); pendingExport = body; saveMd.launch(vm.safeName(t) + ".md") } },
            onSaveJson = { scope.launch { val (t, body) = vm.exportText(c.id, json = true); pendingExport = body; saveJson.launch(vm.safeName(t) + ".json") } },
            onDismiss = { exportTarget = null },
        )
    }
}

@Composable
private fun EmptyChat(incognito: Boolean, onSuggestion: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val greetings = stringArrayResource(R.array.greetings)
    val suggestions = stringArrayResource(R.array.suggestions)
    val greeting = remember(incognito) { greetings.random() }
    val picks = remember { suggestions.toList().shuffled().take(4) }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (incognito) Icon(Icons.Rounded.VisibilityOff, null, Modifier.size(64.dp), tint = cs.primary) else BreathingLogo(96.dp)
        Text(
            if (incognito) stringResource(R.string.incognito_title) else greeting,
            style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp),
        )
        if (incognito) {
            Text(
                stringResource(R.string.incognito_body), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            picks.forEach { s -> SuggestionChip(onClick = { onSuggestion(s) }, label = { Text(s) }) }
        }
    }
}
