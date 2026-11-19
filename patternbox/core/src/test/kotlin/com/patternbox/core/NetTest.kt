package com.patternbox.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetTest {
    private fun waitFor(ms: Long = 3000, cond: () -> Boolean) {
        val end = System.currentTimeMillis() + ms
        while (!cond()) {
            if (System.currentTimeMillis() > end) throw AssertionError("timed out")
            Thread.sleep(10)
        }
    }

    @Test fun snapshotRoundTrip() {
        val m = Match(Mode.FFA, Presets.all.take(4).map { Entry(it.first, it.second) }, GameMap.CRATES)
        m.start()
        repeat(60 * 8) { m.step() }
        val a = m.arena
        val bytes = Snapshot.write(a, listOf(Event.Ko, Event.Hit(5, true)), "hello")
        val mirror = Arena()
        val (events, extra) = Snapshot.apply(mirror, bytes)
        assertEquals("hello", extra)
        assertEquals(2, events.size)
        assertEquals(a.fighters.size, mirror.fighters.size)
        for ((x, y) in a.fighters.zip(mirror.fighters)) {
            assertEquals(x.x, y.x, 0f); assertEquals(x.hp, y.hp, 0f); assertEquals(x.name, y.name)
            assertEquals(x.loadout, y.loadout); assertEquals(x.dead, y.dead)
        }
        assertEquals(a.props.size, mirror.props.size)
        assertEquals(a.width, mirror.width, 0f)
        // the mirror can render: weapon points work off the copied swing state
        for (f in mirror.fighters) if (!f.dead) mirror.weaponWorld(f)
    }

    @Test fun hostAndClientOverLocalhost() {
        val host = NetHost(port = 0)
        host.start()
        val client = NetClient()
        val me = Presets.all[2]
        client.connect("127.0.0.1", "friend", me.second.encode(), port = host.localPort)
        assertTrue(client.connected)
        waitFor { host.readyPeers.size == 1 }
        val peer = host.readyPeers[0]
        assertEquals("friend", peer.name)

        host.sendLobby("host", Loadout.DEFAULT_P1.encode(), Mode.VERSUS, GameMap.FLAT)
        waitFor { client.lobby != null }
        assertEquals(2, client.lobby!!.players.size)

        val entries = listOf(Entry("host", Loadout.DEFAULT_P1), Entry(peer.name, peer.loadout!!))
        host.sendStart(Mode.VERSUS, GameMap.FLAT, entries) { 1 }
        waitFor { client.start != null }
        assertEquals(1, client.start!!.yourFighter)

        val m = Match(Mode.VERSUS, entries, GameMap.FLAT)
        m.start()
        val mine = m.arena.fighters[1]
        val x0 = mine.x
        // friend holds left and swings once
        client.sendInput(left = true, right = false, jump = false)
        client.swing(); client.sendInput(left = true, right = false, jump = false)
        waitFor { peer.left && peer.swings == 1 }
        var swung = false
        repeat(30) {
            applyPeerInput(m.arena, mine, peer)
            if (mine.swingT >= 0) swung = true
            m.step()
            host.sendSnapshot(Snapshot.write(m.arena, emptyList(), ""))
        }
        assertTrue("moved left", mine.x < x0)
        assertTrue("swing went through", swung)
        waitFor { client.latestSnap.get() != null }
        val mirror = Arena()
        Snapshot.apply(mirror, client.latestSnap.get()!!)
        assertEquals(2, mirror.fighters.size)

        client.close()
        waitFor { host.readyPeers.isEmpty() }
        host.close()
    }

    @Test fun newStuffDoesntCrash() {
        for (map in listOf(GameMap.LAVA, GameMap.WINDY, GameMap.MOON)) {
            for (mode in listOf(Mode.KOTH, Mode.BOSS)) {
                val entries = List(4) { i ->
                    val l = Loadout.DEFAULT_P1.copy(
                        weaponType = listOf(WeaponType.LASER, WeaponType.SAW, WeaponType.BOW, WeaponType.BOMB)[i],
                        weaponMat = listOf(WeaponMat.POISON, WeaponMat.VAMPIRE, WeaponMat.FIRE, WeaponMat.STEEL)[i],
                        armourMat = listOf(ArmourMat.MIRROR, ArmourMat.BALLOON, ArmourMat.MIRROR, ArmourMat.STEEL)[i],
                        ability = listOf(Ability.SUMMON, Ability.MAGNET, Ability.INVISIBLE, Ability.SUMMON)[i],
                    )
                    Entry("f$i", l)
                }
                val m = Match(mode, entries, map)
                m.start()
                var t = 0
                while (!m.over && t < 60 * 200) { m.step(); t++ }
                assertTrue("$mode on $map didn't finish", m.over)
                assertNotNull(m.arena.winnerTeam)
            }
        }
    }
}
