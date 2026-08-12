package com.orbitalsurf.core.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionSystemTest {
    private fun distanceMission(system: MissionSystem) = system.activeMissions.first { it.goal is MissionGoal.TravelDistance }
    private fun coinMission(system: MissionSystem) =
        system.activeMissions.first { (it.goal as? MissionGoal.CollectPickupCount)?.kind == MissionPickupKind.PLATES_COIN }
    private fun powerupMission(system: MissionSystem) =
        system.activeMissions.first { (it.goal as? MissionGoal.CollectPickupCount)?.kind == MissionPickupKind.ANY_POWERUP }

    @Test
    fun `starts with exactly 3 missions at tier 0 and multiplier 1_0`() {
        val system = MissionSystem(seed = 1L)
        assertEquals(3, system.activeMissions.size)
        assertEquals(0, system.tier)
        assertEquals(1.0, system.multiplier, 1e-9)
    }

    @Test
    fun `distance progress only advances the distance mission`() {
        val system = MissionSystem(seed = 1L)
        val before = coinMission(system).progress
        system.onDistanceTraveled(50.0)
        assertTrue(distanceMission(system).progress > 0.0)
        assertEquals(before, coinMission(system).progress, 1e-9)
    }

    @Test
    fun `collecting a plates coin only advances the coin mission, not the powerup mission`() {
        val system = MissionSystem(seed = 1L)
        val beforePowerup = powerupMission(system).progress
        system.onPickupCollected(MissionPickupKind.PLATES_COIN)
        assertEquals(1.0, coinMission(system).progress, 1e-9)
        assertEquals(beforePowerup, powerupMission(system).progress, 1e-9)
    }

    @Test
    fun `ANY_POWERUP goal accepts any powerup kind but not plates coins`() {
        val system = MissionSystem(seed = 1L)
        system.onPickupCollected(MissionPickupKind.MAGNET)
        assertEquals(1.0, powerupMission(system).progress, 1e-9)
        system.onPickupCollected(MissionPickupKind.SHIELD)
        assertEquals(2.0, powerupMission(system).progress, 1e-9)
    }

    @Test
    fun `completing all 3 missions escalates tier and multiplier and rolls a new set immediately`() {
        val system = MissionSystem(seed = 1L)
        val oldIds = system.activeMissions.map { it.id }.toSet()

        // Complete each mission with exactly the progress it needs -- no more -- so nothing
        // spills over into whatever set rolls in next (that would falsely make it look
        // pre-completed).
        val distanceTarget = (distanceMission(system).goal as MissionGoal.TravelDistance).meters
        val coinTarget = (coinMission(system).goal as MissionGoal.CollectPickupCount).count
        val powerupTarget = (powerupMission(system).goal as MissionGoal.CollectPickupCount).count

        system.onDistanceTraveled(distanceTarget)
        repeat(coinTarget) { system.onPickupCollected(MissionPickupKind.PLATES_COIN) }
        repeat(powerupTarget) { system.onPickupCollected(MissionPickupKind.MAGNET) }

        assertEquals(1, system.tier)
        assertEquals(MissionSystem.MULTIPLIER_STEPS[1], system.multiplier, 1e-9)
        assertEquals(3, system.activeMissions.size)
        assertTrue("expected a fresh set of mission ids", system.activeMissions.none { it.id in oldIds })
        assertTrue("expected the new set to start fresh, not pre-completed", system.activeMissions.none { it.isComplete })
    }

    @Test
    fun `the new mission set after escalation is harder -- its distance target is strictly larger`() {
        val system = MissionSystem(seed = 1L)
        val tier0Target = (distanceMission(system).goal as MissionGoal.TravelDistance).meters

        system.onDistanceTraveled(1_000_000.0)
        repeat(1_000) { system.onPickupCollected(MissionPickupKind.PLATES_COIN) }
        repeat(1_000) { system.onPickupCollected(MissionPickupKind.MAGNET) }

        val tier1Target = (distanceMission(system).goal as MissionGoal.TravelDistance).meters
        assertTrue("expected tier 1's distance target ($tier1Target) to exceed tier 0's ($tier0Target)", tier1Target > tier0Target)
    }

    @Test
    fun `multiplier escalates through the documented steps across repeated completions`() {
        val system = MissionSystem(seed = 1L)
        repeat(4) {
            val distanceTarget = (distanceMission(system).goal as MissionGoal.TravelDistance).meters
            val coinTarget = (coinMission(system).goal as MissionGoal.CollectPickupCount).count
            val powerupTarget = (powerupMission(system).goal as MissionGoal.CollectPickupCount).count

            system.onDistanceTraveled(distanceTarget)
            repeat(coinTarget) { system.onPickupCollected(MissionPickupKind.PLATES_COIN) }
            repeat(powerupTarget) { system.onPickupCollected(MissionPickupKind.MAGNET) }
        }
        assertEquals(4, system.tier)
        assertEquals(MissionSystem.MULTIPLIER_STEPS[4], system.multiplier, 1e-9)
    }

    @Test
    fun `forceComplete finishes exactly one mission, not the whole set`() {
        val system = MissionSystem(seed = 1L)
        val target = distanceMission(system)

        val ok = system.forceComplete(target.id)

        assertTrue(ok)
        assertTrue(distanceMission(system).isComplete)
        assertTrue("the other 2 missions should be untouched", system.activeMissions.count { !it.isComplete } == 2)
        // Only 1 of 3 complete -- the set should NOT have rolled over yet.
        assertEquals(0, system.tier)
    }

    @Test
    fun `forceComplete on an already-complete or unknown mission id returns false`() {
        val system = MissionSystem(seed = 1L)
        assertEquals(false, system.forceComplete("not-a-real-id"))
    }

    @Test
    fun `forceComplete-ing all 3 missions one at a time still escalates the set`() {
        val system = MissionSystem(seed = 1L)
        val ids = system.activeMissions.map { it.id }
        ids.forEach { system.forceComplete(it) }
        assertEquals(1, system.tier)
        assertEquals(3, system.activeMissions.size)
    }
}
