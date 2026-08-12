package com.orbitalsurf.core.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckpointScheduleTest {
    @Test
    fun `checkpoint distances are exact multiples of the interval`() {
        assertEquals(500.0, CheckpointSchedule.checkpointDistance(1), 0.0)
        assertEquals(1000.0, CheckpointSchedule.checkpointDistance(2), 0.0)
        assertEquals(5000.0, CheckpointSchedule.checkpointDistance(10), 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `checkpoint number below 1 is rejected`() {
        CheckpointSchedule.checkpointDistance(0)
    }

    @Test
    fun `checkpointAt finds the chunk containing checkpoint 1's distance`() {
        val checkpoint1Distance = CheckpointSchedule.checkpointDistance(1)
        val chunkIndex = (checkpoint1Distance / WorldConstants.CHUNK_LENGTH).toLong()
        assertEquals(1, CheckpointSchedule.checkpointAt(chunkIndex))
    }

    @Test
    fun `checkpointAt returns null for chunks with no checkpoint`() {
        // A chunk immediately after checkpoint 1's chunk is very unlikely to also contain checkpoint 2
        // (interval 500 >> chunk length 40), so this is a solid "no checkpoint here" case.
        val checkpoint1Distance = CheckpointSchedule.checkpointDistance(1)
        val chunkIndex = (checkpoint1Distance / WorldConstants.CHUNK_LENGTH).toLong() + 1
        assertNull(CheckpointSchedule.checkpointAt(chunkIndex))
    }

    @Test
    fun `every checkpoint number appears in exactly one chunk, at the right distance`() {
        for (n in 1..20) {
            val distance = CheckpointSchedule.checkpointDistance(n)
            val chunkIndex = (distance / WorldConstants.CHUNK_LENGTH).toLong()
            assertEquals("checkpoint $n should be found in its own distance's chunk", n, CheckpointSchedule.checkpointAt(chunkIndex))

            val start = chunkIndex * WorldConstants.CHUNK_LENGTH
            val end = start + WorldConstants.CHUNK_LENGTH
            assertTrue(distance >= start && distance < end)
        }
    }
}
