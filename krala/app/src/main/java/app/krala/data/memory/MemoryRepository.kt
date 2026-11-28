package app.krala.data.memory

import app.krala.core.KralaTags
import app.krala.core.SecretScrubber
import app.krala.data.db.MemoryDao
import app.krala.data.db.MemoryEntryEntity
import app.krala.data.db.MemoryKind
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepository @Inject constructor(private val dao: MemoryDao) {
    val entries: Flow<List<MemoryEntryEntity>> = dao.observeAll()

    suspend fun active(): List<MemoryEntryEntity> = dao.active()

    suspend fun latest(limit: Int = 8) = dao.latest(limit)

    /** lines for the --memory-- section, archive first. the current chat's own notes are left out. */
    suspend fun blockLines(exceptChatId: String?): List<KralaTags.MemoryLine> =
        dao.active().filter { it.chatId == null || it.chatId != exceptChatId }.map {
            KralaTags.MemoryLine(if (it.kind == MemoryKind.ARCHIVE) "archive" else it.chatName, it.notes)
        }

    fun chatEntryId(chatId: String) = "chat:$chatId"

    suspend fun saveChatNotes(chatId: String, chatName: String, notes: String) {
        val now = System.currentTimeMillis()
        val id = chatEntryId(chatId)
        val old = dao.get(id)
        dao.upsert(
            MemoryEntryEntity(
                id = id,
                kind = MemoryKind.CHAT,
                chatId = chatId,
                chatName = chatName,
                notes = SecretScrubber.scrub(notes.trim()),
                // the chat went on after it was archived: its fresh notes are live again
                archivedInto = null,
                createdAt = old?.createdAt ?: now,
                updatedAt = now,
                deleted = false,
                syncedAt = old?.syncedAt ?: 0,
            )
        )
    }

    suspend fun saveArchive(notes: String, mergedIds: List<String>) {
        val now = System.currentTimeMillis()
        val old = dao.get(ARCHIVE_ID)
        dao.upsert(
            MemoryEntryEntity(
                id = ARCHIVE_ID,
                kind = MemoryKind.ARCHIVE,
                chatId = null,
                chatName = "archive",
                notes = SecretScrubber.scrub(notes.trim()),
                createdAt = old?.createdAt ?: now,
                updatedAt = now,
                syncedAt = old?.syncedAt ?: 0,
            )
        )
        if (mergedIds.isNotEmpty()) dao.archive(mergedIds, ARCHIVE_ID, now)
    }

    suspend fun edit(id: String, chatName: String, notes: String) {
        val e = dao.get(id) ?: return
        dao.upsert(e.copy(chatName = chatName.trim().ifBlank { e.chatName }, notes = SecretScrubber.scrub(notes), updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(id: String) = dao.softDelete(id, System.currentTimeMillis())

    suspend fun deleteForChat(chatId: String) = dao.softDelete(chatEntryId(chatId), System.currentTimeMillis())

    suspend fun forgetEverything() = dao.softDeleteAll(System.currentTimeMillis())

    companion object {
        const val ARCHIVE_ID = "archive"
    }
}
