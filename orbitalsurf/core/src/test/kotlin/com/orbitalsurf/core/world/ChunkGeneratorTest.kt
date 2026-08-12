package com.orbitalsurf.core.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChunkGeneratorTest {
    private val seed = 1234L

    @Test
    fun `generating the same chunk twice is byte-for-byte identical`() {
        val a = ChunkGenerator.generate(seed, 42L)
        val b = ChunkGenerator.generate(seed, 42L)
        assertEquals(a, b)
    }

    @Test
    fun `different chunk indices produce different content`() {
        val a = ChunkGenerator.generate(seed, 0L)
        val b = ChunkGenerator.generate(seed, 1L)
        assertNotEquals(a.segments, b.segments)
    }

    @Test
    fun `different seeds produce different content for the same index`() {
        val a = ChunkGenerator.generate(1L, 10L)
        val b = ChunkGenerator.generate(2L, 10L)
        assertNotEquals(a, b)
    }

    @Test
    fun `a chunk's segments exactly and contiguously cover its distance span`() {
        for (index in 0L until 50L) {
            val chunk = ChunkGenerator.generate(seed, index)
            val sorted = chunk.segments.sortedBy { it.startDistance }
            assertTrue("chunk $index has no segments", sorted.isNotEmpty())
            assertEquals("chunk $index first segment doesn't start at chunk start", chunk.startDistance, sorted.first().startDistance, 1e-9)
            assertEquals("chunk $index last segment doesn't end at chunk end", chunk.endDistance, sorted.last().endDistance, 1e-9)
            for (i in 1 until sorted.size) {
                assertEquals(
                    "chunk $index has a coverage gap/overlap between segments ${i - 1} and $i",
                    sorted[i - 1].endDistance,
                    sorted[i].startDistance,
                    1e-9,
                )
            }
        }
    }

    @Test
    fun `rooftop height is continuous across a chunk boundary`() {
        val epsilon = 1e-6
        for (index in 0L until 30L) {
            val chunkA = ChunkGenerator.generate(seed, index)
            val chunkB = ChunkGenerator.generate(seed, index + 1)

            val lastOfA = chunkA.segments.maxByOrNull { it.endDistance }!!
            val firstOfB = chunkB.segments.minByOrNull { it.startDistance }!!

            val heightJustBeforeSeam = lastOfA.heightAt(chunkA.endDistance - epsilon, (lastOfA.lateralMin + lastOfA.lateralMax) / 2.0)
            val heightJustAfterSeam = firstOfB.heightAt(chunkB.startDistance + epsilon, (firstOfB.lateralMin + firstOfB.lateralMax) / 2.0)

            assertNotNull("chunk $index has no surface right before the seam", heightJustBeforeSeam)
            assertNotNull("chunk ${index + 1} has no surface right after the seam", heightJustAfterSeam)
            assertEquals(
                "height discontinuity at the boundary between chunk $index and ${index + 1}",
                heightJustBeforeSeam!!,
                heightJustAfterSeam!!,
                0.01,
            )
        }
    }

    @Test
    fun `a chunk containing a checkpoint distance is flagged and has a CheckpointInterior segment`() {
        val checkpoint1Distance = CheckpointSchedule.checkpointDistance(1)
        val chunkIndex = (checkpoint1Distance / WorldConstants.CHUNK_LENGTH).toLong()
        val chunk = ChunkGenerator.generate(seed, chunkIndex)

        assertEquals(1, chunk.checkpointIndex)
        val interior = chunk.segments.filterIsInstance<SurfaceSegment.CheckpointInterior>()
        assertTrue("expected exactly one CheckpointInterior segment", interior.size == 1)
        assertEquals(1, interior.first().checkpointIndex)
    }

    @Test
    fun `a checkpoint chunk never contains a Gap`() {
        val checkpoint1Distance = CheckpointSchedule.checkpointDistance(1)
        val chunkIndex = (checkpoint1Distance / WorldConstants.CHUNK_LENGTH).toLong()
        val chunk = ChunkGenerator.generate(seed, chunkIndex)
        assertTrue(chunk.segments.none { it is SurfaceSegment.Gap })
    }

    @Test
    fun `a chunk with no checkpoint has a null checkpointIndex`() {
        val checkpoint1Distance = CheckpointSchedule.checkpointDistance(1)
        val chunkIndex = (checkpoint1Distance / WorldConstants.CHUNK_LENGTH).toLong() + 1
        val chunk = ChunkGenerator.generate(seed, chunkIndex)
        assertNull(chunk.checkpointIndex)
    }

    @Test
    fun `obstacles and pickups stay within their chunk's distance span`() {
        for (index in 0L until 100L) {
            val chunk = ChunkGenerator.generate(seed, index)
            chunk.obstacles.forEach {
                assertTrue("obstacle ${it.id} outside chunk span", it.distance >= chunk.startDistance && it.distance < chunk.endDistance)
            }
            chunk.pickups.forEach {
                assertTrue("pickup ${it.id} outside chunk span", it.distance >= chunk.startDistance && it.distance < chunk.endDistance)
            }
        }
    }

    @Test
    fun `far from the start, chunks tend to have more obstacles than near the start`() {
        val nearCounts = (0L until 20L).sumOf { ChunkGenerator.generate(seed, it).obstacles.size }
        val farCounts = (5000L until 5020L).sumOf { ChunkGenerator.generate(seed, it).obstacles.size }
        assertTrue("expected far chunks ($farCounts) to have at least as many obstacles as near chunks ($nearCounts)", farCounts >= nearCounts)
    }
}
