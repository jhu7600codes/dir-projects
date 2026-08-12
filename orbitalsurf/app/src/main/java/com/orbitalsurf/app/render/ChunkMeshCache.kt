package com.orbitalsurf.app.render

import com.orbitalsurf.core.world.Chunk
import com.orbitalsurf.core.world.SurfaceSampler

/**
 * Mirrors `WorldStreamer`'s active-chunk window on the GPU side: builds a chunk's structure
 * mesh the first time it becomes active, reuses it every frame after that, and drops it once
 * the chunk leaves the active window (matching `WorldStreamerTest`'s eviction semantics in
 * `:core`). Meshes are plain client-side vertex arrays (see `Mesh`'s kdoc), so "dropping" one
 * is just removing it from these maps -- nothing to explicitly free on the GL side.
 *
 * Pickups get their own tiny mesh each, cached by pickup id rather than baked into the chunk's
 * structure mesh: that way a collected pickup is simply skipped when drawing (`GameRenderer`
 * checks `GameSession.collectedPickupIds`) instead of needing the whole chunk mesh rebuilt.
 */
class ChunkMeshCache {
    private val structureMeshes = mutableMapOf<Long, Mesh>()
    private val pickupMeshes = mutableMapOf<String, Mesh>()
    private val pickupOwnerChunk = mutableMapOf<String, Long>()

    fun sync(activeChunks: List<Chunk>) {
        val activeIndices = activeChunks.map { it.index }.toSet()
        structureMeshes.keys.retainAll(activeIndices)

        val stalePickupIds = pickupOwnerChunk.filterValues { it !in activeIndices }.keys.toList()
        stalePickupIds.forEach {
            pickupOwnerChunk.remove(it)
            pickupMeshes.remove(it)
        }

        for (chunk in activeChunks) {
            structureMeshes.getOrPut(chunk.index) { ProceduralMeshFactory.buildChunkStructure(chunk) }
            for (pickup in chunk.pickups) {
                if (pickup.id !in pickupMeshes) {
                    val groundHeight = SurfaceSampler.sampleHeight(listOf(chunk), pickup.distance, pickup.lateral)?.height ?: 0.0
                    pickupMeshes[pickup.id] = ProceduralMeshFactory.buildPickup(pickup, groundHeight)
                }
                pickupOwnerChunk[pickup.id] = chunk.index
            }
        }
    }

    fun structureMeshFor(chunkIndex: Long): Mesh? = structureMeshes[chunkIndex]

    fun pickupMesh(pickupId: String): Mesh? = pickupMeshes[pickupId]
}
