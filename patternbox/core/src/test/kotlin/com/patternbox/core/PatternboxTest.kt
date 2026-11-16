package com.patternbox.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternboxTest {
    @Test fun skippingOverMiddleDotGrabsIt() {
        assertEquals(listOf(0, 4, 8), patternOf(0, 8))
        assertEquals(listOf(0, 1, 2), patternOf(0, 2))
        // already used middle dot is not added again
        assertEquals(listOf(4, 0, 8), patternOf(4, 0, 8))
        // knight move has no middle dot
        assertEquals(listOf(0, 5), patternOf(0, 5))
    }

    @Test fun codeRoundTrips() {
        val l = Loadout.DEFAULT_P2
        assertEquals(l, Loadout.decode(l.encode()))
        assertEquals("14789-159-456", Loadout.DEFAULT_P1.encode())
        assertNull(Loadout.decode("12-34-56"))   // body too short
        assertNull(Loadout.decode("garbage"))
        assertNotNull(Loadout.decode("1478-19-46"))
    }

    @Test fun randomLoadoutsAreValid() {
        repeat(500) {
            val l = Loadout.random()
            for (p in Part.values()) assertTrue(l[p].size >= p.minDots)
            assertTrue(l.stats.hp > 0)
        }
    }

    @Test fun botsFinishAFight() {
        val arena = Arena()
        arena.start(Loadout.DEFAULT_P1, "a", Loadout.DEFAULT_P2, "b")
        var ticks = 0
        while (arena.winner == null && ticks < 60 * 180) { arena.step(); ticks++ }
        assertNotNull("bots should ko someone within 3 minutes", arena.winner)
    }

    @Test fun throwingIntoWallHurts() {
        val arena = Arena()
        arena.start(Loadout.DEFAULT_P1, "a", Loadout.DEFAULT_P2, "b")
        arena.bot[0] = false; arena.bot[1] = false
        val f = arena.fighters[0]
        val hp = f.hp
        arena.pointerDown(Tool.GRAB, f.x, f.y)
        arena.step()
        arena.pointerMove(f.x - 40, f.y)
        arena.pointerUp()
        repeat(60) { arena.step() }
        assertTrue(f.hp < hp)
    }
}
