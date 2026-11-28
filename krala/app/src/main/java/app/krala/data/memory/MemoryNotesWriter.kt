package app.krala.data.memory

import app.krala.core.KralaTags
import app.krala.core.MemoryBudget
import app.krala.core.Tokens
import app.krala.data.db.ConversationDao
import app.krala.data.db.MemoryKind
import app.krala.data.db.MessageDao
import app.krala.data.db.MessageStatus
import app.krala.data.db.Role
import app.krala.data.provider.ApiMessage
import app.krala.data.provider.ProviderRepository
import app.krala.data.provider.ProviderRouter
import app.krala.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The client side memory: after a chat ends, the provider picked in settings writes exhaustive
 * notes for it. The model never decides what is kept; it only writes down everything.
 */
@Singleton
class MemoryNotesWriter @Inject constructor(
    private val conversations: ConversationDao,
    private val messages: MessageDao,
    private val memory: MemoryRepository,
    private val router: ProviderRouter,
    private val providers: ProviderRepository,
    private val settings: SettingsRepository,
) {
    enum class Outcome { WRITTEN, SKIPPED, BUSY }

    private val notesRules = """
        you write memory notes for krala, an ai friend app, so she remembers this chat later.
        read the chat and write exhaustive notes. drop nothing. include:
        - every fact about the user: name, nicknames, age if they said it, people, pets, places, school or work, hobbies, likes and dislikes, gear and devices, anything they told about themselves
        - decisions, plans, deadlines, dates and promises (by the user or by krala)
        - preferences about how krala should talk or behave
        - running jokes, nicknames, bits and recurring references
        - what was talked about and what krala said, in order, briefly
        write compact plain text lines starting with "- ", no headings, no markdown, in the language the chat used.
        never write passwords, api keys, tokens, card numbers, codes or any other secret; write [secret removed] instead.
        output only the notes.
    """.trimIndent()

    suspend fun writeFor(chatId: String): Outcome {
        val conv = conversations.get(chatId) ?: return Outcome.SKIPPED
        if (conv.deleted || conv.incognito) return Outcome.SKIPPED
        if (conv.updatedAt <= conv.notesAt) return Outcome.SKIPPED
        val msgs = messages.listFor(chatId)
        if (msgs.any { it.status == MessageStatus.STREAMING }) return Outcome.BUSY
        val lines = msgs.filter { it.status != MessageStatus.ERROR && it.content.isNotBlank() }.map {
            (if (it.role == Role.USER) "user: " else "krala: ") + it.content.trim()
        }
        if (lines.isEmpty()) {
            conversations.markNotes(chatId, conv.updatedAt)
            return Outcome.SKIPPED
        }
        val s = settings.current()
        val ctx = contextFor(s.memoryProviderId)
        val chunkBudget = (ctx * 0.45).toInt().coerceAtLeast(1_500) - Tokens.estimate(notesRules)
        val chunks = chunk(lines, chunkBudget)

        var notes = ""
        chunks.forEachIndexed { i, part ->
            val user = buildString {
                append("chat name: ").append(conv.title).append('\n')
                if (chunks.size > 1) append("part ${i + 1} of ${chunks.size}\n")
                if (notes.isNotBlank()) {
                    append("\nnotes so far (keep every line, add the new facts from this part):\n").append(notes).append('\n')
                }
                append("\n--- chat ---\n").append(part).append("\n--- end ---")
            }
            val (text, _) = router.complete(
                s.memoryProviderId, s.memoryModel,
                listOf(ApiMessage("system", notesRules), ApiMessage("user", user)),
            )
            notes = KralaTags.strip(text).trim()
        }
        if (notes.isBlank()) return Outcome.SKIPPED
        memory.saveChatNotes(chatId, conv.title, notes)
        conversations.markNotes(chatId, conv.updatedAt)
        return Outcome.WRITTEN
    }

    /**
     * When the entries pass 60% of the model's context, the oldest ones are folded into the
     * archive entry. Merged entries stay in the database (marked archived), nothing is deleted.
     */
    suspend fun condenseIfNeeded(): Boolean {
        val s = settings.current()
        val chatCtx = contextFor(s.defaultProviderId)
        val active = memory.active()
        val budgetEntries = active.map {
            MemoryBudget.Entry(it.id, Tokens.estimate(it.chatName) + Tokens.estimate(it.notes) + 4, it.updatedAt, it.kind == MemoryKind.ARCHIVE)
        }
        val plan = MemoryBudget.plan(budgetEntries, chatCtx) ?: return false
        val byId = active.associateBy { it.id }
        val archive = active.firstOrNull { it.kind == MemoryKind.ARCHIVE }
        val merging = plan.toMerge.mapNotNull { byId[it] }
        val words = (plan.archiveBudget * 0.7).toInt()
        val rules = """
            you condense memory notes for krala, an ai friend app.
            merge everything below into one archive of notes. keep every fact, name, date, decision, plan, preference,
            running joke and promise. only remove repetition and filler wording. when facts conflict, keep the newer one
            and mention the change. plain lines starting with "- ", no headings. stay under about $words words.
            never write passwords, api keys, card numbers or other secrets.
            output only the merged notes.
        """.trimIndent()
        val parts = buildList {
            archive?.let { add("[archive] " + it.notes) }
            merging.forEach { add("[${it.chatName}] " + it.notes) }
        }
        val memCtx = contextFor(s.memoryProviderId)
        val chunks = chunk(parts, (memCtx * 0.45).toInt().coerceAtLeast(1_500))
        var merged = ""
        for (c in chunks) {
            val input = if (merged.isBlank()) c else "[archive so far] $merged\n$c"
            merged = KralaTags.strip(
                router.complete(s.memoryProviderId, s.memoryModel, listOf(ApiMessage("system", rules), ApiMessage("user", input))).first
            ).trim()
        }
        if (merged.isBlank()) return false
        memory.saveArchive(merged, merging.map { it.id })
        return true
    }

    /** context size of a provider, or of the first one in the priority list */
    private suspend fun contextFor(providerId: String?): Int {
        val list = providers.list().filter { it.entity.enabled }
        val p = providerId?.let { id -> list.firstOrNull { it.id == id } } ?: list.firstOrNull()
        return p?.entity?.contextSize ?: 32_000
    }

    private fun chunk(lines: List<String>, budget: Int): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var used = 0
        for (l in lines) {
            val line = if (Tokens.estimate(l) > budget) l.take(budget * 3) else l
            val t = Tokens.estimate(line)
            if (used + t > budget && cur.isNotEmpty()) {
                out += cur.toString(); cur.clear(); used = 0
            }
            cur.append(line).append('\n')
            used += t
        }
        if (cur.isNotEmpty()) out += cur.toString()
        return out
    }
}
