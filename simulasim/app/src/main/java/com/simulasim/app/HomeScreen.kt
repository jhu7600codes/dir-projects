package com.simulasim.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simulasim.app.ui.Accent
import com.simulasim.app.ui.Blue
import com.simulasim.app.ui.Green
import com.simulasim.app.ui.Hspace
import com.simulasim.app.ui.Orange
import com.simulasim.app.ui.Red
import com.simulasim.app.ui.SimCard
import com.simulasim.app.ui.TextDim
import com.simulasim.app.ui.TextMain
import com.simulasim.app.ui.Vspace
import com.simulasim.app.ui.Yellow

private data class Entry(val sim: Sim, val icon: String, val title: String, val blurb: String, val color: Color)

private val entries = listOf(
    Entry(Sim.CRYPTO, "📈", "rugpull sim", "trade memecoins that might rug you, or launch your own and rug everyone else", Green),
    Entry(Sim.TEACHER, "🍎", "teacher sim", "a full school week. 12 kids, 5 periods a day, one coffee machine. survive the principal", Yellow),
    Entry(Sim.SHAKE, "🔦", "shakelight sim", "shake shake = flashlight. strobe, sos, screen light, live g-force meter", Blue),
    Entry(Sim.HACKER, "💻", "hacker sim", "mash the screen, look like you're hacking the pentagon", Red),
    Entry(Sim.BUBBLE, "🫧", "bubble wrap sim", "pop. pop. pop. that's it. that's the sim", Orange),
)

@Composable
fun HomeScreen(onOpen: (Sim) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Vspace(12)
        Text("simulasim", fontSize = 38.sp, fontWeight = FontWeight.Black, color = TextMain)
        Text("a pocket full of simulators", color = TextDim, fontSize = 15.sp)
        Vspace(20)
        entries.forEach { e ->
            SimCard(Modifier.fillMaxWidth(), onClick = { onOpen(e.sim) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(e.color.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) { Text(e.icon, fontSize = 26.sp) }
                    Hspace(14)
                    Column(Modifier.weight(1f)) {
                        Text(e.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextMain)
                        Text(e.blurb, color = TextDim, fontSize = 13.sp)
                    }
                }
            }
            Vspace(10)
        }
        Vspace(8)
        Text("v1.0 · everything here is fake. especially the money.", color = TextDim.copy(alpha = 0.6f), fontSize = 12.sp)
        Text("more sims soon", color = Accent, fontSize = 12.sp)
    }
}
