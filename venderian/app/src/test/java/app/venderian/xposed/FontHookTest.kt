package app.venderian.xposed

import android.graphics.Paint
import android.graphics.Typeface
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** runs the hook the way lsposed would, in a fake app, and checks text can draw venderian letters */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class FontHookTest {
    @Test
    fun `apps get the letters, and nothing else changes`() {
        // a stand-in for the module apk: just the font asset
        val apk = File.createTempFile("module", ".apk")
        ZipOutputStream(apk.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("assets/venderian-pua.ttf"))
            zip.write(File("src/main/assets/venderian-pua.ttf").readBytes())
            zip.closeEntry()
        }
        val before = Paint()
        assertFalse(before.hasGlyph(""))
        val latinWidth = before.measureText("hello")

        val hook = FontHook()
        hook.initZygote(IXposedHookZygoteInit.StartupParam().apply { modulePath = apk.path })
        hook.handleLoadPackage(XC_LoadPackage.LoadPackageParam().apply {
            packageName = "org.telegram.messenger"
            isFirstApplication = true
        })

        val after = Paint()
        assertTrue("default text has the letters", after.hasGlyph(""))
        assertTrue("sans-serif has the letters", Paint().apply { typeface = Typeface.create("sans-serif", Typeface.NORMAL) }.hasGlyph(""))
        assertTrue("latin text looks the same", Math.abs(after.measureText("hello") - latinWidth) < 0.01f)
    }
}
