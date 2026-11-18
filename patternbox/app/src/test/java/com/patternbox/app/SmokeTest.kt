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
        click("bomb")
        click("teleport")
        click("try it", wait = 6000)
        click("boom"); click("spawn"); click("crate"); click("tnt")
        tick(4000)
        systemBack(); systemBack()
        node("presets").assertExists()

        for (mode in listOf("1v1", "2v2", "free for all", "survival", "sandbox")) {
            click("play")
            click(mode)
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
}
