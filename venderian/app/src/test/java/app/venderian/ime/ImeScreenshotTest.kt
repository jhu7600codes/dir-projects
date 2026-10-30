package app.venderian.ime

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import app.venderian.VenderianApp
import app.venderian.ui.theme.VenderianTheme
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** renders the system keyboard's layouts to app/build/outputs/roborazzi/ */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ImeScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun ime(): VenderianIme {
        val ime = Robolectric.setupService(VenderianIme::class.java)
        // wait for cmudict, so the preview strip has something to show
        runBlocking { (RuntimeEnvironment.getApplication() as VenderianApp).converter.await() }
        compose.waitUntil(20_000) { ime.converter != null }
        return ime
    }

    @Test
    fun layouts() {
        val ime = ime()
        ime.state = ime.state.copy(before = "hello my friend, I like water", translate = true)
        compose.setContent { VenderianTheme { KeyboardUi(ime) } }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/ime-1-qwerty.png")
        ime.state = ime.state.copy(layout = Layout.Venderian, before = " ")
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/ime-2-venderian.png")
        ime.state = ime.state.copy(layout = Layout.Symbols)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/ime-3-symbols.png")
    }

    @Test
    fun `message image`() {
        val ime = ime()
        val (script, length) = ime.imageMessage("\uE007\uE021 hello world")!!
        assertEquals(11, length)
        val bitmap = ScriptImage.render(RuntimeEnvironment.getApplication(), script, app.venderian.convert.Sounds.scriptToRoman(script))
        java.io.File("build/outputs/roborazzi").mkdirs()
        java.io.File("build/outputs/roborazzi/ime-4-image.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        // only venderian before the cursor: that becomes the picture
        assertEquals("\uE007\uE021 \uE00F\uE022." to 6, ime.imageMessage("hey \uE007\uE021 \uE00F\uE022."))
        assertEquals(null, ime.imageMessage(""))
    }

    @Test
    fun `translate only takes the english after the last venderian`() {
        val ime = ime()
        assertEquals("hello world", ime.englishTail(" hello world"))
        assertEquals("hello world", ime.englishTail("hello world"))
        assertEquals("", ime.englishTail(""))
    }
}
