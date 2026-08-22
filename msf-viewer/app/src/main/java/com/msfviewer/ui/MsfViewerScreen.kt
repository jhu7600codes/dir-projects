package com.msfviewer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
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

/**
 * The entire app: the rendered image (or the fish easter egg) plus a
 * Material 3 card of facts about the opened file. Nothing else -- no
 * navigation, no menus, no settings.
 */
@Composable
fun MsfViewerScreen(filename: String?) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (filename == null) {
            NoFileOpenedContent()
            return@Surface
        }

        val parseResult = remember(filename) { MesfParser.parse(filename) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (parseResult.isEasterEgg) {
                FishEasterEggContent()
                FileFactsCard(filename = filename, parseResult = parseResult, layout = null)
            } else {
                val layout = remember(parseResult) { MesfLayoutEngine.layout(parseResult.units) }
                MesfImageCanvas(layout = layout, modifier = Modifier.fillMaxWidth())
                FileFactsCard(filename = filename, parseResult = parseResult, layout = layout)
            }
        }
    }
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
                text = "No .msf file is open. Open one from a file manager's " +
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
private fun FileFactsCard(filename: String, parseResult: MesfParseResult, layout: MesfLayout?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "File info", style = MaterialTheme.typography.titleMedium)
            FactRow("Filename", filename)
            FactRow("Parsed name", parseResult.baseName)
            FactRow("Easter egg triggered", if (parseResult.isEasterEgg) "Yes (fish)" else "No")
            if (!parseResult.isEasterEgg && layout != null) {
                FactRow("Unit count", parseResult.units.size.toString())
                FactRow("Rows", layout.rowCount.toString())
                FactRow(
                    "Layout size",
                    "${layout.widthUnits} x ${layout.heightUnits} base units",
                )
            }
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
