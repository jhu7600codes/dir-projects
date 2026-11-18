package com.patternbox.app

import android.graphics.Bitmap
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** saves screenshots of the fight screen to build/screens when SCREENS_DIR is set, otherwise just runs */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h808dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun tick(ms: Long = 300) { rule.mainClock.advanceTimeBy(ms); rule.waitForIdle() }
    private fun click(t: String, last: Boolean = false) {
        val n = rule.onAllNodesWithText(t).let { if (last) it.onLast() else it.onFirst() }
        n.performClick(); tick()
    }
    private fun shot(name: String) {
        val dir = System.getenv("SCREENS_DIR") ?: return
        var bmp: Bitmap? = null
        rule.activityRule.scenario.onActivity { a ->
            val v = a.window.decorView
            bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888).also { v.draw(android.graphics.Canvas(it)) }
        }
        File(dir).mkdirs()
        File(dir, "$name.png").outputStream().use { bmp!!.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun fightControls() {
        rule.mainClock.autoAdvance = false
        tick(500)
        click("play"); click("1v1")
        // second slot on pad 2
        click("pad 2", last = true)
        rule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("fight"))
        rule.onAllNodesWithText("fight").onLast().performSemanticsAction(SemanticsActions.OnClick)
        tick(3000)
        shot("fight-2p")
    }
}
