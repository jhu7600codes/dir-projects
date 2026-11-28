package app.krala.data.chat

import app.krala.core.ContextWindow
import app.krala.core.FallbackPlanner
import app.krala.core.FallbackPlanner.ErrorKind
import app.krala.core.Ids
import app.krala.core.KralaTags
import app.krala.core.RateLimits
import app.krala.core.Tokens
import app.krala.data.db.ConversationDao
import app.krala.data.db.ConversationEntity
import app.krala.data.db.MessageDao
import app.krala.data.db.MessageEntity
import app.krala.data.db.MessageStatus
import app.krala.data.db.Role
import app.krala.data.memory.MemoryRepository
import app.krala.data.memory.RecallService
import app.krala.data.provider.ApiMessage
import app.krala.data.provider.Attempt
import app.krala.data.provider.OpenAiClient
import app.krala.data.provider.ProviderException
import app.krala.data.provider.ProviderRepository
import app.krala.data.provider.ProviderRouter
import app.krala.data.provider.StreamEvent
import app.krala.data.settings.SettingsRepository
import app.krala.di.AppScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** a reply being written right now */
data class LiveReply(
    val messageId: String,
    val text: String = "",
    val started: Boolean = false,
    val providerName: String? = null,
    val model: String? = null,
)

sealed interface ChatEvent {
    data class ReplyDone(val conversationId: String) : ChatEvent
    data class ReplyFailed(val conversationId: String, val message: String) : ChatEvent
    data class FellBack(val conversationId: String, val from: String, val to: String) : ChatEvent
}

/**
 * Sends messages and streams replies. Lives in the app scope so a reply keeps streaming through
 * rotation and screen changes.
 */
@Singleton
class ChatEngine @Inject constructor(
    private val conversations: ConversationDao,
    private val messages: MessageDao,
    private val settings: SettingsRepository,
    private val router: ProviderRouter,
    private val providers: ProviderRepository,
    private val client: OpenAiClient,
    private val memory: MemoryRepository,
    private val recall: RecallService,
    private val attachments: AttachmentStore,
    @AppScope private val scope: CoroutineScope,
) {
    private val _live = MutableStateFlow<Map<String, LiveReply>>(emptyMap())
    val live: StateFlow<Map<String, LiveReply>> = _live.asStateFlow()

    private val _events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()

    private val jobs = ConcurrentHashMap<String, Job>()

    fun isBusy(conversationId: String) = jobs[conversationId]?.isActive == true

    suspend fun createConversation(incognito: Boolean, providerId: String?, model: String?, systemPrompt: String?): String {
        val now = System.currentTimeMillis()
        val id = Ids.new()
        conversations.upsert(
            ConversationEntity(
                id = id, title = "", incognito = incognito, providerId = providerId, model = model,
                systemPrompt = systemPrompt, createdAt = now, updatedAt = now, lastMessageAt = now,
            )
        )
        return id
    }

    /** the hidden krala tags block for the first message of a chat */
    suspend fun buildPrefix(conv: ConversationEntity): String {
        val s = settings.current()
        val mem = if (conv.incognito) "" else KralaTags.formatMemory(memory.blockLines(exceptChatId = conv.id))
        return KralaTags.build(
            KralaTags.Block(
                memory = mem,
                systemPrompt = conv.systemPrompt ?: s.effectiveSystemPrompt,
                userInstructions = s.customInstructions,
            )
        )
    }

    fun send(conversationId: String, text: String, images: List<String>, extraRecall: List<KralaTags.RecallLine>) {
        if (isBusy(conversationId)) return
        scope.launch {
            val conv = conversations.get(conversationId) ?: return@launch
            val now = System.currentTimeMillis()
            val first = messages.count(conversationId) == 0
            val prefix = if (first) buildPrefix(conv) else null
            messages.upsert(
                MessageEntity(
                    id = Ids.new(), conversationId = conversationId, role = Role.USER, content = text.trim(),
                    hiddenPrefix = prefix, attachments = attachments.encode(images), createdAt = now, updatedAt = now,
                )
            )
            if (first && conv.titleAuto) {
                conversations.setTitle(conversationId, heuristicTitle(text, images.isNotEmpty()), auto = true, now = now)
            }
            conversations.touch(conversationId, now)
            generate(conversationId, text, extraRecall)
        }
    }

    fun regenerate(conversationId: String) {
        if (isBusy(conversationId)) return
        scope.launch {
            val list = messages.listFor(conversationId)
            val lastUser = list.lastOrNull { it.role == Role.USER } ?: return@launch
            messages.softDeleteAfter(conversationId, lastUser.createdAt, lastUser.id, System.currentTimeMillis())
            generate(conversationId, lastUser.content, emptyList())
        }
    }

    fun editAndResend(conversationId: String, messageId: String, newText: String) {
        if (isBusy(conversationId)) return
        scope.launch {
            val msg = messages.get(messageId) ?: return@launch
            val now = System.currentTimeMillis()
            messages.softDeleteAfter(conversationId, msg.createdAt, msg.id, now)
            messages.setContent(messageId, newText.trim(), now)
            conversations.touch(conversationId, now)
            generate(conversationId, newText, emptyList())
        }
    }

    fun stop(conversationId: String) {
        jobs[conversationId]?.cancel()
    }

    private fun generate(conversationId: String, queryText: String, extraRecall: List<KralaTags.RecallLine>) {
        val job = scope.launch {
            val conv = conversations.get(conversationId) ?: return@launch
            val s = settings.current()
            val created = System.currentTimeMillis() + 1
            val reply = MessageEntity(
                id = Ids.new(), conversationId = conversationId, role = Role.ASSISTANT, content = "",
                status = MessageStatus.STREAMING, createdAt = created, updatedAt = created,
            )
            messages.upsert(reply)
            _live.update { it + (conversationId to LiveReply(reply.id)) }

            var text = StringBuilder()
            var used: Attempt? = null
            var finalStatus = MessageStatus.ERROR
            var finalError: String? = null
            try {
                refreshPrompts(conv)
                val history = messages.listFor(conversationId).filter {
                    it.id != reply.id && it.status != MessageStatus.ERROR && (it.role == Role.USER || it.content.isNotBlank())
                }
                val recallLines = buildList {
                    addAll(extraRecall)
                    if (!conv.incognito) addAll(runCatching { recall.auto(conversationId, queryText) }.getOrDefault(emptyList()))
                }.distinctBy { it.excerpt }
                val needsVision = history.any { attachments.decode(it.attachments).isNotEmpty() }
                val attempts = router.attempts(conv.providerId ?: s.defaultProviderId, conv.model ?: s.defaultModel, needsVision)
                if (attempts.isEmpty()) {
                    finalError = "no provider with a model is set up. add one in settings → providers."
                    return@launch
                }
                val api = toApi(history, recallLines)
                var lastError: ProviderException? = null
                for ((index, attempt) in attempts.withIndex()) {
                    if (index > 0) {
                        _events.tryEmit(ChatEvent.FellBack(conversationId, attempts[index - 1].config.name, attempt.config.name))
                    }
                    val fitted = fit(api, attempt.config.contextSize)
                    val promptTokens = fitted.sumOf { Tokens.estimate(it.text) + it.images.size * Tokens.IMAGE_TOKENS }
                    val start = System.currentTimeMillis()
                    var firstTokenAt = 0L
                    var headers: Map<String, String> = emptyMap()
                    text = StringBuilder()
                    var lastSave = 0L
                    _live.update { it + (conversationId to LiveReply(reply.id, providerName = attempt.config.name, model = attempt.model)) }
                    try {
                        client.stream(attempt.config, attempt.model, fitted).collect { ev ->
                            when (ev) {
                                is StreamEvent.Opened -> headers = ev.headers
                                is StreamEvent.Delta -> {
                                    if (firstTokenAt == 0L) firstTokenAt = System.currentTimeMillis()
                                    text.append(ev.text)
                                    val shown = KralaTags.strip(text.toString())
                                    _live.update {
                                        it + (conversationId to LiveReply(reply.id, shown, started = shown.isNotEmpty(), attempt.config.name, attempt.model))
                                    }
                                    val t = System.currentTimeMillis()
                                    if (t - lastSave > 1_500) {
                                        lastSave = t
                                        messages.setContent(reply.id, shown, t)
                                    }
                                }
                            }
                        }
                        if (KralaTags.strip(text.toString()).isBlank()) throw ProviderException(ErrorKind.EMPTY, null, "the reply was empty")
                        val end = System.currentTimeMillis()
                        providers.markSuccess(attempt.config.id, RateLimits.remainingRequests(headers), end - start)
                        providers.recordUsage(
                            attempt.config, true, false, promptTokens, Tokens.estimate(text.toString()),
                            end - start, (if (firstTokenAt > 0) firstTokenAt else end) - start,
                        )
                        used = attempt
                        finalStatus = MessageStatus.DONE
                        finalError = null
                        break
                    } catch (e: ProviderException) {
                        lastError = e
                        router.onError(attempt, e)
                        providers.recordUsage(attempt.config, false, e.isRateLimit, promptTokens, 0, 0, 0)
                        val streamed = KralaTags.strip(text.toString()).isNotBlank()
                        if (!FallbackPlanner.shouldFallback(e.kind, streamed)) {
                            used = attempt
                            finalStatus = if (streamed) MessageStatus.STOPPED else MessageStatus.ERROR
                            finalError = "${attempt.config.name}: ${router.describe(e)}"
                            break
                        }
                    }
                }
                if (used == null) {
                    finalError = lastError?.let { router.describe(it) } ?: "every provider failed"
                    if (attempts.size > 1) finalError = "all ${attempts.size} providers failed. last: $finalError"
                }
            } catch (e: CancellationException) {
                finalStatus = MessageStatus.STOPPED
                finalError = null
                throw e
            } catch (e: Exception) {
                android.util.Log.e("krala", "generate crashed", e)
                finalError = e.message ?: e.javaClass.simpleName
            } finally {
                withContext(NonCancellable) {
                    val content = KralaTags.strip(text.toString())
                    val now = System.currentTimeMillis()
                    if (finalStatus == MessageStatus.STOPPED && content.isBlank()) {
                        // stopped before anything arrived: drop the empty bubble
                        messages.softDelete(reply.id, now)
                    } else {
                        messages.finish(reply.id, content, finalStatus, finalError, used?.config?.name, used?.model, now)
                    }
                    conversations.touch(conversationId, now)
                    _live.update { it - conversationId }
                    jobs.remove(conversationId)
                    if (finalStatus == MessageStatus.DONE) {
                        _events.tryEmit(ChatEvent.ReplyDone(conversationId))
                        used?.let { maybeTitle(conversationId, it) }
                    } else if (finalStatus == MessageStatus.ERROR) {
                        _events.tryEmit(ChatEvent.ReplyFailed(conversationId, finalError ?: "error"))
                    }
                }
            }
        }
        jobs[conversationId] = job
    }

    /** if the chat's prompt or instructions changed since the block was written, rewrite it */
    private suspend fun refreshPrompts(conv: ConversationEntity) {
        val first = messages.listFor(conv.id).firstOrNull { it.role == Role.USER } ?: return
        val prefix = first.hiddenPrefix ?: return
        val parsed = KralaTags.parse(prefix) ?: return
        val s = settings.current()
        val prompt = conv.systemPrompt ?: s.effectiveSystemPrompt
        if (parsed.systemPrompt != prompt.trim() || parsed.userInstructions != s.customInstructions.trim()) {
            messages.setPrefix(first.id, KralaTags.updatePrompts(prefix, prompt, s.customInstructions), System.currentTimeMillis())
        }
    }

    private suspend fun toApi(history: List<MessageEntity>, recallLines: List<KralaTags.RecallLine>): List<ApiMessage> {
        // images only travel with the last few messages that have them, older ones become a note
        val withImages = history.filter { attachments.decode(it.attachments).isNotEmpty() }.takeLast(4).map { it.id }.toSet()
        val refreshed = history.map { m -> if (m.role == Role.USER && m.hiddenPrefix != null) messages.get(m.id) ?: m else m }
        return refreshed.mapIndexed { i, m ->
            val names = attachments.decode(m.attachments)
            val images = if (m.id in withImages) names.mapNotNull { attachments.dataUrl(it) } else emptyList()
            var content = KralaTags.compose(m.hiddenPrefix, m.content)
            if (names.isNotEmpty() && images.isEmpty()) content += "\n[${names.size} image(s) attached earlier]"
            if (i == refreshed.lastIndex && m.role == Role.USER && recallLines.isNotEmpty()) {
                content = KralaTags.withRecall(content, KralaTags.formatRecall(recallLines))
            }
            ApiMessage(m.role, content, images)
        }
    }

    private fun fit(api: List<ApiMessage>, contextSize: Int): List<ApiMessage> {
        val items = api.map { ContextWindow.Item(Tokens.estimate(it.text) + it.images.size * Tokens.IMAGE_TOKENS + Tokens.MESSAGE_OVERHEAD) }
        val keep = ContextWindow.fit(items, contextSize, pinnedFirst = api.firstOrNull()?.role == Role.USER)
        return keep.map { api[it] }
    }

    private fun maybeTitle(conversationId: String, attempt: Attempt) {
        scope.launch {
            val conv = conversations.get(conversationId) ?: return@launch
            if (!conv.titleAuto) return@launch
            val list = messages.listFor(conversationId)
            if (list.size != 2) return@launch
            val q = list[0].content.take(600)
            val a = list[1].content.take(600)
            val prompt = "write a short title (2 to 5 words, lowercase, same language as the chat, no quotes, no emojis) " +
                "for this chat. reply with the title only.\n\nuser: $q\nassistant: $a"
            runCatching { client.complete(attempt.config, attempt.model, listOf(ApiMessage("user", prompt)), maxTokens = 24) }
                .getOrNull()
                ?.let { cleanTitle(it) }
                ?.takeIf { it.isNotBlank() }
                ?.let { title ->
                    val now = conversations.get(conversationId)
                    if (now != null && now.titleAuto) conversations.setTitle(conversationId, title, auto = false, now = System.currentTimeMillis())
                }
        }
    }

    companion object {
        fun heuristicTitle(text: String, hasImage: Boolean): String {
            val words = text.trim().replace(Regex("\\s+"), " ").split(' ').filter { it.isNotBlank() }
            if (words.isEmpty()) return if (hasImage) "image" else "new chat"
            val t = words.take(6).joinToString(" ")
            return (if (t.length > 42) t.take(42).trimEnd() + "…" else t).lowercase()
        }

        fun cleanTitle(raw: String): String {
            val line = KralaTags.strip(raw).lines().firstOrNull { it.isNotBlank() }?.trim() ?: return ""
            return line.removePrefix("title:").removePrefix("Title:").trim()
                .trim('"', '\'', '«', '»', '“', '”', '*', '#', '.', ' ')
                .take(48)
        }
    }
}
