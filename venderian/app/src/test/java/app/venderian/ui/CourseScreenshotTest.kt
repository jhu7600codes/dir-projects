package app.venderian.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import app.venderian.MainActivity
import app.venderian.VenderianApp
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** the course map and a lesson, with the script already learned */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class CourseScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    // the current lesson's node pulses forever, so the clock is driven by hand
    private fun settle() {
        repeat(4) {
            Thread.sleep(150) // room works on its own threads
            compose.mainClock.advanceTimeBy(500)
        }
    }

    private fun shot(name: String) {
        settle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    private fun has(text: String) = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    private fun waitFor(text: String) {
        repeat(40) { if (has(text)) return; settle() }
        error("never showed up: $text")
    }

    @Test
    fun practice() {
        val app = RuntimeEnvironment.getApplication() as VenderianApp
        runBlocking {
            app.alphabet.lessons.forEach { app.repository.completeLesson(it) }
            app.course.lessons.forEach { app.repository.completeCourseLesson(it, mistakes = 0) }
        }
        compose.mainClock.autoAdvance = false
        settle()
        compose.onAllNodesWithText("course").onFirst().performClick()
        waitFor("start a practice lesson")
        shot("c5-practice-card")
        compose.onAllNodesWithText("start a practice lesson").onLast().performClick()
        waitFor("check")
        shot("c6-practice-lesson")
    }

    @Test
    fun dictionary() {
        compose.mainClock.autoAdvance = false
        settle()
        compose.onNodeWithContentDescription("dictionary").performClick()
        waitFor("search english or venderian")
        shot("d1-dictionary")
        compose.onNode(hasSetTextAction()).performTextInput("dog")
        waitFor("vuf")
        shot("d2-search")
        compose.onNodeWithText("vuf").performClick()
        waitFor("practice this word")
        shot("d3-word")
    }

    @Test
    fun course() {
        val app = RuntimeEnvironment.getApplication() as VenderianApp
        runBlocking { app.alphabet.lessons.forEach { app.repository.completeLesson(it) } }
        compose.mainClock.autoAdvance = false
        settle()
        compose.onAllNodesWithText("sounds").onFirst().performClick()
        waitFor("the lazy vowel")
        shot("s1-sounds")
        compose.onAllNodesWithText("course").onFirst().performClick()
        waitFor("first steps")
        shot("c1-map")
        compose.onAllNodesWithText("1").onFirst().performClick()
        waitFor("grammar tip")
        shot("c2-tip")
        compose.onNodeWithText("start").performClick()
        waitFor("new word")
        shot("c3-new-word")
        // go through the lesson: new words, then answer whatever comes, and screenshot each kind once
        val seen = mutableSetOf<String>()
        repeat(40) {
            settle()
            when {
                has("lesson done") -> return@repeat
                has("continue") -> compose.onAllNodesWithText("continue").onFirst().performClick()
                has("got it") -> compose.onNodeWithText("got it").performClick()
                else -> {
                    val kind = listOf("what does this mean", "write this in english", "write this in venderian", "match the pairs", "read it", "which one")
                        .firstOrNull { has(it) } ?: return@repeat
                    if (seen.add(kind)) shot("c4-" + kind.replace(' ', '-'))
                    // the answers are checked in unit tests; here just pictures, so stop at the first quiz
                    return
                }
            }
        }
    }
}
