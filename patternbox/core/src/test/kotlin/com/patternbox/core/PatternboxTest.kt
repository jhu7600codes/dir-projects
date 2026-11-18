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
        assertEquals(listOf(4, 0, 8), patternOf(4, 0, 8))
        assertEquals(listOf(0, 5), patternOf(0, 5))
    }

    @Test fun codeRoundTrips() {
        repeat(300) {
            val l = Loadout.random()
            assertEquals(l, Loadout.decode(l.encode()))
        }
        for ((_, l) in Presets.all) assertEquals(l, Loadout.decode(l.encode()))
        assertEquals("14789-159-456-123-0000", Loadout.DEFAULT_P1.encode())
    }

    @Test fun oldCodesStillLoad() {
        val l = Loadout.decode("14789-159-456")!!
        assertEquals(WeaponType.BLADE, l.weaponType)
        assertTrue(l.hat.isEmpty())
        assertNull(Loadout.decode("12-34-56"))
        assertNull(Loadout.decode("garbage"))
        assertNull(Loadout.decode("1478-19-46--9999"))
        assertNotNull(Loadout.decode("1478-19-46--1234"))
    }

    @Test fun randomLoadoutsAreValid() {
        repeat(500) {
            val l = Loadout.random()
            for (p in Part.values()) assertTrue(l[p].size >= p.minDots)
            val s = l.stats
            assertTrue(s.hp > 0 && s.speed > 0 && s.dmg > 0 && s.def in 0f..0.8f)
        }
    }

    @Test fun everyWeaponTypeCanWin() {
        // each type vs a plain blade, many maps; all should finish without crashing and win sometimes
        for (wt in WeaponType.values()) {
            var wins = 0
            for (i in 0 until 12) {
                val a = Entry("a", Loadout.DEFAULT_P1.copy(weaponType = wt, weaponMat = WeaponMat.values()[i % 6], ability = Ability.values()[i % 8]))
                val b = Entry("b", Loadout.DEFAULT_P1)
                if (Match.simulate(a, b, GameMap.values()[i % GameMap.values().size]) == 0) wins++
            }
            assertTrue("$wt never won", wins > 0)
        }
    }

    @Test fun teamsAndFfaFinish() {
        for (mode in listOf(Mode.TEAMS, Mode.FFA)) {
            val entries = List(4) { Loadout.random().let { l -> Entry("f$it", l) } }
            val m = Match(mode, entries, GameMap.PLATFORMS)
            m.start()
            var t = 0
            while (!m.over && t < 60 * 240) { m.step(); t++ }
            assertTrue("$mode didn't finish", m.over)
            assertTrue(m.winners().isNotEmpty() || m.arena.winnerTeam == -1)
        }
    }

    @Test fun survivalMakesWaves() {
        val m = Match(Mode.SURVIVAL, listOf(Entry("me", Presets.all[1].second)), GameMap.FLAT)
        m.start()
        var t = 0
        while (!m.over && t < 60 * 300) { m.step(); t++ }
        assertTrue(m.wave >= 1)
    }

    @Test fun sandboxNeverEndsAndToolsWork() {
        val m = Match(Mode.SANDBOX, listOf(Entry("a", Loadout.DEFAULT_P1), Entry("b", Loadout.DEFAULT_P2)), GameMap.CRATES)
        m.start()
        val a = m.arena
        for (tool in Tool.values()) a.pointerDown(tool, a.width / 2, a.floorY - 100).also { a.pointerUp() }
        repeat(60 * 60) { m.step() }
        assertNull(a.winnerTeam)
    }

    @Test fun tournamentCrownsAChampion() {
        val t = Tournament(listOf(Entry("me", Loadout.DEFAULT_P1), Entry("you", Loadout.DEFAULT_P2)), GameMap.FLAT)
        assertEquals(8, t.players.size)
        var n = 0
        while (t.simulateNext()) n++
        assertEquals(7, n)
        assertNotNull(t.champion)
    }

    @Test fun playersTurnTheWayTheyWalk() {
        val arena = Arena()
        arena.start(Loadout.DEFAULT_P1, "a", Loadout.DEFAULT_P2, "b")
        val f = arena.fighters[0]   // starts on the left facing right, enemy to the right
        f.bot = false
        arena.fighters[1].bot = false
        f.input.left = true
        repeat(10) { arena.step() }
        assertEquals(-1f, f.face)
        f.input.left = false
        repeat(10) { arena.step() }
        assertEquals("keeps facing left after stopping", -1f, f.face)
        f.input.right = true
        repeat(10) { arena.step() }
        assertEquals(1f, f.face)
    }

    @Test fun throwingIntoWallHurts() {
        val arena = Arena()
        arena.start(Loadout.DEFAULT_P1, "a", Loadout.DEFAULT_P2, "b")
        arena.fighters.forEach { it.bot = false }
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
