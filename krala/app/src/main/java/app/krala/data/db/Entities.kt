package app.krala.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations", indices = [Index("updatedAt"), Index("lastMessageAt")])
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val titleAuto: Boolean = true,
    val pinned: Boolean = false,
    val incognito: Boolean = false,
    /** null = automatic (provider priority order) */
    val providerId: String? = null,
    val model: String? = null,
    /** null = the global system prompt from settings */
    val systemPrompt: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val lastMessageAt: Long,
    val deleted: Boolean = false,
    /** local only: last time this row was pushed */
    val syncedAt: Long = 0,
    /** local only: updatedAt of the chat when its memory notes were written */
    val notesAt: Long = 0,
)

object Role {
    const val USER = "user"
    const val ASSISTANT = "assistant"
}

object MessageStatus {
    const val DONE = "done"
    const val STREAMING = "streaming"
    const val STOPPED = "stopped"
    const val ERROR = "error"
}

@Entity(tableName = "messages", indices = [Index("conversationId"), Index("updatedAt")])
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    /** the krala tags block sent before the first message; hidden in the ui */
    val hiddenPrefix: String? = null,
    /** json array of attachment file names inside files/attachments */
    val attachments: String = "[]",
    val status: String = MessageStatus.DONE,
    val error: String? = null,
    val providerName: String? = null,
    val model: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val syncedAt: Long = 0,
    /** web sources used for this reply, one "title\turl" per line */
    val sources: String? = null,
    /** the model's visible thinking, shown folded above the reply, never sent back */
    val reasoning: String? = null,
)

object MemoryKind {
    const val CHAT = "chat"
    const val ARCHIVE = "archive"
}

@Entity(tableName = "memory_entries", indices = [Index("chatId"), Index("updatedAt")])
data class MemoryEntryEntity(
    @PrimaryKey val id: String,
    val kind: String = MemoryKind.CHAT,
    val chatId: String?,
    val chatName: String,
    val notes: String,
    /** set when this entry was folded into the archive; kept forever, just not sent */
    val archivedInto: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val syncedAt: Long = 0,
)

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val baseUrl: String,
    /** json array of model ids */
    val models: String = "[]",
    val defaultModel: String? = null,
    val contextSize: Int = 32_000,
    val priority: Int = 0,
    val enabled: Boolean = true,
    val presetId: String? = null,
    val supportsVision: Boolean = false,
    /** api key + custom headers, encrypted with the android keystore. never uploaded as is. */
    val secretCipher: String? = null,
    /** local only: rate limit state */
    val rateLimitedUntil: Long = 0,
    val remainingRequests: Int? = null,
    val lastError: String? = null,
    val lastErrorAt: Long = 0,
    val lastLatencyMs: Long = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val syncedAt: Long = 0,
)

@Entity(tableName = "usage", primaryKeys = ["providerId", "day"])
data class UsageEntity(
    val providerId: String,
    /** epoch day */
    val day: Long,
    val providerName: String,
    val requests: Int = 0,
    val successes: Int = 0,
    val failures: Int = 0,
    val rateLimits: Int = 0,
    val promptTokens: Long = 0,
    val completionTokens: Long = 0,
    val totalLatencyMs: Long = 0,
    val firstTokenMs: Long = 0,
)

/** optional embeddings for recall, one per message */
@Entity(tableName = "embeddings")
data class EmbeddingEntity(
    @PrimaryKey val messageId: String,
    val conversationId: String,
    val model: String,
    val dims: Int,
    val vector: ByteArray,
    val createdAt: Long,
) {
    override fun equals(other: Any?) = other is EmbeddingEntity && other.messageId == messageId && other.model == model
    override fun hashCode() = messageId.hashCode()
}

/** a user made command: krala follows [instructions] when it's called */
@Entity(tableName = "plugins")
data class PluginEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val instructions: String,
    val web: Boolean = false,
    val enabled: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)
