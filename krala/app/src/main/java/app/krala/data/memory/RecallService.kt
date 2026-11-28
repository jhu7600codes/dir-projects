package app.krala.data.memory

import androidx.sqlite.db.SimpleSQLiteQuery
import app.krala.core.KralaTags
import app.krala.core.RecallQuery
import app.krala.core.SecretScrubber
import app.krala.data.db.Fts
import app.krala.data.db.MemoryHit
import app.krala.data.db.MessageHit
import app.krala.data.db.SearchDao
import app.krala.data.settings.SettingsRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

sealed interface RecallResult {
    val title: String
    val excerpt: String
    val date: Long

    data class FromChat(val hit: MessageHit) : RecallResult {
        override val title get() = hit.title
        override val excerpt get() = hit.excerpt
        override val date get() = hit.createdAt
    }

    data class FromMemory(val hit: MemoryHit) : RecallResult {
        override val title get() = hit.chatName
        override val excerpt get() = hit.excerpt
        override val date get() = hit.updatedAt
    }
}

@Singleton
class RecallService @Inject constructor(
    private val search: SearchDao,
    private val settings: SettingsRepository,
    private val embeddings: EmbeddingIndex,
) {
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())

    fun label(title: String, date: Long) = "$title · ${dateFmt.format(Instant.ofEpochMilli(date))}"

    suspend fun searchMessages(match: String?, like: List<String>, excludeConversation: String?, limit: Int): List<MessageHit> {
        val exclude = excludeConversation ?: ""
        if (Fts.ftsAvailable && match != null) {
            return try {
                search.messages(
                    SimpleSQLiteQuery(
                        """
                        SELECT m.id AS messageId, m.conversationId AS conversationId, c.title AS title, m.role AS role,
                               snippet(messages_fts, 0, char(2), char(3), '…', 40) AS excerpt, m.content AS content,
                               m.createdAt AS createdAt, bm25(messages_fts) AS score
                        FROM messages_fts
                        JOIN messages m ON m.rowid = messages_fts.rowid
                        JOIN conversations c ON c.id = m.conversationId
                        WHERE messages_fts MATCH ? AND m.deleted = 0 AND c.deleted = 0 AND c.incognito = 0 AND c.id != ?
                        ORDER BY score LIMIT ?
                        """.trimIndent(),
                        arrayOf(match, exclude, limit),
                    )
                )
            } catch (e: Exception) {
                android.util.Log.w("krala", "fts query failed, using LIKE", e)
                likeMessages(like, exclude, limit)
            }
        }
        return likeMessages(like, exclude, limit)
    }

    private suspend fun likeMessages(terms: List<String>, exclude: String, limit: Int): List<MessageHit> {
        if (terms.isEmpty()) return emptyList()
        val where = terms.joinToString(" OR ") { "m.content LIKE ? ESCAPE '\\'" }
        val args = terms.map { "%" + escapeLike(it) + "%" } + listOf(exclude, limit)
        val rows = search.messages(
            SimpleSQLiteQuery(
                """
                SELECT m.id AS messageId, m.conversationId AS conversationId, c.title AS title, m.role AS role,
                       substr(m.content, 1, 280) AS excerpt, m.content AS content, m.createdAt AS createdAt, 0.0 AS score
                FROM messages m JOIN conversations c ON c.id = m.conversationId
                WHERE ($where) AND m.deleted = 0 AND c.deleted = 0 AND c.incognito = 0 AND c.id != ?
                ORDER BY m.createdAt DESC LIMIT ?
                """.trimIndent(),
                args.toTypedArray(),
            )
        )
        return rows.map { it.copy(excerpt = highlight(windowAround(it.content, terms), terms)) }
    }

    suspend fun searchMemory(text: String, limit: Int): List<MemoryHit> {
        val match = RecallQuery.ftsAll(text)
        if (Fts.ftsAvailable && match != null) {
            try {
                return search.memory(
                    SimpleSQLiteQuery(
                        """
                        SELECT e.id AS id, e.chatId AS chatId, e.chatName AS chatName,
                               snippet(memory_fts, 1, char(2), char(3), '…', 40) AS excerpt, e.updatedAt AS updatedAt,
                               bm25(memory_fts) AS score
                        FROM memory_fts JOIN memory_entries e ON e.rowid = memory_fts.rowid
                        WHERE memory_fts MATCH ? AND e.deleted = 0
                        ORDER BY score LIMIT ?
                        """.trimIndent(),
                        arrayOf(match, limit),
                    )
                )
            } catch (e: Exception) {
                android.util.Log.w("krala", "memory fts failed", e)
            }
        }
        val words = text.split(Regex("\\s+")).filter { it.length >= 2 }
        if (words.isEmpty()) return emptyList()
        val where = words.joinToString(" AND ") { "(e.notes LIKE ? ESCAPE '\\' OR e.chatName LIKE ? ESCAPE '\\')" }
        val args = words.flatMap { listOf("%" + escapeLike(it) + "%", "%" + escapeLike(it) + "%") } + listOf(limit)
        return search.memory(
            SimpleSQLiteQuery(
                "SELECT e.id AS id, e.chatId AS chatId, e.chatName AS chatName, substr(e.notes, 1, 280) AS excerpt, e.updatedAt AS updatedAt, 0.0 AS score " +
                    "FROM memory_entries e WHERE ($where) AND e.deleted = 0 ORDER BY e.updatedAt DESC LIMIT ?",
                args.toTypedArray(),
            )
        )
    }

    /** the /recall command: past chats and memory entries */
    suspend fun command(query: String, currentConversation: String?): List<RecallResult> {
        val chats = searchMessages(RecallQuery.ftsAll(query), RecallQuery.terms(query).ifEmpty { listOf(query.trim()) }, null, 30)
            .distinctBy { it.messageId }
            .map { RecallResult.FromChat(it) }
        val mem = searchMemory(query, 10).map { RecallResult.FromMemory(it) }
        return (mem + chats).take(40)
    }

    /**
     * Automatic recall before a message: the best matching excerpts from other chats, or nothing.
     * Only strong matches count, so small talk doesn't drag random old messages in.
     */
    suspend fun auto(conversationId: String, text: String): List<KralaTags.RecallLine> {
        val s = settings.current()
        if (!s.autoRecall) return emptyList()
        val terms = RecallQuery.terms(text)
        val out = LinkedHashMap<String, KralaTags.RecallLine>()
        if (terms.isNotEmpty()) {
            val hits = searchMessages(RecallQuery.fts(terms), terms, conversationId, 12)
            val strong = hits.filter { h -> matchedTerms(h.content, terms) >= minOf(2, terms.size) }
            strong.take(3).forEach { h ->
                out[h.messageId] = KralaTags.RecallLine(
                    label(h.title, h.createdAt) + " · " + (if (h.role == "user") "user" else "krala"),
                    clean(RecallQuery.stripHighlights(h.excerpt)),
                )
            }
        }
        if (s.embeddingsEnabled) {
            runCatching { embeddings.nearest(text, conversationId, 3) }.getOrDefault(emptyList()).forEach { h ->
                if (out.size < 5 && h.messageId !in out) {
                    out[h.messageId] = KralaTags.RecallLine(
                        label(h.title, h.createdAt) + " · " + (if (h.role == "user") "user" else "krala"),
                        clean(windowAround(h.content, terms)),
                    )
                }
            }
        }
        return out.values.toList()
    }

    fun clean(s: String) = SecretScrubber.scrub(s.replace('\n', ' ').replace(Regex("\\s+"), " ").trim()).take(600)

    private fun matchedTerms(content: String, terms: List<String>): Int {
        val lc = content.lowercase()
        return terms.count { t -> lc.contains(if (t.length >= 5) t.dropLast(1) else t) }
    }

    private fun windowAround(content: String, terms: List<String>, size: Int = 280): String {
        val lc = content.lowercase()
        val idx = terms.map { lc.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: 0
        val start = (idx - size / 3).coerceAtLeast(0)
        val end = (start + size).coerceAtMost(content.length)
        return (if (start > 0) "…" else "") + content.substring(start, end) + (if (end < content.length) "…" else "")
    }

    private fun highlight(s: String, terms: List<String>): String {
        var out = s
        terms.forEach { t ->
            out = out.replace(Regex(Regex.escape(t), RegexOption.IGNORE_CASE)) { "${RecallQuery.HL_START}${it.value}${RecallQuery.HL_END}" }
        }
        return out
    }

    private fun escapeLike(s: String) = s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
