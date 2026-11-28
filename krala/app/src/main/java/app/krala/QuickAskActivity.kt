package app.krala

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.activity.viewModels
import app.krala.data.chat.ChatEngine
import app.krala.data.chat.ChatRepository
import app.krala.data.db.MessageStatus
import app.krala.data.db.Role
import app.krala.data.memory.MemoryScheduler
import app.krala.data.settings.AppSettings
import app.krala.data.settings.SettingsRepository
import app.krala.ui.components.KralaLogo
import app.krala.ui.components.MarkdownText
import app.krala.ui.components.TypingIndicator
import app.krala.ui.theme.KralaTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuickAnswer(val text: String, val waiting: Boolean, val streaming: Boolean, val error: String?)

@HiltViewModel
class QuickAskViewModel @Inject constructor(
    private val engine: ChatEngine,
    chats: ChatRepository,
    private val scheduler: MemoryScheduler,
    settings: SettingsRepository,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    private val _chat = MutableStateFlow<String?>(null)
    val chat: StateFlow<String?> = _chat

    val answer: StateFlow<QuickAnswer?> = _chat.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else combine(chats.messages(id), engine.live) { list, live ->
            val lr = live[id]
            val last = list.lastOrNull { it.role == Role.ASSISTANT }
            when {
                lr != null -> QuickAnswer(lr.text, waiting = !lr.started, streaming = true, error = null)
                last != null -> QuickAnswer(last.content, false, false, if (last.status == MessageStatus.ERROR) last.error else null)
                else -> QuickAnswer("", waiting = true, streaming = true, error = null)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun ask(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val s = settings.value
            val id = _chat.value ?: engine.createConversation(false, s.defaultProviderId, s.defaultModel, null).also { _chat.value = it }
            engine.send(id, text, emptyList(), emptyList())
        }
    }

    fun stop() = _chat.value?.let { engine.stop(it) }

    override fun onCleared() {
        _chat.value?.let { scheduler.chatEnded(it) }
        super.onCleared()
    }
}

/** the widget's floating ask box: ask, read the answer, jump into the full chat if you want */
@AndroidEntryPoint
class QuickAskActivity : AppCompatActivity() {
    private val vm: QuickAskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val s by vm.settings.collectAsStateWithLifecycle()
            KralaTheme(mode = s.themeMode, dynamic = s.dynamicColor, haptics = s.haptics) {
                QuickAsk(
                    vm = vm,
                    onClose = ::finish,
                    onOpen = { id ->
                        startActivity(
                            Intent(this, MainActivity::class.java)
                                .putExtra(MainActivity.EXTRA_OPEN_CHAT, id)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        )
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun QuickAsk(vm: QuickAskViewModel, onClose: () -> Unit, onOpen: (String) -> Unit) {
    val answer by vm.answer.collectAsStateWithLifecycle()
    val chat by vm.chat.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose).imePadding().navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = cs.surfaceContainerLow,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(12.dp).clickable(remember { MutableInteractionSource() }, indication = null) {}.animateContentSize(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KralaLogo(size = 26.dp)
                    Text(stringResource(R.string.quick_ask), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 10.dp).weight(1f))
                    chat?.let { id ->
                        TextButton(onClick = { onOpen(id) }) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.open_in_krala))
                        }
                    }
                }
                AnimatedVisibility(answer != null) {
                    val a = answer
                    Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                        when {
                            a == null -> Unit
                            a.waiting -> TypingIndicator()
                            a.error != null -> Text(a.error, color = cs.error, style = MaterialTheme.typography.bodyMedium)
                            else -> MarkdownText(a.text)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = text, onValueChange = { text = it },
                        placeholder = { Text(stringResource(R.string.widget_hint)) },
                        maxLines = 4, shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { vm.ask(text); text = "" }),
                        modifier = Modifier.weight(1f).focusRequester(focus),
                    )
                    Spacer(Modifier.width(8.dp))
                    if (answer?.streaming == true) {
                        FilledTonalIconButton(onClick = { vm.stop() }) { Icon(Icons.Rounded.Stop, stringResource(R.string.stop)) }
                    } else {
                        FilledIconButton(onClick = { vm.ask(text); text = "" }, enabled = text.isNotBlank()) {
                            Icon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.send))
                        }
                    }
                }
            }
        }
    }
}
