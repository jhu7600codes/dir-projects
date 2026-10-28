package dev.jhu.crossi.generator

import kotlinx.serialization.Serializable

@Serializable
enum class Direction { ACROSS, DOWN }

/** A maximal run of white cells, i.e. where one word goes. [cells] are row-major indices. */
class Slot(val id: Int, val direction: Direction, val row: Int, val col: Int, val length: Int, size: Int) {
    val cells: IntArray = IntArray(length) { i ->
        if (direction == Direction.ACROSS) row * size + col + i else (row + i) * size + col
    }
}

/** Square block layout. Index = row * size + col. */
class Shape(val size: Int, val blocks: BooleanArray) {
    init {
        require(blocks.size == size * size)
    }

    fun isBlock(row: Int, col: Int): Boolean = blocks[row * size + col]

    val blockCount: Int get() = blocks.count { it }

    /** Every across + down run of length >= 1 (a valid shape has none shorter than 3). */
    val slots: List<Slot> by lazy { computeSlots() }

    private fun computeSlots(): List<Slot> {
        val out = ArrayList<Slot>()
        for (dir in Direction.entries) {
            for (line in 0 until size) {
                var start = -1
                for (i in 0..size) {
                    val white = i < size && !(if (dir == Direction.ACROSS) isBlock(line, i) else isBlock(i, line))
                    if (white && start < 0) start = i
                    if (!white && start >= 0) {
                        val len = i - start
                        out += if (dir == Direction.ACROSS) Slot(out.size, dir, line, start, len, size)
                        else Slot(out.size, dir, start, line, len, size)
                        start = -1
                    }
                }
            }
        }
        return out
    }

    fun isSymmetric(): Boolean = blocks.indices.all { blocks[it] == blocks[blocks.size - 1 - it] }

    fun isConnected(): Boolean {
        val start = blocks.indexOfFirst { !it }
        if (start < 0) return false
        val seen = BooleanArray(blocks.size)
        val stack = ArrayDeque<Int>().apply { add(start) }
        seen[start] = true
        var count = 0
        while (stack.isNotEmpty()) {
            val cell = stack.removeLast()
            count++
            val r = cell / size
            val c = cell % size
            for ((dr, dc) in NEIGHBORS) {
                val nr = r + dr
                val nc = c + dc
                if (nr !in 0 until size || nc !in 0 until size) continue
                val n = nr * size + nc
                if (!blocks[n] && !seen[n]) {
                    seen[n] = true
                    stack.add(n)
                }
            }
        }
        return count == blocks.count { !it }
    }

    companion object {
        private val NEIGHBORS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    }
}
