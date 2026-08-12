package com.orbitalsurf.core.economy

import com.orbitalsurf.core.world.CheckpointSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckpointUnlocksTest {
    @Test
    fun `a fresh unlocks set has nothing unlocked`() {
        assertFalse(CheckpointUnlocks().isUnlocked(1))
    }

    @Test
    fun `marking a checkpoint reached unlocks it`() {
        val unlocks = CheckpointUnlocks().markReached(3)
        assertTrue(unlocks.isUnlocked(3))
        assertFalse(unlocks.isUnlocked(4))
    }

    @Test
    fun `marking the same checkpoint reached twice is idempotent`() {
        val once = CheckpointUnlocks().markReached(3)
        val twice = once.markReached(3)
        assertEquals(once, twice)
    }

    @Test
    fun `headstartStartDistance is null for a locked checkpoint`() {
        assertNull(CheckpointUnlocks().headstartStartDistance(2))
    }

    @Test
    fun `headstartStartDistance matches CheckpointSchedule for an unlocked checkpoint`() {
        val unlocks = CheckpointUnlocks().markReached(2)
        assertEquals(CheckpointSchedule.checkpointDistance(2), unlocks.headstartStartDistance(2)!!, 0.0)
    }
}
