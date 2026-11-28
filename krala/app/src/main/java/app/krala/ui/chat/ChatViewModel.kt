package app.krala.ui.chat

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.krala.ActiveChat
import app.krala.core.KralaTags
import app.krala.data.chat.AttachmentStore
import app.krala.data.chat.ChatEngine
import app.krala.data.chat.ChatEvent
import app.krala.data.chat.ChatRepository
import app.krala.data.chat.ExportService
import app.krala.data.chat.LiveReply
import app.krala.data.db.ConversationEntity
import app.krala.data.db.MessageEntity
import app.krala.data.memory.MemoryRepository
import app.krala.data.memory.MemoryScheduler
import app.krala.data.memory.RecallResult
import app.krala.data.memory.RecallService
import app.krala.data.provider.Provider
import app.krala.data.provider.ProviderRepository
import app.krala.data.settings.AppSettings
import app.krala.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UiMessage(
    val entity: MessageEntity,
    val images: List<String>,
    val text: String,
    val streaming: Boolean,
    val started: Boolean,
    val docs: List<app.krala.data.chat.AttachedDoc> = emptyList(),
    val searching: Boolean = false,
    val sources: List<Pair<String, String>> = emptyList(),
)

/** settings for a chat that doesn't exist yet (created on the first send) */
data class Draft(
    val incognito: Boolean = false,
    val providerId: String? = null,
    val model: String? = null,
    val systemPrompt: String? = null,
)

data class Composer(
    val text: String = "",
    val images: List<String> = emptyList(),
    val recall: List<KralaTags.RecallLine> = emptyList(),
    val editingId: String? = null,
    val importing: Boolean = false,
    val docs: List<app.krala.data.chat.AttachedDoc> = emptyList(),
    val webSearch: Boolean = false,
)

sealed interface RecallPanel {
    data object Hidden : RecallPanel
    data object Loading : RecallPanel
    data class Latest(val entries: List<app.krala.data.db.MemoryEntryEntity>) : RecallPanel
    data class Results(val query: String, val results: List<RecallResult>) : RecallPanel
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chats: ChatRepository,
    private val engine: ChatEngine,
    private val providersRepo: ProviderRepository,
    private val settingsRepo: SettingsRepository,
    private val memory: MemoryRepository,
    private val scheduler: MemoryScheduler,
    private val recall: RecallService,
    private val attachments: AttachmentStore,
    private val documents: app.krala.data.chat.Documents,
    private val export: ExportService,
    private val activeChat: ActiveChat,
) : ViewModel() {
    private val _current = MutableStateFlow<String?>(null)
    val current: StateFlow<String?> = _current

    private val _draft = MutableStateFlow(Draft())
    val draft: StateFlow<Draft> = _draft

    private val _composer = MutableStateFlow(Composer())
    val composer: StateFlow<Composer> = _composer

    val events: SharedFlow<ChatEvent> = engine.events

    val settings: StateFlow<AppSettings> = settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val providers: StateFlow<List<Provider>> = providersRepo.providers.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val conversations: StateFlow<List<ConversationEntity>> = chats.all.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val conversation: StateFlow<ConversationEntity?> = _current.flatMapLatest { id ->
        if (id == null) flowOf(null) else chats.conversation(id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val live: StateFlow<LiveReply?> = combine(_current, engine.live) { id, map -> id?.let { map[it] } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val messages: StateFlow<List<UiMessage>> = _current.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else combine(chats.messages(id), engine.live.map { it[id] }) { list, lr ->
            list.map { m ->
                val isLive = lr != null && lr.messageId == m.id
                UiMessage(
                    entity = m,
                    images = attachments.images(m.attachments),
                    text = if (isLive) lr!!.text else m.content,
                    streaming = isLive,
                    started = if (isLive) lr!!.started else true,
                    docs = attachments.docs(m.attachments),
                    searching = isLive && lr!!.status == "searching",
                    sources = m.sources?.lines()?.mapNotNull { l -> l.split('\t').takeIf { it.size == 2 }?.let { it[0] to it[1] } }.orEmpty(),
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---- drawer search ----
    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search
    val searchMatches: StateFlow<Set<String>> = _search.debounce(200).mapLatest { q ->
        if (q.isBlank()) emptySet() else runCatching { chats.searchConversationIds(q) }.getOrDefault(emptySet())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun setSearch(q: String) {
        _search.value = q
    }

    // ---- /recall ----
    private val recallQuery = MutableStateFlow<String?>(null)
    val recallPanel: StateFlow<RecallPanel> = recallQuery.debounce(180).mapLatest { q ->
        when {
            q == null -> RecallPanel.Hidden
            q.isBlank() -> RecallPanel.Latest(memory.latest(8))
            else -> RecallPanel.Results(q, runCatching { recall.command(q, _current.value) }.getOrDefault(emptyList()))
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RecallPanel.Hidden)

    fun recallLabel(title: String, date: Long) = recall.label(title, date)

    // ---- lifecycle of a chat ----

    private fun leaveCurrent() {
        _current.value?.let { scheduler.chatEnded(it) }
    }

    fun open(id: String) {
        if (id == _current.value) return
        leaveCurrent()
        _current.value = id
        activeChat.id = id
        _composer.value = Composer()
        recallQuery.value = null
    }

    fun newChat(incognito: Boolean = false) {
        leaveCurrent()
        _current.value = null
        activeChat.id = null
        val s = settings.value
        _draft.value = Draft(incognito = incognito, providerId = s.defaultProviderId, model = s.defaultModel)
        _composer.value = Composer()
        recallQuery.value = null
    }

    fun setDraftIncognito(on: Boolean) = _draft.update { it.copy(incognito = on) }

    /** share sheet: a fresh chat with the shared text and images ready to send */
    fun startWithShare(text: String, images: List<Uri>) {
        newChat()
        _composer.value = Composer(text = text)
        onInput(text)
        images.forEach { addImage(it) }
    }

    // ---- composer ----

    fun onInput(text: String) {
        _composer.update { it.copy(text = text) }
        val t = text.trimStart()
        recallQuery.value = when {
            t.equals("/recall", ignoreCase = true) -> ""
            t.startsWith("/recall ", ignoreCase = true) -> t.substring(8).trim()
            else -> null
        }
    }

    fun closeRecall() {
        if (_composer.value.text.trimStart().startsWith("/recall", ignoreCase = true)) onInput("")
        recallQuery.value = null
    }

    fun giveToKrala(r: RecallResult) {
        val line = KralaTags.RecallLine(recall.label(r.title, r.date), recall.clean(app.krala.core.RecallQuery.stripHighlights(r.excerpt)))
        _composer.update { c -> c.copy(recall = (c.recall + line).distinctBy { it.excerpt }, text = "") }
        recallQuery.value = null
    }

    fun giveMemoryToKrala(e: app.krala.data.db.MemoryEntryEntity) {
        val line = KralaTags.RecallLine(recall.label(e.chatName, e.updatedAt) + " · notes", recall.clean(e.notes).take(1200))
        _composer.update { c -> c.copy(recall = (c.recall + line).distinctBy { it.excerpt }, text = "") }
        recallQuery.value = null
    }

    fun removeRecall(line: KralaTags.RecallLine) = _composer.update { c -> c.copy(recall = c.recall - line) }

    fun addImage(uri: Uri) {
        viewModelScope.launch {
            _composer.update { it.copy(importing = true) }
            val name = attachments.import(uri)
            _composer.update { c -> c.copy(importing = false, images = if (name != null) (c.images + name).take(6) else c.images) }
        }
    }

    private val _notice = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** short messages for a snackbar (a file that couldn't be read, ...) */
    val notice: kotlinx.coroutines.flow.SharedFlow<String> = _notice

    fun addDocument(uri: Uri) {
        viewModelScope.launch {
            _composer.update { it.copy(importing = true) }
            try {
                val d = documents.import(uri)
                _composer.update { c -> c.copy(importing = false, docs = (c.docs + d).take(8)) }
            } catch (e: app.krala.data.chat.Documents.Unsupported) {
                _composer.update { it.copy(importing = false) }
                _notice.emit(e.name)
            } catch (e: Exception) {
                _composer.update { it.copy(importing = false) }
                _notice.emit(e.message ?: "file")
            }
        }
    }

    fun removeDocument(d: app.krala.data.chat.AttachedDoc) {
        _composer.update { it.copy(docs = it.docs - d) }
        documents.delete(d.stored)
    }

    fun toggleWeb() = _composer.update { it.copy(webSearch = !it.webSearch) }

    fun removeImage(name: String) {
        _composer.update { it.copy(images = it.images - name) }
        attachments.delete(name)
    }

    fun startEdit(m: UiMessage) {
        _composer.value = Composer(text = m.entity.content, editingId = m.entity.id)
    }

    fun cancelEdit() {
        _composer.value = Composer()
    }

    fun send() {
        val c = _composer.value
        val text = c.text.trim()
        if (text.startsWith("/recall", ignoreCase = true)) return
        if (text.isEmpty() && c.images.isEmpty() && c.docs.isEmpty()) return
        val id = _current.value
        if (c.editingId != null && id != null) {
            engine.editAndResend(id, c.editingId, text)
            _composer.value = Composer()
            return
        }
        // web search stays switched on for the next message until turned off
        _composer.value = Composer(webSearch = c.webSearch)
        recallQuery.value = null
        viewModelScope.launch {
            val convId = id ?: run {
                val d = _draft.value
                engine.createConversation(d.incognito, d.providerId, d.model, d.systemPrompt).also {
                    _current.value = it
                    activeChat.id = it
                }
            }
            engine.send(convId, text, c.images + c.docs.map { app.krala.data.chat.Documents.encode(it) }, c.recall, webSearch = c.webSearch)
        }
    }

    fun stop() {
        _current.value?.let { engine.stop(it) }
    }

    fun regenerate() {
        _current.value?.let { engine.regenerate(it) }
    }

    fun retry() = regenerate()

    // ---- chat settings ----

    fun selectModel(providerId: String?, model: String?) {
        val id = _current.value
        if (id == null) _draft.update { it.copy(providerId = providerId, model = model) }
        else viewModelScope.launch { chats.setModel(id, providerId, model) }
    }

    fun setDefaultModel(providerId: String?, model: String?) = viewModelScope.launch { settingsRepo.setDefaultModel(providerId, model) }

    fun setChatPrompt(prompt: String?) {
        val id = _current.value
        if (id == null) _draft.update { it.copy(systemPrompt = prompt?.takeIf { p -> p.isNotBlank() }) }
        else viewModelScope.launch { chats.setSystemPrompt(id, prompt) }
    }

    fun rename(id: String, title: String) = viewModelScope.launch { chats.rename(id, title) }
    fun togglePin(c: ConversationEntity) = viewModelScope.launch { chats.setPinned(c.id, !c.pinned) }

    fun delete(id: String, forgetMemory: Boolean) = viewModelScope.launch {
        chats.delete(id, forgetMemory)
        if (_current.value == id) {
            _current.value = null
            activeChat.id = null
        }
    }

    suspend fun shareIntent(id: String): Intent = export.shareIntent(id)
    suspend fun exportText(id: String, json: Boolean): Pair<String, String> = if (json) export.json(id) else export.markdown(id)
    fun writeExport(uri: Uri, text: String) = viewModelScope.launch { export.writeTo(uri, text) }
    fun safeName(title: String) = export.safeName(title)

    override fun onCleared() {
        leaveCurrent()
        super.onCleared()
    }
}
