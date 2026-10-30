package app.venderian.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import app.venderian.MainActivity
import app.venderian.VenderianApp
import app.venderian.WordWidget
import app.venderian.data.Holiday
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** the widget, and the app on a holiday */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class WidgetScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @After
    fun reset() {
        Holiday.override = null
    }

    @Test
    fun widget() {
        val app = RuntimeEnvironment.getApplication() as VenderianApp
        runBlocking { app.repository.addXp(10) }
        val views = runBlocking { WordWidget.views(app) }
        val parent = FrameLayout(app)
        val view = views.apply(app, parent)
        val w = 1000
        val h = 330
        view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, w, h)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File("build/outputs/roborazzi").mkdirs()
        File("build/outputs/roborazzi/w1-widget.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun holiday() {
        Holiday.override = Holiday.HALLOWEEN
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/w2-halloween.png")
        Holiday.override = Holiday.VENDERI_DAY
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/w3-venderi-day.png")
    }
}
