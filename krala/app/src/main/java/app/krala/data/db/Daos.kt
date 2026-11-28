package app.krala.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE deleted = 0 ORDER BY pinned DESC, lastMessageAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    fun observe(id: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun get(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations")
    suspend fun all(): List<ConversationEntity>

    @Upsert
    suspend fun upsert(c: ConversationEntity)

    @Upsert
    suspend fun upsertAll(c: List<ConversationEntity>)

    @Query("UPDATE conversations SET title = :title, titleAuto = :auto, updatedAt = :now WHERE id = :id")
    suspend fun setTitle(id: String, title: String, auto: Boolean, now: Long)

    @Query("UPDATE conversations SET pinned = :pinned, updatedAt = :now WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, now: Long)

    @Query("UPDATE conversations SET providerId = :providerId, model = :model, updatedAt = :now WHERE id = :id")
    suspend fun setModel(id: String, providerId: String?, model: String?, now: Long)

    @Query("UPDATE conversations SET systemPrompt = :prompt, updatedAt = :now WHERE id = :id")
    suspend fun setSystemPrompt(id: String, prompt: String?, now: Long)

    @Query("UPDATE conversations SET updatedAt = :now, lastMessageAt = :now WHERE id = :id")
    suspend fun touch(id: String, now: Long)

    @Query("UPDATE conversations SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE conversations SET deleted = 1, updatedAt = :now WHERE deleted = 0")
    suspend fun softDeleteAll(now: Long)

    @Query("SELECT * FROM conversations WHERE updatedAt > syncedAt AND incognito = 0")
    suspend fun dirty(): List<ConversationEntity>

    @Query("UPDATE conversations SET syncedAt = :at WHERE id = :id")
    suspend fun markSynced(id: String, at: Long)

    @Query("UPDATE conversations SET notesAt = :at WHERE id = :id")
    suspend fun markNotes(id: String, at: Long)

    @Query("SELECT * FROM conversations WHERE deleted = 0 AND incognito = 0 AND updatedAt > notesAt AND id != :exceptId")
    suspend fun needingNotes(exceptId: String): List<ConversationEntity>

    @Query("UPDATE conversations SET syncedAt = 0")
    suspend fun resetSynced()

    @Query("SELECT COUNT(*) FROM conversations WHERE deleted = 0")
    fun observeCount(): Flow<Int>
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :cid AND deleted = 0 ORDER BY createdAt, id")
    fun observeFor(cid: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :cid AND deleted = 0 ORDER BY createdAt, id")
    suspend fun listFor(cid: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun get(id: String): MessageEntity?

    @Query("SELECT * FROM messages")
    suspend fun all(): List<MessageEntity>

    @Upsert
    suspend fun upsert(m: MessageEntity)

    @Upsert
    suspend fun upsertAll(m: List<MessageEntity>)

    @Query("UPDATE messages SET content = :content, status = :status, error = :error, providerName = :provider, model = :model, updatedAt = :now WHERE id = :id")
    suspend fun finish(id: String, content: String, status: String, error: String?, provider: String?, model: String?, now: Long)

    @Query("UPDATE messages SET content = :content, updatedAt = :now WHERE id = :id")
    suspend fun setContent(id: String, content: String, now: Long)

    @Query("UPDATE messages SET hiddenPrefix = :prefix, updatedAt = :now WHERE id = :id")
    suspend fun setPrefix(id: String, prefix: String?, now: Long)

    @Query("UPDATE messages SET deleted = 1, updatedAt = :now WHERE conversationId = :cid AND deleted = 0 AND (createdAt > :after OR (createdAt = :after AND id > :afterId))")
    suspend fun softDeleteAfter(cid: String, after: Long, afterId: String, now: Long)

    @Query("UPDATE messages SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE messages SET deleted = 1, updatedAt = :now WHERE conversationId = :cid AND deleted = 0")
    suspend fun softDeleteFor(cid: String, now: Long)

    @Query("UPDATE messages SET status = 'stopped', updatedAt = :now WHERE status = 'streaming'")
    suspend fun closeDanglingStreams(now: Long)

    @Query("SELECT m.* FROM messages m JOIN conversations c ON c.id = m.conversationId WHERE m.updatedAt > m.syncedAt AND c.incognito = 0")
    suspend fun dirty(): List<MessageEntity>

    @Query("UPDATE messages SET syncedAt = :at WHERE id = :id")
    suspend fun markSynced(id: String, at: Long)

    @Query("UPDATE messages SET syncedAt = 0")
    suspend fun resetSynced()

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :cid AND deleted = 0")
    suspend fun count(cid: String): Int

    @Query(
        "SELECT m.* FROM messages m JOIN conversations c ON c.id = m.conversationId " +
            "LEFT JOIN embeddings e ON e.messageId = m.id AND e.model = :model " +
            "WHERE e.messageId IS NULL AND m.deleted = 0 AND c.deleted = 0 AND c.incognito = 0 AND m.status = 'done' " +
            "ORDER BY m.createdAt DESC LIMIT :limit"
    )
    suspend fun withoutEmbedding(model: String, limit: Int): List<MessageEntity>
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_entries WHERE deleted = 0 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MemoryEntryEntity>>

    /** what goes into the krala tags block: live chat entries + the archive */
    @Query("SELECT * FROM memory_entries WHERE deleted = 0 AND archivedInto IS NULL ORDER BY CASE kind WHEN 'archive' THEN 0 ELSE 1 END, updatedAt")
    suspend fun active(): List<MemoryEntryEntity>

    @Query("SELECT * FROM memory_entries WHERE deleted = 0 AND archivedInto IS NULL ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun latest(limit: Int): List<MemoryEntryEntity>

    @Query("SELECT * FROM memory_entries WHERE id = :id")
    suspend fun get(id: String): MemoryEntryEntity?

    @Query("SELECT * FROM memory_entries")
    suspend fun all(): List<MemoryEntryEntity>

    @Upsert
    suspend fun upsert(e: MemoryEntryEntity)

    @Upsert
    suspend fun upsertAll(e: List<MemoryEntryEntity>)

    @Query("UPDATE memory_entries SET archivedInto = :archiveId, updatedAt = :now WHERE id IN (:ids)")
    suspend fun archive(ids: List<String>, archiveId: String, now: Long)

    @Query("UPDATE memory_entries SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE memory_entries SET deleted = 1, updatedAt = :now WHERE deleted = 0")
    suspend fun softDeleteAll(now: Long)

    @Query("SELECT * FROM memory_entries WHERE updatedAt > syncedAt")
    suspend fun dirty(): List<MemoryEntryEntity>

    @Query("UPDATE memory_entries SET syncedAt = :at WHERE id = :id")
    suspend fun markSynced(id: String, at: Long)

    @Query("UPDATE memory_entries SET syncedAt = 0")
    suspend fun resetSynced()
}

@Dao
interface ProviderDao {
    @Query("SELECT * FROM providers WHERE deleted = 0 ORDER BY priority, createdAt")
    fun observeAll(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers WHERE deleted = 0 ORDER BY priority, createdAt")
    suspend fun list(): List<ProviderEntity>

    @Query("SELECT * FROM providers")
    suspend fun all(): List<ProviderEntity>

    @Query("SELECT * FROM providers WHERE id = :id")
    suspend fun get(id: String): ProviderEntity?

    @Upsert
    suspend fun upsert(p: ProviderEntity)

    @Upsert
    suspend fun upsertAll(p: List<ProviderEntity>)

    @Query("UPDATE providers SET priority = :priority, updatedAt = :now WHERE id = :id")
    suspend fun setPriority(id: String, priority: Int, now: Long)

    @Transaction
    suspend fun reorder(ids: List<String>, now: Long) {
        ids.forEachIndexed { i, id -> setPriority(id, i, now) }
    }

    @Query("UPDATE providers SET rateLimitedUntil = :until, remainingRequests = :remaining WHERE id = :id")
    suspend fun setRateLimit(id: String, until: Long, remaining: Int?)

    @Query("UPDATE providers SET remainingRequests = :remaining, lastLatencyMs = :latency, lastError = NULL WHERE id = :id")
    suspend fun recordSuccess(id: String, remaining: Int?, latency: Long)

    @Query("UPDATE providers SET lastError = :error, lastErrorAt = :at WHERE id = :id")
    suspend fun recordError(id: String, error: String, at: Long)

    @Query("UPDATE providers SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("SELECT * FROM providers WHERE updatedAt > syncedAt")
    suspend fun dirty(): List<ProviderEntity>

    @Query("UPDATE providers SET syncedAt = :at WHERE id = :id")
    suspend fun markSynced(id: String, at: Long)

    @Query("UPDATE providers SET syncedAt = 0")
    suspend fun resetSynced()

    @Query("SELECT COUNT(*) FROM providers")
    suspend fun countAll(): Int
}

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage WHERE day >= :fromDay ORDER BY day")
    fun observeSince(fromDay: Long): Flow<List<UsageEntity>>

    @Query("SELECT * FROM usage WHERE providerId = :pid AND day = :day")
    suspend fun get(pid: String, day: Long): UsageEntity?

    @Upsert
    suspend fun upsert(u: UsageEntity)

    @Query("DELETE FROM usage")
    suspend fun clear()

    @Transaction
    suspend fun record(
        pid: String,
        name: String,
        day: Long,
        success: Boolean,
        rateLimited: Boolean,
        promptTokens: Int,
        completionTokens: Int,
        latencyMs: Long,
        firstTokenMs: Long,
    ) {
        val cur = get(pid, day) ?: UsageEntity(pid, day, name)
        upsert(
            cur.copy(
                providerName = name,
                requests = cur.requests + 1,
                successes = cur.successes + if (success) 1 else 0,
                failures = cur.failures + if (success) 0 else 1,
                rateLimits = cur.rateLimits + if (rateLimited) 1 else 0,
                promptTokens = cur.promptTokens + promptTokens,
                completionTokens = cur.completionTokens + completionTokens,
                totalLatencyMs = cur.totalLatencyMs + if (success) latencyMs else 0,
                firstTokenMs = cur.firstTokenMs + if (success) firstTokenMs else 0,
            )
        )
    }
}

@Dao
interface EmbeddingDao {
    @Upsert
    suspend fun upsertAll(e: List<EmbeddingEntity>)

    @Query("SELECT * FROM embeddings WHERE model = :model")
    suspend fun allFor(model: String): List<EmbeddingEntity>

    @Query("DELETE FROM embeddings")
    suspend fun clear()
}

/** columns returned by the fts queries in [SearchDao] */
data class MessageHit(
    val messageId: String,
    val conversationId: String,
    val title: String,
    val role: String,
    val excerpt: String,
    val content: String,
    val createdAt: Long,
    val score: Double,
)

data class MemoryHit(
    val id: String,
    val chatId: String?,
    val chatName: String,
    val excerpt: String,
    val updatedAt: Long,
    val score: Double,
)

@Dao
interface SearchDao {
    @RawQuery
    suspend fun messages(q: SupportSQLiteQuery): List<MessageHit>

    @RawQuery
    suspend fun memory(q: SupportSQLiteQuery): List<MemoryHit>

    @RawQuery
    suspend fun ids(q: SupportSQLiteQuery): List<String>
}
