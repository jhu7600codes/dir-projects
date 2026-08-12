package com.orbitalsurf.core.world

import kotlin.math.floor

/**
 * Owns the bounded window of chunks that are actually generated and held in memory: it
 * generates ahead of the ball as it advances, and evicts chunks that have fully scrolled
 * behind, so an infinite run never accumulates unbounded state. Because `ChunkGenerator` is a
 * pure function of `(seed, index)`, [reset] can jump the window straight to an arbitrary
 * starting distance (a Headstart) without ever generating anything before it.
 */
class WorldStreamer(
    private val seed: Long,
    private val generateAheadDistance: Double = 300.0,
    private val evictBehindDistance: Double = 80.0,
) {
    private val chunksByIndex = LinkedHashMap<Long, Chunk>()
    private var nextIndexToGenerate: Long? = null

    val activeChunks: List<Chunk>
        get() = chunksByIndex.values.sortedBy { it.index }

    /** (Re)starts the stream so its window begins at [startDistance] -- the Headstart entry point. */
    fun reset(startDistance: Double) {
        chunksByIndex.clear()
        nextIndexToGenerate = floor(startDistance / WorldConstants.CHUNK_LENGTH).toLong()
        update(startDistance)
    }

    /** Call every tick (or whenever ball distance changes) to keep the window centered on the ball. */
    fun update(ballDistance: Double) {
        if (nextIndexToGenerate == null) {
            nextIndexToGenerate = floor(ballDistance / WorldConstants.CHUNK_LENGTH).toLong()
        }
        val furthestNeededIndex = floor((ballDistance + generateAheadDistance) / WorldConstants.CHUNK_LENGTH).toLong()
        var idx = nextIndexToGenerate!!
        while (idx <= furthestNeededIndex) {
            chunksByIndex[idx] = ChunkGenerator.generate(seed, idx)
            idx++
        }
        nextIndexToGenerate = idx

        val evictBefore = ballDistance - evictBehindDistance
        val toEvict = chunksByIndex.values.filter { it.endDistance < evictBefore }.map { it.index }
        toEvict.forEach { chunksByIndex.remove(it) }
    }
}
