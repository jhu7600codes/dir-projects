package app.krala.core

/**
 * The hidden context block krala prepends to the first message of every chat.
 *
 * --krala tags--
 * --memory--
 * [chat name] notes
 * --system prompt--
 * ...
 * --user instructions--
 * ...
 * --krala tags end--
 *
 * Every section is always present, even when empty. Matching is case insensitive, so a block
 * typed or echoed as --KRALA TAGS-- is still recognised and stripped.
 */
object KralaTags {
    const val OPEN = "--krala tags--"
    const val CLOSE = "--krala tags end--"
    const val MEMORY = "--memory--"
    const val SYSTEM_PROMPT = "--system prompt--"
    const val USER_INSTRUCTIONS = "--user instructions--"
    const val RECALL = "--recall--"

    private val sectionNames = listOf(MEMORY, SYSTEM_PROMPT, USER_INSTRUCTIONS, RECALL)

    data class MemoryLine(val chatName: String, val notes: String)
    data class RecallLine(val label: String, val excerpt: String)

    data class Block(
        val memory: String = "",
        val systemPrompt: String = "",
        val userInstructions: String = "",
        val recall: String? = null,
    )

    fun formatMemory(entries: List<MemoryLine>): String =
        entries.joinToString("\n") { "[${clean(it.chatName)}] ${it.notes.trim()}" }

    fun formatRecall(lines: List<RecallLine>): String =
        lines.joinToString("\n") { "[${clean(it.label)}] ${it.excerpt.trim()}" }

    private fun clean(name: String) = name.replace('[', '(').replace(']', ')').replace('\n', ' ').trim()

    fun build(block: Block): String = buildString {
        append(OPEN).append('\n')
        append(MEMORY).append('\n')
        if (block.memory.isNotBlank()) append(block.memory.trim()).append('\n')
        append(SYSTEM_PROMPT).append('\n')
        if (block.systemPrompt.isNotBlank()) append(block.systemPrompt.trim()).append('\n')
        append(USER_INSTRUCTIONS).append('\n')
        if (block.userInstructions.isNotBlank()) append(block.userInstructions.trim()).append('\n')
        if (block.recall != null) {
            append(RECALL).append('\n')
            if (block.recall.isNotBlank()) append(block.recall.trim()).append('\n')
        }
        append(CLOSE)
    }

    /** a block with only a recall section, used for later messages of a chat */
    fun buildRecallOnly(recall: String): String = buildString {
        append(OPEN).append('\n')
        append(RECALL).append('\n')
        append(recall.trim()).append('\n')
        append(CLOSE)
    }

    /** the text actually sent to the model for a message */
    fun compose(prefix: String?, message: String): String =
        if (prefix.isNullOrBlank()) message else prefix.trimEnd() + "\n\n" + message

    /**
     * Adds (or replaces) a recall section inside an existing block, or prepends a recall-only
     * block when the text has none.
     */
    fun withRecall(text: String, recall: String): String {
        if (recall.isBlank()) return text
        val range = findBlock(text) ?: return buildRecallOnly(recall) + "\n\n" + text
        val parsed = parse(text.substring(range)) ?: return text
        val rebuilt = build(parsed.copy(recall = recall))
        return text.replaceRange(range, rebuilt)
    }

    /** position of the first complete block in [text], or null */
    fun findBlock(text: String): IntRange? {
        val start = text.indexOf(OPEN, ignoreCase = true)
        if (start < 0) return null
        val end = text.indexOf(CLOSE, start + OPEN.length, ignoreCase = true)
        if (end < 0) return null
        return start until end + CLOSE.length
    }

    /** parses a block. section headers are matched case insensitively, unknown text is kept. */
    fun parse(text: String): Block? {
        val range = findBlock(text) ?: return null
        val inner = text.substring(range).let {
            it.substring(OPEN.length, it.length - CLOSE.length)
        }
        val sections = mutableMapOf<String, StringBuilder>()
        var current: String? = null
        for (line in inner.lines()) {
            val header = sectionNames.firstOrNull { line.trim().equals(it, ignoreCase = true) }
            if (header != null) {
                current = header
                sections.getOrPut(header) { StringBuilder() }
                continue
            }
            val cur = current ?: continue
            val sb = sections.getValue(cur)
            if (sb.isNotEmpty()) sb.append('\n')
            sb.append(line)
        }
        return Block(
            memory = sections[MEMORY]?.toString()?.trim().orEmpty(),
            systemPrompt = sections[SYSTEM_PROMPT]?.toString()?.trim().orEmpty(),
            userInstructions = sections[USER_INSTRUCTIONS]?.toString()?.trim().orEmpty(),
            recall = sections[RECALL]?.toString()?.trim(),
        )
    }

    /**
     * Removes every krala tags block from [text]. Used on model output (in case it echoes the
     * block) and anywhere text is shown to the user. A dangling opening tag with no end (a
     * streaming echo) hides everything after it.
     */
    fun strip(text: String): String {
        var out = text
        while (true) {
            val range = findBlock(out) ?: break
            out = out.removeRange(range)
        }
        val dangling = out.indexOf(OPEN, ignoreCase = true)
        if (dangling >= 0) out = out.substring(0, dangling)
        // stray section markers on their own lines
        out = out.lines().filterNot { line ->
            val t = line.trim()
            t.equals(CLOSE, true) || sectionNames.any { t.equals(it, true) }
        }.joinToString("\n")
        return out.trim()
    }

    /** replaces the system prompt + instructions of the block in [prefix], keeping memory */
    fun updatePrompts(prefix: String, systemPrompt: String, instructions: String): String {
        val parsed = parse(prefix) ?: return prefix
        return build(parsed.copy(systemPrompt = systemPrompt, userInstructions = instructions, recall = null))
    }
}
