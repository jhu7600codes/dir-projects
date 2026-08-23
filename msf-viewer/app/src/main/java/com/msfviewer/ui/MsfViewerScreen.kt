package com.msfviewer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msfviewer.parser.MesfLayout
import com.msfviewer.parser.MesfLayoutEngine
import com.msfviewer.parser.MesfParseResult
import com.msfviewer.parser.MesfParser
import com.msfviewer.parser.VsfParseResult
import com.msfviewer.parser.VsfParser
import kotlinx.coroutines.delay

private const val VSF_EXTENSION = ".vsf"
private const val VSF_FRAME_DELAY_MS = 350L

/**
 * The entire app: the rendered image (or the fish easter egg) plus a
 * Material 3 card of facts about the opened file. Nothing else -- no
 * navigation, no menus, no settings. Dispatches on the filename's
 * extension: ".vsf" (VESF, a sequence of MESF frames played back as a
 * looping animation) or everything else, treated as MESF/".msf".
 */
@Composable
fun MsfViewerScreen(filename: String?) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (filename == null) {
            NoFileOpenedContent()
            return@Surface
        }

        val isVsf = filename.trim().endsWith(VSF_EXTENSION, ignoreCase = true)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (isVsf) {
                VsfContent(filename)
            } else {
                MsfContent(filename)
            }
        }
    }
}

@Composable
private fun MsfContent(filename: String) {
    val parseResult = remember(filename) { MesfParser.parse(filename) }

    if (parseResult.isEasterEgg) {
        FishEasterEggContent()
        MsfFactsCard(filename = filename, parseResult = parseResult, layout = null)
    } else {
        val layout = remember(parseResult) { MesfLayoutEngine.layout(parseResult.units) }
        MesfImageCanvas(layout = layout, modifier = Modifier.fillMaxWidth())
        MsfFactsCard(filename = filename, parseResult = parseResult, layout = layout)
    }
}

@Composable
private fun VsfContent(filename: String) {
    val parseResult = remember(filename) { VsfParser.parse(filename) }

    if (parseResult.isEasterEgg) {
        FishEasterEggContent()
        VsfFactsCard(filename = filename, parseResult = parseResult, frameIndex = 0)
        return
    }

    val layouts = remember(parseResult) {
        parseResult.frames.map { frameUnits -> MesfLayoutEngine.layout(frameUnits) }
    }

    var frameIndex by remember(parseResult) { mutableIntStateOf(0) }

    // Auto-advancing playback loop: a single frame just renders once and
    // stops (nothing to advance to); more than one loops indefinitely at
    // a fixed rate. No play/pause/scrub controls, matching the rest of
    // this app's "just the facts" minimalism.
    if (layouts.size > 1) {
        LaunchedEffect(parseResult) {
            while (true) {
                delay(VSF_FRAME_DELAY_MS)
                frameIndex = (frameIndex + 1) % layouts.size
            }
        }
    }

    val currentLayout = layouts.getOrNull(frameIndex)
    if (currentLayout != null) {
        MesfImageCanvas(layout = currentLayout, modifier = Modifier.fillMaxWidth())
    }
    VsfFactsCard(filename = filename, parseResult = parseResult, frameIndex = frameIndex)
}

@Composable
private fun NoFileOpenedContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(colors = CardDefaults.cardColors()) {
            Text(
                text = "No .msf or .vsf file is open. Open one from a file manager's " +
                    "\"open with\" menu to view it here.",
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun FishEasterEggContent() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0A0E14),
            contentColor = Color(0xFFE8EAF0),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "YOU KNOW WHAT THAT MEANS?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "FISH",
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun MsfFactsCard(filename: String, parseResult: MesfParseResult, layout: MesfLayout?) {
    FactsCard {
        FactRow("Filename", filename)
        FactRow("Parsed name", parseResult.baseName)
        FactRow("Easter egg triggered", if (parseResult.isEasterEgg) "Yes (fish)" else "No")
        if (!parseResult.isEasterEgg && layout != null) {
            FactRow("Unit count", parseResult.units.size.toString())
            FactRow("Rows", layout.rowCount.toString())
            FactRow("Layout size", "${layout.widthUnits} x ${layout.heightUnits} base units")
        }
    }
}

@Composable
private fun VsfFactsCard(filename: String, parseResult: VsfParseResult, frameIndex: Int) {
    FactsCard {
        FactRow("Filename", filename)
        FactRow("Parsed name", parseResult.baseName)
        FactRow("Easter egg triggered", if (parseResult.isEasterEgg) "Yes (fish)" else "No")
        if (!parseResult.isEasterEgg) {
            FactRow("Frame count", parseResult.frames.size.toString())
            val current = parseResult.frames.getOrNull(frameIndex)
            if (current != null) {
                FactRow("Current frame", "${frameIndex + 1} / ${parseResult.frames.size}")
                FactRow("Current frame unit count", current.size.toString())
            }
        }
    }
}

@Composable
private fun FactsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "File info", style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
