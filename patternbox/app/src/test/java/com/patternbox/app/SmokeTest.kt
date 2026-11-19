package com.patternbox.app

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click as touchClick
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** clicks through every screen and mode so crashes show up without a phone */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class SmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun tick(ms: Long = 300) {
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    private fun node(text: String, last: Boolean = false, substring: Boolean = false): SemanticsNodeInteraction {
        val merged = rule.onAllNodesWithText(text, substring = substring)
        val nodes = if (merged.fetchSemanticsNodes().isNotEmpty()) merged else rule.onAllNodesWithText(text, substring = substring, useUnmergedTree = true)
        return if (last) nodes.onLast() else nodes.onFirst()
    }

    private fun click(text: String, last: Boolean = false, substring: Boolean = false, wait: Long = 300) {
        node(text, last, substring).performClick()
        tick(wait)
    }

    /** semantics click instead of a fake touch, for buttons right at the screen edge */
    private fun press(text: String, wait: Long) {
        node(text, last = true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        tick(wait)
    }

    private fun scrollTo(text: String) {
        // performScrollToNode spins forever when nothing matches, so fail fast instead
        val found = rule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() ||
            rule.onAllNodes(hasText(text), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        if (!found) throw AssertionError("nothing says \"$text\" on screen: " + rule.onAllNodes(androidx.compose.ui.test.hasClickAction()).fetchSemanticsNodes().take(15).map { it.config.toString().substringAfter("Text : ").take(40) })
        rule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText(text))
        tick()
    }

    private fun back() {
        rule.onNodeWithContentDescription("back").performClick()
        tick()
    }

    private fun systemBack() {
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        tick()
    }

    @Test fun clickThroughEverything() {
        rule.mainClock.autoAdvance = false
        tick(500)
        // roster, presets, editor
        click("presets")
        click("big gleb", last = true)
        click("done")
        click("new", wait = 1000)
        click("random")
        scrollTo("try it")
        click("hammer")
        click("saw")
        click("vampire")
        click("balloon")
        click("summon")
        click("try it", wait = 6000)
        click("boom"); click("spawn"); click("crate"); click("tnt")
        tick(4000)
        systemBack(); systemBack()
        node("presets").assertExists()

        for (mode in listOf("1v1", "2v2", "free for all", "survival", "king of the hill", "boss fight", "sandbox")) {
            click("play")
            scrollTo(mode)
            // semantics click: cards scrolled to the bottom sit under the nav bar
            // bottom cards hide under the nav bar until you scroll all the way, like a real thumb would
            rule.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() }; tick()
            node(mode).performTouchInput { touchClick(androidx.compose.ui.geometry.Offset(centerX, top + 12f)) }; tick()
            scrollTo("fight")
            press("fight", 25000)
            back(); back()
        }

        click("play")
        click("tournament")
        click("start tournament", substring = true, wait = 500)
        click("watch", wait = 8000)
        back()
        click("sim all", wait = 500)
        node("is the champion", substring = true).assertExists()
        back()

        click("settings")
        click("huge")
        click("medium")
    }

    @Test fun hostAWifiGame() {
        rule.mainClock.autoAdvance = false
        tick(500)
        click("play")
        click("play with friends")
        click("host a game", wait = 500)
        // a friend joins from "another phone" (same jvm, real socket)
        val friend = com.patternbox.core.NetClient()
        val t = Thread { friend.connect("127.0.0.1", "friend", com.patternbox.core.Presets.all[3].second.encode()) }
        t.start(); t.join(5000)
        org.junit.Assert.assertTrue(friend.connected)
        repeat(30) { tick(100); Thread.sleep(20) }
        node("friend").assertExists()
        scrollTo("start")
        press("start", 300)
        repeat(60) { tick(50); Thread.sleep(10) }
        org.junit.Assert.assertNotNull("friend got the start", friend.start)
        org.junit.Assert.assertEquals(1, friend.start!!.yourFighter)
        // friend walks right for a bit, snapshots flow back
        friend.sendInput(left = false, right = true, jump = false)
        repeat(60) { tick(50); Thread.sleep(10) }
        org.junit.Assert.assertNotNull("snapshots arrive", friend.latestSnap.get())
        friend.close()
        repeat(10) { tick(100); Thread.sleep(10) }
        back(); back(); back()
    }

    @Test fun joinAWifiGame() {
        rule.mainClock.autoAdvance = false
        val host = com.patternbox.core.NetHost()
        host.start()
        try {
            tick(500)
            click("play")
            click("play with friends")
            click("join a game", wait = 500)
            rule.onAllNodes(androidx.compose.ui.test.hasSetTextAction()).onFirst().performTextInput("127.0.0.1")
            tick()
            press("join", 300)
            var waited = 0
            while (host.readyPeers.isEmpty() && waited++ < 100) { Thread.sleep(30); tick(30) }
            org.junit.Assert.assertEquals(1, host.readyPeers.size)
            val peer = host.readyPeers[0]
            val entries = listOf(com.patternbox.core.Entry("host", com.patternbox.core.Loadout.DEFAULT_P1), com.patternbox.core.Entry(peer.name, peer.loadout!!))
            host.sendStart(com.patternbox.core.Mode.VERSUS, com.patternbox.core.GameMap.LAVA, entries) { 1 }
            val m = com.patternbox.core.Match(com.patternbox.core.Mode.VERSUS, entries, com.patternbox.core.GameMap.LAVA)
            m.start()
            repeat(120) {
                com.patternbox.core.applyPeerInput(m.arena, m.arena.fighters[1], peer)
                m.step()
                if (it % 2 == 0) host.sendSnapshot(com.patternbox.core.Snapshot.write(m.arena, emptyList(), "lava"))
                Thread.sleep(5); tick(16)
            }
            // the joined phone is drawing the fight and has its buttons
            node("hit").assertExists()
            click("hit")
            repeat(20) { Thread.sleep(10); tick(16) }
            org.junit.Assert.assertEquals("hit reached the host", 1, peer.swings)
        } finally {
            host.close()
        }
    }
}
