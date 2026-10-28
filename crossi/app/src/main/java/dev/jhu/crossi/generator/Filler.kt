package dev.jhu.crossi.generator

import java.util.BitSet

/**
 * Backtracking crossword filler.
 *
 * Each step picks the most constrained open slot, tries its best few candidates
 * (common words first, then a one-step lookahead on the crossing slots), and
 * forward-checks that no crossing slot is left with zero options. Dead ends
 * backjump to the slot that actually caused them.
 *
 * The search is capped by [nodeBudget] (a node count, never wall-clock time) so the
 * same seed gives the same puzzle on a slow phone and a fast one.
 */
internal class Filler(
    private val index: WordIndex,
    private val shape: Shape,
    private val rng: Rng,
    private val nodeBudget: Int,
    private val maxBranch: Int,
) {
    private val n = shape.size
    private val slots = shape.slots
    private val buckets = slots.map { index.bucket(it.length) }
    private val letters = IntArray(n * n) { EMPTY }
    private val assigned = BooleanArray(slots.size)
    private val crossings: Array<IntArray>
    private val used = HashMap<Int, LongArray>()
    private val scratch: Array<LongArray>
    private var nodes = 0

    val nodesUsed: Int get() = nodes

    init {
        val acrossOf = IntArray(n * n) { -1 }
        val downOf = IntArray(n * n) { -1 }
        for (slot in slots) {
            val owner = if (slot.direction == Direction.ACROSS) acrossOf else downOf
            for (cell in slot.cells) owner[cell] = slot.id
        }
        crossings = Array(slots.size) { id ->
            val slot = slots[id]
            val other = if (slot.direction == Direction.ACROSS) downOf else acrossOf
            slot.cells.map { other[it] }.filter { it >= 0 }.toIntArray()
        }
        scratch = Array(slots.size) { LongArray(buckets[it]?.longs ?: 0) }
        for (b in buckets) if (b != null) used.getOrPut(b.length) { LongArray(b.longs) }
    }

    /** Returns the filled grid rows ('#' = block) or null if no fill was found within budget. */
    fun fill(seeds: List<SeedPlacement> = emptyList()): List<String>? {
        for (seed in seeds) {
            val slot = slots.firstOrNull {
                it.direction == Direction.ACROSS && it.row == seed.row && it.col == seed.col &&
                    it.length == seed.word.length
            } ?: return null
            for (i in seed.word.indices) letters[slot.cells[i]] = index.alphabet.code(seed.word[i])
            assigned[slot.id] = true
            // a long word can also be a normal dictionary word, don't let the fill use it twice
            buckets[slot.id]?.indexOf(seed.word)?.takeIf { it >= 0 }?.let { Bits.set(used.getValue(slot.length), it) }
        }
        // A slot without any word of its length can never be filled.
        if (slots.indices.any { !assigned[it] && buckets[it] == null }) return null
        if (solve() !== SUCCESS) return null
        return List(n) { r ->
            buildString {
                for (c in 0 until n) {
                    val cell = r * n + c
                    append(if (shape.blocks[cell]) BLOCK else index.alphabet.letter(letters[cell]))
                }
            }
        }
    }

    /**
     * Conflict-directed backjumping: a failed subtree reports which assigned slots
     * caused it. If the current slot isn't one of them, trying its other words is
     * pointless, so we jump straight back to a slot that is.
     */
    private fun solve(): BitSet {
        if (++nodes > nodeBudget) return ABORT
        var best = -1
        var bestCount = Int.MAX_VALUE
        for (s in slots.indices) {
            if (assigned[s]) continue
            val count = candidates(s, scratch[s])
            if (count == 0) return constrainers(s)
            if (count < bestCount || (count == bestCount && slots[s].length > slots[best].length)) {
                best = s
                bestCount = count
            }
        }
        if (best < 0) return SUCCESS

        val conflict = BitSet()
        for (word in ordered(best, scratch[best].copyOf(), bestCount, conflict)) {
            val changed = assign(best, word)
            val dead = firstDeadCrossing(best)
            if (dead >= 0) {
                conflict.or(constrainers(dead))
                unassign(best, word, changed)
                continue
            }
            val result = solve()
            if (result === SUCCESS) return SUCCESS
            unassign(best, word, changed)
            if (result === ABORT) return ABORT
            if (!result.get(best)) return result
            conflict.or(result)
        }
        conflict.or(constrainers(best))
        conflict.clear(best)
        return conflict
    }

    /** Assigned slots that put letters into [slotId]'s cells. */
    private fun constrainers(slotId: Int): BitSet {
        val out = BitSet()
        for (c in crossings[slotId]) if (assigned[c]) out.set(c)
        return out
    }

    private fun candidates(slotId: Int, out: LongArray): Int {
        val slot = slots[slotId]
        val bucket = buckets[slotId] ?: return 0
        bucket.all.copyInto(out)
        for (i in 0 until slot.length) {
            val code = letters[slot.cells[i]]
            if (code != EMPTY) {
                val bits = bucket.letterBits(i, code)
                for (w in out.indices) out[w] = out[w] and bits[w]
            }
        }
        val usedBits = used.getValue(slot.length)
        for (w in out.indices) out[w] = out[w] and usedBits[w].inv()
        return Bits.count(out)
    }

    /** Candidate words for [slotId], best first, capped at [maxBranch]. */
    private fun ordered(slotId: Int, bits: LongArray, count: Int, conflict: BitSet): List<Int> {
        val bucket = buckets[slotId]!!
        val keyed = ArrayList<Long>(count)
        Bits.forEach(bits) { w ->
            val key = bucket.scores[w].toLong() * JITTER + rng.nextInt(JITTER)
            keyed += (key shl 32) or w.toLong()
        }
        keyed.sortDescending()
        val shortlist = keyed.take(maxBranch * 3).map { (it and 0xFFFFFFFFL).toInt() }
        if (shortlist.size <= 1) return shortlist

        // One-step lookahead: prefer words that leave the crossing slots with the most options.
        val scored = shortlist.mapNotNull { word ->
            val changed = assign(slotId, word)
            var room = 0.0
            var dead = false
            for (c in crossings[slotId]) {
                if (assigned[c]) continue
                val options = candidates(c, scratch[c])
                if (options == 0) {
                    conflict.or(constrainers(c))
                    dead = true
                    break
                }
                room += kotlin.math.ln(options.toDouble())
            }
            unassign(slotId, word, changed)
            if (dead) null else word to room
        }
        return scored.sortedByDescending { it.second }.take(maxBranch).map { it.first }
    }

    private fun firstDeadCrossing(slotId: Int): Int =
        crossings[slotId].firstOrNull { !assigned[it] && candidates(it, scratch[it]) == 0 } ?: -1

    private fun assign(slotId: Int, word: Int): IntArray {
        val slot = slots[slotId]
        val codes = buckets[slotId]!!.codes[word]
        val changed = IntArray(slot.length)
        var k = 0
        for (i in 0 until slot.length) {
            val cell = slot.cells[i]
            if (letters[cell] == EMPTY) {
                letters[cell] = codes[i].toInt()
                changed[k++] = cell
            }
        }
        assigned[slotId] = true
        Bits.set(used.getValue(slot.length), word)
        return changed.copyOf(k)
    }

    private fun unassign(slotId: Int, word: Int, changed: IntArray) {
        for (cell in changed) letters[cell] = EMPTY
        assigned[slotId] = false
        Bits.clear(used.getValue(slots[slotId].length), word)
    }

    companion object {
        const val BLOCK = '#'
        private const val EMPTY = -1
        private const val JITTER = 1000

        // Identity sentinels for solve()'s result.
        private val SUCCESS = BitSet()
        private val ABORT = BitSet()
    }
}
