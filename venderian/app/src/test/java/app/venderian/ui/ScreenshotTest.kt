package app.venderian.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import app.venderian.MainActivity
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RobolectricTestRunner

/**
 * renders each screen to app/build/outputs/roborazzi/ so the ui can be looked at without
 * a phone or emulator. it only checks that the screens render.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun shot(name: String) {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(5_000)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    private fun waitFor(text: String) = compose.waitUntil(30_000) {
        compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun screens() {
        shot("1-learn")
        compose.onNodeWithText("how the script works").performClick()
        shot("2-learn-open")
        compose.onNodeWithText("2. lips").performClick()
        shot("3-lesson")
        compose.onNodeWithText("tap to hear").assertExists()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onAllNodesWithText("Keyboard").onFirst().performClick()
        shot("4-keyboard")
        compose.onNodeWithText("from english").performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithText("english").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasSetTextAction()).performTextInput("hello my friend, I like water")
        compose.onNodeWithText("translate").performClick()
        Thread.sleep(3000)
        shot("5-keyboard-english")
        compose.onAllNodesWithText("Quiz").onFirst().performClick()
        waitFor("what sound is this?")
        shot("6-quiz")
        compose.onAllNodesWithText("Quiz").onFirst().performClick()
        compose.onAllNodesWithText("Learn").onFirst().performClick()
        compose.onNodeWithText("1. vowels").performClick()
        repeat(6) {
            compose.onNodeWithTag("pager").performTouchInput { swipeLeft() }
            compose.waitForIdle()
        }
        compose.onNodeWithText("finish", substring = true).performClick()
        waitFor("2. lips")
        compose.onAllNodesWithText("Practice").onFirst().performClick()
        waitFor("tap the card to flip it")
        shot("7-practice")
        compose.onNodeWithText("show answer").performClick()
        shot("7b-practice-flipped")
        compose.onAllNodesWithText("Progress").onFirst().performClick()
        waitFor("6 / 28") // the vowel lesson's letters were counted
        shot("8-progress")
    }
}
