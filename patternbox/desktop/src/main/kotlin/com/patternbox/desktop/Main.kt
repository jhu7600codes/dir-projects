package com.patternbox.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.DragData
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.onExternalDrag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.patternbox.app.GameState
import com.patternbox.app.KeyBus
import com.patternbox.app.NoLanFinder
import com.patternbox.app.PatternboxApp
import com.patternbox.app.Platform
import com.patternbox.app.importMessage
import kotlinx.coroutines.delay
import java.awt.FileDialog
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.net.URI

/** where the save lives: ~/.local/share/patternbox (or $XDG_DATA_HOME/patternbox) */
fun dataDir(): File {
    val base = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() } ?: (System.getProperty("user.home") + "/.local/share")
    return File(base, "patternbox").apply { mkdirs() }
}

/** the back button: escape walks back through screens */
private object Back { var action: (() -> Unit)? = null }

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    val state = remember { GameState(PropertiesStore(File(dataDir(), "save.properties"))) }
    val sfx = remember { DesktopSfx() }
    var toast by remember { mutableStateOf<String?>(null) }
    val windowState = rememberWindowState(size = DpSize(1000.dp, 860.dp))

    fun import(text: String, from: String) { toast = importMessage(state.importText(text), from) }

    val platform = remember {
        Platform(
            name = "linux",
            touchControls = false,
            hasDynamicColor = false,
            dynamicScheme = { null },
            feedback = sfx,
            lan = NoLanFinder(),
            backHandler = { enabled, onBack -> SideEffect { Back.action = if (enabled) onBack else null } },
            openTextFile = {
                val d = FileDialog(null as java.awt.Frame?, "import fighters", FileDialog.LOAD)
                d.isVisible = true
                d.file?.let { runCatching { File(d.directory, it).readText() }.getOrNull() }
            },
            saveTextFile = { name, text ->
                val d = FileDialog(null as java.awt.Frame?, "export fighters", FileDialog.SAVE)
                d.file = name
                d.isVisible = true
                d.file?.let { runCatching { File(d.directory, it).writeText(text); true }.getOrDefault(false) } ?: false
            },
        )
    }

    Window(
        onCloseRequest = { sfx.close(); exitApplication() },
        title = "patternbox",
        state = windowState,
        icon = androidx.compose.ui.res.painterResource("icon.png"),
        onPreviewKeyEvent = { e ->
            when {
                KeyBus.dispatch(e) -> true
                e.type == KeyEventType.KeyDown && e.key == Key.Escape -> { Back.action?.invoke(); Back.action != null }
                // ctrl+i: import whatever fighter codes are on the clipboard
                e.type == KeyEventType.KeyDown && e.isCtrlPressed && e.key == Key.I -> {
                    val t = runCatching { Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String }.getOrDefault("")
                    import(t, "clipboard"); true
                }
                else -> false
            }
        },
    ) {
        Box(
            Modifier.fillMaxSize().onExternalDrag(onDrop = { drop ->
                // drag a .txt file (or a bunch) or some selected text onto the window
                when (val data = drop.dragData) {
                    is DragData.FilesList -> {
                        val text = data.readFiles().joinToString("\n") { uri -> runCatching { File(URI(uri)).readText() }.getOrDefault("") }
                        import(text, "dropped file")
                    }
                    is DragData.Text -> import(data.readText(), "dropped text")
                    else -> toast = "can't read that, drop a text file or some text with codes"
                }
            })
        ) {
            PatternboxApp(state, platform)
            toast?.let { msg ->
                LaunchedEffect(msg) { delay(3500); toast = null }
                Surface(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.inverseSurface,
                ) {
                    Text(msg, Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.inverseOnSurface)
                }
            }
        }
    }
}
