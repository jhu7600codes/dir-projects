package app.krala.data.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import app.krala.data.db.ConversationDao
import app.krala.data.db.MessageDao
import app.krala.data.db.MessageStatus
import app.krala.data.db.Role
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val conversations: ConversationDao,
    private val messages: MessageDao,
) {
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

    @Serializable
    data class ExportedMessage(val role: String, val content: String, val time: Long, val model: String? = null)

    @Serializable
    data class ExportedChat(val title: String, val created: Long, val messages: List<ExportedMessage>)

    suspend fun markdown(id: String): Pair<String, String> {
        val c = conversations.get(id) ?: return "chat" to ""
        val list = messages.listFor(id).filter { it.status != MessageStatus.ERROR }
        val title = c.title.ifBlank { "chat" }
        val body = buildString {
            append("# ").append(title).append("\n\n")
            append("_").append(fmt.format(Instant.ofEpochMilli(c.createdAt))).append("_\n\n")
            list.forEach { m ->
                append("**").append(if (m.role == Role.USER) "you" else "krala").append("**")
                m.model?.let { append(" · ").append(it) }
                append("  \n")
                append(m.content.trim()).append("\n\n")
            }
        }
        return title to body
    }

    suspend fun json(id: String): Pair<String, String> {
        val c = conversations.get(id) ?: return "chat" to "{}"
        val list = messages.listFor(id).filter { it.status != MessageStatus.ERROR }
        val data = ExportedChat(c.title, c.createdAt, list.map { ExportedMessage(it.role, it.content, it.createdAt, it.model) })
        return c.title to Json { prettyPrint = true }.encodeToString(ExportedChat.serializer(), data)
    }

    suspend fun writeTo(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
    }

    /** a share sheet intent with the chat as a .md file */
    suspend fun shareIntent(id: String): Intent = withContext(Dispatchers.IO) {
        val (title, md) = markdown(id)
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, safeName(title) + ".md")
        file.writeText(md)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        Intent(Intent.ACTION_SEND).apply {
            type = "text/markdown"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, md.take(90_000))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun safeName(title: String) = title.replace(Regex("[^\\p{L}\\p{N} _-]"), "").trim().ifBlank { "krala-chat" }.take(60)
}
