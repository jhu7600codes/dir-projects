package app.krala.data.chat

import app.krala.data.db.ConversationDao
import app.krala.data.db.ConversationEntity
import app.krala.data.db.MessageDao
import app.krala.data.db.MessageEntity
import app.krala.data.memory.MemoryRepository
import app.krala.data.memory.MemoryScheduler
import app.krala.data.memory.RecallService
import app.krala.core.RecallQuery
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val conversations: ConversationDao,
    private val messages: MessageDao,
    private val memory: MemoryRepository,
    private val scheduler: MemoryScheduler,
    private val recall: RecallService,
) {
    val all: Flow<List<ConversationEntity>> = conversations.observeAll()

    fun conversation(id: String): Flow<ConversationEntity?> = conversations.observe(id)
    fun messages(id: String): Flow<List<MessageEntity>> = messages.observeFor(id)

    suspend fun get(id: String) = conversations.get(id)
    suspend fun messagesOnce(id: String) = messages.listFor(id)

    suspend fun rename(id: String, title: String) =
        conversations.setTitle(id, title.trim().ifBlank { "chat" }, auto = false, now = System.currentTimeMillis())

    suspend fun setPinned(id: String, pinned: Boolean) = conversations.setPinned(id, pinned, System.currentTimeMillis())

    suspend fun setModel(id: String, providerId: String?, model: String?) =
        conversations.setModel(id, providerId, model, System.currentTimeMillis())

    suspend fun setSystemPrompt(id: String, prompt: String?) =
        conversations.setSystemPrompt(id, prompt?.takeIf { it.isNotBlank() }, System.currentTimeMillis())

    suspend fun delete(id: String, forgetMemory: Boolean) {
        val now = System.currentTimeMillis()
        scheduler.cancel(id)
        messages.softDeleteFor(id, now)
        conversations.softDelete(id, now)
        if (forgetMemory) memory.deleteForChat(id)
    }

    suspend fun deleteAll() {
        val now = System.currentTimeMillis()
        conversations.all().filter { !it.deleted }.forEach {
            scheduler.cancel(it.id)
            messages.softDeleteFor(it.id, now)
        }
        conversations.softDeleteAll(now)
    }

    /** conversation ids whose messages match [query] (drawer search) */
    suspend fun searchConversationIds(query: String): Set<String> {
        if (query.isBlank()) return emptySet()
        return recall.searchMessages(RecallQuery.ftsAll(query), listOf(query.trim()), null, 200).map { it.conversationId }.toSet()
    }
}
