package app.krala.data.backup

import android.content.Context
import android.net.Uri
import app.krala.data.db.ConversationDao
import app.krala.data.db.ConversationEntity
import app.krala.data.db.MemoryDao
import app.krala.data.db.MemoryEntryEntity
import app.krala.data.db.MessageDao
import app.krala.data.db.MessageEntity
import app.krala.data.db.ProviderDao
import app.krala.data.db.ProviderEntity
import app.krala.data.settings.SettingsRepository
import app.krala.data.settings.SyncedSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A full backup as one json file. Api keys are left out (they only exist encrypted with this
 * device's keystore); providers come back without keys.
 */
@Singleton
class BackupService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val conversations: ConversationDao,
    private val messages: MessageDao,
    private val memory: MemoryDao,
    private val providers: ProviderDao,
    private val settings: SettingsRepository,
) {
    @Serializable
    data class Conv(
        val id: String, val title: String, val titleAuto: Boolean, val pinned: Boolean, val incognito: Boolean,
        val providerId: String? = null, val model: String? = null, val systemPrompt: String? = null,
        val createdAt: Long, val updatedAt: Long, val lastMessageAt: Long, val deleted: Boolean,
    )

    @Serializable
    data class Msg(
        val id: String, val conversationId: String, val role: String, val content: String, val hiddenPrefix: String? = null,
        val attachments: String = "[]", val status: String, val error: String? = null, val providerName: String? = null,
        val model: String? = null, val createdAt: Long, val updatedAt: Long, val deleted: Boolean,
    )

    @Serializable
    data class Mem(
        val id: String, val kind: String, val chatId: String? = null, val chatName: String, val notes: String,
        val archivedInto: String? = null, val createdAt: Long, val updatedAt: Long, val deleted: Boolean,
    )

    @Serializable
    data class Prov(
        val id: String, val name: String, val baseUrl: String, val models: String, val defaultModel: String? = null,
        val contextSize: Int, val priority: Int, val enabled: Boolean, val presetId: String? = null,
        val supportsVision: Boolean, val createdAt: Long, val updatedAt: Long, val deleted: Boolean,
    )

    @Serializable
    data class Backup(
        val app: String = "krala",
        val version: Int = 1,
        val createdAt: Long,
        val conversations: List<Conv>,
        val messages: List<Msg>,
        val memory: List<Mem>,
        val providers: List<Prov>,
        val settings: SyncedSettings,
    )

    data class Summary(val chats: Int, val messages: Int, val memory: Int, val providers: Int)

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun export(uri: Uri): Summary = withContext(Dispatchers.IO) {
        val b = Backup(
            createdAt = System.currentTimeMillis(),
            conversations = conversations.all().map {
                Conv(it.id, it.title, it.titleAuto, it.pinned, it.incognito, it.providerId, it.model, it.systemPrompt, it.createdAt, it.updatedAt, it.lastMessageAt, it.deleted)
            },
            messages = messages.all().map {
                Msg(it.id, it.conversationId, it.role, it.content, it.hiddenPrefix, it.attachments, it.status, it.error, it.providerName, it.model, it.createdAt, it.updatedAt, it.deleted)
            },
            memory = memory.all().map {
                Mem(it.id, it.kind, it.chatId, it.chatName, it.notes, it.archivedInto, it.createdAt, it.updatedAt, it.deleted)
            },
            providers = providers.all().map {
                Prov(it.id, it.name, it.baseUrl, it.models, it.defaultModel, it.contextSize, it.priority, it.enabled, it.presetId, it.supportsVision, it.createdAt, it.updatedAt, it.deleted)
            },
            settings = settings.toSynced(settings.current()),
        )
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.encodeToString(Backup.serializer(), b).toByteArray()) }
            ?: error("could not open the file")
        Summary(b.conversations.count { !it.deleted }, b.messages.count { !it.deleted }, b.memory.count { !it.deleted }, b.providers.count { !it.deleted })
    }

    /** merges a backup in: rows that are newer in the file win, nothing local is wiped */
    suspend fun restore(uri: Uri): Summary = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: error("could not open the file")
        val b = json.decodeFromString(Backup.serializer(), text)
        require(b.app == "krala") { "not a krala backup" }
        var c = 0; var m = 0; var e = 0; var p = 0
        b.conversations.forEach { r ->
            val l = conversations.get(r.id)
            if (l == null || r.updatedAt > l.updatedAt) {
                conversations.upsert(
                    ConversationEntity(r.id, r.title, r.titleAuto, r.pinned, r.incognito, r.providerId, r.model, r.systemPrompt, r.createdAt, r.updatedAt, r.lastMessageAt, r.deleted, syncedAt = 0, notesAt = l?.notesAt ?: 0)
                ); c++
            }
        }
        b.messages.chunked(500).forEach { batch ->
            val rows = batch.filter { r -> messages.get(r.id).let { it == null || r.updatedAt > it.updatedAt } }.map { r ->
                MessageEntity(r.id, r.conversationId, r.role, r.content, r.hiddenPrefix, r.attachments, r.status, r.error, r.providerName, r.model, r.createdAt, r.updatedAt, r.deleted)
            }
            if (rows.isNotEmpty()) messages.upsertAll(rows)
            m += rows.size
        }
        b.memory.forEach { r ->
            val l = memory.get(r.id)
            if (l == null || r.updatedAt > l.updatedAt) {
                memory.upsert(MemoryEntryEntity(r.id, r.kind, r.chatId, r.chatName, r.notes, r.archivedInto, r.createdAt, r.updatedAt, r.deleted)); e++
            }
        }
        b.providers.forEach { r ->
            val l = providers.get(r.id)
            if (l == null || r.updatedAt > l.updatedAt) {
                providers.upsert(
                    ProviderEntity(
                        id = r.id, name = r.name, baseUrl = r.baseUrl, models = r.models, defaultModel = r.defaultModel,
                        contextSize = r.contextSize, priority = r.priority, enabled = r.enabled, presetId = r.presetId,
                        supportsVision = r.supportsVision, secretCipher = l?.secretCipher, createdAt = r.createdAt,
                        updatedAt = r.updatedAt, deleted = r.deleted,
                    )
                ); p++
            }
        }
        settings.applySynced(b.settings, System.currentTimeMillis())
        Summary(c, m, e, p)
    }
}
