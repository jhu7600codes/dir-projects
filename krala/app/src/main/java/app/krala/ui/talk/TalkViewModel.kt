package app.krala.ui.talk

import android.content.Context
import android.speech.SpeechRecognizer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.krala.ActiveChat
import app.krala.core.VoiceScript
import app.krala.data.chat.ChatEngine
import app.krala.data.chat.ChatEvent
import app.krala.data.chat.ChatRepository
import app.krala.data.db.MessageStatus
import app.krala.data.db.Role
import app.krala.data.memory.MemoryScheduler
import app.krala.data.settings.SettingsRepository
import app.krala.voice.Listener
import app.krala.voice.Speaker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TalkPhase { IDLE, LISTENING, THINKING, SPEAKING, ERROR }

data class TalkCode(val lang: String, val code: String, val running: Boolean = true, val result: app.krala.code.RunResult? = null)

data class TalkState(
    val phase: TalkPhase = TalkPhase.IDLE,
    val heard: String = "",
    val reply: String = "",
    val error: String? = null,
    val handsFree: Boolean = true,
    /** code from the last reply, run on the phone and shown on screen */
    val code: List<TalkCode> = emptyList(),
)

/**
 * Hands-free voice chat: listen → send (with talk mode cues) → speak the reply sentence by
 * sentence while it streams → listen again. Uses the normal chat engine, so talk turns are
 * regular messages in the chat, with memory, recall and provider fallback.
 */
@HiltViewModel
class TalkViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: ChatEngine,
    private val chats: ChatRepository,
    private val speaker: Speaker,
    private val settings: SettingsRepository,
    private val scheduler: MemoryScheduler,
    private val activeChat: ActiveChat,
    private val codeRunner: app.krala.code.CodeRunner,
    handle: SavedStateHandle,
) : ViewModel() {
    private val listener = Listener(context)
    private var chatId: String? = handle["chatId"]
    private var turn: Job? = null
    private var misses = 0
    /** set once the recognizer refused the app's language; from then on use the phone's own */
    private var systemLanguage = false
    private var closed = false

    private val _state = MutableStateFlow(TalkState())
    val state: StateFlow<TalkState> = _state

    val level: StateFlow<Float> = listener.level
    val speaking: StateFlow<Boolean> = speaker.speaking
    val energy: StateFlow<Float> = speaker.tone.map {
        when (it) {
            VoiceScript.Tone.EXCITED, VoiceScript.Tone.HYPE -> 1f
            VoiceScript.Tone.TEASING -> 0.7f
            VoiceScript.Tone.NEUTRAL -> 0.5f
            VoiceScript.Tone.HOLD, VoiceScript.Tone.CALM -> 0.35f
            VoiceScript.Tone.SOFT, VoiceScript.Tone.SAD, VoiceScript.Tone.WHISPER -> 0.15f
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.5f)

    val title: StateFlow<String> = MutableStateFlow("").also { flow ->
        viewModelScope.launch { chatId?.let { id -> chats.get(id)?.title?.let { flow.value = it } } }
    }

    val recognitionAvailable get() = listener.available

    val transcript: StateFlow<Boolean> = settings.settings.map { it.talkTranscript }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    fun setTranscript(on: Boolean) = viewModelScope.launch { settings.setTalkTranscript(on) }

    init {
        viewModelScope.launch { speaker.init() }
        chatId?.let { activeChat.id = it }
        viewModelScope.launch {
            engine.events.filterIsInstance<ChatEvent.ReplyFailed>().collect { e ->
                if (e.conversationId == chatId) _state.value = _state.value.copy(phase = TalkPhase.ERROR, error = e.message)
            }
        }
    }

    fun chatId() = chatId

    private fun set(f: (TalkState) -> TalkState) { _state.value = f(_state.value) }

    fun setHandsFree(on: Boolean) = set { it.copy(handsFree = on) }

    fun tapStar() {
        when (_state.value.phase) {
            TalkPhase.IDLE, TalkPhase.ERROR -> listen()
            TalkPhase.LISTENING -> listener.finish()
            TalkPhase.THINKING -> {
                chatId?.let { engine.stop(it) }
                turn?.cancel()
                speaker.stop()
                set { it.copy(phase = TalkPhase.IDLE) }
            }
            TalkPhase.SPEAKING -> {
                // barge in: cut her off and listen
                turn?.cancel()
                chatId?.let { engine.stop(it) }
                speaker.stop()
                listen()
            }
        }
    }

    fun listen() {
        if (closed) return
        speaker.stop()
        set { it.copy(phase = TalkPhase.LISTENING, heard = "", error = null) }
        viewModelScope.launch {
            val lang = if (systemLanguage) null else when (settings.current().language) { "ru" -> "ru-RU"; "en" -> "en-US"; else -> null }
            listener.start(lang) { ev ->
                when (ev) {
                    is Listener.Event.Partial -> set { it.copy(heard = ev.text) }
                    is Listener.Event.Final -> { misses = 0; send(ev.text) }
                    is Listener.Event.Error -> onListenError(ev.code)
                }
            }
        }
    }

    private fun onListenError(code: Int) {
        listener.stop()
        when (code) {
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                misses++
                if (_state.value.handsFree && misses < 3) listen() else set { it.copy(phase = TalkPhase.IDLE) }
            }
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> set { it.copy(phase = TalkPhase.ERROR, error = context.getString(app.krala.R.string.mic_permission)) }
            SpeechRecognizer.ERROR_CLIENT -> set { it.copy(phase = TalkPhase.IDLE) }
            // 12 language not supported, 13 language not downloaded: listen in the phone's language
            12, 13 -> if (!systemLanguage) { systemLanguage = true; listen() }
                else set { it.copy(phase = TalkPhase.ERROR, error = context.getString(app.krala.R.string.voice_language)) }
            // 11 recognizer service dropped, 8 busy: try once more
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED, SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                if (misses++ < 2) viewModelScope.launch { kotlinx.coroutines.delay(400); listen() } else set { it.copy(phase = TalkPhase.IDLE) }
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                set { it.copy(phase = TalkPhase.ERROR, error = context.getString(app.krala.R.string.voice_network)) }
            else -> set { it.copy(phase = TalkPhase.ERROR, error = context.getString(app.krala.R.string.voice_error, code)) }
        }
    }

    /** hands a code result back to krala as the next turn */
    fun sendCode(c: TalkCode) {
        val r = c.result ?: return
        speaker.stop()
        val body = listOfNotNull(r.output.ifBlank { null }, r.error?.let { "error: $it" }).joinToString("\n").ifBlank { "(no output)" }
        send("ran the ${c.lang} code" + (if (r.ok) "" else ", it failed") + ":\n```text\n" + body.take(6_000) + "\n```")
    }

    private fun runCode(reply: String) {
        val blocks = app.krala.core.Markdown.parse(reply).filterIsInstance<app.krala.core.Markdown.Block.Code>()
            .filter { it.closed && codeRunner.language(it.language) != null }.takeLast(2)
        if (blocks.isEmpty()) return
        set { it.copy(code = blocks.map { b -> TalkCode(b.language.ifBlank { "code" }, b.code) }) }
        blocks.forEachIndexed { i, b ->
            viewModelScope.launch {
                val r = codeRunner.run(b.language, b.code)
                set { st -> st.copy(code = st.code.mapIndexed { j, c -> if (j == i && c.code == b.code) c.copy(running = false, result = r) else c }) }
            }
        }
    }

    private fun send(text: String) {
        listener.stop()
        set { it.copy(phase = TalkPhase.THINKING, heard = text, reply = "", code = emptyList()) }
        turn?.cancel()
        turn = viewModelScope.launch {
            val s = settings.current()
            val id = chatId ?: engine.createConversation(false, s.defaultProviderId, s.defaultModel, null).also {
                chatId = it
                activeChat.id = it
            }
            speaker.init()
            engine.send(id, text, emptyList(), emptyList(), voice = true)

            var spoken = 0
            var tone = VoiceScript.Tone.NEUTRAL
            var seen = false
            var started = false
            fun feed(textNow: String, final: Boolean) {
                val end = if (final) textNow.length else VoiceScript.sentenceEnd(textNow, spoken)
                if (end > spoken) {
                    if (!started) { started = true; speaker.setLanguageFor(textNow, s.language) }
                    val r = VoiceScript.parse(textNow.substring(spoken, end), tone)
                    tone = r.endTone
                    spoken = end
                    speaker.speak(r.segments)
                    set { it.copy(phase = TalkPhase.SPEAKING) }
                }
            }
            // follow the stream until the reply is finished
            engine.live.map { it[id] }.first { lr ->
                if (lr != null) {
                    seen = true
                    set { it.copy(reply = lr.text) }
                    feed(lr.text, final = false)
                    false
                } else seen
            }
            val last = chats.messagesOnce(id).lastOrNull { it.role == Role.ASSISTANT }
            if (last != null && last.status != MessageStatus.ERROR) {
                set { it.copy(reply = last.content) }
                feed(last.content, final = true)
                runCode(last.content)
            }
            if (_state.value.phase == TalkPhase.ERROR) return@launch
            // let her finish talking, then listen again
            speaker.speaking.first { !it }
            if (_state.value.handsFree && !closed) listen() else set { it.copy(phase = TalkPhase.IDLE) }
        }
    }

    fun close() {
        closed = true
        turn?.cancel()
        listener.stop()
        speaker.stop()
    }

    override fun onCleared() {
        close()
        chatId?.let { scheduler.chatEnded(it) }
        super.onCleared()
    }
}
