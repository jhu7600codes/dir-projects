package com.orbitalsurf.core.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldStreamerTest {
    @Test
    fun `starting from distance zero generates a window ahead of the ball`() {
        val streamer = WorldStreamer(seed = 1L, generateAheadDistance = 200.0, evictBehindDistance = 80.0)
        streamer.update(0.0)

        val indices = streamer.activeChunks.map { it.index }
        assertTrue("expected chunk 0 to be generated", indices.contains(0L))
        val expectedFurthest = (200.0 / WorldConstants.CHUNK_LENGTH).toInt()
        assertTrue("expected chunks generated out to ~$expectedFurthest, got ${indices.max()}", indices.max() >= expectedFurthest - 1)
    }

    @Test
    fun `the window advances as the ball moves forward`() {
        val streamer = WorldStreamer(seed = 1L, generateAheadDistance = 200.0, evictBehindDistance = 1_000_000.0)
        streamer.update(0.0)
        val initialMax = streamer.activeChunks.maxOf { it.index }

        streamer.update(1000.0)
        val laterMax = streamer.activeChunks.maxOf { it.index }

        assertTrue("expected the generated window to advance", laterMax > initialMax)
    }

    @Test
    fun `chunks that scroll far behind the ball are evicted`() {
        val streamer = WorldStreamer(seed = 1L, generateAheadDistance = 100.0, evictBehindDistance = 80.0)
        streamer.update(0.0)
        assertTrue(streamer.activeChunks.any { it.index == 0L })

        streamer.update(5000.0)
        assertTrue("chunk 0 should have been evicted by now", streamer.activeChunks.none { it.index == 0L })
    }

    @Test
    fun `the active window stays bounded in size as the ball travels indefinitely`() {
        val streamer = WorldStreamer(seed = 1L, generateAheadDistance = 200.0, evictBehindDistance = 80.0)
        var maxWindowSize = 0
        var distance = 0.0
        repeat(500) {
            streamer.update(distance)
            maxWindowSize = maxOf(maxWindowSize, streamer.activeChunks.size)
            distance += 15.0
        }
        // Window covers roughly (generateAhead + evictBehind) meters of chunks -- give a
        // generous multiple of that as the bound so this is a real regression guard, not a
        // brittle exact count.
        val roughExpected = ((200.0 + 80.0) / WorldConstants.CHUNK_LENGTH).toInt() + 2
        assertTrue("window grew unbounded: max size $maxWindowSize, expected around $roughExpected", maxWindowSize <= roughExpected * 3)
    }

    @Test
    fun `reset jumps the window straight to a headstart distance without generating everything before it`() {
        val streamer = WorldStreamer(seed = 1L, generateAheadDistance = 200.0, evictBehindDistance = 80.0)
        val headstartDistance = 50_000.0

        streamer.reset(headstartDistance)

        val indices = streamer.activeChunks.map { it.index }
        val expectedStartIndex = (headstartDistance / WorldConstants.CHUNK_LENGTH).toLong()
        assertEquals(expectedStartIndex, indices.min())
        assertTrue("reset should not have generated chunk 0", indices.none { it == 0L })
    }

    @Test
    fun `chunk content after a reset is identical to generating that chunk directly`() {
        val seed = 7L
        val streamer = WorldStreamer(seed = seed, generateAheadDistance = 200.0, evictBehindDistance = 80.0)
        val headstartDistance = 20_000.0
        streamer.reset(headstartDistance)

        val expectedIndex = (headstartDistance / WorldConstants.CHUNK_LENGTH).toLong()
        val fromStreamer = streamer.activeChunks.first { it.index == expectedIndex }
        val direct = ChunkGenerator.generate(seed, expectedIndex)
        assertEquals(direct, fromStreamer)
    }
}
