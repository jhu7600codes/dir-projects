package app.krala.core

/**
 * Decides when memory entries have grown too big for the model and which ones get folded into
 * the archive. Nothing is ever deleted: merged entries are only marked as archived.
 */
object MemoryBudget {
    /** entries may use up to this share of the context before condensing starts */
    const val TRIGGER_RATIO = 0.60

    /** after condensing, the remaining live entries should fit in this share */
    const val TARGET_RATIO = 0.40

    /** the condensed archive itself is asked to stay under this share */
    const val ARCHIVE_RATIO = 0.20

    data class Entry(val id: String, val tokens: Int, val updatedAt: Long, val isArchive: Boolean)

    data class Plan(
        /** oldest live entries to merge into the archive, oldest first */
        val toMerge: List<String>,
        /** token budget the new archive should respect */
        val archiveBudget: Int,
    )

    fun total(entries: List<Entry>): Int = entries.sumOf { it.tokens }

    fun triggerLimit(contextSize: Int): Int = (contextSize * TRIGGER_RATIO).toInt()

    fun needsCondense(entries: List<Entry>, contextSize: Int): Boolean =
        contextSize > 0 && total(entries) > triggerLimit(contextSize)

    /**
     * Returns null when the entries fit. Otherwise the oldest live (non archive) entries are
     * picked until what is left, plus the archive budget, drops under the target. At least two
     * entries get merged (or one plus the existing archive), so every run makes progress.
     */
    fun plan(entries: List<Entry>, contextSize: Int): Plan? {
        if (!needsCondense(entries, contextSize)) return null
        val archiveBudget = (contextSize * ARCHIVE_RATIO).toInt().coerceAtLeast(256)
        val target = (contextSize * TARGET_RATIO).toInt()
        val archive = entries.filter { it.isArchive }
        val live = entries.filterNot { it.isArchive }.sortedWith(compareBy<Entry> { it.updatedAt }.thenBy { it.id })
        if (live.isEmpty()) {
            // only the archive is over budget: re-condense it on its own
            return if (archive.isNotEmpty()) Plan(emptyList(), archiveBudget) else null
        }
        val picked = mutableListOf<String>()
        var remaining = total(live)
        for (e in live) {
            if (remaining <= target && picked.size >= minPick(archive.isNotEmpty(), live.size)) break
            picked += e.id
            remaining -= e.tokens
        }
        return Plan(picked, archiveBudget)
    }

    private fun minPick(hasArchive: Boolean, liveCount: Int): Int =
        if (hasArchive) 1 else minOf(2, liveCount)
}
