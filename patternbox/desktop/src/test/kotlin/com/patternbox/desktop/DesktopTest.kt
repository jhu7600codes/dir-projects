package com.patternbox.desktop

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.Density
import com.patternbox.app.GameState
import com.patternbox.app.NoLanFinder
import com.patternbox.app.PatternboxApp
import com.patternbox.app.Platform
import com.patternbox.core.Codes
import com.patternbox.core.Presets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

@OptIn(ExperimentalTestApi::class)
class DesktopTest {
    private fun platform(files: MutableList<String>) = Platform(
        name = "linux-test", touchControls = false, hasDynamicColor = false, dynamicScheme = { null },
        feedback = object : com.patternbox.app.Feedback {}, lan = NoLanFinder(),
        backHandler = { _, _ -> },
        openTextFile = { files.removeFirstOrNull() },
        saveTextFile = { _, text -> files += text; true },
    )

    @Test fun saveFileRoundTrips() {
        val dir = createTempDir()
        val f = File(dir, "save.properties")
        val a = GameState(PropertiesStore(f))
        val n = a.roster.size
        a.importText("big guy: ${Presets.all[1].second.encode()}")
        a.changeGameSpeed(1.5f)
        val b = GameState(PropertiesStore(f))
        assertEquals(n + 1, b.roster.size)
        assertEquals("big guy", b.roster[0].name)
        assertEquals(1.5f, b.gameSpeed)
    }

    @Test fun clickAroundAndImport() = runDesktopComposeUiTest(width = 1000, height = 860) {
        val dir = createTempDir()
        val state = GameState(PropertiesStore(File(dir, "save.properties")))
        val files = mutableListOf(Codes.export(Presets.all.take(3).map { it.first to it.second }))
        mainClock.autoAdvance = false
        setContent { PatternboxApp(state, platform(files)) }
        fun tick(ms: Long = 200) { mainClock.advanceTimeBy(ms); waitForIdle() }
        val before = state.roster.size
        val out = System.getenv("SCREENS_DIR")
        fun shot(name: String) {
            if (out == null) return
            File(out).mkdirs()
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(out, "$name.png"))
        }
        tick()
        shot("desktop-roster")
        onAllNodesWithText("import file").onFirst().performClick()
        tick()
        assertEquals(before + 3, state.roster.size)
        onAllNodesWithText("export file").onFirst().performClick()
        tick()
        assertTrue(files.last().contains(Presets.all[0].second.encode()))

        // a 1v1 with keyboard hints
        onAllNodesWithText("play").onFirst().performClick(); tick()
        onAllNodesWithText("1v1").onFirst().performSemanticsAction(SemanticsActions.OnClick); tick()
        onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("fight"))
        onAllNodesWithText("fight").onLast().performSemanticsAction(SemanticsActions.OnClick)
        tick(4000)
        onAllNodesWithText("move:", substring = true).onFirst().assertExists()
        shot("desktop-fight")
    }
}

@Suppress("unused") private val keepDensity = Density(1f)
