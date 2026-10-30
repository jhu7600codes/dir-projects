package app.venderian.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import app.venderian.VenderianApp
import app.venderian.course.Exercise
import app.venderian.course.ExerciseMaker
import app.venderian.ui.course.ExerciseView
import app.venderian.ui.theme.VenderianTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.random.Random

/** one picture of each kind of course exercise, from a gold lesson */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ExerciseScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun exercises() {
        val app = RuntimeEnvironment.getApplication() as VenderianApp
        val maker = ExerciseMaker(app.course, app.lexicon.entries, Random(3))
        val all = maker.make(app.course.lesson("m1-l5")) + maker.make(app.course.lesson("m4.5-gold"))
        val kinds = all.groupBy { it::class.simpleName }.mapValues { it.value.first() }
        var current by mutableStateOf<Exercise>(all.first())
        compose.setContent {
            VenderianTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize()) { ExerciseView(current, locked = false, onAnswer = { _, _ -> }, onNext = {}) }
                }
            }
        }
        for ((name, e) in kinds) {
            current = e
            compose.waitForIdle()
            compose.onRoot().captureRoboImage("build/outputs/roborazzi/ex-$name.png")
        }
    }
}
