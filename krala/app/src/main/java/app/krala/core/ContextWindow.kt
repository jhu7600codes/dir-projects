package app.krala.core

/**
 * Fits a chat history into a model's context. The newest messages win, but the very first user
 * message (which carries the krala tags block) is always kept.
 */
object ContextWindow {
    data class Item(val tokens: Int)

    /** room left for the reply */
    fun replyReserve(contextSize: Int): Int = (contextSize * 0.15).toInt().coerceIn(256, 8192)

    /**
     * Returns the indices to keep, in order. [items] is the full history, oldest first, where the
     * last item is the message being answered.
     */
    fun fit(items: List<Item>, contextSize: Int, pinnedFirst: Boolean = true): List<Int> {
        if (items.isEmpty()) return emptyList()
        val budget = (contextSize - replyReserve(contextSize)).coerceAtLeast(512)
        val keep = ArrayDeque<Int>()
        var used = 0
        val first = if (pinnedFirst) 0 else -1
        if (first == 0) used += items[0].tokens
        // always keep the newest message, even if it alone is over budget
        for (i in items.indices.reversed()) {
            if (i == first) continue
            val t = items[i].tokens
            if (keep.isNotEmpty() && used + t > budget) break
            keep.addFirst(i)
            used += t
        }
        if (first == 0) keep.addFirst(0)
        return keep.toList()
    }
}
